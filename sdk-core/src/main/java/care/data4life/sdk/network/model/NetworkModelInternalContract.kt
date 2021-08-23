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

package care.data4life.sdk.network.model

import care.data4life.sdk.crypto.GCKey
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource

internal class NetworkModelInternalContract {
    interface EncryptedKeyMaker {
        fun create(key: ByteArray): NetworkModelContract.EncryptedKey
    }

    // FIXME remove nullable type
    interface DecryptedFhir3Record<T : Fhir3Resource?> : NetworkModelContract.DecryptedBaseRecord<T>

    interface DecryptedFhir4Record<T : Fhir4Resource> : NetworkModelContract.DecryptedBaseRecord<T>

    interface DecryptedCustomDataRecord : NetworkModelContract.DecryptedBaseRecord<DataResource> {
        override var attachmentsKey: GCKey?
            get() = null
            set(_) {}
    }
}
