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
import care.data4life.crypto.error.CryptoException.DecryptionFailed
import care.data4life.crypto.error.CryptoException.EncryptionFailed
import care.data4life.fhir.FhirException
import care.data4life.fhir.FhirParser
import care.data4life.fhir.stu3.model.DocumentReference
import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.sdk.CryptoService
import com.google.common.truth.Truth
import io.reactivex.Single
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class FhirServiceTest {
    private val parseException = FhirException(FhirException.ErrorType.DECODE, FhirException.ErrorCode.FAILED_TO_PARSE_JSON, "")
    private val unkwnownException = RuntimeException()
    private val fhirClass: Class<*> = DocumentReference::class.java
    private val fhirType = DocumentReference.resourceType
    private val mockDocumentReference = Mockito.mock(DocumentReference::class.java)
    private val dataKey = Mockito.mock(GCKey::class.java)
    private lateinit var mockFhirParser: FhirParser<Any>
    private lateinit var mockCryptoService: CryptoService
    private lateinit var fhirService : FhirService

    @Before
    fun setUp() {
        mockCryptoService = Mockito.mock(CryptoService::class.java)
        @Suppress("UNCHECKED_CAST")
        mockFhirParser = Mockito.mock(FhirParser::class.java) as FhirParser<Any>
        fhirService = FhirService(mockCryptoService, mockFhirParser)
    }

    @Test
    @Throws(FhirException::class)
    fun decryptResource_shouldReturnResource() {
        // Given
        Mockito.`when`(mockCryptoService.decryptString(dataKey, ENCRYPTED_RESOURCE)).thenReturn(Single.just(JSON_RESOURCE))
        Mockito.`when`<Any>(mockFhirParser.toFhir(fhirClass, JSON_RESOURCE)).thenReturn(mockDocumentReference)

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
        Mockito.`when`(mockFhirParser.fromFhir(mockDocumentReference)).thenReturn(JSON_RESOURCE)
        Mockito.`when`(mockCryptoService.encryptString(dataKey, JSON_RESOURCE)).thenReturn(Single.just(ENCRYPTED_RESOURCE))

        // when
        val result = fhirService.encryptResource(dataKey, mockDocumentReference)

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
        Mockito.`when`(mockCryptoService.decryptString(dataKey, ENCRYPTED_RESOURCE)).thenThrow(unkwnownException)
        try {
            // when
            fhirService.decryptResource<DomainResource>(dataKey, DocumentReference.resourceType, ENCRYPTED_RESOURCE)
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
        Mockito.`when`(mockCryptoService.decryptString(dataKey, ENCRYPTED_RESOURCE)).thenReturn(Single.just(JSON_RESOURCE))
        Mockito.`when`<Any>(mockFhirParser.toFhir(DocumentReference::class.java, JSON_RESOURCE)).thenThrow(parseException)
        try {
            // when
            fhirService.decryptResource<DomainResource>(dataKey, DocumentReference.resourceType, ENCRYPTED_RESOURCE)
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
    }

    @Test
    @Throws(FhirException::class)
    fun encryptResource_shouldThrowException_whenParseErrorHappens() {
        // given
        Mockito.`when`(mockFhirParser.fromFhir(mockDocumentReference)).thenThrow(parseException)
        try {
            // when
            fhirService.encryptResource(dataKey, mockDocumentReference)
            Assert.fail("Exception expected!")
        } catch (e: RuntimeException) {

            // then
            val firstException = e.cause!!.cause as FhirException?
            Truth.assertThat(firstException).isEqualTo(parseException)
            Truth.assertThat(e.cause).isInstanceOf(EncryptionFailed::class.java)
            Truth.assertThat(e.cause!!.message).isEqualTo("Failed to encrypt resource")
        }
        val inOrder = Mockito.inOrder(mockCryptoService, mockFhirParser)
        inOrder.verify(mockFhirParser)!!.fromFhir(mockDocumentReference)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(FhirException::class)
    fun encryptResource_shouldThrowException_whenEncryptErrorHappens() {
        // given
        Mockito.`when`(mockFhirParser.fromFhir(mockDocumentReference)).thenReturn(JSON_RESOURCE)
        Mockito.`when`(mockCryptoService.encryptString(dataKey, JSON_RESOURCE)).thenThrow(unkwnownException)
        try {
            // when
            fhirService.encryptResource(dataKey, mockDocumentReference)
            Assert.fail("Exception expected!")
        } catch (e: RuntimeException) {

            // then
            val firstException = e.cause!!.cause as RuntimeException?
            Truth.assertThat(firstException).isEqualTo(unkwnownException)
            Truth.assertThat(e.cause).isInstanceOf(EncryptionFailed::class.java)
            Truth.assertThat(e.cause!!.message).isEqualTo("Failed to encrypt resource")
        }
        val inOrder = Mockito.inOrder(mockCryptoService, mockFhirParser)
        inOrder.verify(mockFhirParser)!!.fromFhir(mockDocumentReference)
        inOrder.verify(mockCryptoService)!!.encryptString(dataKey, JSON_RESOURCE)
        inOrder.verifyNoMoreInteractions()
    }

    companion object {
        private const val ENCRYPTED_RESOURCE = "encryptedResource"
        private const val JSON_RESOURCE = "jsonResource"
    }
}
