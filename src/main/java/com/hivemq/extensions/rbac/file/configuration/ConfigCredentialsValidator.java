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
package com.hivemq.extensions.rbac.file.configuration;

import com.hivemq.extensions.rbac.file.configuration.entities.ExtensionConfig;
import com.hivemq.extensions.rbac.file.configuration.entities.FileAuthConfig;
import com.hivemq.extensions.rbac.file.configuration.entities.PasswordType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

class ConfigCredentialsValidator {

    static @NotNull ValidationResult validateConfig(
            final @NotNull ExtensionConfig extensionConfig,
            final @NotNull FileAuthConfig config) {
        final var errors = new ArrayList<String>();
        var validationSuccessful = true;
        final var users = config.getUsers();
        final var roles = config.getRoles();
        if (users == null || users.isEmpty()) {
            errors.add("No Users found in configuration file");
            validationSuccessful = false;
        }
        if (roles == null || roles.isEmpty()) {
            errors.add("No Roles found in configuration file");
            validationSuccessful = false;
        }
        // if users or roles are missing stop here
        if (!validationSuccessful) {
            return new ValidationResult(errors, false);
        }
        final var roleIds = new HashSet<String>();
        for (final var role : roles) {
            if (role.getId() == null || role.getId().isEmpty()) {
                errors.add("A Role is missing an ID");
                validationSuccessful = false;
                continue;
            }
            if (roleIds.contains(role.getId())) {
                errors.add("Duplicate ID '" + role.getId() + "' for role");
                validationSuccessful = false;
                continue;
            }
            roleIds.add(role.getId());
            if (role.getPermissions() == null || role.getPermissions().isEmpty()) {
                errors.add("Role '" + role.getId() + "' is missing permissions");
                validationSuccessful = false;
                continue;
            }
            for (final var permission : role.getPermissions()) {
                if (permission.getTopic() == null || permission.getTopic().isEmpty()) {
                    errors.add("A Permission for role with id '" + role.getId() + "' is missing a topic filter");
                    validationSuccessful = false;
                }
                if (permission.getActivity() == null) {
                    errors.add("Invalid value for Activity in Permission for role with id '" + role.getId() + "'");
                    validationSuccessful = false;
                }
                if (permission.getQos() == null) {
                    errors.add("Invalid value for QoS in Permission for role with id '" + role.getId() + "'");
                    validationSuccessful = false;
                }
                if (permission.getRetain() == null) {
                    errors.add("Invalid value for Retain in Permission for role with id '" + role.getId() + "'");
                    validationSuccessful = false;
                }
                if (permission.getSharedGroup() == null || permission.getSharedGroup().isEmpty()) {
                    errors.add("Invalid value for Shared Group in Permission for role with id '" + role.getId() + "'");
                    validationSuccessful = false;
                }
                if (permission.getSharedSubscription() == null) {
                    errors.add("Invalid value for Shared Subscription in Permission for role with id '" +
                            role.getId() +
                            "'");
                    validationSuccessful = false;
                }
            }
        }
        final var userNames = new HashSet<String>();
        for (final var user : users) {
            if (user.getName() == null || user.getName().isEmpty()) {
                errors.add("A User is missing a name");
                validationSuccessful = false;
                continue;
            }
            if (userNames.contains(user.getName())) {
                errors.add("Duplicate Name '" + user.getName() + "' for user");
                validationSuccessful = false;
                continue;
            }
            userNames.add(user.getName());
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                errors.add("User '" + user.getName() + "' is missing a password");
                validationSuccessful = false;
                continue;
            }
            if (extensionConfig.getPasswordType() == PasswordType.HASHED) {
                final var password = user.getPassword();
                final var split = password.split(":");
                if (split.length < 2 || split[0].isEmpty() || split[1].isEmpty()) {
                    errors.add("User '" + user.getName() + "' has invalid password");
                    validationSuccessful = false;
                    continue;
                }
            }
            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                errors.add("User '" + user.getName() + "' is missing roles");
                validationSuccessful = false;
                continue;
            }
            for (final var role : user.getRoles()) {
                if (role == null || role.isEmpty()) {
                    errors.add("Invalid role for user '" + user.getName() + "'");
                    validationSuccessful = false;
                    continue;
                }
                if (!roleIds.contains(role)) {
                    errors.add("Unknown role '" + role + "' for user '" + user.getName() + "'");
                    validationSuccessful = false;
                }
            }
        }
        return new ValidationResult(errors, validationSuccessful);
    }

    static class ValidationResult {

        private final @NotNull List<String> errors;
        private final boolean validationSuccessful;

        private ValidationResult(final @NotNull List<String> errors, final boolean validationSuccessful) {
            this.errors = errors;
            this.validationSuccessful = validationSuccessful;
        }

        public @NotNull List<String> getErrors() {
            return errors;
        }

        public boolean isValidationSuccessful() {
            return validationSuccessful;
        }
    }
}
