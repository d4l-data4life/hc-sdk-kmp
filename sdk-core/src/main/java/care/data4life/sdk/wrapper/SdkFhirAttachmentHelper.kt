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

package care.data4life.sdk.wrapper

import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.resource.Fhir3Attachment
import care.data4life.sdk.resource.Fhir3AttachmentHelper
import care.data4life.sdk.resource.Fhir3Identifier
import care.data4life.sdk.resource.Fhir3Resource
import care.data4life.sdk.resource.Fhir4Attachment
import care.data4life.sdk.resource.Fhir4AttachmentHelper
import care.data4life.sdk.resource.Fhir4Identifier
import care.data4life.sdk.resource.Fhir4Resource

internal object SdkFhirAttachmentHelper : HelperContract.FhirAttachmentHelper {

    @Throws(CoreRuntimeException.InternalFailure::class)
    override fun hasAttachment(resource: Any): Boolean {
        return when (resource) {
            is Fhir4Resource -> Fhir4AttachmentHelper.hasAttachment(resource)
            is Fhir3Resource -> Fhir3AttachmentHelper.hasAttachment(resource)
            else -> throw CoreRuntimeException.InternalFailure()
        }
    }

    @Throws(CoreRuntimeException.InternalFailure::class)
    override fun getAttachment(resource: Any): MutableList<Any?>? {
        @Suppress("UNCHECKED_CAST")
        return when (resource) {
            is Fhir4Resource -> Fhir4AttachmentHelper.getAttachment(resource) as MutableList<Any?>?
            is Fhir3Resource -> Fhir3AttachmentHelper.getAttachment(resource) as MutableList<Any?>?
            else -> throw CoreRuntimeException.InternalFailure()
        }
    }

    @Throws(CoreRuntimeException.InternalFailure::class)
    override fun updateAttachmentData(resource: Any, attachmentData: HashMap<Any, String?>?) {
        return when (resource) {
            is Fhir4Resource -> updateFhir4AttachmentData(resource, attachmentData)
            is Fhir3Resource -> updateFhir3AttachmentData(resource, attachmentData)
            else -> throw CoreRuntimeException.InternalFailure()
        }
    }

    private fun updateFhir3AttachmentData(
        resource: Fhir3Resource,
        attachmentData: HashMap<Any, String?>?
    ) {
        val attachment = if (attachmentData == null) {
            null
        } else {
            attachmentData as HashMap<Fhir3Attachment, String?>
        }

        Fhir3AttachmentHelper.updateAttachmentData(
            resource,
            attachment
        )
    }

    private fun updateFhir4AttachmentData(
        resource: Fhir4Resource,
        attachmentData: HashMap<Any, String?>?
    ) {
        val attachment = if (attachmentData == null) {
            null
        } else {
            attachmentData as HashMap<Fhir4Attachment, String?>
        }

        Fhir4AttachmentHelper.updateAttachmentData(
            resource,
            attachment
        )
    }

    @Throws(CoreRuntimeException.InternalFailure::class)
    override fun getIdentifier(resource: Any): List<Any>? {
        return when (resource) {
            is Fhir4Resource -> Fhir4AttachmentHelper.getIdentifier(resource)
            is Fhir3Resource -> Fhir3AttachmentHelper.getIdentifier(resource)
            else -> throw CoreRuntimeException.InternalFailure()
        }
    }

    @Throws(CoreRuntimeException.InternalFailure::class)
    override fun setIdentifier(resource: Any, updatedIdentifiers: List<Any>) {
        return when (resource) {
            is Fhir4Resource -> Fhir4AttachmentHelper.setIdentifier(
                resource,
                updatedIdentifiers as List<Fhir4Identifier>
            )
            is Fhir3Resource -> Fhir3AttachmentHelper.setIdentifier(
                resource,
                updatedIdentifiers as List<Fhir3Identifier>
            )
            else -> throw CoreRuntimeException.InternalFailure()
        }
    }

    @Throws(CoreRuntimeException.InternalFailure::class)
    override fun appendIdentifier(resource: Any, identifier: String, assigner: String) {
        return when (resource) {
            is Fhir4Resource -> Fhir4AttachmentHelper.appendIdentifier(
                resource,
                identifier,
                assigner
            )
            is Fhir3Resource -> Fhir3AttachmentHelper.appendIdentifier(
                resource,
                identifier,
                assigner
            )
            else -> throw CoreRuntimeException.InternalFailure()
        }
    }
}
