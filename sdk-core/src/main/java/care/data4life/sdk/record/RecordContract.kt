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

package care.data4life.sdk.record

import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.model.Record
import io.reactivex.Completable
import io.reactivex.Single
import org.threeten.bp.LocalDate

interface RecordContract {

    interface Service {
        fun createRecord(userId: String, resource: DataResource, annotations: List<String>): Single<DataRecord<DataResource>>
        fun <T : Fhir3Resource> createRecord(userId: String, resource: T, annotations: List<String>): Single<Record<T>>
        fun <T : Fhir4Resource> createRecord(userId: String, resource: T, annotations: List<String>): Single<Fhir4Record<T>>

        fun updateRecord(userId: String, recordId: String, resource: DataResource, annotations: List<String>): Single<DataRecord<DataResource>>
        fun <T : Fhir3Resource> updateRecord(userId: String, recordId: String, resource: T, annotations: List<String>): Single<Record<T>>
        fun <T : Fhir4Resource> updateRecord(userId: String, recordId: String, resource: T, annotations: List<String>): Single<Fhir4Record<T>>

        fun deleteRecord(userId: String, recordId: String): Completable

        fun fetchDataRecord(userId: String, recordId: String): Single<DataRecord<DataResource>>
        fun <T : Fhir3Resource> fetchFhir3Record(userId: String, recordId: String): Single<Record<T>>
        fun <T : Fhir4Resource> fetchFhir4Record(userId: String, recordId: String): Single<Fhir4Record<T>>

        fun fetchDataRecords(userId: String, annotations: List<String>, startDate: LocalDate?, endDate: LocalDate?, pageSize: Int, offset: Int): Single<List<DataRecord<DataResource>>>
        fun <T : Fhir3Resource> fetchFhir3Records(userId: String, resourceType: Class<T>, annotations: List<String>, startDate: LocalDate?, endDate: LocalDate?, pageSize: Int, offset: Int): Single<List<Record<T>>>
        fun <T : Fhir4Resource> fetchFhir4Records(userId: String, resourceType: Class<T>, annotations: List<String>, startDate: LocalDate?, endDate: LocalDate?, pageSize: Int, offset: Int): Single<List<Fhir4Record<T>>>

        fun countFhir3Records(type: Class<out Fhir3Resource>, userId: String, annotations: List<String>): Single<Int>

        fun countAllFhir3Records(userId: String, annotations: List<String>): Single<Int>
    }
}
