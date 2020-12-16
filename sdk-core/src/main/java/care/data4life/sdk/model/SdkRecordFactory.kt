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

package care.data4life.sdk.model

import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.model.definitions.BaseRecord
import care.data4life.sdk.model.definitions.RecordFactory
import care.data4life.sdk.network.model.NetworkRecordContract
import care.data4life.sdk.wrapper.WrapperContract
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder
import java.util.*

internal object SdkRecordFactory : RecordFactory {
    private const val DATE_FORMAT = "yyyy-MM-dd"
    private const val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss[.SSS]"
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)
    private val DATE_TIME_FORMATTER = DateTimeFormatterBuilder()
            .parseLenient()
            .appendPattern(DATE_TIME_FORMAT)
            .toFormatter(Locale.US)

    @Throws(CoreRuntimeException.InternalFailure::class)
    override fun getInstance(record: NetworkRecordContract.DecryptedRecord): BaseRecord<Any> {
        @Suppress("UNCHECKED_CAST")
        return when (record.resource.type) {
            WrapperContract.Resource.TYPE.FHIR3 -> Record(
                    record.resource.unwrap() as Fhir3Resource,
                    buildMeta(record),
                    record.annotations
            )
            WrapperContract.Resource.TYPE.FHIR4 -> TODO()
            // TODO app data
            WrapperContract.Resource.TYPE.DATA -> DataRecord(
                    record.identifier!!,
                    (record.resource.unwrap()) as DataResource,
                    buildMeta(record),
                    record.annotations
            )
            // TODO FHIR 4
        } as BaseRecord<Any>
    }

    @JvmStatic
    private fun buildMeta(
            record: NetworkRecordContract.DecryptedRecord
    ): Meta = Meta(
            LocalDate.parse(record.customCreationDate, DATE_FORMATTER),
            LocalDateTime.parse(record.updatedDate, DATE_TIME_FORMATTER)
    )
}
