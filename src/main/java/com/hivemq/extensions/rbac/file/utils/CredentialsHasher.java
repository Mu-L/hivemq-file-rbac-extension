/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.rbac.file.utils;

import com.codahale.metrics.MetricRegistry;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@ThreadSafe
public class CredentialsHasher {

    static final @NotNull String HASH_CACHE_HITRATE = "com.hivemq.extensions.file-rbac.hash.cache.hitrate";
    private static final @NotNull String HASH_TIME = "com.hivemq.extensions.file-rbac.hash.sampled-time";

    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull Cache<String, byte[]> credentialHashCache;

    public CredentialsHasher(final @NotNull MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        this.credentialHashCache =
                Caffeine.newBuilder().recordStats().expireAfterWrite(30, TimeUnit.SECONDS).maximumSize(1000).build();
    }

    public boolean checkCredentials(
            final @NotNull String base64Password,
            final @NotNull String saltPasswordFromConfig) {
        final var saltPw = saltPasswordFromConfig.split(":");
        if (saltPw.length != 3) {
            return false;
        }
        final var saltFromConfigBase64 = saltPw[0];
        final var iterations = Integer.parseInt(saltPw[1]);
        final var passwordHashFromConfigBase64 = saltPw[2];
        final var cacheString = base64Password + saltFromConfigBase64 + iterations;
        var credentialsHash = credentialHashCache.getIfPresent(cacheString);
        if (credentialsHash != null) {
            // found in cache
            metricRegistry.meter(HASH_CACHE_HITRATE).mark();
        } else {
            // not found in cache
            final var timer = metricRegistry.timer(HASH_TIME);
            try (final var ignored = timer.time()) {
                credentialsHash = Hashing.createHash(base64Password, saltFromConfigBase64, iterations);
            }
            credentialHashCache.put(cacheString, credentialsHash);
        }
        // we use a time constant equality check for passwords to avoid timing attacks
        return MessageDigest.isEqual(credentialsHash, Base64.getDecoder().decode(passwordHashFromConfigBase64));
    }
}
