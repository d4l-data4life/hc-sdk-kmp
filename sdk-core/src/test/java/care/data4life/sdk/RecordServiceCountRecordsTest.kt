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

import care.data4life.fhir.stu3.model.CarePlan
import com.google.common.truth.Truth
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.io.IOException

class RecordServiceCountRecordsTest: RecordServiceTestBase() {
    @Before
    fun setup() {
        init()
    }

    @Test
    @Throws(InterruptedException::class)
    fun countRecords_shouldReturnRecordsCount() {
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
    fun countRecordsForType_shouldReturnRecordsCount() {
        // Given
        Mockito.`when`(mockTaggingService.getTagFromType(CarePlan.resourceType)).thenReturn(mockTags)
        Mockito.`when`(mockTagEncryptionService.encryptTags(mockTags)).thenReturn(mockEncryptedTags)
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
        inOrder.verify(mockApiService).getCount(ALIAS, USER_ID, mockEncryptedTags)
        inOrder.verifyNoMoreInteractions()
    }
}
