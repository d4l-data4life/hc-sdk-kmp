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

package care.data4life.sdk.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import care.data4life.fhir.stu3.model.Attachment
import care.data4life.fhir.stu3.model.CodeSystemContactPointSystem
import care.data4life.fhir.stu3.model.CodeSystemDocumentReferenceStatus
import care.data4life.fhir.stu3.model.CodeableConcept
import care.data4life.fhir.stu3.model.Coding
import care.data4life.fhir.stu3.model.DocumentReference
import care.data4life.fhir.stu3.model.FhirInstant
import care.data4life.fhir.stu3.model.Practitioner
import care.data4life.fhir.stu3.util.FhirDateTimeParser
import care.data4life.sdk.helpers.stu3.AttachmentBuilder
import care.data4life.sdk.helpers.stu3.DocumentReferenceBuilder
import care.data4life.sdk.helpers.stu3.PractitionerBuilder
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.listener.Callback
import care.data4life.sdk.listener.ResultListener
import care.data4life.sdk.model.DownloadResult
import care.data4life.sdk.model.Record
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.threeten.bp.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Ignore
class CrossSDKTest : BaseTestLogin() {
    //region document properties
    private val attachmentTitle = "Brain MRI"
    private val createdDate = FhirDateTimeParser.parseDateTime("2013-04-03")
    private val contentType = "image/jpeg"
    private val data = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2d)
    private val dataSizeBytes = data.size
    private val dataBase64 = "JVBERi0="
    private val dataHashBase64 = "6GUZUCson6BgvpmHylP3m4FZU4s="

    private val title = "Physical"
    private val indexed: FhirInstant = FhirDateTimeParser.parseInstant("2013-04-03T15:30:10+01:00")
    private val status = CodeSystemDocumentReferenceStatus.CURRENT
    private val documentCode = "34108-1"
    private val documentDisplay = "Outpatient Note"
    private val documentSystem = "http://loinc.org"
    private val practiceSpecialityCode = "General Medicine"
    private val practiceSpecialityDisplay = "General Medicine"
    private val practiceSpecialitySystem =
        "http://www.ihe.net/xds/connectathon/practiceSettingCodes"
    //endregion

    //region practitioner properties
    val TEXT = "Dr. Bruce Banner, Praxis fuer Allgemeinmedizin"
    val NAME = "Bruce"
    val SURNAME = "Banner"
    val PREFIX = "Dr."
    val SUFFIX = "MD"
    val STREET = "Walvisbaai 3"
    val POSTAL_CODE = "2333ZA"
    val CITY = "Den helder"
    val TELEPHONE = "+31715269111"
    val WEBSITE = "www.webpage.com"
    //endregion

    val TIMEOUT = 10L
    val ANDROID_ID = "android"
    val MAX_PAGE_SIZE = 1000
    var clientCallSuccessful = true
    lateinit var latch: CountDownLatch

    companion object {
        @JvmStatic
        lateinit var recordIds: List<String>

        @JvmStatic
        var androidRecordId: String? = null
    }

    var expectedClientIds = mutableSetOf(ANDROID_ID, "ios", "web")

    @Test
    fun t01_fetchRecords_shouldFetchRecords() {
        clientCallSuccessful = true
        latch = CountDownLatch(1)
        lateinit var fetchedRecords: List<Record<DocumentReference>>

        client.fetchRecords(
            DocumentReference::class.java,
            null,
            LocalDate.now(),
            MAX_PAGE_SIZE, 0,
            object : ResultListener<List<Record<DocumentReference>>> {
                override fun onSuccess(records: List<Record<DocumentReference>>) {
                    fetchedRecords = records
                    latch.countDown()
                }

                override fun onError(exception: D4LException) {
                    clientCallSuccessful = false
                    latch.countDown()
                }
            }
        )
        latch.await(TIMEOUT, TimeUnit.SECONDS)
        assertTrue("Fetch records failed", clientCallSuccessful)
        assertEquals(3, fetchedRecords.size)

        recordIds = fetchedRecords.map {
            if (it.fhirResource.getAdditionalIds()?.first()?.value?.equals(ANDROID_ID)!!) {
                androidRecordId = it.fhirResource.id!!
            }
            it.fhirResource.id!!
        }
    }

    @Test
    fun t02_downloadRecords_shouldDownloadRecords() {
        clientCallSuccessful = true
        latch = CountDownLatch(1)
        lateinit var downloadResult: DownloadResult<DocumentReference>

        client.downloadRecords(
            recordIds,
            object : ResultListener<DownloadResult<DocumentReference>> {
                override fun onSuccess(result: DownloadResult<DocumentReference>) {
                    downloadResult = result
                    latch.countDown()
                }

                override fun onError(exception: D4LException) {
                    clientCallSuccessful = false
                    latch.countDown()
                }
            }
        )
        latch.await(TIMEOUT, TimeUnit.SECONDS)

        assertTrue("Download records failed", clientCallSuccessful)
        assertTrue(downloadResult.failedDownloads.isEmpty())
        assertEquals(3, downloadResult.successfulDownloads.size)

        downloadResult.successfulDownloads.map {
            val clientId = it.fhirResource.getAdditionalIds()?.first()?.value
            assertTrue(expectedClientIds.remove(clientId))
            assertDocumentExpectations(it.fhirResource)
        }
        assertTrue(expectedClientIds.isEmpty())
    }

    @Test
    fun t03_createRecord_shouldCreateRecord() {
        androidRecordId?.let {
            deleteRecord(it)
        }

        clientCallSuccessful = true
        latch = CountDownLatch(1)

        val doc = buildTestDocument().apply {
            addAdditionalId(ANDROID_ID)
        }

        client.createRecord(
            doc,
            object : ResultListener<Record<DocumentReference>> {
                override fun onSuccess(record: Record<DocumentReference>) {
                    latch.countDown()
                }

                override fun onError(exception: D4LException) {
                    exception.printStackTrace()
                    clientCallSuccessful = false
                    latch.countDown()
                }
            }
        )
        latch.await(TIMEOUT, TimeUnit.SECONDS)
        assertTrue("Create record failed", clientCallSuccessful)
    }

    private fun deleteRecord(recordId: String) {
        clientCallSuccessful = true
        latch = CountDownLatch(1)

        client.deleteRecord(
            recordId,
            object : Callback {
                override fun onSuccess() {
                    latch.countDown()
                }

                override fun onError(exception: D4LException) {
                    exception.printStackTrace()
                    clientCallSuccessful = false
                    latch.countDown()
                }
            }
        )
        latch.await(TIMEOUT, TimeUnit.SECONDS)
        assertTrue("Delete record failed", clientCallSuccessful)
    }

    private fun assertDocumentExpectations(doc: DocumentReference) {
        assertNotNull(doc.id)
        assertEquals(indexed, doc.indexed)
        assertEquals(status, doc.status)
        assertNotNull(doc.type)
        assertEquals(1, doc.type.coding?.size)
        assertEquals(documentCode, doc.type.coding?.first()?.code)
        assertEquals(documentDisplay, doc.type.coding?.first()?.display)
        assertEquals(documentSystem, doc.type.coding?.first()?.system)
        assertEquals(1, doc.context?.practiceSetting?.coding?.size)
        assertEquals(practiceSpecialityCode, doc.context?.practiceSetting?.coding?.first()?.code)
        assertEquals(
            practiceSpecialityDisplay,
            doc.context?.practiceSetting?.coding?.first()?.display
        )
        assertEquals(
            practiceSpecialitySystem,
            doc.context?.practiceSetting?.coding?.first()?.system
        )
        assertEquals(1, doc.getAttachments()?.size)
        assertAttachmentExpectations(doc.getAttachments()?.first()!!)
        assertAuthorExpectations(doc.getPractitioner()!!)
    }

    private fun assertAttachmentExpectations(attachment: Attachment) {
        assertNotNull(attachment.id)
        assertEquals(attachmentTitle, attachment.title)
        assertEquals(createdDate, attachment.creation)
        assertEquals(contentType, attachment.contentType)
        assertEquals(dataHashBase64, attachment.hash)
        assertEquals(dataSizeBytes, attachment.size)
        assertEquals(dataBase64, attachment.data)
    }

    private fun assertAuthorExpectations(practitioner: Practitioner) {
        assertEquals(1, practitioner.name?.size)
        assertEquals(1, practitioner.name!![0].given?.size)
        assertEquals(NAME, practitioner.name!![0].given!![0])
        assertEquals(SURNAME, practitioner.name!![0].family)
        assertEquals(1, practitioner.name!![0].prefix?.size)
        assertEquals(PREFIX, practitioner.name!![0].prefix!![0])
        assertEquals(1, practitioner.name!![0].suffix?.size)
        assertEquals(SUFFIX, practitioner.name!![0].suffix!![0])
        assertEquals(1, practitioner.address?.size)
        assertEquals(1, practitioner.address!![0].line?.size)
        assertEquals(STREET, practitioner.address!![0].line!![0])
        assertEquals(POSTAL_CODE, practitioner.address!![0].postalCode)
        assertEquals(CITY, practitioner.address!![0].city)
        assertEquals(2, practitioner.telecom?.size)
        assertEquals(CodeSystemContactPointSystem.PHONE, practitioner.telecom!![0].system)
        assertEquals(TELEPHONE, practitioner.telecom!![0].value)
        assertEquals(CodeSystemContactPointSystem.URL, practitioner.telecom!![1].system)
        assertEquals(WEBSITE, practitioner.telecom!![1].value)
    }

    private fun buildTestDocument(): DocumentReference {
        val attachment = AttachmentBuilder.buildWith(
            attachmentTitle,
            createdDate,
            contentType,
            data
        )

        val author = PractitionerBuilder.buildWith(
            NAME,
            SURNAME,
            PREFIX,
            SUFFIX,
            STREET,
            POSTAL_CODE,
            CITY,
            TELEPHONE,
            WEBSITE
        )

        val docTypeCoding = Coding().apply {
            code = documentCode
            display = documentDisplay
            system = documentSystem
        }
        val docType = CodeableConcept()
        docType.coding = listOf(docTypeCoding)

        val practiceSpecialityCoding = Coding().apply {
            code = practiceSpecialityCode
            display = practiceSpecialityDisplay
            system = practiceSpecialitySystem
        }
        val practiceSpeciality = CodeableConcept()
        practiceSpeciality.coding = listOf(practiceSpecialityCoding)

        return DocumentReferenceBuilder.buildWith(
            title,
            indexed,
            status,
            listOf(attachment),
            docType,
            author,
            practiceSpeciality
        )
    }
}
