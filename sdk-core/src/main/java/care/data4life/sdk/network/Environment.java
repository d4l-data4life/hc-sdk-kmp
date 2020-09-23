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

package care.data4life.sdk.network;

import java.util.Locale;

public enum Environment {
    SANDBOX("sandbox"),
    DEVELOPMENT("development"),
    STAGING("staging"),
    LOCAL("local"),
    PRODUCTION("production");

    private static final String D4L = "d4l";
    private static final String S4H = "s4h";

    private String name;

    private Environment(String name) {
        this.name = name;
    }

    public static Environment fromName(String name) {
        if (name != null && !name.isEmpty()) {
            if (SANDBOX.getName().equals(name.toLowerCase(Locale.US))) {
                return SANDBOX;
            } else if (DEVELOPMENT.getName().equals(name.toLowerCase(Locale.US))) {
                return DEVELOPMENT;
            } else if (STAGING.getName().equals(name.toLowerCase(Locale.US))) {
                return STAGING;
            } else {
                return LOCAL.getName().equals(name.toLowerCase(Locale.US)) ? LOCAL : PRODUCTION;
            }
        } else {
            return PRODUCTION;
        }
    }

    public String getApiBaseURL(String platform) {
        if (D4L.equalsIgnoreCase(platform)) {
            return d4lBaseUrl();
        } else if (S4H.equalsIgnoreCase(platform)) {
            return s4hBaseUrl();
        }
        throw new IllegalArgumentException("No supported platform found for value(" + platform + ")");
    }

    private String d4lBaseUrl() {
        switch (this) {
            case SANDBOX:
                return "https://api-phdp-sandbox.hpsgc.de";
            case DEVELOPMENT:
                return "https://api-phdp-dev.hpsgc.de";
            case STAGING:
                return "https://api-staging.data4life.care";
            case LOCAL:
                return "https://api.data4life.local";
            case PRODUCTION:
            default:
                return "https://api.data4life.care";
        }
    }

    private String s4hBaseUrl() {
        switch (this) {
            case SANDBOX:
                return "https://api-sandbox.smart4health.eu";
            case DEVELOPMENT:
                return "https://api-dev.smart4health.eu";
            case STAGING:
                return "https://api-staging.smart4health.eu";
            case LOCAL:
                return "https://api.smart4health.local";
            case PRODUCTION:
            default:
                return "https://api.smart4health.eu";
        }
    }

    public String getCertificatePin(String platform) {
        if (D4L.equalsIgnoreCase(platform)) {
            return d4lCertificatePin();
        } else if (S4H.equalsIgnoreCase(platform)) {
            return sh4CertificatePin();
        }
        throw new IllegalArgumentException("No supported platform found for value(" + platform + ")");
    }

    private String d4lCertificatePin() {
        switch (this) {
            case SANDBOX:
            case DEVELOPMENT:
            case LOCAL:
                return "sha256/3f81qEv2rjHvcrwof2egbKo5MjjSHaN/4DOl7R+pH0E=";
            case STAGING:
            case PRODUCTION:
            default:
                return "sha256/AJvjswWs1n4m1KDmFNnTqBit2RHFvXsrVU3Uhxcoe4Y=";
        }
    }

    private String sh4CertificatePin() {
        switch (this) {
            case SANDBOX:
            case DEVELOPMENT:
            case LOCAL:
            case STAGING:
            case PRODUCTION:
            default:
                return "sha256/yPBKbgJMVnMeovGKbAtuz65sfy/gpDu0WTiuB8bE5G0=";
        }
    }

    public String getName() {
        return this.name;
    }
}
