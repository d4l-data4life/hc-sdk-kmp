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
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.model.ModelContract.BaseRecord
import care.data4life.sdk.model.ModelContract.RecordFactory
import care.data4life.sdk.network.model.NetworkModelContract.DecryptedBaseRecord
import care.data4life.sdk.network.model.NetworkModelContract.DecryptedCustomDataRecord
import care.data4life.sdk.network.model.NetworkModelContract.DecryptedFhir3Record
import care.data4life.sdk.network.model.NetworkModelContract.DecryptedFhir4Record
import care.data4life.sdk.wrapper.SdkDateTimeFormatter

internal object RecordMapper : RecordFactory {

    @Throws(CoreRuntimeException.InternalFailure::class)
    override fun <T : Any> getInstance(record: DecryptedBaseRecord<T>): BaseRecord<T> {
        @Suppress("UNCHECKED_CAST")
        return when (record) {
            is DecryptedFhir3Record -> Record(
                record.resource as Fhir3Resource,
                SdkDateTimeFormatter.buildMeta(record),
                record.annotations
            )
            is DecryptedFhir4Record -> Fhir4Record(
                record.identifier ?: "", // FIXME
                record.resource as Fhir4Resource,
                SdkDateTimeFormatter.buildMeta(record),
                record.annotations
            )
            is DecryptedCustomDataRecord -> DataRecord(
                record.identifier ?: "", // FIXME
                record.resource,
                SdkDateTimeFormatter.buildMeta(record),
                record.annotations
            )
            else -> throw CoreRuntimeException.InternalFailure()
        } as BaseRecord<T>
    }
}
