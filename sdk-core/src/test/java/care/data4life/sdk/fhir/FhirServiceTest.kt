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
import care.data4life.sdk.CryptoService
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.util.Base64
import care.data4life.sdk.wrapper.SdkFhirParser
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
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.Locale

class FhirServiceTest {
    private val parseException = FhirException(
        FhirException.ErrorType.DECODE,
        FhirException.ErrorCode.FAILED_TO_PARSE_JSON,
        ""
    )
    private val unkwnownException = RuntimeException()
    private val fhirClass: Class<*> = DocumentReference::class.java
    private val fhirType = DocumentReference.resourceType
    private val mockDocumentReference = Mockito.mock(DocumentReference::class.java)
    private val dataKey = Mockito.mock(GCKey::class.java)
    private lateinit var mockFhirParser: FhirParser<Any>
    private lateinit var mockCryptoService: CryptoService
    private lateinit var cryptoService: CryptoService

    private lateinit var fhirService: FhirService
    private lateinit var _fhirService: FhirService

    @Before
    fun setUp() {
        cryptoService = mockk()

        _fhirService = FhirService(cryptoService)

        mockkObject(SdkFhirParser)
        mockkObject(Base64)

        mockCryptoService = Mockito.mock(CryptoService::class.java)
        @Suppress("UNCHECKED_CAST")
        mockFhirParser = Mockito.mock(FhirParser::class.java) as FhirParser<Any>
        fhirService = FhirService(mockCryptoService, mockFhirParser)
    }

    @After
    fun tearDown() {
        unmockkObject(SdkFhirParser)
        unmockkObject(Base64)
    }

    @Test
    @Throws(FhirException::class)
    fun `Given encryptResource is called with a Fhir3Resource, it encrypts it`() { // encryptResource_shouldReturnEncryptedResource
        // given
        val resource = Fhir3Resource()

        every { SdkFhirParser.fromResource(resource) } returns JSON_RESOURCE
        every { cryptoService.encryptAndEncodeString(dataKey, JSON_RESOURCE) } returns Single.just(
            ENCRYPTED_RESOURCE
        )

        // when
        val result = _fhirService._encryptResource(dataKey, resource)

        // then
        Truth.assertThat(result).isEqualTo(ENCRYPTED_RESOURCE)
    }

    @Test
    @Throws(FhirException::class)
    fun `Given encryptResource is called with a Fhir4Resource, it encrypts it`() { // encryptResource_shouldReturnEncryptedResource
        // given
        val resource = Fhir4Resource()

        every { SdkFhirParser.fromResource(resource) } returns JSON_RESOURCE
        every { cryptoService.encryptAndEncodeString(dataKey, JSON_RESOURCE) } returns Single.just(
            ENCRYPTED_RESOURCE
        )

        // when
        val result = _fhirService._encryptResource(dataKey, resource)

        // then
        Truth.assertThat(result).isEqualTo(ENCRYPTED_RESOURCE)
    }

    @Test
    @Throws(FhirException::class)
    fun `Given encryptResource is called with a DataResource, it encrypts it`() { // encryptResource_shouldReturnEncryptedResource
        // Given
        val raw = ByteArray(1)
        val resource = DataResource(raw)

        val encrypted = ByteArray(23)
        val encoded = "encryptedResource"

        every {
            cryptoService.encrypt(
                dataKey,
                raw
            )
        } returns Single.just(encrypted)
        every { Base64.encodeToString(encrypted) } returns encoded

        // when
        val result = _fhirService._encryptResource(dataKey, resource)

        // then
        Truth.assertThat(result).isEqualTo(ENCRYPTED_RESOURCE)
    }

    @Test
    @Throws(FhirException::class)
    fun `Given, encryptResource is called with a FhirResource, it fails on a Parser Error`() { // encryptResource_shouldThrowException_whenParseErrorHappens() {
        // given
        val resource = Fhir3Resource()

        every { SdkFhirParser.fromResource(resource) } throws parseException

        try {
            // when
            fhirService._encryptResource(dataKey, resource)
            Assert.fail("Exception expected!")
        } catch (e: RuntimeException) {

            // then
            val firstException = e.cause!!.cause as FhirException
            Truth.assertThat(firstException).isEqualTo(parseException)
            Truth.assertThat(e.cause).isInstanceOf(EncryptionFailed::class.java)
            Truth.assertThat(e.cause!!.message).isEqualTo("Failed to encrypt resource")
        }
    }

    @Test
    @Throws(FhirException::class)
    fun `Given, encryptResource is called with a FhirResource, it fails on a Encryption Error`() { // encryptResource_shouldThrowException_whenEncryptErrorHappens() {
        // given
        val resource = Fhir3Resource()

        every { SdkFhirParser.fromResource(resource) } returns JSON_RESOURCE
        every {
            cryptoService.encryptAndEncodeString(
                dataKey,
                JSON_RESOURCE
            )
        } throws unkwnownException

        try {
            // when
            _fhirService._encryptResource(dataKey, resource)
            Assert.fail("Exception expected!")
        } catch (e: RuntimeException) {

            // then
            val firstException = e.cause!!.cause as RuntimeException
            Truth.assertThat(firstException).isEqualTo(unkwnownException)
            Truth.assertThat(e.cause).isInstanceOf(EncryptionFailed::class.java)
            Truth.assertThat(e.cause!!.message).isEqualTo("Failed to encrypt resource")
        }
    }

    @Test
    @Throws(FhirException::class)
    fun `Given, decryptResource is called with a DataKey, a Type, Tags and a encrypted Fhir3Resource, it decrypts it`() { // decryptResource_shouldReturnResource() {
        // Given
        every {
            cryptoService.decodeAndDecryptString(
                dataKey,
                ENCRYPTED_RESOURCE
            )
        } returns Single.just(JSON_RESOURCE)
        every { SdkFhirParser.toFhir3(fhirType, JSON_RESOURCE) } returns mockDocumentReference

        // When
        val resource = _fhirService.decryptResource<Fhir3Resource>(
            dataKey,
            hashMapOf(
                TAG_FHIR_VERSION to FhirContract.FhirVersion.FHIR_3.version,
                TAG_RESOURCE_TYPE to fhirType
            ),
            ENCRYPTED_RESOURCE
        )

        // Then
        Truth.assertThat(resource).isEqualTo(mockDocumentReference)
    }

    @Test
    @Throws(FhirException::class)
    fun `Given, decryptResource is called with a DataKey, a Type, Tags and a encrypted Fhir4Resource, it decrypts it`() { // decryptResource_shouldReturnResource() {
        val decrypted = mockk<Fhir4Resource>()

        // Given
        every {
            cryptoService.decodeAndDecryptString(
                dataKey,
                ENCRYPTED_RESOURCE
            )
        } returns Single.just(JSON_RESOURCE)
        every { SdkFhirParser.toFhir4(fhirType, JSON_RESOURCE) } returns decrypted

        // When
        val resource = _fhirService.decryptResource<Fhir4Resource>(
            dataKey,
            hashMapOf(
                TAG_FHIR_VERSION to FhirContract.FhirVersion.FHIR_4.version,
                TAG_RESOURCE_TYPE to fhirType
            ),
            ENCRYPTED_RESOURCE
        )

        // Then
        Truth.assertThat(resource).isEqualTo(decrypted)
    }

    @Test
    @Throws(FhirException::class)
    fun `Given, decryptResource is called with a DataKey, a Type, Tags and a encrypted DataResource, it decrypts it`() { // decryptResource_shouldReturnResource() {
        val decoded = ByteArray(23)
        val decrypted = ByteArray(1)

        // Given
        every { Base64.decode(ENCRYPTED_RESOURCE) } returns decoded
        every { cryptoService.decrypt(dataKey, decoded) } returns Single.just(decrypted)

        // When
        val resource = _fhirService.decryptResource<DataResource>(
            dataKey,
            hashMapOf(TAG_APPDATA_KEY to TAG_APPDATA_VALUE),
            ENCRYPTED_RESOURCE
        )

        // Then
        Truth.assertThat(resource).isEqualTo(DataResource(decrypted))
    }

    @Test
    fun `Given, decryptResource is called with a DataKey, a Type, Tags and a encrypted FhirResource, it fails on a CryptoError`() { // decryptResource_shouldThrowException_whenDecryptErrorHappens() {
        // given
        every {
            cryptoService.decodeAndDecryptString(
                dataKey,
                ENCRYPTED_RESOURCE
            )
        } throws unkwnownException

        try {
            // when
            _fhirService.decryptResource<DataResource>(
                dataKey,
                hashMapOf(
                    TAG_FHIR_VERSION to FhirContract.FhirVersion.FHIR_4.version,
                    TAG_RESOURCE_TYPE to fhirType
                ),
                ENCRYPTED_RESOURCE
            )
            Assert.fail("Exception expected")
        } catch (e: RuntimeException) {

            // Then
            val firstException = e.cause!!.cause as RuntimeException?
            Truth.assertThat(firstException).isEqualTo(unkwnownException)
            Truth.assertThat(e.cause).isInstanceOf(DecryptionFailed::class.java)
            Truth.assertThat(e.cause!!.message).isEqualTo("Failed to decrypt resource")
        }
    }

    @Test
    @Throws(FhirException::class)
    fun `Given, decryptResource is called with a DataKey, a Type, Tags and a encrypted FhirResource, it fails on a ParserError`() { // decryptResource_shouldThrowException_whenParseErrorHappens() {
        // given
        every {
            cryptoService.decodeAndDecryptString(
                dataKey,
                ENCRYPTED_RESOURCE
            )
        } returns Single.just(JSON_RESOURCE)
        every { SdkFhirParser.fromResource(JSON_RESOURCE) } throws parseException

        try {
            // when
            _fhirService.decryptResource<DataResource>(
                dataKey,
                hashMapOf(
                    TAG_FHIR_VERSION to FhirContract.FhirVersion.FHIR_4.version,
                    TAG_RESOURCE_TYPE to fhirType
                ),
                ENCRYPTED_RESOURCE
            )
            Assert.fail("Exception expected")
        } catch (e: RuntimeException) {

            // Then
            val firstExc = e.cause!!.cause as FhirException?
            Truth.assertThat(firstExc).isEqualTo(parseException)
            Truth.assertThat(e.cause).isInstanceOf(DecryptionFailed::class.java)
            Truth.assertThat(e.cause!!.message).isEqualTo("Failed to decrypt resource")
        }
    }

    @Test
    fun `Given, the legacy method encryptResource is called with a raw resource, it returns it encrypted`() {
        val resource = mockk<Fhir3Resource>()
        val service = spyk(fhirService)

        every { service._encryptResource(dataKey, resource) } returns "something"

        service.encryptResource(dataKey, resource)

        verify(exactly = 1) { service._encryptResource(dataKey, resource) }
    }

    @Test
    @Throws(FhirException::class)
    fun legacy_decryptResource_shouldReturnResource() {
        // Given
        Mockito.`when`(mockCryptoService.decodeAndDecryptString(dataKey, ENCRYPTED_RESOURCE))
            .thenReturn(Single.just(JSON_RESOURCE))
        Mockito.`when`<Any>(mockFhirParser.toFhir(fhirClass, JSON_RESOURCE))
            .thenReturn(mockDocumentReference)

        // When
        val resource =
            fhirService.decryptResource<Fhir3Resource>(dataKey, fhirType, ENCRYPTED_RESOURCE)

        // Then
        Truth.assertThat(resource).isEqualTo(mockDocumentReference)
        val inOrder = Mockito.inOrder(mockCryptoService, mockFhirParser)
        inOrder.verify(mockCryptoService)!!.decodeAndDecryptString(dataKey, ENCRYPTED_RESOURCE)
        inOrder.verify(mockFhirParser)!!.toFhir(fhirClass, JSON_RESOURCE)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun legacy_decryptResource_shouldThrowException_whenDecryptErrorHappens() {
        // given
        Mockito.`when`(mockCryptoService.decodeAndDecryptString(dataKey, ENCRYPTED_RESOURCE))
            .thenThrow(unkwnownException)
        try {
            // when
            fhirService.decryptResource<Fhir3Resource>(
                dataKey,
                DocumentReference.resourceType,
                ENCRYPTED_RESOURCE
            )
            Assert.fail("Exception expected")
        } catch (e: RuntimeException) {

            // Then
            val firstException = e.cause!!.cause as RuntimeException?
            Truth.assertThat(firstException).isEqualTo(unkwnownException)
            Truth.assertThat(e.cause).isInstanceOf(DecryptionFailed::class.java)
            Truth.assertThat(e.cause!!.message).isEqualTo("Failed to decrypt resource")
        }
        val inOrder = Mockito.inOrder(mockCryptoService, mockFhirParser)
        inOrder.verify(mockCryptoService)!!.decodeAndDecryptString(dataKey, ENCRYPTED_RESOURCE)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(FhirException::class)
    fun legacy_decryptResource_shouldThrowException_whenParseErrorHappens() {
        // given
        Mockito.`when`(mockCryptoService.decodeAndDecryptString(dataKey, ENCRYPTED_RESOURCE))
            .thenReturn(Single.just(JSON_RESOURCE))
        Mockito.`when`<Any>(mockFhirParser.toFhir(DocumentReference::class.java, JSON_RESOURCE))
            .thenThrow(parseException)
        try {
            // when
            fhirService.decryptResource<Fhir3Resource>(
                dataKey,
                DocumentReference.resourceType,
                ENCRYPTED_RESOURCE
            )
            Assert.fail("Exception expected")
        } catch (e: RuntimeException) {

            // Then
            val firstExc = e.cause!!.cause as FhirException?
            Truth.assertThat(firstExc).isEqualTo(parseException)
            Truth.assertThat(e.cause).isInstanceOf(DecryptionFailed::class.java)
            Truth.assertThat(e.cause!!.message).isEqualTo("Failed to decrypt resource")
        }
        val inOrder = Mockito.inOrder(mockCryptoService, mockFhirParser)
        inOrder.verify(mockCryptoService)!!.decodeAndDecryptString(dataKey, ENCRYPTED_RESOURCE)
        inOrder.verify(mockFhirParser)!!.toFhir(fhirClass, JSON_RESOURCE)
        inOrder.verifyNoMoreInteractions()
    }

    companion object {
        private val US_LOCALE = Locale.US
        private const val ENCRYPTED_RESOURCE = "encryptedResource"
        private const val JSON_RESOURCE = "jsonResource"
        private const val TAG_FHIR_VERSION = "fhirversion"
        private val TAG_RESOURCE_TYPE = "resourceType".toLowerCase(US_LOCALE)
        private const val TAG_APPDATA_KEY = "flag"
        private const val TAG_APPDATA_VALUE = "appdata"
    }
}
