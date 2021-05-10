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

import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.model.NetworkModelContract
import care.data4life.sdk.network.model.NetworkModelContract.DecryptedBaseRecord
import care.data4life.sdk.network.model.RecordCryptoService
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.PARTNER_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.spyk
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertSame

class RecordServiceCryptoTest {
    private lateinit var recordService: RecordService
    private val apiService: NetworkingContract.Service = mockk()
    private val cryptoService: CryptoContract.Service = mockk()
    private val resourceCryptoService: FhirContract.CryptoService = mockk()
    private val tagCryptoService: TaggingContract.CryptoService = mockk()
    private val taggingService: TaggingContract.Service = mockk()
    private val attachmentService: AttachmentContract.Service = mockk()
    private val errorHandler: SdkContract.ErrorHandler = mockk()

    @Before
    fun setUp() {
        clearAllMocks()
        mockkConstructor(RecordCryptoService::class)

        recordService = spyk(
            RecordService(
                PARTNER_ID,
                ALIAS,
                apiService,
                tagCryptoService,
                taggingService,
                resourceCryptoService,
                attachmentService,
                cryptoService,
                errorHandler,
                mockk()
            )
        )
    }

    @After
    fun tearDown() {
        unmockkConstructor(RecordCryptoService::class)
    }

    @Test
    fun `Given, fromResource is called with a Resource and Annotations it delegates it to the ResourceEncryptionService`() {
        // Given
        val resource: Any = mockk()
        val annotations: Annotations = mockk()
        val expected: DecryptedBaseRecord<Any> = mockk()

        every {
            anyConstructed<RecordCryptoService>().fromResource(
                resource,
                annotations
            )
        } returns expected

        // When
        val actual = recordService.fromResource(resource, annotations).blockingGet()

        // Then
        assertSame(
            actual = actual,
            expected = expected
        )

        verify(exactly = 1) {
            anyConstructed<RecordCryptoService>().fromResource(
                resource,
                annotations
            )
        }
    }

    @Test
    fun `Given, encryptRecord is called with a DecryptedRecord it delegates it to the ResourceEncryptionService`() {
        // Given
        val record: DecryptedBaseRecord<Any> = mockk()
        val expected: NetworkModelContract.EncryptedRecord = mockk()

        every {
            anyConstructed<RecordCryptoService>().encrypt(record)
        } returns expected

        // When
        val actual = recordService.encryptRecord(record)

        // Then
        assertSame(
            actual = actual,
            expected = expected
        )

        verify(exactly = 1) {
            anyConstructed<RecordCryptoService>().encrypt(record)
        }
    }

    @Test
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, delegates it to the ResourceEncryptionService`() {
        // Given
        val expected: DecryptedBaseRecord<Any> = mockk()
        val record: NetworkModelContract.EncryptedRecord = mockk()

        every {
            anyConstructed<RecordCryptoService>().decrypt<Any>(record, USER_ID)
        } returns expected

        // When
        val actual = recordService.decryptRecord<Any>(record, USER_ID)

        // Then
        assertSame(
            actual = actual,
            expected = expected
        )

        verify(exactly = 1) {
            anyConstructed<RecordCryptoService>().decrypt<Any>(record, USER_ID)
        }
    }
}
