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
package care.data4life.sdk.network.model

import care.data4life.crypto.GCKey
import care.data4life.fhir.stu3.model.DomainResource
import java.io.Serializable
import kotlin.collections.HashMap

data class DecryptedRecord<T : DomainResource>(
        override var identifier: String?,
        var resource: T?,
        override var tags: HashMap<String, String>?,
        override var annotations: List<String>,
        override var customCreationDate: String?,
        override var updatedDate: String?,
        override var dataKey: GCKey?,
        var attachmentsKey: GCKey?,
        override var modelVersion: Int?
): DecryptedRecordBase, Serializable {
    constructor(
            identifier: String?,
            resource: T?,
            tags: HashMap<String, String>?,
            customCreationDate: String?,
            updatedDate: String?,
            dataKey: GCKey?,
            attachmentsKey: GCKey?,
            modelVersion: Int
    ): this(
            identifier,
            resource,
            tags,
            listOf(),
            customCreationDate,
            updatedDate,
            dataKey,
            attachmentsKey,
            modelVersion
    )
}
