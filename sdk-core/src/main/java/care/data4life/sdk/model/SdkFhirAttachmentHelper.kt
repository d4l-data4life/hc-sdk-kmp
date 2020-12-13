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

import care.data4life.fhir.stu3.model.Attachment
import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.fhir.stu3.model.Identifier
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.fhir.stu3.util.FhirAttachmentHelper as Fhir3AttachmentHelper
import care.data4life.sdk.model.definitions.FhirAttachmentHelper
import java.util.HashMap

internal object SdkFhirAttachmentHelper: FhirAttachmentHelper {
    override fun hasAttachment(resource: Any): Boolean {
        return if(resource !is DomainResource) {
             false
        } else {
            Fhir3AttachmentHelper.hasAttachment(resource)
        }
    }

    @Throws(CoreRuntimeException.InternalFailure::class)
    override fun getAttachment(resource: Any): MutableList<Any> {
        @Suppress("UNCHECKED_CAST")
        return if(resource !is DomainResource) {
            throw CoreRuntimeException.InternalFailure()
        } else {
            val result = Fhir3AttachmentHelper.getAttachment(resource)
            (result ?: mutableListOf()) as MutableList<Any>
        }
    }

    @Throws(CoreRuntimeException.InternalFailure::class)
    private fun validateFhir3Attachments(attachments: Set<Any>) {
        for(attachment in attachments) {
            if(attachment !is Attachment) {
                throw CoreRuntimeException.InternalFailure()
            }
        }
    }

    @Throws(CoreRuntimeException.InternalFailure::class)
    override fun updateAttachmentData(resource: Any, attachmentData: HashMap<Any, String>?) {
        if(resource !is DomainResource) {
            throw CoreRuntimeException.InternalFailure()
        } else {
            if(attachmentData == null || attachmentData.isEmpty() ) {
                return
            }

            validateFhir3Attachments(attachmentData.keys)

            @Suppress("UNCHECKED_CAST")
            return Fhir3AttachmentHelper.updateAttachmentData(resource, attachmentData as HashMap<Attachment, String>)
        }
    }

    @Throws(CoreRuntimeException.InternalFailure::class)
    override fun getIdentifiers(resource: Any): List<Any> {
        return if (resource !is DomainResource) {
            throw CoreRuntimeException.InternalFailure()
        } else {
            Fhir3AttachmentHelper.getIdentifier(resource) ?: mutableListOf()
        }
    }

    @Throws(CoreRuntimeException.InternalFailure::class)
    private fun validateFhir3Identifiers(identifiers: List<Any>) {
        for( identifier in identifiers) {
            if (identifier !is Identifier) {
                throw CoreRuntimeException.InternalFailure()
            }
        }
    }

    @Throws(CoreRuntimeException.InternalFailure::class)
    override fun setIdentifier(resource: Any, updatedIdentifiers: List<Any>) {
        if (resource !is DomainResource) {
            throw CoreRuntimeException.InternalFailure()
        } else {
            if(updatedIdentifiers.isEmpty()) {
                return
            }

            validateFhir3Identifiers(updatedIdentifiers)
            @Suppress("UNCHECKED_CAST")
            Fhir3AttachmentHelper.setIdentifier(resource, updatedIdentifiers as List<Identifier>)
        }
    }

    @Throws(CoreRuntimeException.InternalFailure::class)
    override fun appendIdentifier(resource: Any, identifier: String, assigner: String) {
        if( resource !is DomainResource ) {
            throw CoreRuntimeException.InternalFailure()
        } else {
            Fhir3AttachmentHelper.appendIdentifier(resource, identifier, assigner)
        }
    }

}
