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
package care.data4life.sdk.fhir

import care.data4life.crypto.GCKey
import care.data4life.crypto.error.CryptoException.EncryptionFailed
import care.data4life.fhir.FhirException
import care.data4life.sdk.CryptoService
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.util.Base64
import care.data4life.sdk.wrapper.FhirParser
import care.data4life.sdk.wrapper.ResourceFactory
import care.data4life.sdk.wrapper.WrapperContract
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import io.reactivex.Single
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class FhirServiceTest {
    private val parseException = FhirException(FhirException.ErrorType.DECODE, FhirException.ErrorCode.FAILED_TO_PARSE_JSON, "")
    private val unkwnownException = RuntimeException()
    private val dataKey = Mockito.mock(GCKey::class.java)
    private lateinit var mockCryptoService: CryptoService
    private lateinit var fhirService: FhirService

    @Before
    fun setUp() {
        mockCryptoService = mockk()

        fhirService = FhirService(mockCryptoService)

        mockkObject(FhirParser)
        mockkObject(Base64)
        mockkObject(ResourceFactory)
    }

    @After
    fun tearDown() {
        unmockkObject(FhirParser)
        unmockkObject(Base64)
        unmockkObject(ResourceFactory)
    }

    /*
    @Test
    @Throws(FhirException::class)
    fun decryptResource_shouldReturnResource() {
        // Given


        // When
        val resource = fhirService.decryptResource<DomainResource>(dataKey, fhirType, ENCRYPTED_RESOURCE)

        // Then
        Truth.assertThat(resource).isEqualTo(mockDocumentReference)
        val inOrder = Mockito.inOrder(mockCryptoService, mockFhirParser)
        inOrder.verify(mockCryptoService)!!.decryptString(dataKey, ENCRYPTED_RESOURCE)
        inOrder.verify(mockFhirParser)!!.toFhir(fhirClass, JSON_RESOURCE)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(FhirException::class)
    fun encryptResource_shouldReturnEncryptedResource() {
        // given
        Mockito.`when`(mockFhirParser!!.fromFhir(mockDocumentReference)).thenReturn(JSON_RESOURCE)
        Mockito.`when`(mockCryptoService!!.encryptString(dataKey, JSON_RESOURCE)).thenReturn(Single.just(ENCRYPTED_RESOURCE))

        // when
        val result = fhirService!!.encryptResource(dataKey, mockDocumentReference)

        // then
        Truth.assertThat(result).isEqualTo(ENCRYPTED_RESOURCE)
        val inOrder = Mockito.inOrder(mockCryptoService, mockFhirParser)
        inOrder.verify(mockFhirParser)!!.fromFhir(mockDocumentReference)
        inOrder.verify(mockCryptoService)!!.encryptString(dataKey, JSON_RESOURCE)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun decryptResource_shouldThrowException_whenDecryptErrorHappens() {
        // given
        Mockito.`when`(mockCryptoService!!.decryptString(dataKey, ENCRYPTED_RESOURCE)).thenThrow(unkwnownException)
        try {
            // when
            fhirService!!.decryptResource<DomainResource>(dataKey, DocumentReference.resourceType, ENCRYPTED_RESOURCE)
            Assert.fail("Exception expected")
        } catch (e: RuntimeException) {

            //Then
            val firstException = e.cause!!.cause as RuntimeException?
            Truth.assertThat(firstException).isEqualTo(unkwnownException)
            Truth.assertThat(e.cause).isInstanceOf(DecryptionFailed::class.java)
            Truth.assertThat(e.cause!!.message).isEqualTo("Failed to decrypt resource")
        }
        val inOrder = Mockito.inOrder(mockCryptoService, mockFhirParser)
        inOrder.verify(mockCryptoService)!!.decryptString(dataKey, ENCRYPTED_RESOURCE)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(FhirException::class)
    fun decryptResource_shouldThrowException_whenParseErrorHappens() {
        // given
        Mockito.`when`(mockCryptoService!!.decryptString(dataKey, ENCRYPTED_RESOURCE)).thenReturn(Single.just(JSON_RESOURCE))
        Mockito.`when`<Any>(mockFhirParser!!.toFhir(DocumentReference::class.java, JSON_RESOURCE)).thenThrow(parseException)
        try {
            // when
            fhirService!!.decryptResource<DomainResource>(dataKey, DocumentReference.resourceType, ENCRYPTED_RESOURCE)
            Assert.fail("Exception expected")
        } catch (e: RuntimeException) {

            //Then
            val firstExc = e.cause!!.cause as FhirException?
            Truth.assertThat(firstExc).isEqualTo(parseException)
            Truth.assertThat(e.cause).isInstanceOf(DecryptionFailed::class.java)
            Truth.assertThat(e.cause!!.message).isEqualTo("Failed to decrypt resource")
        }
        val inOrder = Mockito.inOrder(mockCryptoService, mockFhirParser)
        inOrder.verify(mockCryptoService)!!.decryptString(dataKey, ENCRYPTED_RESOURCE)
        inOrder.verify(mockFhirParser)!!.toFhir(fhirClass, JSON_RESOURCE)
        inOrder.verifyNoMoreInteractions()
    }*/

    @Test
    @Throws(FhirException::class)
    fun encryptResource_shouldThrowException_whenParseErrorHappens() {
        // given
        val resource = mockk<WrapperContract.Resource>()

        every { FhirParser.fromResource(resource) } throws parseException

        try {
            // when
            fhirService.encryptResource(dataKey, resource)
            Assert.fail("Exception expected!")
        } catch (e: RuntimeException) {

            // then
            val firstException = e.cause!!.cause as FhirException?
            Truth.assertThat(firstException).isEqualTo(parseException)
            Truth.assertThat(e.cause).isInstanceOf(EncryptionFailed::class.java)
            Truth.assertThat(e.cause!!.message).isEqualTo("Failed to encrypt resource")
        }
    }

    @Test
    @Throws(FhirException::class)
    fun encryptResource_shouldThrowException_whenEncryptErrorHappens() {
        // given
        val resource = mockk<WrapperContract.Resource>()
        val json = "json"

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { FhirParser.fromResource(resource) } returns json
        every { mockCryptoService.encryptString(dataKey, json) } throws unkwnownException

        try {
            // when
            fhirService.encryptResource(dataKey, resource)
            Assert.fail("Exception expected!")
        } catch (e: RuntimeException) {

            // then
            val firstException = e.cause!!.cause as RuntimeException?
            Truth.assertThat(firstException).isEqualTo(unkwnownException)
            Truth.assertThat(e.cause).isInstanceOf(EncryptionFailed::class.java)
            Truth.assertThat(e.cause!!.message).isEqualTo("Failed to encrypt resource")
        }
    }

    // ToDo better phrasing
    @Test
    fun `Given, encryptResource is called with a FhirResource, it returns it encrypted`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val json = "json"
        val encrypted = "encryptedResource"

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { FhirParser.fromResource(resource) } returns json
        every { mockCryptoService.encryptString(dataKey, json) } returns Single.just(encrypted)

        // Then
        assertEquals(
                fhirService.encryptResource(dataKey, resource),
                encrypted
        )
    }

    @Test
    fun `Given, encryptResource is called with a DataResource, it returns it encrypted`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val raw = ByteArray(1)
        val wrappedRaw = mockk<DataResource>()

        val encrypted = ByteArray(23)
        val encoded = "encryptedResource"

        every { resource.type } returns WrapperContract.Resource.TYPE.DATA
        every { resource.unwrap() } returns wrappedRaw
        every { wrappedRaw.asByteArray() } returns raw

        every {
            mockCryptoService.encrypt(
                    dataKey,
                    raw
            )
        } returns Single.just(encrypted)
        every { Base64.encodeToString(encrypted) } returns encoded

        // Then
        assertEquals(
                fhirService.encryptResource(dataKey, resource),
                encoded
        )
    }

    @Test
    fun `Given, the legacy method encryptResource is called with a raw resource, it returns it encrypted`() {
        val resource = mockk<Fhir3Resource>()
        val service = spyk(fhirService)

        every { ResourceFactory.wrap(resource) } returns mockk()
        every { service.encryptResource(dataKey, any<WrapperContract.Resource>()) } returns "something"

        service.encryptResource(dataKey, resource)

        verify(exactly = 1) { ResourceFactory.wrap(resource) }
    }
}
