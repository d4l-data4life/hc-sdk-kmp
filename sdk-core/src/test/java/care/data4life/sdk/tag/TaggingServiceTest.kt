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
package care.data4life.sdk.tag


import care.data4life.fhir.stu3.model.Patient
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.wrapper.SdkFhirElementFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import care.data4life.fhir.r4.model.Patient as R4Patient

class TaggingServiceTest {
    // SUT
    private lateinit var taggingService: TaggingService

    @Before
    fun setUp() {
        taggingService = TaggingService(CLIENT_ID)
    }

    @Test
    fun `Given, appendDefaultTags is called with a Fhir3Resource and null, it returns the default tags`() { //tag_shouldReturnMapWithResourceAndClientTag
        // Given
        val resource = Fhir3Resource()

        // When
        val result = taggingService.appendDefaultTags(resource, null)

        // Then
        assertEquals(4, result.size)
        assertTrue(result.containsKey(TAG_RESOURCE_TYPE))
        assertEquals(resource.resourceType, result[TAG_RESOURCE_TYPE])
        assertTrue(result.containsKey(TAG_CLIENT))
        assertEquals(CLIENT_ID, result[TAG_CLIENT])
        assertTrue(result.containsKey(TAG_PARTNER))
        assertEquals(PARTNER_ID, result[TAG_PARTNER])
        assertFalse(result.containsKey(TAG_APPDATA_KEY))
        assertTrue(result.containsKey(TAG_FHIR_VERSION))
        assertEquals(FhirContract.FhirVersion.FHIR_3.version, result[TAG_FHIR_VERSION])
        assertFalse(result.containsKey(TAG_APPDATA_KEY))
    }

    @Test
    fun `Given, appendDefaultTags is called with a Fhir4Resource and null, it returns the default tags`() { //tag_shouldReturnMapWithResourceAndClientTag
        // Given
        val resource = Fhir4Resource()

        // When
        val result = taggingService.appendDefaultTags(resource, null)

        // Then
        assertEquals(4, result.size)
        assertTrue(result.containsKey(TAG_RESOURCE_TYPE))
        assertEquals(resource.resourceType, result[TAG_RESOURCE_TYPE])
        assertTrue(result.containsKey(TAG_CLIENT))
        assertEquals(CLIENT_ID, result[TAG_CLIENT])
        assertTrue(result.containsKey(TAG_PARTNER))
        assertEquals(PARTNER_ID, result[TAG_PARTNER])
        assertFalse(result.containsKey(TAG_APPDATA_KEY))
        assertTrue(result.containsKey(TAG_FHIR_VERSION))
        assertEquals(FhirContract.FhirVersion.FHIR_4.version, result[TAG_FHIR_VERSION])
        assertFalse(result.containsKey(TAG_APPDATA_KEY))
    }

    @Test
    fun `Given, appendDefaultTags is called with a DataResource and null, it returns the default tags`() {
        // Given
        val resource = DataResource(ByteArray(1))
        // When
        val result = taggingService.appendDefaultTags(resource, null)

        // Then
        assertEquals(3, result.size)
        assertFalse(result.containsKey(TAG_RESOURCE_TYPE))
        assertTrue(result.containsKey(TAG_CLIENT))
        assertEquals(CLIENT_ID, result[TAG_CLIENT])
        assertTrue(result.containsKey(TAG_PARTNER))
        assertEquals(PARTNER_ID, result[TAG_PARTNER])
        assertTrue(result.containsKey(TAG_APPDATA_KEY))
        assertEquals(TAG_APPDATA_VALUE, result[TAG_APPDATA_KEY])
        assertFalse(result.containsKey(TAG_FHIR_VERSION))
    }

    @Test
    fun `Given, appendDefaultTags is called with a Resource and old Tags, it preserves the existing Tag and updates the Type`() { //annotatedTag_shouldPreserveExistingTagsAndUpdate
        // Given
        val type = Fhir3Resource()
        val existingTags = HashMap<String, String>()
        existingTags["tag_1_key"] = "tag_1_value"
        existingTags["tag_2_key"] = "tag_2_value"
        existingTags[TAG_RESOURCE_TYPE] = "old_typ"

        // When
        val result = taggingService.appendDefaultTags(type, existingTags)

        // Then
        assertEquals(6, result.size)
        assertTrue(result.containsKey("tag_1_key"))
        assertTrue(result.containsKey("tag_2_key"))
        assertTrue(result.containsKey(TAG_RESOURCE_TYPE))
        assertEquals(type.resourceType, result[TAG_RESOURCE_TYPE])
        assertTrue(result.containsKey(TAG_CLIENT))
        assertEquals(CLIENT_ID, result[TAG_CLIENT])
        assertTrue(result.containsKey(TAG_PARTNER))
        assertEquals(PARTNER_ID, result[TAG_PARTNER])
        assertTrue(result.containsKey(TAG_FHIR_VERSION))
        assertEquals(FhirContract.FhirVersion.FHIR_3.version, result[TAG_FHIR_VERSION])
        assertFalse(result.containsKey(TAG_APPDATA_KEY))
    }

    @Test
    fun `Given, appendDefaultTags is called with a Resource and old Tags, sets UpdatedByClient Tag, if the TAG_CLIENT Tag is present`() { //annotatedTag_shouldSetUpdatedByClientTag_whenClientAlreadySet
        // Given
        val type = Fhir3Resource()
        val existingTags = HashMap<String, String>()
        existingTags[TAG_CLIENT] = OTHER_CLIENT_ID

        // When
        val result = taggingService.appendDefaultTags(type, existingTags)

        // Then
        assertEquals(5, result.size)
        assertTrue(result.containsKey(TAG_CLIENT))
        assertEquals(OTHER_CLIENT_ID, result[TAG_CLIENT])
        assertTrue(result.containsKey(TAG_UPDATED_BY_CLIENT))
        assertEquals(CLIENT_ID, result[TAG_UPDATED_BY_CLIENT])
        assertTrue(result.containsKey(TAG_PARTNER))
        assertEquals(PARTNER_ID, result[TAG_PARTNER])
        assertTrue(result.containsKey(TAG_FHIR_VERSION))
        assertEquals(FhirContract.FhirVersion.FHIR_3.version, result[TAG_FHIR_VERSION])
        assertFalse(result.containsKey(TAG_APPDATA_KEY))
    }

    @Test
    fun `Given, getTagsFromType is called with a Class of a Fhir3Resource, it returns a Map, which contains TAG_RESOURCE_TYPE and TAG_FHIR_VERSION for the given type`() {
        // Given
        val type: Patient = mockk()
        val resourceType = "fhir4Resource"

        mockkObject(SdkFhirElementFactory)
        every { SdkFhirElementFactory.getFhirTypeForClass(Patient::class.java) } returns resourceType
        every { SdkFhirElementFactory.resolveFhirVersion(Patient::class.java) } returns FhirContract.FhirVersion.FHIR_3
        // When
        @Suppress("UNCHECKED_CAST")
        val result = taggingService.getTagsFromType(type::class.java as Class<Any>)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.containsKey(TAG_RESOURCE_TYPE))
        assertEquals(resourceType, result[TAG_RESOURCE_TYPE])
        assertTrue(result.containsKey(TAG_FHIR_VERSION))
        assertEquals(FhirContract.FhirVersion.FHIR_3.version, result[TAG_FHIR_VERSION])

        verify(exactly = 1) { SdkFhirElementFactory.getFhirTypeForClass(Patient::class.java) }
        verify(exactly = 1) { SdkFhirElementFactory.resolveFhirVersion(Patient::class.java) }

        unmockkObject(SdkFhirElementFactory)
    }

    @Test
    fun `Given, getTagFromsType is called with a Class of a Fhir3Resource, it does not set the TAG_RESOURCE_TYPE but the TAG_FHIR_VERSION, if the resource was not determined`() {
        // Given
        val type: Fhir3Resource = mockk()
        val resourceType = null

        mockkObject(SdkFhirElementFactory)
        every { SdkFhirElementFactory.getFhirTypeForClass(Fhir3Resource::class.java) } returns resourceType
        every { SdkFhirElementFactory.resolveFhirVersion(Fhir3Resource::class.java) } returns FhirContract.FhirVersion.FHIR_3
        // When
        @Suppress("UNCHECKED_CAST")
        val result = taggingService.getTagsFromType(type::class.java as Class<Any>)

        // Then
        assertEquals(1, result.size)
        assertFalse(result.containsKey(TAG_RESOURCE_TYPE))
        assertTrue(result.containsKey(TAG_FHIR_VERSION))
        assertEquals(FhirContract.FhirVersion.FHIR_3.version, result[TAG_FHIR_VERSION])

        verify(exactly = 1) { SdkFhirElementFactory.getFhirTypeForClass(Fhir3Resource::class.java) }
        verify(exactly = 1) { SdkFhirElementFactory.resolveFhirVersion(Fhir3Resource::class.java) }

        unmockkObject(SdkFhirElementFactory)
    }

    @Test
    fun `Given, getTagsFromType is called with a Class of a Fhir4Resource, it returns a Map, which contains TAG_RESOURCE_TYPE and TAG_FHIR_VERSION for the given type`() {
        // Given
        val type: R4Patient = mockk()
        val resourceType = "fhir4Resource"

        mockkObject(SdkFhirElementFactory)
        every { SdkFhirElementFactory.getFhirTypeForClass(R4Patient::class.java) } returns resourceType
        every { SdkFhirElementFactory.resolveFhirVersion(R4Patient::class.java) } returns FhirContract.FhirVersion.FHIR_4
        // When
        @Suppress("UNCHECKED_CAST")
        val result = taggingService.getTagsFromType(type::class.java as Class<Any>)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.containsKey(TAG_RESOURCE_TYPE))
        assertEquals(resourceType, result[TAG_RESOURCE_TYPE])
        assertTrue(result.containsKey(TAG_FHIR_VERSION))
        assertEquals(FhirContract.FhirVersion.FHIR_4.version, result[TAG_FHIR_VERSION])

        verify(exactly = 1) { SdkFhirElementFactory.getFhirTypeForClass(R4Patient::class.java) }
        verify(exactly = 1) { SdkFhirElementFactory.resolveFhirVersion(R4Patient::class.java) }

        unmockkObject(SdkFhirElementFactory)
    }

    @Test
    fun `Given, getTagsFromType is called with a Class of a Fhir4Resource, it does not set the TAG_RESOURCE_TYPE but the TAG_FHIR_VERSION, if the resource was not determined`() {
        // Given
        val type: Fhir4Resource = mockk()
        val resourceType = null

        mockkObject(SdkFhirElementFactory)
        every { SdkFhirElementFactory.getFhirTypeForClass(Fhir4Resource::class.java) } returns resourceType
        every { SdkFhirElementFactory.resolveFhirVersion(Fhir4Resource::class.java) } returns FhirContract.FhirVersion.FHIR_4
        // When
        @Suppress("UNCHECKED_CAST")
        val result = taggingService.getTagsFromType(type::class.java as Class<Any>)

        // Then
        assertEquals(1, result.size)
        assertFalse(result.containsKey(TAG_RESOURCE_TYPE))
        assertTrue(result.containsKey(TAG_FHIR_VERSION))
        assertEquals(FhirContract.FhirVersion.FHIR_4.version, result[TAG_FHIR_VERSION])

        verify(exactly = 1) { SdkFhirElementFactory.getFhirTypeForClass(Fhir4Resource::class.java) }
        verify(exactly = 1) { SdkFhirElementFactory.resolveFhirVersion(Fhir4Resource::class.java) }

        unmockkObject(SdkFhirElementFactory)
    }

    @Test
    fun `Given, getTagsFromType is called with a Class of a non Resource, it returns a Map, which contains TAG_APPDATA_KEY`() {
        // Given
        val type: Fhir4Resource = mockk()
        val resourceType = null

        mockkObject(SdkFhirElementFactory)
        every { SdkFhirElementFactory.getFhirTypeForClass(Fhir4Resource::class.java) } returns resourceType
        every { SdkFhirElementFactory.resolveFhirVersion(Fhir4Resource::class.java) } returns FhirContract.FhirVersion.FHIR_4
        // When
        @Suppress("UNCHECKED_CAST")
        val result = taggingService.getTagsFromType(type::class.java as Class<Any>)

        // Then
        assertEquals(1, result.size)
        assertFalse(result.containsKey(TAG_RESOURCE_TYPE))
        assertTrue(result.containsKey(TAG_FHIR_VERSION))
        assertEquals(FhirContract.FhirVersion.FHIR_4.version, result[TAG_FHIR_VERSION])

        verify(exactly = 1) { SdkFhirElementFactory.getFhirTypeForClass(Fhir4Resource::class.java) }
        verify(exactly = 1) { SdkFhirElementFactory.resolveFhirVersion(Fhir4Resource::class.java) }

        unmockkObject(SdkFhirElementFactory)
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
