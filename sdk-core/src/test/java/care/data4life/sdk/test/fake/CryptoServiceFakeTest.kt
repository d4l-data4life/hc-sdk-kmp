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

package care.data4life.sdk.test.fake

import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.crypto.GCKey
import care.data4life.sdk.crypto.GCKeyPair
import care.data4life.sdk.crypto.KeyType
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.test.util.GenericTestDataProvider.COMMON_KEY_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.IV
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test

class CryptoServiceFakeTest {
    private lateinit var fake: CryptoServiceFake
    private var iteration: CryptoServiceIteration = mockk(relaxed = true)

    @Before
    fun setup() {
        clearAllMocks()
        fake = CryptoServiceFake()

        every { iteration.hashFunction } returns { "not your hash" }
    }

    @Test
    fun `It fulfils CryptoService`() {
        val service: Any = CryptoServiceFake()
        assertTrue(service is CryptoContract.Service)
    }

    @Test
    fun `It sets a new CryptoServiceIteration`() {
        // When
        fake.iteration = iteration

        // Then
        assertSame(
            actual = fake.iteration,
            expected = iteration
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and encrypt is called, it fails, if the key does not match the Iterations dataKey or attachmentKey`() {
        // Given
        val resource = "Just a test"

        every { iteration.resources } returns listOf(resource)

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.encrypt(
                mockk(),
                resource.toByteArray()
            )
        }

        assertTrue(error.message!!.startsWith("Unable to fake resource encryption"))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and encrypt is called, it fails, if the data does not match the Iterations resources`() {
        // Given
        val dataKey: GCKey = mockk()

        every { iteration.dataKey } returns dataKey
        every { iteration.resources } returns listOf("Just a test")

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.encrypt(
                dataKey,
                ByteArray(23)
            )
        }

        assertTrue(error.message!!.startsWith("Unable to fake resource encryption"))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and encrypt is called, with a resource and the dataKey, it returns the encrypted resource, while using the hash function of the Iteration`() {
        // Given
        val dataKey: GCKey = mockk()
        val resource = "Just a test"
        val hashedResource = "Jo"

        val hash = { _: String -> hashedResource }

        every { iteration.dataKey } returns dataKey
        every { iteration.resources } returns listOf(resource)
        every { iteration.hashFunction } returns hash

        // When
        fake.iteration = iteration

        val actual = fake.encrypt(
            dataKey,
            resource.toByteArray()
        ).blockingGet()

        // Then
        assertTrue(actual!!.contentEquals(hashedResource.toByteArray()))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and encrypt is called, with a resource and the attachmentKey, it returns the encrypted resource, while using the hash function of the Iteration`() {
        // Given
        val attachmentKey: GCKey = mockk()
        val resource = "Just a test"
        val hashedResource = "Jo"

        val hash = { _: String -> hashedResource }

        every { iteration.dataKey } returns mockk()
        every { iteration.attachmentKey } returns attachmentKey
        every { iteration.resources } returns listOf(resource)
        every { iteration.hashFunction } returns hash

        // When
        fake.iteration = iteration

        val actual = fake.encrypt(
            attachmentKey,
            resource.toByteArray()
        ).blockingGet()

        // Then
        assertTrue(actual!!.contentEquals(hashedResource.toByteArray()))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and decrypt is called, it fails, if the key does not match the Iterations dataKey or attachmentKey`() {
        // Given
        val resource = "Just a test"

        every { iteration.resources } returns listOf(resource)

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.decrypt(
                mockk(),
                resource.toByteArray()
            )
        }

        assertTrue(error.message!!.startsWith("Unable to fake resource decryption"))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and decrypt is called, it fails, if the data does not match the hashed Iterations resource`() {
        // Given
        val dataKey: GCKey = mockk()
        val resource = "Just a test"

        val hash = { _: String -> "something" }

        every { iteration.dataKey } returns dataKey
        every { iteration.resources } returns listOf(resource)
        every { iteration.hashFunction } returns hash

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.decrypt(
                dataKey,
                ByteArray(23)
            )
        }

        assertTrue(error.message!!.startsWith("Unable to fake resource decryption"))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and decrypt is called, with the dataKey and the encrypted resource, it returns the Iterations resource`() {
        // Given
        val dataKey: GCKey = mockk()
        val resource = "Just a test"
        val hashedResource = "Jo"

        val hash = { _: String -> hashedResource }

        every { iteration.dataKey } returns dataKey
        every { iteration.resources } returns listOf(resource)
        every { iteration.hashFunction } returns hash

        // When
        fake.iteration = iteration

        val actual = fake.decrypt(
            dataKey,
            hashedResource.toByteArray()
        ).blockingGet()

        // Then
        assertTrue(actual!!.contentEquals(resource.toByteArray()))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and decrypt is called, with the attachmentKey and the encrypted resource, it returns the Iterations resource`() {
        // Given
        val attachmentKey: GCKey = mockk()
        val resource = "Just a test"
        val hashedResource = "Jo"

        val hash = { _: String -> hashedResource }

        every { iteration.dataKey } returns mockk()
        every { iteration.attachmentKey } returns attachmentKey
        every { iteration.resources } returns listOf(resource)
        every { iteration.hashFunction } returns hash

        // When
        fake.iteration = iteration

        val actual = fake.decrypt(
            attachmentKey,
            hashedResource.toByteArray()
        ).blockingGet()

        // Then
        assertTrue(actual!!.contentEquals(resource.toByteArray()))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and encryptAndEncodeString is called, it fails, if the key does not match the Iterations dataKey`() {
        // Given
        val resource = "Just a test"

        every { iteration.resources } returns listOf(resource)

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.encryptAndEncodeString(
                mockk(),
                resource
            )
        }

        assertTrue(error.message!!.startsWith("Unable to fake resource encryptAndEncodeString"))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and encryptAndEncodeString is called, it fails, if the data does not match the Iterations resource`() {
        // Given
        val dataKey: GCKey = mockk()

        every { iteration.dataKey } returns dataKey
        every { iteration.resources } returns listOf("Just a test")

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.encryptAndEncodeString(
                dataKey,
                "Nope"
            )
        }

        assertTrue(error.message!!.startsWith("Unable to fake resource encryptAndEncodeString"))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and encryptAndEncodeString is called, it returns the encrypted resource, while using the hash function of the Iteration`() {
        // Given
        val dataKey: GCKey = mockk()
        val resource = "Just a test"
        val hashedResource = "Jo"

        val hash = { _: String -> hashedResource }

        every { iteration.dataKey } returns dataKey
        every { iteration.resources } returns listOf(resource)
        every { iteration.hashFunction } returns hash

        // When
        fake.iteration = iteration

        val actual = fake.encryptAndEncodeString(
            dataKey,
            resource
        ).blockingGet()

        // Then
        assertEquals(
            actual = actual,
            expected = hashedResource
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and encryptAndEncodeByteArray is called, it fails, if the key does not match the Iterations dataKey`() {
        // Given
        val resource = "Just a test"

        every { iteration.resources } returns listOf(resource)

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.encryptAndEncodeByteArray(
                mockk(),
                resource.toByteArray()
            )
        }

        assertTrue(error.message!!.startsWith("Unable to fake resource encryptAndEncodeString"))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and encryptAndEncodeByteArray is called, it fails, if the data does not match the Iterations resource`() {
        // Given
        val dataKey: GCKey = mockk()

        every { iteration.dataKey } returns dataKey
        every { iteration.resources } returns listOf("Just a test")

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.encryptAndEncodeByteArray(
                dataKey,
                "Nope".toByteArray()
            )
        }

        assertTrue(error.message!!.startsWith("Unable to fake resource encryptAndEncodeString"))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and encryptAndEncodeByteArray is called, it returns the encrypted resource, while using the hash function of the Iteration`() {
        // Given
        val dataKey: GCKey = mockk()
        val resource = "Just a test"
        val hashedResource = "Jo"

        val hash = { _: String -> hashedResource }

        every { iteration.dataKey } returns dataKey
        every { iteration.resources } returns listOf(resource)
        every { iteration.hashFunction } returns hash

        // When
        fake.iteration = iteration

        val actual = fake.encryptAndEncodeByteArray(
            dataKey,
            resource.toByteArray()
        ).blockingGet()

        // Then
        assertEquals(
            actual = actual,
            expected = hashedResource
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and decodeAndDecryptString is called, it fails, if the key does not match the Iterations dataKey`() {
        // Given
        val resource = "Just a test"

        every { iteration.resources } returns listOf(resource)

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.decodeAndDecryptString(
                mockk(),
                resource
            )
        }

        assertTrue(error.message!!.startsWith("Unable to fake resource decodeAndDecryptString"))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and decodeAndDecryptString is called, it fails, if the data does not match the hashed Iterations resource`() {
        // Given
        val dataKey: GCKey = mockk()
        val resource = "Just a test"

        val hash = { _: String -> "something" }

        every { iteration.dataKey } returns dataKey
        every { iteration.resources } returns listOf(resource)
        every { iteration.hashFunction } returns hash

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.decodeAndDecryptString(
                dataKey,
                "nope"
            )
        }

        assertTrue(error.message!!.startsWith("Unable to fake resource decodeAndDecryptString"))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and decodeAndDecryptString is called, it returns the Iterations resource`() {
        // Given
        val dataKey: GCKey = mockk()
        val resource = "Just a test"
        val hashedResource = "Jo"

        val hash = { _: String -> hashedResource }

        every { iteration.dataKey } returns dataKey
        every { iteration.resources } returns listOf(resource)
        every { iteration.hashFunction } returns hash

        // When
        fake.iteration = iteration

        val actual = fake.decodeAndDecryptString(
            dataKey,
            hashedResource
        ).blockingGet()

        // Then
        assertEquals(
            actual = actual,
            expected = resource
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and decodeAndDecryptByteArray is called, it fails, if the key does not match the Iterations dataKey`() {
        // Given
        val resource = "Just a test"

        every { iteration.resources } returns listOf(resource)

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.decodeAndDecryptByteArray(
                mockk(),
                resource
            )
        }

        assertTrue(error.message!!.startsWith("Unable to fake resource decodeAndDecryptString"))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and decodeAndDecryptByteArray is called, it fails, if the data does not match the hashed Iterations resource`() {
        // Given
        val dataKey: GCKey = mockk()
        val resource = "Just a test"

        val hash = { _: String -> "something" }

        every { iteration.dataKey } returns dataKey
        every { iteration.resources } returns listOf(resource)
        every { iteration.hashFunction } returns hash

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.decodeAndDecryptByteArray(
                dataKey,
                "nope"
            )
        }

        assertTrue(error.message!!.startsWith("Unable to fake resource decodeAndDecryptString"))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and decodeAndDecryptByteArray is called, it returns the Iterations resource`() {
        // Given
        val dataKey: GCKey = mockk()
        val resource = "Just a test"
        val hashedResource = "Jo"

        val hash = { _: String -> hashedResource }

        every { iteration.dataKey } returns dataKey
        every { iteration.resources } returns listOf(resource)
        every { iteration.hashFunction } returns hash

        // When
        fake.iteration = iteration

        val actual = fake.decodeAndDecryptByteArray(
            dataKey,
            hashedResource
        ).blockingGet()

        // Then
        assertTrue(actual.contentEquals(resource.toByteArray()))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and encryptSymmetricKey is called, it fails, if the commonKey does not match`() {
        // Given
        every { iteration.commonKeyIsStored } returns true

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.encryptSymmetricKey(
                mockk(),
                mockk(),
                mockk()
            )
        }

        assertTrue(error.message!!.startsWith("Expected commonKey as parameter in encryptSymmetricKey and got"))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and encryptSymmetricKey is called, it fails, if the gcKey is not the dataKey, while the KeyType is DATA_KEY`() {
        // Given
        val commonKey: GCKey = mockk()
        val dataKey: GCKey = mockk()

        every { iteration.dataKey } returns dataKey
        every { iteration.commonKey } returns commonKey
        every { iteration.commonKeyIsStored } returns true

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.encryptSymmetricKey(
                commonKey,
                KeyType.DATA_KEY,
                mockk()
            )
        }

        assertTrue(error.message!!.startsWith("Unexpected payload for encryptSymmetricKey"))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and encryptSymmetricKey is called, it returns the Iteration encrypted dataKey`() {
        // Given
        val commonKey: GCKey = mockk()
        val dataKey: GCKey = mockk()
        val encryptedDataKey: EncryptedKey = mockk()

        every { iteration.dataKey } returns dataKey
        every { iteration.commonKey } returns commonKey
        every { iteration.encryptedDataKey } returns encryptedDataKey

        // When
        fake.iteration = iteration

        val result = fake.encryptSymmetricKey(
            commonKey,
            KeyType.DATA_KEY,
            dataKey
        ).blockingGet()

        // Then
        assertEquals(
            actual = result,
            expected = encryptedDataKey
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and encryptSymmetricKey is called, it fails, if the gcKey is not the attachmentKey, while the KeyType is ATTACHMENT_KEY`() {
        // Given
        val commonKey: GCKey = mockk()
        val attachmentKey: GCKey = mockk()

        every { iteration.attachmentKey } returns attachmentKey
        every { iteration.commonKey } returns commonKey
        every { iteration.commonKeyIsStored } returns true

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.encryptSymmetricKey(
                commonKey,
                KeyType.ATTACHMENT_KEY,
                mockk()
            )
        }

        assertTrue(error.message!!.startsWith("Unexpected payload for encryptSymmetricKey"))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and encryptSymmetricKey is called, it returns the Iteration encrypted attachmentKey`() {
        // Given
        val commonKey: GCKey = mockk()
        val attachmentKey: GCKey = mockk()
        val encryptedAttachmentKey: EncryptedKey = mockk()

        every { iteration.attachmentKey } returns attachmentKey
        every { iteration.commonKey } returns commonKey
        every { iteration.encryptedAttachmentKey } returns encryptedAttachmentKey

        // When
        fake.iteration = iteration

        val result = fake.encryptSymmetricKey(
            commonKey,
            KeyType.ATTACHMENT_KEY,
            attachmentKey
        ).blockingGet()

        // Then
        assertEquals(
            actual = result,
            expected = encryptedAttachmentKey
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and symDecryptSymmetricKey is called, it fails, if the commonKey does not match`() {
        // Given
        every { iteration.commonKeyIsStored } returns true

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.symDecryptSymmetricKey(
                mockk(),
                mockk()
            )
        }

        assertTrue(error.message!!.startsWith("Expected commonKey as parameter in symDecryptSymmetricKey and got"))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and symDecryptSymmetricKey is called, it fails, if the encryptedKey does not match a known key`() {
        // Given
        val commonKey: GCKey = mockk()
        every { iteration.commonKeyIsStored } returns true

        every { iteration.commonKey } returns commonKey

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.symDecryptSymmetricKey(
                commonKey,
                mockk()
            )
        }

        assertTrue(error.message!!.startsWith("Unexpected payload for symDecryptSymmetricKey"))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and symDecryptSymmetricKey is called, it returns, if the dataKey`() {
        // Given
        val commonKey: GCKey = mockk()
        val dataKey: GCKey = mockk()
        val encryptedKey: EncryptedKey = mockk()

        every { iteration.commonKey } returns commonKey
        every { iteration.dataKey } returns dataKey
        every { iteration.encryptedDataKey } returns encryptedKey
        every { iteration.commonKeyIsStored } returns true

        // When
        fake.iteration = iteration

        val key = fake.symDecryptSymmetricKey(
            commonKey,
            encryptedKey
        ).blockingGet()

        // Then
        assertEquals(
            actual = key,
            expected = dataKey
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and symDecryptSymmetricKey is called, it returns, if the attachmentKey`() {
        // Given
        val commonKey: GCKey = mockk()
        val attachmentKey: GCKey = mockk()
        val encryptedKey: EncryptedKey = mockk()

        every { iteration.commonKeyIsStored } returns true
        every { iteration.commonKey } returns commonKey
        every { iteration.attachmentKey } returns attachmentKey
        every { iteration.encryptedAttachmentKey } returns encryptedKey

        // When
        fake.iteration = iteration

        val key = fake.symDecryptSymmetricKey(
            commonKey,
            encryptedKey
        ).blockingGet()

        // Then
        assertEquals(
            actual = key,
            expected = attachmentKey
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and symEncrypt is called, it fails, if tag or annotation is unknown`() {
        // Given
        val tags: List<String> = emptyList()
        val annotations: Annotations = emptyList()
        val tagEncryptionKey: GCKey = mockk()

        every { iteration.tags } returns tags
        every { iteration.annotations } returns annotations
        every { iteration.tagEncryptionKey } returns tagEncryptionKey

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.symEncrypt(
                tagEncryptionKey,
                "something".toByteArray(),
                IV
            )
        }

        assertTrue(
            error.message!!.startsWith("Unexpected payload for symEncrypt(probably tag/annotation encryption)")
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and symEncrypt is called, it fails, if the tagEncryptionKey does not match`() {
        // Given
        val tag = "a"
        val tags: List<String> = listOf(tag)
        val annotations: Annotations = emptyList()
        val tagEncryptionKey: GCKey = mockk()

        every { iteration.tags } returns tags
        every { iteration.annotations } returns annotations
        every { iteration.tagEncryptionKey } returns tagEncryptionKey

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.symEncrypt(
                mockk(),
                tag.toByteArray(),
                IV
            )
        }

        assertTrue(
            error.message!!.startsWith("Unexpected payload for symEncrypt(probably tag/annotation encryption)")
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and symEncrypt is called, it fails, if the initialisation vector does not match`() {
        // Given
        val tag = "a"
        val tags: List<String> = listOf(tag)
        val annotations: Annotations = emptyList()
        val tagEncryptionKey: GCKey = mockk()

        every { iteration.tags } returns tags
        every { iteration.annotations } returns annotations
        every { iteration.tagEncryptionKey } returns tagEncryptionKey

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.symEncrypt(
                tagEncryptionKey,
                tag.toByteArray(),
                ByteArray(0)
            )
        }

        assertTrue(
            error.message!!.startsWith("Unexpected payload for symEncrypt(probably tag/annotation encryption)")
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and symEncrypt is called, it encrypts tags, while using the Iterations hash function`() {
        // Given
        val tag = "a"
        val tags: List<String> = listOf(tag)
        val annotations: Annotations = emptyList()
        val tagEncryptionKey: GCKey = mockk()
        val hashed = "Jo"
        var hashParameter = ""
        val hashFunction = { parameter: String -> hashed.also { hashParameter = parameter } }

        every { iteration.tags } returns tags
        every { iteration.annotations } returns annotations
        every { iteration.tagEncryptionKey } returns tagEncryptionKey
        every { iteration.hashFunction } returns hashFunction

        // When
        fake.iteration = iteration

        val result = fake.symEncrypt(
            tagEncryptionKey,
            tag.toByteArray(),
            IV
        )

        // Then
        assertTrue(result.contentEquals(hashed.toByteArray()))
        assertEquals(
            actual = hashParameter,
            expected = tag
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and symEncrypt is called, it encrypts annotations, while using the Iterations hash function`() {
        // Given
        val annotation = "a"
        val tags: List<String> = emptyList()
        val annotations: Annotations = listOf(annotation)
        val tagEncryptionKey: GCKey = mockk()
        val hashed = "Jo"
        var hashParameter = ""
        val hashFunction = { parameter: String -> hashed.also { hashParameter = parameter } }

        every { iteration.tags } returns tags
        every { iteration.annotations } returns annotations
        every { iteration.tagEncryptionKey } returns tagEncryptionKey
        every { iteration.hashFunction } returns hashFunction

        // When
        fake.iteration = iteration

        val result = fake.symEncrypt(
            tagEncryptionKey,
            annotation.toByteArray(),
            IV
        )

        // Then
        assertTrue(result.contentEquals(hashed.toByteArray()))
        assertEquals(
            actual = hashParameter,
            expected = annotation
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and symDecrypt is called, it fails, if tag or annotation is unknown`() {
        // Given
        val tags: List<String> = emptyList()
        val annotations: Annotations = emptyList()
        val tagEncryptionKey: GCKey = mockk()

        every { iteration.tags } returns tags
        every { iteration.annotations } returns annotations
        every { iteration.tagEncryptionKey } returns tagEncryptionKey

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.symDecrypt(
                tagEncryptionKey,
                "something".toByteArray(),
                IV
            )
        }

        assertTrue(
            error.message!!.startsWith("Unexpected payload for symDecrypt(probably tag/annotation encryption)")
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and symDecrypt is called, it fails, if the tagEncryptionKey does not match`() {
        // Given
        val tag = "a"
        val tags: List<String> = listOf(tag)
        val annotations: Annotations = emptyList()
        val tagEncryptionKey: GCKey = mockk()

        every { iteration.tags } returns tags
        every { iteration.annotations } returns annotations
        every { iteration.tagEncryptionKey } returns tagEncryptionKey

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.symDecrypt(
                mockk(),
                tag.toByteArray(),
                IV
            )
        }

        assertTrue(
            error.message!!.startsWith("Unexpected payload for symDecrypt(probably tag/annotation encryption)")
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and symDecrypt is called, it fails, if the initialisation vector does not match`() {
        // Given
        val tag = "a"
        val tags: List<String> = listOf(tag)
        val annotations: Annotations = emptyList()
        val tagEncryptionKey: GCKey = mockk()

        every { iteration.tags } returns tags
        every { iteration.annotations } returns annotations
        every { iteration.tagEncryptionKey } returns tagEncryptionKey

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.symDecrypt(
                tagEncryptionKey,
                tag.toByteArray(),
                ByteArray(0)
            )
        }

        assertTrue(
            error.message!!.startsWith("Unexpected payload for symDecrypt(probably tag/annotation encryption)")
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and symDecrypt is called, it decrypts tags`() {
        // Given
        val tag = "a"
        val tags: List<String> = listOf(tag)
        val annotations: Annotations = emptyList()
        val tagEncryptionKey: GCKey = mockk()
        val hashed = "Jo"
        val hashFunction = { _: String -> hashed }

        every { iteration.tags } returns tags
        every { iteration.annotations } returns annotations
        every { iteration.tagEncryptionKey } returns tagEncryptionKey
        every { iteration.hashFunction } returns hashFunction

        // When
        fake.iteration = iteration

        val result = fake.symDecrypt(
            tagEncryptionKey,
            hashed.toByteArray(),
            IV
        )

        // Then
        assertTrue(result.contentEquals(tag.toByteArray()))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and symDecrypt is called, it decrypts annotations`() {
        // Given
        val annotation = "a"
        val tags: List<String> = emptyList()
        val annotations: Annotations = listOf(annotation)
        val tagEncryptionKey: GCKey = mockk()
        val hashed = "Jo"
        val hashFunction = { _: String -> hashed }

        every { iteration.tags } returns tags
        every { iteration.annotations } returns annotations
        every { iteration.tagEncryptionKey } returns tagEncryptionKey
        every { iteration.hashFunction } returns hashFunction

        // When
        fake.iteration = iteration

        val result = fake.symDecrypt(
            tagEncryptionKey,
            hashed.toByteArray(),
            IV
        )

        // Then
        assertTrue(result.contentEquals(annotation.toByteArray()))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and generateGCKey is called, it returns the keys in the preset order util no further order was set`() {
        // Given
        val order: List<GCKey> = listOf(mockk(), mockk(), mockk())

        every { iteration.gcKeyOrder } returns order

        // When
        fake.iteration = iteration

        order.forEach {
            // Then
            assertEquals(
                actual = fake.generateGCKey().blockingGet(),
                expected = it
            )
        }

        assertFails { fake.generateGCKey().blockingGet() }
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and hasCommonKey with the commonKeyId is called, it fails if the commonKeyId is unknown`() {
        // Given
        every { iteration.commonKeyId } returns "something"
        every { iteration.commonKeyIsStored } returns true

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.hasCommonKey("an id")
        }

        assertEquals(
            actual = error.message,
            expected = "Unexpected commonKeyId an id"
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and hasCommonKey with the commonKeyId is called, it returns true if the Iterations commonKeyIsStored is set to true`() {
        // Given
        val commonKeyId = COMMON_KEY_ID

        every { iteration.commonKeyId } returns commonKeyId
        every { iteration.commonKeyIsStored } returns true

        // When
        fake.iteration = iteration

        // Then
        assertTrue(fake.hasCommonKey(commonKeyId))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and hasCommonKey with the commonKeyId is called, it returns false if the Iterations commonKeyIsStored is set to false`() {
        // Given
        val commonKeyId = COMMON_KEY_ID

        every { iteration.commonKeyId } returns commonKeyId
        every { iteration.commonKeyIsStored } returns false

        // When
        fake.iteration = iteration

        // Then
        assertFalse(fake.hasCommonKey(commonKeyId))
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and commonKeyId is called, it returns the commonKeyId`() {
        // Given
        val commonKeyId = COMMON_KEY_ID

        every { iteration.commonKeyIsStored } returns true
        every { iteration.commonKeyId } returns commonKeyId

        // When
        fake.iteration = iteration

        // Then
        assertEquals(
            expected = commonKeyId,
            actual = fake.currentCommonKeyId
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and commonKeyId is called, it fails if commonKeyIsStored is false`() {
        // Given
        val commonKeyId = COMMON_KEY_ID

        every { iteration.commonKeyIsStored } returns false
        every { iteration.commonKeyId } returns commonKeyId

        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration

            fake.currentCommonKeyId
        }

        // Then
        assertEquals(
            expected = error.message,
            actual = "You need to init the commonKey first."
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and fetchGCKeyPair called, it returns a unique KeyPair`() {
        // When
        fake.iteration = iteration
        val keyPair1: Any = fake.fetchGCKeyPair().blockingGet()
        val keyPair2: Any = fake.fetchGCKeyPair().blockingGet()

        // Then
        assertTrue(keyPair1 is GCKeyPair)
        assertNotEquals(
            keyPair1,
            keyPair2
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set, fetchGCKeyPair and asymDecryptSymetricKey is called, it fails, if the KeyPair does not match the fetched Pair`() {
        // Given
        val commonEncryptionKey: EncryptedKey = mockk()

        every { iteration.encryptedCommonKey } returns commonEncryptionKey

        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration
            fake.fetchGCKeyPair().blockingGet()
            fake.asymDecryptSymetricKey(
                mockk(),
                commonEncryptionKey
            )
        }

        assertEquals(
            actual = error.message,
            expected = "Something is not in order, you need to fetch the a GCKeyPair first."
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set, fetchGCKeyPair and asymDecryptSymetricKey is called, it fails, if the encrypted commonKey is null`() {
        // Given
        every { iteration.encryptedCommonKey } returns null

        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration
            val pair = fake.fetchGCKeyPair().blockingGet()
            fake.asymDecryptSymetricKey(
                pair,
                mockk()
            )
        }

        assertEquals(
            actual = error.message,
            expected = "You did not provided a encrypted CommonKey."
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set, fetchGCKeyPair and asymDecryptSymetricKey is called, it returns the commonKey`() {
        // Given
        val commonEncryptionKey: EncryptedKey = mockk()
        val commonKey: GCKey = mockk()

        every { iteration.encryptedCommonKey } returns commonEncryptionKey
        every { iteration.commonKey } returns commonKey

        // When
        fake.iteration = iteration
        val pair = fake.fetchGCKeyPair().blockingGet()
        val key = fake.asymDecryptSymetricKey(
            pair,
            commonEncryptionKey
        ).blockingGet()

        // Then
        assertEquals(
            expected = commonKey,
            actual = key
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set, fetchGCKeyPair and asymDecryptSymetricKey is called, while it accepts, it unlocks the GCKeyPair`() {
        // Given
        val commonEncryptionKey: EncryptedKey = mockk()
        val commonKey: GCKey = mockk()

        every { iteration.encryptedCommonKey } returns commonEncryptionKey
        every { iteration.commonKey } returns commonKey

        // When
        fake.iteration = iteration
        val pair = fake.fetchGCKeyPair().blockingGet()
        val key = fake.asymDecryptSymetricKey(
            pair,
            commonEncryptionKey
        ).blockingGet()

        // Then
        assertEquals(
            expected = commonKey,
            actual = key
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and storeCommonKey is called, it fails, if id does not match commonKeyId`() {
        // Given
        val commonKeyId = COMMON_KEY_ID
        val commonKey: GCKey = mockk()

        every { iteration.commonKeyId } returns commonKeyId
        every { iteration.commonKey } returns commonKey

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration
            fake.storeCommonKey("not the id you are looking for", commonKey)
        }

        assertEquals(
            expected = "The given commonKeyId, which was meant to be stored, is unexpected.",
            actual = error.message
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and storeCommonKey is called, it fails, if key does not match commonKey`() {
        // Given
        val commonKeyId = COMMON_KEY_ID
        val commonKey: GCKey = mockk()

        every { iteration.commonKeyId } returns commonKeyId
        every { iteration.commonKey } returns commonKey

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration
            fake.storeCommonKey(commonKeyId, mockk())
        }

        assertEquals(
            expected = "The given commonKey, which was meant to be stored, is unexpected.",
            actual = error.message
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and storeCommonKey is called, it accepts and unlocks the currentCommonKeyId`() {
        // Given
        val commonKeyId = COMMON_KEY_ID
        val commonKey: GCKey = mockk()

        every { iteration.commonKeyId } returns commonKeyId
        every { iteration.commonKey } returns commonKey

        // When
        fake.iteration = iteration
        fake.storeCommonKey(commonKeyId, commonKey)

        // Then

        assertEquals(
            expected = commonKeyId,
            actual = fake.currentCommonKeyId
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and fetchTagEncryptionKey is called, it returns the Iterations TagEncryptionKey`() {
        // Given
        val tagEncryptionKey: GCKey = mockk()

        every { iteration.tagEncryptionKey } returns tagEncryptionKey
        every { iteration.tagEncryptionKeyCalls } returns 1

        // When
        fake.iteration = iteration
        val key = fake.fetchTagEncryptionKey()

        // Then

        assertEquals(
            expected = key,
            actual = tagEncryptionKey
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and fetchTagEncryptionKey is called, it fails if the Iterations TagEncryptionKeyCalls had been exceeded`() {
        // Given
        val tagEncryptionKey: GCKey = mockk()

        every { iteration.tagEncryptionKey } returns tagEncryptionKey
        every { iteration.tagEncryptionKeyCalls } returns 1

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration
            fake.fetchTagEncryptionKey()
            fake.fetchTagEncryptionKey()
        }

        assertEquals(
            expected = error.message,
            actual = "The fetchTagEncryptionKey exceeds its given limit."
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and fetchCurrentCommonKey is called, it returns the Iterations commonKey`() {
        // Given
        val commonKey: GCKey = mockk()

        every { iteration.commonKey } returns commonKey
        every { iteration.commonKeyFetchCalls } returns 1

        // When
        fake.iteration = iteration
        val key = fake.fetchCurrentCommonKey()

        // Then
        assertEquals(
            expected = key,
            actual = commonKey
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and fetchCurrentCommonKey is called, it unlocks the currentCommonKeyId`() {
        // Given
        val commonKey: GCKey = mockk()
        val commonKeyId: String = COMMON_KEY_ID

        every { iteration.commonKeyId } returns commonKeyId
        every { iteration.commonKey } returns commonKey
        every { iteration.commonKeyFetchCalls } returns 1

        // When
        fake.iteration = iteration
        fake.fetchCurrentCommonKey()

        // Then
        assertEquals(
            expected = commonKeyId,
            actual = fake.currentCommonKeyId
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and fetchCurrentCommonKey is called, it fails if the Iterations CommonKeyFetchCalls had been exceeded`() {
        // Given
        val commonKey: GCKey = mockk()

        every { iteration.commonKey } returns commonKey
        every { iteration.commonKeyFetchCalls } returns 1

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration
            fake.fetchCurrentCommonKey()
            fake.fetchCurrentCommonKey()
        }

        assertEquals(
            expected = error.message,
            actual = "The fetchCurrentCommonKey exceeds its given limit."
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and getCommonKeyById is called, it fails if the Iterations CommonKeyId does not match`() {
        // Given
        val commonKey: GCKey = mockk()
        val commonKeyId: String = COMMON_KEY_ID

        every { iteration.commonKeyId } returns commonKeyId
        every { iteration.commonKey } returns commonKey

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            fake.iteration = iteration
            fake.getCommonKeyById("not your id")
        }

        assertEquals(
            expected = error.message,
            actual = "The given commonKeyId (not your id) was not expected in getCommonKeyById."
        )
    }

    @Test
    fun `Given, a CryptoServiceIteration is set and getCommonKeyById is called, it returns the Iterations CommonKey`() {
        // Given
        val commonKey: GCKey = mockk()
        val commonKeyId: String = COMMON_KEY_ID

        every { iteration.commonKeyId } returns commonKeyId
        every { iteration.commonKey } returns commonKey

        // When
        fake.iteration = iteration
        val key = fake.getCommonKeyById(commonKeyId)

        // Then
        assertEquals(
            expected = commonKey,
            actual = key
        )
    }
}
