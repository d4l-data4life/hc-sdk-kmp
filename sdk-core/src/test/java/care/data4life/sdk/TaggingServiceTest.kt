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
package care.data4life.sdk

import care.data4life.sdk.model.ModelVersion
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*

class TaggingServiceTest {
    // SUT
    private lateinit var taggingService: TaggingService
    @Before
    fun setUp() {
        taggingService = TaggingService(CLIENT_ID)
    }

    @Test
    fun tag_shouldReturnMapWithResourceAndClientTag() {
        // Given
        val type = "type"

        // When
        val result = taggingService.appendDefaultTags(type, null)

        // Then
        Assert.assertEquals(4, result.size.toLong())
        Assert.assertTrue(result.containsKey(TAG_RESOURCE_TYPE))
        Assert.assertEquals(type, result[TAG_RESOURCE_TYPE])
        Assert.assertTrue(result.containsKey(TAG_CLIENT))
        Assert.assertEquals(CLIENT_ID, result[TAG_CLIENT])
        Assert.assertTrue(result.containsKey(TAG_PARTNER))
        Assert.assertEquals(PARTNER_ID, result[TAG_PARTNER])
        Assert.assertFalse(result.containsKey(TAG_APPDATA_KEY))
        Assert.assertTrue(result.containsKey(TAG_FHIR_VERSION))
        Assert.assertEquals(ModelVersion.FHIR_VERSION, result[TAG_FHIR_VERSION])
    }

    @Test
    fun tag_shouldReturnMapWithClientTag_whenTypeInvalid() {
        // Given
        val type = ""

        // When
        val result = taggingService.appendDefaultTags(type, null)

        // Then
        Assert.assertEquals(3, result.size.toLong())
        Assert.assertTrue(result.containsKey(TAG_CLIENT))
        Assert.assertEquals(CLIENT_ID, result[TAG_CLIENT])
        Assert.assertTrue(result.containsKey(TAG_PARTNER))
        Assert.assertEquals(PARTNER_ID, result[TAG_PARTNER])
        Assert.assertFalse(result.containsKey(TAG_APPDATA_KEY))
        Assert.assertTrue(result.containsKey(TAG_FHIR_VERSION))
        Assert.assertEquals(ModelVersion.FHIR_VERSION, result[TAG_FHIR_VERSION])
    }

    @Test
    fun tag_shouldPreserveExistingTagsAndUpdate() {
        // Given
        val type = "type"
        val existingTags = HashMap<String, String>()
        existingTags["tag_1_key"] = "tag_1_value"
        existingTags["tag_2_key"] = "tag_2_value"
        existingTags[TAG_RESOURCE_TYPE] = "old_typ"

        // When
        val result = taggingService.appendDefaultTags(type, existingTags)

        // Then
        Assert.assertEquals(6, result.size.toLong())
        Assert.assertTrue(result.containsKey("tag_1_key"))
        Assert.assertTrue(result.containsKey("tag_2_key"))
        Assert.assertTrue(result.containsKey(TAG_RESOURCE_TYPE))
        Assert.assertEquals(type, result[TAG_RESOURCE_TYPE])
        Assert.assertTrue(result.containsKey(TAG_CLIENT))
        Assert.assertEquals(CLIENT_ID, result[TAG_CLIENT])
        Assert.assertTrue(result.containsKey(TAG_PARTNER))
        Assert.assertEquals(PARTNER_ID, result[TAG_PARTNER])
        Assert.assertFalse(result.containsKey(TAG_APPDATA_KEY))
        Assert.assertTrue(result.containsKey(TAG_FHIR_VERSION))
        Assert.assertEquals(ModelVersion.FHIR_VERSION, result[TAG_FHIR_VERSION])
    }

    @Test
    fun tag_shouldSetUpdatedByClientTag_whenClientAlreadySet() {
        // Given
        val type = "type"
        val existingTags = HashMap<String, String>()
        existingTags[TAG_CLIENT] = OTHER_CLIENT_ID

        // When
        val result = taggingService.appendDefaultTags(type, existingTags)

        // Then
        Assert.assertEquals(5, result.size.toLong())
        Assert.assertTrue(result.containsKey(TAG_CLIENT))
        Assert.assertEquals(OTHER_CLIENT_ID, result[TAG_CLIENT])
        Assert.assertTrue(result.containsKey(TAG_UPDATED_BY_CLIENT))
        Assert.assertEquals(CLIENT_ID, result[TAG_UPDATED_BY_CLIENT])
        Assert.assertTrue(result.containsKey(TAG_PARTNER))
        Assert.assertEquals(PARTNER_ID, result[TAG_PARTNER])
        Assert.assertFalse(result.containsKey(TAG_APPDATA_KEY))
        Assert.assertTrue(result.containsKey(TAG_FHIR_VERSION))
        Assert.assertEquals(ModelVersion.FHIR_VERSION, result[TAG_FHIR_VERSION])
    }

    @Test
    fun annotatedTag_shouldReturnMapWithResourceAndClientTag() {
        // Given
        val type = "type"

        // When
        val result= taggingService.appendDefaultAnnotatedTags(type, null)

        // Then
        Assert.assertEquals(4, result!!.size.toLong())
        Assert.assertTrue(result.containsKey(TAG_RESOURCE_TYPE))
        Assert.assertEquals(type, result[TAG_RESOURCE_TYPE])
        Assert.assertTrue(result.containsKey(TAG_CLIENT))
        Assert.assertEquals(CLIENT_ID, result[TAG_CLIENT])
        Assert.assertTrue(result.containsKey(TAG_PARTNER))
        Assert.assertEquals(PARTNER_ID, result[TAG_PARTNER])
        Assert.assertFalse(result.containsKey(TAG_FHIR_VERSION))
        Assert.assertTrue(result.containsKey(TAG_APPDATA_KEY))
        Assert.assertEquals(TAG_APPDATA_VALUE, result[TAG_APPDATA_KEY])
    }

    @Test
    fun annotatedTag_shouldReturnMapWithClientTag_whenTypeInvalid() {
        // Given
        val type = ""

        // When
        val result= taggingService.appendDefaultAnnotatedTags(type, null)

        // Then
        Assert.assertEquals(3, result!!.size.toLong())
        Assert.assertTrue(result.containsKey(TAG_CLIENT))
        Assert.assertEquals(CLIENT_ID, result[TAG_CLIENT])
        Assert.assertTrue(result.containsKey(TAG_PARTNER))
        Assert.assertEquals(PARTNER_ID, result[TAG_PARTNER])
        Assert.assertFalse(result.containsKey(TAG_FHIR_VERSION))
        Assert.assertTrue(result.containsKey(TAG_APPDATA_KEY))
        Assert.assertEquals(TAG_APPDATA_VALUE, result[TAG_APPDATA_KEY])
    }

    @Test
    fun annotatedTag_shouldPreserveExistingTagsAndUpdate() {
        // Given
        val type = "type"
        val existingTags = HashMap<String, String>()
        existingTags["tag_1_key"] = "tag_1_value"
        existingTags["tag_2_key"] = "tag_2_value"
        existingTags[TAG_RESOURCE_TYPE] = "old_typ"

        // When
        val result= taggingService.appendDefaultAnnotatedTags(type, existingTags)

        // Then
        Assert.assertEquals(6, result!!.size.toLong())
        Assert.assertTrue(result.containsKey("tag_1_key"))
        Assert.assertTrue(result.containsKey("tag_2_key"))
        Assert.assertTrue(result.containsKey(TAG_RESOURCE_TYPE))
        Assert.assertEquals(type, result[TAG_RESOURCE_TYPE])
        Assert.assertTrue(result.containsKey(TAG_CLIENT))
        Assert.assertEquals(CLIENT_ID, result[TAG_CLIENT])
        Assert.assertTrue(result.containsKey(TAG_PARTNER))
        Assert.assertEquals(PARTNER_ID, result[TAG_PARTNER])
        Assert.assertFalse(result.containsKey(TAG_FHIR_VERSION))
        Assert.assertTrue(result.containsKey(TAG_APPDATA_KEY))
        Assert.assertEquals(TAG_APPDATA_VALUE, result[TAG_APPDATA_KEY])
    }

    @Test
    fun annotatedTag_shouldSetUpdatedByClientTag_whenClientAlreadySet() {
        // Given
        val type = "type"
        val existingTags = HashMap<String, String>()
        existingTags[TAG_CLIENT] = OTHER_CLIENT_ID

        // When
        val result= taggingService.appendDefaultAnnotatedTags(type, existingTags)

        // Then
        Assert.assertEquals(5, result!!.size.toLong())
        Assert.assertTrue(result.containsKey(TAG_CLIENT))
        Assert.assertEquals(OTHER_CLIENT_ID, result[TAG_CLIENT])
        Assert.assertTrue(result.containsKey(TAG_UPDATED_BY_CLIENT))
        Assert.assertEquals(CLIENT_ID, result[TAG_UPDATED_BY_CLIENT])
        Assert.assertTrue(result.containsKey(TAG_PARTNER))
        Assert.assertEquals(PARTNER_ID, result[TAG_PARTNER])
        Assert.assertFalse(result.containsKey(TAG_FHIR_VERSION))
        Assert.assertTrue(result.containsKey(TAG_APPDATA_KEY))
        Assert.assertEquals(TAG_APPDATA_VALUE, result[TAG_APPDATA_KEY])
    }

    @Test
    fun appendAppDataTags_returnsAppDataTaggedList() {
        // Given
        val tags = hashMapOf<String, String>()

        // When
        val result= taggingService.appendAppDataTags(tags)

        // Then
        Assert.assertEquals(1, result!!.size.toLong())
        Assert.assertTrue(result.containsKey(TAG_APPDATA_KEY))
        Assert.assertEquals(TAG_APPDATA_VALUE, result[TAG_APPDATA_KEY])
    }

    @Test
    fun appendAppDataTags_returnsNull_whenTheTagsHadBeenNull() {
        Assert.assertNull(taggingService.appendAppDataTags(null))
    }

    @Test
    fun getTagFromType_shouldReturnListWithResourceTypeTag() {
        // Given
        val type = "type"

        // When
        val result = taggingService.getTagFromType(type)

        // Then
        Assert.assertEquals(1, result.size.toLong())
        Assert.assertTrue(result.containsKey(TAG_RESOURCE_TYPE))
        Assert.assertEquals(type, result[TAG_RESOURCE_TYPE])
    }

    @Test
    fun getTagFromType_shouldReturnEmptyList_whenResourceTypeNull() {
        // When
        val result = taggingService.getTagFromType(null)

        // Then
        Assert.assertTrue(result.isEmpty())
    }

    companion object {
        private const val CLIENT_ID = "client_id#platform"
        private const val OTHER_CLIENT_ID = "other_client_id"
        private const val TAG_PARTNER = "partner"
        private const val PARTNER_ID = "client_id"
        private const val TAG_RESOURCE_TYPE = "resourcetype"
        private const val TAG_CLIENT = "client"
        private const val TAG_UPDATED_BY_CLIENT = "updatedbyclient"
        private const val TAG_FHIR_VERSION = "fhirversion"
        private const val TAG_APPDATA_KEY = "flag"
        private const val TAG_APPDATA_VALUE = "appdata"
    }
}
