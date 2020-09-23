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

package care.data4life.sdk.sample.presentation.upload

import care.data4life.fhir.stu3.model.CodeSystems
import care.data4life.fhir.stu3.model.CodeableConcept
import care.data4life.fhir.stu3.model.DocumentReference
import care.data4life.fhir.stu3.model.Organization
import care.data4life.fhir.stu3.util.FhirDateTimeConverter
import care.data4life.sdk.Data4LifeClient
import care.data4life.sdk.helpers.AttachmentBuilder
import care.data4life.sdk.helpers.DocumentReferenceBuilder
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.listener.ResultListener
import care.data4life.sdk.model.Record
import care.data4life.sdk.sample.presentation.BaseView
import care.data4life.sdk.sample.presentation.View
import care.data4life.sdk.sample.presentation.data.Message
import care.data4life.sdk.sample.presentation.main.MultiMainView
import care.data4life.sdk.sample.presentation.main.SingleMainView
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.parameter.parametersOf
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.util.*

class UploadView(private val alias: String, private val isMulti: Boolean = false) : BaseView(), KoinComponent {


    private val client: Data4LifeClient by inject { parametersOf(alias) }


    override val type: String = "upload"


    override fun renderContent(): View {
        renderMessage(Message("Enter the title of the document"))
        val titleInput = renderPrompt() ?: "Not set"

        renderMessage(Message("Do you want to add an attachment? Enter [yes / y] or [no / n]"))
        val addAttachment = renderPrompt()

        val now = Date.from(Instant.now())
        val attachments = if (addAttachment == "y" || addAttachment == "yes") {
            renderMessage(Message("Paste the path to the attachment you want to add to the document"))
            renderMessage(Message("Or write 'skip'"))
            val filePath = renderPrompt()
            val path = Paths.get(filePath)
            val data = Files.readAllBytes(path)
            val contentType = URLConnection.guessContentTypeFromName(path.fileName.toString())
            val creationTime = FhirDateTimeConverter.toFhirDateTime(now)
            val attachment = AttachmentBuilder.buildWith("Title",
                    creationTime,
                    contentType,
                    data
            )
            listOf(attachment)
        } else {
            listOf()
        }

        val indexed = FhirDateTimeConverter.toFhirInstant(now)
        val record = DocumentReferenceBuilder.buildWith(
                titleInput,
                indexed,
                CodeSystems.DocumentReferenceStatus.CURRENT,
                attachments,
                CodeableConcept(),
                Organization()
        )

        client.createRecord(record, object : ResultListener<Record<DocumentReference>> {
            override fun onSuccess(t: Record<DocumentReference>?) {
                renderMessage(Message("Document created."))
            }

            override fun onError(exception: D4LException?) {
                renderMessage(Message("Failed to create document"))
                exception?.printStackTrace()
            }

        })


        return if (isMulti) {
            MultiMainView()
        } else {
            SingleMainView(alias)
        }
    }

}
