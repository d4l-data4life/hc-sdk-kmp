/*
 * Copyright (c) 2021 D4L data4life gGmbH / All rights reserved.
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
package care.data4life.sdk.resource

import care.data4life.crypto.GCKey
import care.data4life.crypto.error.CryptoException
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.tag.TaggingContract.Companion.TAG_APPDATA_KEY
import care.data4life.sdk.tag.TaggingContract.Companion.TAG_APPDATA_VALUE
import care.data4life.sdk.tag.TaggingContract.Companion.TAG_FHIR_VERSION
import care.data4life.sdk.tag.TaggingContract.Companion.TAG_RESOURCE_TYPE
import care.data4life.sdk.wrapper.SdkFhirParser
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ResourceCryptoServiceTest {
    private var cryptoService: CryptoContract.Service = mockk()
    private lateinit var resourceCryptoService: ResourceContract.CryptoService

    @Before
    fun setUp() {
        clearAllMocks()
        mockkObject(SdkFhirParser)

        resourceCryptoService = ResourceCryptoService(cryptoService)
    }

    @After
    fun tearDown() {
        unmockkObject(SdkFhirParser)
    }

    @Test
    fun `It fulfils CryptoService`() {
        val service: Any = ResourceCryptoService(cryptoService)
        assertTrue(service is ResourceContract.CryptoService)
    }

    // encrypt
    // FHIR3
    @Test
    fun `Given, encryptResource is called with a Fhir3Resource and a DataKey, it propagates failures of the Parser`() {
        // Given
        val exception = RuntimeException("Happy failure")
        val resource: Fhir3Resource = mockk()
        val dataKey: GCKey = mockk()

        every { SdkFhirParser.fromResource(any()) } throws exception

        // Then
        val error = assertFailsWith<CryptoException.EncryptionFailed> {
            // When
            resourceCryptoService.encryptResource(dataKey, resource)
        }

        assertEquals(
            actual = error.message,
            expected = "Failed to encrypt resource"
        )
    }

    @Test
    fun `Given, encryptResource is called with a Fhir3Resource and a DataKey, it propagates failures of the CryptoService`() {
        // Given
        val exception = RuntimeException("Happy failure")
        val resource: Fhir3Resource = mockk()
        val dataKey: GCKey = mockk()

        every { SdkFhirParser.fromResource(any()) } returns "not important"
        every { cryptoService.encryptAndEncodeString(dataKey, any()) } throws exception

        // Then
        val error = assertFailsWith<CryptoException.EncryptionFailed> {
            // When
            resourceCryptoService.encryptResource(dataKey, resource)
        }

        assertEquals(
            actual = error.message,
            expected = "Failed to encrypt resource"
        )
    }

    @Test
    fun `Given encryptResource is called with a Fhir3Resource, it encrypts it`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val dataKey: GCKey = mockk()
        val encryptedResource = "encryptedResource"
        val serializedResource = "jsonResource"

        every { SdkFhirParser.fromResource(resource) } returns serializedResource
        every {
            cryptoService.encryptAndEncodeString(dataKey, serializedResource)
        } returns Single.just(encryptedResource)

        // When
        val result = resourceCryptoService.encryptResource(dataKey, resource)

        // Then
        assertEquals(
            actual = result,
            expected = encryptedResource
        )
    }

    // FHIR 4
    @Test
    fun `Given, encryptResource is called with a Fhir4Resource and a DataKey, it propagates failures of the Parser`() {
        // Given
        val exception = RuntimeException("Happy failure")
        val resource: Fhir4Resource = mockk()
        val dataKey: GCKey = mockk()

        every { SdkFhirParser.fromResource(any()) } throws exception

        // Then
        val error = assertFailsWith<CryptoException.EncryptionFailed> {
            // When
            resourceCryptoService.encryptResource(dataKey, resource)
        }

        assertEquals(
            actual = error.message,
            expected = "Failed to encrypt resource"
        )
    }

    @Test
    fun `Given, encryptResource is called with a Fhir4Resource and a DataKey, it propagates failures of the CryptoService`() {
        // Given
        val exception = RuntimeException("Happy failure")
        val resource: Fhir4Resource = mockk()
        val dataKey: GCKey = mockk()

        every { SdkFhirParser.fromResource(any()) } returns "not important"
        every { cryptoService.encryptAndEncodeString(dataKey, any()) } throws exception

        // Then
        val error = assertFailsWith<CryptoException.EncryptionFailed> {
            // When
            resourceCryptoService.encryptResource(dataKey, resource)
        }

        assertEquals(
            actual = error.message,
            expected = "Failed to encrypt resource"
        )
    }

    @Test
    fun `Given encryptResource is called with a Fhir4Resource, it encrypts it`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val dataKey: GCKey = mockk()
        val encryptedResource = "encryptedResource"
        val serializedResource = "jsonResource"

        every { SdkFhirParser.fromResource(resource) } returns serializedResource
        every {
            cryptoService.encryptAndEncodeString(dataKey, serializedResource)
        } returns Single.just(encryptedResource)

        // When
        val result = resourceCryptoService.encryptResource(dataKey, resource)

        // Then
        assertEquals(
            actual = result,
            expected = encryptedResource
        )
    }

    // Arbitrary Data
    @Test
    fun `Given, encryptResource is called with a DataResource and a DataKey, it propagates failures of the CryptoService`() {
        // Given
        val exception = RuntimeException("Happy failure")
        val resource: DataResource = mockk()
        val dataKey: GCKey = mockk()

        every { resource.asByteArray() } returns "not important".toByteArray()
        every { cryptoService.encryptAndEncodeByteArray(dataKey, any()) } throws exception

        // Then
        val error = assertFailsWith<CryptoException.EncryptionFailed> {
            // When
            resourceCryptoService.encryptResource(dataKey, resource)
        }

        assertEquals(
            actual = error.message,
            expected = "Failed to encrypt resource"
        )
    }

    @Test
    fun `Given encryptResource is called with a DataResource, it encrypts it`() {
        // Given
        val dataKey: GCKey = mockk()
        val resource: DataResource = mockk()
        val dataValue = "data".toByteArray()
        val encryptedResource = "encryptedResource"

        every { resource.asByteArray() } returns dataValue
        every {
            cryptoService.encryptAndEncodeByteArray(dataKey, dataValue)
        } returns Single.just(encryptedResource)

        // When
        val result = resourceCryptoService.encryptResource(dataKey, resource)

        // Then
        assertEquals(
            actual = result,
            expected = encryptedResource
        )
    }

    // decrypt
    // FHIR 3
    @Test
    fun `Given, decryptResource is called with a encrypted Fhir3Resource, Tags and DataKey, it filters blanks and fails`() {
        // Given
        val dataKey: GCKey = mockk()
        val tags = mapOf(
            TAG_FHIR_VERSION to ResourceContract.FhirVersion.FHIR_3.version,
            TAG_RESOURCE_TYPE to "fhirType"
        )

        // Then
        val error = assertFailsWith<CryptoException.DecryptionFailed> {
            // When
            resourceCryptoService.decryptResource<Fhir3Resource>(
                dataKey,
                tags,
                "      "
            )
        }

        assertEquals(
            actual = error.message,
            expected = "Failed to decrypt resource"
        )
    }

    @Test
    fun `Given, decryptResource is called with a encrypted Fhir3Resource, Tags and DataKey, it propagates CryptoService Errors`() {
        // Given
        val exception = RuntimeException("Happy failure")
        val dataKey: GCKey = mockk()
        val encryptedResource = "encryptedResource"
        val tags = mapOf(
            TAG_FHIR_VERSION to ResourceContract.FhirVersion.FHIR_3.version,
            TAG_RESOURCE_TYPE to "fhirType"
        )

        every { cryptoService.decodeAndDecryptString(dataKey, encryptedResource) } throws exception

        // Then
        val error = assertFailsWith<CryptoException.DecryptionFailed> {
            // When
            resourceCryptoService.decryptResource<Fhir3Resource>(
                dataKey,
                tags,
                encryptedResource
            )
        }

        assertEquals(
            actual = error.message,
            expected = "Failed to decrypt resource"
        )
    }

    @Test
    fun `Given, decryptResource is called with a encrypted Fhir3Resource, Tags and DataKey, it propagates Parser Errors`() {
        // Given
        val exception = RuntimeException("Happy failure")
        val dataKey: GCKey = mockk()
        val encryptedResource = "encryptedResource"
        val serializedResource = "serialzedResource"
        val tags = mapOf(
            TAG_FHIR_VERSION to ResourceContract.FhirVersion.FHIR_3.version,
            TAG_RESOURCE_TYPE to "fhirType"
        )

        every {
            cryptoService.decodeAndDecryptString(
                dataKey,
                encryptedResource
            )
        } returns Single.just(serializedResource)

        every {
            SdkFhirParser.toFhir<Fhir3Resource>(
                tags[TAG_RESOURCE_TYPE]!!,
                tags[TAG_FHIR_VERSION]!!,
                serializedResource
            )
        } throws exception

        // Then
        val error = assertFailsWith<CryptoException.DecryptionFailed> {
            // When
            resourceCryptoService.decryptResource<Fhir3Resource>(
                dataKey,
                tags,
                encryptedResource
            )
        }

        assertEquals(
            actual = error.message,
            expected = "Failed to decrypt resource"
        )
    }

    @Test
    fun `Given, decryptResource is called with a encrypted Fhir3Resource, Tags and DataKey, it decrypts the resource`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val dataKey: GCKey = mockk()
        val encryptedResource = "encryptedResource"
        val serializedResource = "serialzedResource"
        val tags = mapOf(
            TAG_FHIR_VERSION to ResourceContract.FhirVersion.FHIR_3.version,
            TAG_RESOURCE_TYPE to "fhirType"
        )

        every {
            cryptoService.decodeAndDecryptString(
                dataKey,
                encryptedResource
            )
        } returns Single.just(serializedResource)

        every {
            SdkFhirParser.toFhir<Fhir3Resource>(
                tags[TAG_RESOURCE_TYPE]!!,
                tags[TAG_FHIR_VERSION]!!,
                serializedResource
            )
        } returns resource

        // When
        val result = resourceCryptoService.decryptResource<Fhir3Resource>(
            dataKey,
            tags,
            encryptedResource
        )

        // Then
        assertSame(
            actual = result,
            expected = resource
        )
    }

    // FHIR 4
    @Test
    fun `Given, decryptResource is called with a encrypted Fhir4Resource, Tags and DataKey, it filters blanks and fails`() {
        // Given
        val dataKey: GCKey = mockk()
        val tags = mapOf(
            TAG_FHIR_VERSION to ResourceContract.FhirVersion.FHIR_4.version,
            TAG_RESOURCE_TYPE to "fhirType"
        )

        // Then
        val error = assertFailsWith<CryptoException.DecryptionFailed> {
            // When
            resourceCryptoService.decryptResource<Fhir4Resource>(
                dataKey,
                tags,
                "      "
            )
        }

        assertEquals(
            actual = error.message,
            expected = "Failed to decrypt resource"
        )
    }

    @Test
    fun `Given, decryptResource is called with a encrypted Fhir4Resource, Tags and DataKey, it propagates CryptoService Errors`() {
        // Given
        val exception = RuntimeException("Happy failure")
        val dataKey: GCKey = mockk()
        val encryptedResource = "encryptedResource"
        val tags = mapOf(
            TAG_FHIR_VERSION to ResourceContract.FhirVersion.FHIR_4.version,
            TAG_RESOURCE_TYPE to "fhirType"
        )

        every { cryptoService.decodeAndDecryptString(dataKey, encryptedResource) } throws exception

        // Then
        val error = assertFailsWith<CryptoException.DecryptionFailed> {
            // When
            resourceCryptoService.decryptResource<Fhir4Resource>(
                dataKey,
                tags,
                encryptedResource
            )
        }

        assertEquals(
            actual = error.message,
            expected = "Failed to decrypt resource"
        )
    }

    @Test
    fun `Given, decryptResource is called with a encrypted Fhir4Resource, Tags and DataKey, it propagates Parser Errors`() {
        // Given
        val exception = RuntimeException("Happy failure")
        val dataKey: GCKey = mockk()
        val encryptedResource = "encryptedResource"
        val serializedResource = "serialzedResource"
        val tags = mapOf(
            TAG_FHIR_VERSION to ResourceContract.FhirVersion.FHIR_4.version,
            TAG_RESOURCE_TYPE to "fhirType"
        )

        every {
            cryptoService.decodeAndDecryptString(
                dataKey,
                encryptedResource
            )
        } returns Single.just(serializedResource)

        every {
            SdkFhirParser.toFhir<Fhir4Resource>(
                tags[TAG_RESOURCE_TYPE]!!,
                tags[TAG_FHIR_VERSION]!!,
                serializedResource
            )
        } throws exception

        // Then
        val error = assertFailsWith<CryptoException.DecryptionFailed> {
            // When
            resourceCryptoService.decryptResource<Fhir4Resource>(
                dataKey,
                tags,
                encryptedResource
            )
        }

        assertEquals(
            actual = error.message,
            expected = "Failed to decrypt resource"
        )
    }

    @Test
    fun `Given, decryptResource is called with a encrypted Fhir4Resource, Tags and DataKey, it decrypts the resource`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val dataKey: GCKey = mockk()
        val encryptedResource = "encryptedResource"
        val serializedResource = "serialzedResource"
        val tags = mapOf(
            TAG_FHIR_VERSION to ResourceContract.FhirVersion.FHIR_4.version,
            TAG_RESOURCE_TYPE to "fhirType"
        )

        every {
            cryptoService.decodeAndDecryptString(
                dataKey,
                encryptedResource
            )
        } returns Single.just(serializedResource)

        every {
            SdkFhirParser.toFhir<Fhir4Resource>(
                tags[TAG_RESOURCE_TYPE]!!,
                tags[TAG_FHIR_VERSION]!!,
                serializedResource
            )
        } returns resource

        // When
        val result = resourceCryptoService.decryptResource<Fhir4Resource>(
            dataKey,
            tags,
            encryptedResource
        )

        // Then
        assertSame(
            actual = result,
            expected = resource
        )
    }

    // Arbitrary Data
    @Test
    fun `Given, decryptResource is called with a encrypted DataResource, Tags and DataKey, it filters blanks and fails`() {
        // Given
        val dataKey: GCKey = mockk()
        val tags = mapOf(
            TAG_APPDATA_KEY to TAG_APPDATA_VALUE
        )

        // Then
        val error = assertFailsWith<CryptoException.DecryptionFailed> {
            // When
            resourceCryptoService.decryptResource<DataResource>(
                dataKey,
                tags,
                "      "
            )
        }

        assertEquals(
            actual = error.message,
            expected = "Failed to decrypt resource"
        )
    }

    @Test
    fun `Given, decryptResource is called with a encrypted DataResource, Tags and DataKey, it propagates CryptoService Errors`() {
        // Given
        val exception = RuntimeException("Happy failure")
        val dataKey: GCKey = mockk()
        val encryptedResource = "encryptedResource"
        val tags = mapOf(
            TAG_APPDATA_KEY to TAG_APPDATA_VALUE
        )

        every { cryptoService.decodeAndDecryptString(dataKey, encryptedResource) } throws exception

        // Then
        val error = assertFailsWith<CryptoException.DecryptionFailed> {
            // When
            resourceCryptoService.decryptResource<DataResource>(
                dataKey,
                tags,
                encryptedResource
            )
        }

        assertEquals(
            actual = error.message,
            expected = "Failed to decrypt resource"
        )
    }

    @Test
    fun `Given, decryptResource is called with a encrypted DataResource, Tags and DataKey, it decrypts the resource`() {
        // Given
        val resource = ByteArray(23)
        val dataKey: GCKey = mockk()
        val encryptedResource = "encryptedResource"
        val tags = mapOf(
            TAG_APPDATA_KEY to TAG_APPDATA_VALUE
        )

        every {
            cryptoService.decodeAndDecryptByteArray(
                dataKey,
                encryptedResource
            )
        } returns Single.just(resource)

        // When
        val result = resourceCryptoService.decryptResource<DataResource>(
            dataKey,
            tags,
            encryptedResource
        )

        // Then
        assertTrue(result.value.contentEquals(resource))
    }
}
