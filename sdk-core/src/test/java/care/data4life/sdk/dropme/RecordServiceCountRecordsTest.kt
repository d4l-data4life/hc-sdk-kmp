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

package care.data4life.sdk.dropme

import care.data4life.fhir.stu3.model.CarePlan
import com.google.common.truth.Truth
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.io.IOException
/*
class RecordServiceCountRecordsTest : RecordServiceTestBase() {
    @Before
    fun setup() {
        init()
    }

    @After
    fun tearDown() {
        stop()
    }

    @Test
    @Throws(InterruptedException::class)
    fun `Given, countRecords is called with a DomainResource and a UserId, it returns amount of occurrences`() {
        // Given
        Mockito.`when`(mockApiService.getCount(ALIAS, USER_ID, null)).thenReturn(Single.just(2))
        // When
        val observer = recordService.countRecords(null, USER_ID).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result).isEqualTo(2)
        inOrder.verify(mockApiService).getCount(ALIAS, USER_ID, null)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class, IOException::class)
    fun `Given, countRecords is called with a DomainResource, a UserId and a Tag, it returns amount of occurrences`() {
        // Given
        Mockito.`when`(mockTaggingService.getTagFromType(CarePlan.resourceType)).thenReturn(mockTags)
        Mockito.`when`(mockTagEncryptionService.encryptTags(mockTags)).thenReturn(mockEncryptedTags)
        Mockito.`when`(
                mockTagEncryptionService.encryptAnnotations(ArgumentMatchers.anyList())
        ).thenReturn(mockEncryptedAnnotations)
        Mockito.`when`(mockApiService.getCount(ALIAS, USER_ID, mockEncryptedTags)).thenReturn(Single.just(2))

        // When
        val observer = recordService.countRecords(CarePlan::class.java, USER_ID).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result).isEqualTo(2)
        inOrder.verify(mockTaggingService).getTagFromType(CarePlan.resourceType)
        inOrder.verify(mockTagEncryptionService).encryptTags(mockTags)
        inOrder.verify(mockTagEncryptionService).encryptAnnotations(ArgumentMatchers.anyList())
        inOrder.verify(mockApiService).getCount(ALIAS, USER_ID, mockEncryptedTags)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class)
    fun `Given, countRecords is called with a DomainResource, a UserId and Annotations, it returns amount of occurrences`() {
        // Given
        Mockito.`when`(mockApiService.getCount(ALIAS, USER_ID, null)).thenReturn(Single.just(2))

        // When
        val observer = recordService.countRecords(null, USER_ID, ANNOTATIONS).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result).isEqualTo(2)
        inOrder.verify(mockApiService).getCount(ALIAS, USER_ID, null)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class, IOException::class)
    fun `Given, countRecords is called with a DomainResource, a UserId, a Tag and Annotations, it returns amount of occurrences`() {
        // Given
        Mockito.`when`(mockTaggingService.getTagFromType(CarePlan.resourceType)).thenReturn(mockTags)
        Mockito.`when`(mockTagEncryptionService.encryptTags(mockTags)).thenReturn(mockEncryptedTags)
        Mockito.`when`(
                mockTagEncryptionService.encryptAnnotations(ANNOTATIONS)
        ).thenReturn(mockEncryptedAnnotations)
        Mockito.`when`(mockApiService.getCount(ALIAS, USER_ID, mockEncryptedTags)).thenReturn(Single.just(2))

        // When
        val observer = recordService.countRecords(CarePlan::class.java, USER_ID, ANNOTATIONS).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result).isEqualTo(2)
        inOrder.verify(mockTaggingService).getTagFromType(CarePlan.resourceType)
        inOrder.verify(mockTagEncryptionService).encryptTags(mockTags)
        inOrder.verify(mockTagEncryptionService).encryptAnnotations(ANNOTATIONS)
        inOrder.verify(mockApiService).getCount(ALIAS, USER_ID, mockEncryptedTags)
        inOrder.verifyNoMoreInteractions()
    }
}

 */
