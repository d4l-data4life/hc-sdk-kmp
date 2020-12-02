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


import java.util.HashMap;
import java.util.Locale;

import javax.annotation.Nullable;
import javax.annotation.Nonnull;

import static care.data4life.sdk.model.ModelVersion.FHIR_VERSION;

class TaggingService {

    private static final Locale US_LOCALE = Locale.US;
    static final String TAG_DELIMITER = "=";
    static final String TAG_RESOURCE_TYPE = "resourceType".toLowerCase(US_LOCALE);
    static final String TAG_CLIENT = "client";
    private static final String TAG_UPDATED_BY_CLIENT = "updatedByClient".toLowerCase(US_LOCALE);
    private static final String TAG_PARTNER = "partner";
    private static final String TAG_UPDATED_BY_PARTNER = "updatedByPartner".toLowerCase(US_LOCALE);
    private static final String TAG_FHIR_VERSION = "fhirVersion".toLowerCase(US_LOCALE);
    private static final String TAG_APPDATA_KEY = "flag";
    private static final String TAG_APPDATA_VALUE = "appdata";
    private static final String SEPARATOR = "#";

    private final String clientId;
    private final String partnerId;

    TaggingService(String clientId) {
        this.clientId = clientId;
        this.partnerId = clientId.split(SEPARATOR)[0];
    }

    private HashMap<String, String> appendCommonDefaultTags(
            String resourceType,
            @Nullable HashMap<String, String> oldTags
    ) {
        HashMap<String, String> tags = new HashMap<>();
        if (oldTags != null && !oldTags.isEmpty()) {
            tags.putAll(oldTags);
        }

        if (resourceType != null && !resourceType.isEmpty()) {
            tags.put(TAG_RESOURCE_TYPE, resourceType.toLowerCase(US_LOCALE));
        }

        if (!tags.containsKey(TAG_CLIENT)) {
            tags.put(TAG_CLIENT, clientId);
        } else {
            tags.put(TAG_UPDATED_BY_CLIENT, clientId);
        }

        if (!tags.containsKey(TAG_PARTNER)) {
            tags.put(TAG_PARTNER, partnerId);
        } else {
            tags.put(TAG_UPDATED_BY_PARTNER, partnerId);
        }

        return tags;
    }

    HashMap<String, String> appendDefaultTags(String resourceType, @Nullable HashMap<String, String> oldTags) {
        HashMap<String, String> tags = this.appendCommonDefaultTags(resourceType, oldTags);

        if (!tags.containsKey(TAG_FHIR_VERSION)) {
            tags.put(TAG_FHIR_VERSION, FHIR_VERSION);
        }

        return tags;
    }

    HashMap<String,String> appendAppDataTags(HashMap<String, String> tags) {
        if(tags != null ) {
            tags.put(TAG_APPDATA_KEY, TAG_APPDATA_VALUE);
        }
        return tags;
    }

    HashMap<String, String> appendDefaultAnnotatedTags(
            String resourceType,
            @Nullable HashMap<String, String> oldTags
    ) {
        return this.appendAppDataTags(
                this.appendCommonDefaultTags(resourceType, oldTags)
        );
    }

    HashMap<String, String> getTagFromType(String resourceType) {
        HashMap<String, String> tags = new HashMap<>();
        if (resourceType != null && !resourceType.isEmpty()) {
            tags.put(TAG_RESOURCE_TYPE, resourceType.toLowerCase(US_LOCALE));
        }
        return tags;
    }
}
