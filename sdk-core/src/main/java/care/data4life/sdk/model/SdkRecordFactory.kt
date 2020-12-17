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

import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.model.definitions.BaseRecord
import care.data4life.sdk.model.definitions.RecordFactory
import care.data4life.sdk.network.model.definitions.DecryptedBaseRecord
import care.data4life.sdk.network.model.definitions.DecryptedDataRecord
import care.data4life.sdk.network.model.definitions.DecryptedFhir3Record
import care.data4life.sdk.network.model.definitions.DecryptedFhir4Record
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
    override fun <T : Any> getInstance(record: DecryptedBaseRecord<T>): BaseRecord<T> {
        @Suppress("UNCHECKED_CAST")
        return when (record) {
            is DecryptedFhir3Record -> Record(
                    record.resource as Fhir3Resource,
                    buildMeta(record),
                    record.annotations
            )
            is DecryptedFhir4Record -> Fhir4Record(
                    record.identifier!!,//FIXME
                    record.resource as Fhir4Resource,
                    buildMeta(record),
                    record.annotations
            )
            // TODO app data
            is DecryptedDataRecord -> AppDataRecord(
                    record.identifier!!,
                    record.resource,
                    buildMeta(record),
                    record.annotations
            )
            else -> throw CoreRuntimeException.InternalFailure()
        } as BaseRecord<T>
    }

    @JvmStatic
    private fun buildMeta(
            record: DecryptedBaseRecord<*>
    ): Meta = Meta(
            LocalDate.parse(record.customCreationDate, DATE_FORMATTER),
            LocalDateTime.parse(record.updatedDate, DATE_TIME_FORMATTER)
    )
}
