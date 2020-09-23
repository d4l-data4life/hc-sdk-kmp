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

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static care.data4life.sdk.model.ModelVersion.FHIR_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TaggingServiceTest {

    private static final String CLIENT_ID = "client_id#platform";
    private static final String OTHER_CLIENT_ID = "other_client_id";
    private static final String TAG_PARTNER = "partner";
    private static final String PARTNER_ID = "client_id";

    private static final String TAG_RESOURCE_TYPE = "resourcetype";
    private static final String TAG_CLIENT = "client";
    private static final String TAG_UPDATED_BY_CLIENT = "updatedbyclient";
    private static final String TAG_FHIR_VERSION = "fhirversion";


    // SUT
    private TaggingService taggingService;


    @Before
    public void setUp() {
        taggingService = new TaggingService(CLIENT_ID);
    }


    @Test
    public void tag_shouldReturnMapWithResourceAndClientTag() {
        // Given
        String type = "type";

        // When
        HashMap<String, String> result = taggingService.appendDefaultTags(type, null);

        // Then
        assertEquals(4, result.size());
        assertTrue(result.containsKey(TAG_RESOURCE_TYPE));
        assertEquals(type, result.get(TAG_RESOURCE_TYPE));
        assertTrue(result.containsKey(TAG_CLIENT));
        assertEquals(CLIENT_ID, result.get(TAG_CLIENT));
        assertTrue(result.containsKey(TAG_PARTNER));
        assertEquals(PARTNER_ID, result.get(TAG_PARTNER));
        assertTrue(result.containsKey(TAG_FHIR_VERSION));
        assertEquals(FHIR_VERSION, result.get(TAG_FHIR_VERSION));
    }

    @Test
    public void tag_shouldReturnMapWithClientTag_whenTypeInvalid() {
        // Given
        String type = "";

        // When
        HashMap<String, String> result = taggingService.appendDefaultTags(type, null);

        // Then
        assertEquals(3, result.size());
        assertTrue(result.containsKey(TAG_CLIENT));
        assertEquals(CLIENT_ID, result.get(TAG_CLIENT));
        assertTrue(result.containsKey(TAG_PARTNER));
        assertEquals(PARTNER_ID, result.get(TAG_PARTNER));
        assertTrue(result.containsKey(TAG_FHIR_VERSION));
        assertEquals(FHIR_VERSION, result.get(TAG_FHIR_VERSION));
    }

    @Test
    public void tag_shouldPreserveExistingTagsAndUpdate() {
        // Given
        String type = "type";

        HashMap<String, String> existingTags = new HashMap<>();
        existingTags.put("tag_1_key", "tag_1_value");
        existingTags.put("tag_2_key", "tag_2_value");
        existingTags.put(TAG_RESOURCE_TYPE, "old_typ");

        // When
        HashMap<String, String> result = taggingService.appendDefaultTags(type, existingTags);

        // Then
        assertEquals(6, result.size());
        assertTrue(result.containsKey("tag_1_key"));
        assertTrue(result.containsKey("tag_2_key"));
        assertTrue(result.containsKey(TAG_RESOURCE_TYPE));
        assertEquals(type, result.get(TAG_RESOURCE_TYPE));
        assertTrue(result.containsKey(TAG_CLIENT));
        assertEquals(CLIENT_ID, result.get(TAG_CLIENT));
        assertTrue(result.containsKey(TAG_PARTNER));
        assertEquals(PARTNER_ID, result.get(TAG_PARTNER));
        assertTrue(result.containsKey(TAG_FHIR_VERSION));
        assertEquals(FHIR_VERSION, result.get(TAG_FHIR_VERSION));
    }

    @Test
    public void tag_shouldSetUpdatedByClientTag_whenClientAlreadySet() {
        // Given
        String type = "type";

        HashMap<String, String> existingTags = new HashMap<>();
        existingTags.put(TAG_CLIENT, OTHER_CLIENT_ID);

        // When
        HashMap<String, String> result = taggingService.appendDefaultTags(type, existingTags);

        // Then
        assertEquals(5, result.size());
        assertTrue(result.containsKey(TAG_CLIENT));
        assertEquals(OTHER_CLIENT_ID, result.get(TAG_CLIENT));
        assertTrue(result.containsKey(TAG_UPDATED_BY_CLIENT));
        assertEquals(CLIENT_ID, result.get(TAG_UPDATED_BY_CLIENT));
        assertTrue(result.containsKey(TAG_PARTNER));
        assertEquals(PARTNER_ID, result.get(TAG_PARTNER));
        assertTrue(result.containsKey(TAG_FHIR_VERSION));
        assertEquals(FHIR_VERSION, result.get(TAG_FHIR_VERSION));
    }

    @Test
    public void getTagFromType_shouldReturnListWithResourceTypeTag() {
        // Given
        String type = "type";

        // When
        HashMap<String, String> result = taggingService.getTagFromType(type);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.containsKey(TAG_RESOURCE_TYPE));
        assertEquals(type, result.get(TAG_RESOURCE_TYPE));
    }


    @Test
    public void getTagFromType_shouldReturnEmptyList_whenResourceTypeNull() {
        // When
        HashMap<String, String> result = taggingService.getTagFromType(null);

        // Then
        assertTrue(result.isEmpty());
    }

}
