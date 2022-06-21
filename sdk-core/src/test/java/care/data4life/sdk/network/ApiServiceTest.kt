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

package care.data4life.sdk.network

import care.data4life.sdk.auth.AuthorizationContract
import care.data4life.sdk.lang.D4LRuntimeException
import care.data4life.sdk.network.NetworkingContract.Companion.MEDIA_TYPE_OCTET_STREAM
import care.data4life.sdk.network.NetworkingContract.Companion.PARAM_TAG_ENCRYPTION_KEY
import care.data4life.sdk.network.model.CommonKeyResponse
import care.data4life.sdk.network.model.DocumentUploadResponse
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.UserInfo
import care.data4life.sdk.network.model.VersionList
import care.data4life.sdk.network.util.ClientFactory
import care.data4life.sdk.network.util.HealthCloudApiFactory
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.CLIENT_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.COMMON_KEY_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.RECORD_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ApiServiceTest {
    private lateinit var service: NetworkingContract.Service
    private val authService: AuthorizationContract.Service = mockk()
    private val env: NetworkingContract.Environment = mockk(relaxed = true)
    private val healthCloudApi: HealthCloudApi = mockk()
    private val client: OkHttpClient = mockk()
    private val clientId = CLIENT_ID
    private val secret = "geheim"
    private val platform = "not important"
    private val version = "does not matter"
    private val connection: NetworkingContract.NetworkConnectivityService = mockk()

    @Before
    fun setup() {
        clearAllMocks()

        mockkObject(HealthCloudApiFactory)
        mockkObject(ClientFactory)

        every { HealthCloudApiFactory.getInstance(client, platform, env) } returns healthCloudApi
        every {
            ClientFactory.getInstanceLegacy(
                authService,
                env,
                clientId,
                secret,
                platform,
                connection,
                NetworkingContract.Client.JAVA,
                version,
                any(),
                false
            )
        } returns client

        service = ApiService(
            authService,
            env,
            clientId,
            secret,
            platform,
            connection,
            NetworkingContract.Client.JAVA,
            version,
            debug = false
        )
    }

    @After
    fun tearDown() {
        unmockkObject(HealthCloudApiFactory)
        unmockkObject(ClientFactory)
    }

    @Test
    fun `It fulfils NetworkingContractService`() {

        val service: Any = ApiService(
            authService,
            env,
            clientId,
            secret,
            platform,
            connection,
            NetworkingContract.Client.JAVA,
            version,
            debug = false
        )

        assertTrue(service is NetworkingContract.Service)
    }

    // keys
    @Test
    fun `Given, fetchCommonKey is called with an Alias, UserId and a CommonKeyId, it delegates it to the HealthCloudApi and returns its result`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val commonKeyId = COMMON_KEY_ID
        val result: Single<CommonKeyResponse> = mockk()

        every { healthCloudApi.fetchCommonKey(alias, userId, commonKeyId) } returns result

        // When
        val actual = service.fetchCommonKey(alias, userId, commonKeyId)

        // Then
        assertSame(
            actual = actual,
            expected = result
        )

        verify(exactly = 1) {
            healthCloudApi.fetchCommonKey(alias, userId, commonKeyId)
        }
    }

    @Test
    fun `Given, uploadTagEncryptionKey is called with an Alias, UserId and a EncryptedKey, it delegates it to the HealthCloudApi and returns its result`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val encryptedKey = "key"
        val result: Completable = mockk()

        val mappedKey = mapOf(
            PARAM_TAG_ENCRYPTION_KEY to encryptedKey
        )

        every { healthCloudApi.uploadTagEncryptionKey(alias, userId, mappedKey) } returns result

        // When
        val actual = service.uploadTagEncryptionKey(alias, userId, encryptedKey)

        // Then
        assertSame(
            actual = actual,
            expected = result
        )

        verify(exactly = 1) {
            healthCloudApi.uploadTagEncryptionKey(alias, userId, mappedKey)
        }
    }

    // records
    @Test
    fun `Given, createRecord is called with an Alias, UserId and a EncryptedRecord, it delegates it to the HealthCloudApi and returns its result`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val record: EncryptedRecord = mockk()
        val result: Single<EncryptedRecord> = mockk()

        every { healthCloudApi.createRecord(alias, userId, record) } returns result

        // When
        val actual = service.createRecord(alias, userId, record)

        // Then
        assertSame(
            actual = actual,
            expected = result
        )

        verify(exactly = 1) {
            healthCloudApi.createRecord(alias, userId, record)
        }
    }

    @Test
    fun `Given, updateRecord is called with an Alias, UserId, RecordId and a EncryptedRecord, it delegates it to the HealthCloudApi and returns its result`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val recordId = RECORD_ID
        val record: EncryptedRecord = mockk()
        val result: Single<EncryptedRecord> = mockk()

        every { healthCloudApi.updateRecord(alias, userId, recordId, record) } returns result

        // When
        val actual = service.updateRecord(alias, userId, recordId, record)

        // Then
        assertSame(
            actual = actual,
            expected = result
        )

        verify(exactly = 1) {
            healthCloudApi.updateRecord(alias, userId, recordId, record)
        }
    }

    @Test
    fun `Given, fetchRecord is called with an Alias, UserId and a RecordId, it delegates it to the HealthCloudApi and returns its result`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val recordId = RECORD_ID
        val result: Single<EncryptedRecord> = mockk()

        every { healthCloudApi.fetchRecord(alias, userId, recordId) } returns result

        // When
        val actual = service.fetchRecord(alias, userId, recordId)

        // Then
        assertSame(
            actual = actual,
            expected = result
        )

        verify(exactly = 1) {
            healthCloudApi.fetchRecord(alias, userId, recordId)
        }
    }

    @Test
    fun `Given, searchRecords is called with its appropriate parameter, it delegates it to the HealthCloudApi and returns its result`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val startDate = "somewhen"
        val endDate = "somewhen else"
        val startUpdateDate = "range start"
        val endUpdateDate = "range end"
        val deletionFlag = true
        val pageSize = 23
        val offset = 42
        val formattedTags = "tags"
        val tags: NetworkingContract.SearchTags = mockk()
        val result: Observable<List<EncryptedRecord>> = mockk()

        every {
            healthCloudApi.searchRecords(
                alias,
                userId,
                startDate,
                endDate,
                startUpdateDate,
                endUpdateDate,
                deletionFlag,
                pageSize,
                offset,
                formattedTags
            )
        } returns result

        every { tags.tagGroups } returns formattedTags

        // When
        val actual = service.searchRecords(
            alias,
            userId,
            startDate,
            endDate,
            startUpdateDate,
            endUpdateDate,
            deletionFlag,
            pageSize,
            offset,
            tags
        )

        // Then
        assertSame(
            actual = actual,
            expected = result
        )

        verify(exactly = 1) {
            healthCloudApi.searchRecords(
                alias,
                userId,
                startDate,
                endDate,
                startUpdateDate,
                endUpdateDate,
                deletionFlag,
                pageSize,
                offset,
                formattedTags
            )
        }

        verify(exactly = 1) { tags.tagGroups }
    }

    @Test
    fun `Given, countRecords is called with an Alias, UserId and Tags, it delegates it to the HealthCloudApi, parses the result and returns it`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val formattedTags = "tags"
        val tags: NetworkingContract.SearchTags = mockk()
        val amount = "23"
        val response: Response<Void> = mockk()
        val headers: Headers = mockk()

        every { tags.tagGroups } returns formattedTags
        every { healthCloudApi.getRecordsHeader(alias, userId, formattedTags) } returns Single.just(
            response
        )
        every { response.headers() } returns headers
        every { headers[NetworkingContract.HEADER_TOTAL_COUNT] } returns amount

        // When
        val actual = service.countRecords(alias, userId, tags).blockingGet()

        // Then
        assertSame(
            actual = actual,
            expected = 23
        )

        verify(exactly = 1) {
            healthCloudApi.getRecordsHeader(alias, userId, formattedTags)
        }
        verify(exactly = 1) { tags.tagGroups }
    }

    @Test
    fun `Given, deleteRecord is called with an Alias, UserId and a RecordId, it delegates it to the HealthCloudApi and returns its result`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val recordId = RECORD_ID
        val result: Completable = mockk()

        every { healthCloudApi.deleteRecord(alias, userId, recordId) } returns result

        // When
        val actual = service.deleteRecord(alias, userId, recordId)

        // Then
        assertSame(
            actual = actual,
            expected = result
        )

        verify(exactly = 1) {
            healthCloudApi.deleteRecord(alias, userId, recordId)
        }
    }

    // attachments
    @Test
    fun `Given, uploadDocument is called with an Alias, UserId and a data payload, while building a RequestBody out of it, it delegates it to the HealthCloudApi and returns the DocumentID`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val payload = ByteArray(12)
        val request = payload.toRequestBody(MEDIA_TYPE_OCTET_STREAM.toMediaType())
        val response: DocumentUploadResponse = mockk()
        val result = "Done"

        val slot = slot<RequestBody>()

        every { healthCloudApi.uploadDocument(alias, userId, capture(slot)) } answers {
            if (slot.captured.contentType() == request.contentType() &&
                // This hard to test, we can apply only a weak indicator
                slot.captured.contentLength() == request.contentLength()
            ) {
                Single.just(response)
            } else {
                throw RuntimeException("uploadDocument has differing RequestBodies")
            }
        }

        every { response.documentId } returns result

        // When
        val actual = service.uploadDocument(alias, userId, payload).blockingGet()

        // Then
        assertSame(
            actual = actual,
            expected = result
        )

        verify(exactly = 1) {
            healthCloudApi.uploadDocument(alias, userId, any())
        }
    }

    @Test
    fun `Given, downloadDocument is called with an Alias, UserId and a DocumentId, it delegates it to the HealthCloudApi and returns the Resonses Datablob`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val documentId = "doc"
        val response: ResponseBody = mockk()
        val document = ByteArray(23)

        every { healthCloudApi.downloadDocument(alias, userId, documentId) } returns Single.just(
            response
        )
        every { response.bytes() } returns document

        // When
        val actual = service.downloadDocument(alias, userId, documentId).blockingGet()

        // Then
        assertSame(
            actual = actual,
            expected = document
        )

        verify(exactly = 1) {
            healthCloudApi.downloadDocument(alias, userId, documentId)
        }
    }

    @Test
    fun `Given, deleteDocument is called with an Alias, UserId and a DocumentId, it delegates it to the HealthCloudApi and returns always true`() {
        // Given
        val alias = ALIAS
        val userId = USER_ID
        val documentId = "doc"

        every { healthCloudApi.deleteDocument(alias, userId, documentId) } returns Single.just(mockk())

        // When
        val actual = service.deleteDocument(alias, userId, documentId).blockingGet()

        // Then
        assertTrue(actual)

        verify(exactly = 1) {
            healthCloudApi.deleteDocument(alias, userId, documentId)
        }
    }

    // Mixed
    @Test
    fun `Given, fetchUserInfo is called with an Alias, it delegates it to the HealthCloudApi, subscripts on the Schedulers returns the result`() {
        // Given
        val alias = ALIAS
        val response: Single<UserInfo> = mockk()

        every { healthCloudApi.fetchUserInfo(alias) } returns response
        every { response.subscribeOn(Schedulers.io()) } returns response

        // When
        val actual = service.fetchUserInfo(alias)

        // Then
        assertSame(
            actual = actual,
            expected = response
        )

        verifyOrder {
            healthCloudApi.fetchUserInfo(alias)
            response.subscribeOn(Schedulers.io())
        }
    }

    @Test
    fun `Given, fetchVersionInfo is called, it delegates it to the HealthCloudApi, subscripts on the Schedulers returns the result`() {
        // Given
        val response: Single<VersionList> = mockk()

        every { healthCloudApi.fetchVersionInfo() } returns response
        every { response.subscribeOn(Schedulers.io()) } returns response

        // When
        val actual = service.fetchVersionInfo()

        // Then
        assertSame(
            actual = actual,
            expected = response
        )

        verifyOrder {
            healthCloudApi.fetchVersionInfo()
            response.subscribeOn(Schedulers.io())
        }
    }

    @Test
    fun `Given, logout is called with an Alias, it fails if service was initialized with a static token`() {
        // Given
        val service = ApiService(
            authService,
            env,
            clientId,
            secret,
            platform,
            connection,
            NetworkingContract.Client.JAVA,
            version,
            "token".toByteArray(),
            false
        )
        val alias = ALIAS

        // Then
        val error = assertFailsWith<D4LRuntimeException> {
            // When
            service.logout(alias)
        }

        assertEquals(
            actual = error.message,
            expected = "Cannot log out when using a static access token!"
        )
    }

    @Test
    fun `Given, logout is called with an Alias, it resolves the accessToken, delegates it with the Alias to the HealthCloudApi and returns its result`() {
        // Given
        val alias = ALIAS
        val token = "token"
        val result = "ignore me"

        every { authService.getRefreshToken(alias) } returns token
        every { healthCloudApi.logout(alias, token) } returns Completable.fromCallable { result }

        // When
        val actual = service.logout(alias).blockingGet()

        // Then
        assertNull(actual)

        verify(exactly = 1) { healthCloudApi.logout(alias, token) }
    }
}
