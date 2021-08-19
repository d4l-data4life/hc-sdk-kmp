/*
 * Copyright (c) 2020 D4L data4life gGmbH / All rights reserved.
 *
 * D4L owns all legal rights, title and interest in and to the Software Development Kit ("SDK"),
 * including any intellectual property rights that subsist in the SDK.
 *
 * The SDK and its documentation may be accessed and used for viewing/review purposes only.
 * Any usage of the SDK for other purposes, including usage for the development of
 * applications/third-party applications shall require the conclusion of a license agreement
 * between you and D4L.
 *
 * If you are interested in licensing the SDK for your own applications/third-party
 * applications and/or if you’d like to contribute to the development of the SDK, please
 * contact D4L by email to help@data4life.care.
 */

package care.data4life.sdk;

import java.util.Set;

import care.data4life.sdk.auth.Authorization;
import care.data4life.fhir.util.Preconditions;

public final class InitializationConfig {
    private static final String DEFAULT_ALIAS = "data4life_android";

    private static final Set<String> DEFAULT_SCOPES = Authorization.getDefaultScopes();

    public static InitializationConfig DEFAULT_CONFIG = new Builder().build();

    private String alias;
    private Set<String> scopes;

    private InitializationConfig(String alias, Set<String> scopes) {
        this.alias = alias;
        this.scopes = scopes;
    }

    public String getAlias() {
        return alias;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public static class Builder {
        private String alias = DEFAULT_ALIAS;
        private Set<String> scopes = DEFAULT_SCOPES;

        public Builder setAlias(String alias) {
            this.alias = alias;
            return this;
        }

        public Builder setScopes(Set<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        public InitializationConfig build() {
            Preconditions.checkArgument(!(alias == null || alias.isEmpty()), "alias is required");
            Preconditions.checkArgument(scopes != null && !scopes.isEmpty(), "scopes are required");

            return new InitializationConfig(alias, scopes);
        }
    }
}
