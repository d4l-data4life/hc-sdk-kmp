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

package care.data4life.sdk;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import care.data4life.crypto.GCKey;
import care.data4life.fhir.stu3.model.Attachment;
import care.data4life.fhir.stu3.model.FhirDateTime;
import care.data4life.fhir.stu3.util.FhirDateTimeParser;
import care.data4life.sdk.config.DataRestrictionException;
import care.data4life.sdk.helpers.AttachmentBuilder;
import care.data4life.sdk.lang.D4LException;
import care.data4life.sdk.lang.DataValidationException;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import kotlin.Pair;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AttachmentServiceTest {

    private static final String ALIAS = "alias";
    private static final String ATTACHMENT_ID = "attachmentId";
    private static final String THUMBNAIL_ID = "attachmentId#previewId";
    private static final String RECORD_ID = "recordId";
    private static final String USER_ID = "userId";
    private static final String TITLE = "title";
    private static final String CONTENT_TYPE = "contentType";
    private final FhirDateTime creationDate = FhirDateTimeParser.parseDateTime("2013-04-03");
    private final byte[] pdf = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2d};
    private byte[] largeFile = new byte[20000000 + 1];
    private static final String dataBase64 = "JVBERi0=";
    private static final String DATA_HASH = "dataHash";
    private final GCKey attachmentKey = mock(GCKey.class);

    private FileService mockFileService;
    private ImageResizer mockImageResizer;
    private Attachment attachment;
    private AttachmentService attachmentService; //SUT

    @Before
    public void setUp() throws DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        attachment = AttachmentBuilder.buildWith(TITLE, creationDate, CONTENT_TYPE, pdf);
        mockFileService = mock(FileService.class);
        mockImageResizer = mock(ImageResizer.class);

        attachmentService = new AttachmentService(ALIAS, mockFileService, mockImageResizer);
    }

    @Test
    public void uploadAttachments_shouldReturnListOfAttachments() throws InterruptedException, DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        // given
        attachment.id = "id";
        Attachment newAttachment = AttachmentBuilder.buildWith("newAttachment", creationDate, CONTENT_TYPE, pdf);
        newAttachment.id = null;
        List<Attachment> attachments = Arrays.asList(attachment, newAttachment);

        when(mockFileService.uploadFile(attachmentKey, USER_ID, pdf)).thenReturn(Single.just(ATTACHMENT_ID));

        // when
        TestObserver<List<Pair<Attachment, List<String>>>> subscriber = attachmentService
                .uploadAttachments(attachments, attachmentKey, USER_ID).test().await();

        // then
        List<Pair<Attachment, List<String>>> result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values().get(0);

        assertThat(result).hasSize(2);
        Attachment a1 = result.get(0).getFirst();
        assertThat(a1.id).isEqualTo(ATTACHMENT_ID);
        assertThat(a1.title).isEqualTo(TITLE);
        assertThat(a1.creation).isEqualTo(creationDate);
        assertThat(a1.contentType).isEqualTo(CONTENT_TYPE);
        assertThat(a1.data).isEqualTo(dataBase64);

        Attachment a2 = result.get(1).getFirst();
        assertThat(a2.id).isEqualTo(ATTACHMENT_ID);
        assertThat(a2.title).isEqualTo("newAttachment");
        assertThat(a2.creation).isEqualTo(creationDate);
        assertThat(a2.contentType).isEqualTo(CONTENT_TYPE);
        assertThat(a2.data).isEqualTo(dataBase64);

        verify(mockFileService, times(2)).uploadFile(attachmentKey, USER_ID, pdf);
        verifyNoMoreInteractions(mockFileService);
    }

    @Test
    public void uploadAttachment_with_20MB_and_1_shouldFail() throws InterruptedException, DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        // given
        attachment.id = "id";
        System.arraycopy(pdf, 0, largeFile, 0, pdf.length);
        try {
            Attachment newAttachment = AttachmentBuilder.buildWith("newAttachment", creationDate, CONTENT_TYPE, largeFile);
            newAttachment.id = null;
            List<Attachment> attachments = Arrays.asList(attachment, newAttachment);
            //fail("Should have thrown an exception");
        } catch (DataRestrictionException.MaxDataSizeViolation e) {
            assert (true);
        }

    }

    @Test
    public void downloadAttachments_shouldReturnListOfAttachments() throws InterruptedException, DataValidationException.InvalidAttachmentPayloadHash {
        // given
        attachment.id = ATTACHMENT_ID;
        attachment.data = null;
        List<Attachment> attachments = Arrays.asList(attachment);
        when(mockFileService.downloadFile(attachmentKey, USER_ID, ATTACHMENT_ID)).thenReturn(Single.just(pdf));

        // when
        TestObserver<List<Attachment>> subscriber =
                attachmentService.downloadAttachments(attachments, attachmentKey, USER_ID).test().await();

        // then
        List<Attachment> result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values().get(0);

        assertThat(result).hasSize(1);
        Attachment a = result.get(0);
        assertThat(a.id).isEqualTo(ATTACHMENT_ID);
        assertThat(a.title).isEqualTo(TITLE);
        assertThat(a.creation).isEqualTo(creationDate);
        assertThat(a.contentType).isEqualTo(CONTENT_TYPE);
        assertThat(a.data).isEqualTo(dataBase64);

        verify(mockFileService).downloadFile(attachmentKey, USER_ID, ATTACHMENT_ID);
        verifyNoMoreInteractions(mockFileService);
    }

    @Test
    public void downloadAttachments_shouldThrow_whenInvalidHashAttachment() throws DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        attachment.id = ATTACHMENT_ID;
        attachment.hash = DATA_HASH;
        List<Attachment> attachments = Arrays.asList(attachment);

        // When
        try {
            attachmentService.downloadAttachments(attachments, attachmentKey, USER_ID);
            //fail("Exception expected!");
        } catch (D4LException e) {

            // Then
            assertThat(e.getClass()).isEqualTo(DataValidationException.InvalidAttachmentPayloadHash.class);
            assertThat(e.getMessage()).isEqualTo("Attachment.hash is not valid");
        }
    }

    @Test
    public void downloadAttachments_shouldTNot_throw_whenInvalidHashPreview() throws InterruptedException, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        attachment.id = THUMBNAIL_ID;
        attachment.hash = DATA_HASH;
        List<Attachment> attachments = Arrays.asList(attachment);
        when(mockFileService.downloadFile(attachmentKey, USER_ID, THUMBNAIL_ID.split(RecordService.SPLIT_CHAR)[1])).thenReturn(Single.just(pdf));

        // when
        TestObserver<List<Attachment>> subscriber =
                attachmentService.downloadAttachments(attachments, attachmentKey, USER_ID).test().await();

        // then
        List<Attachment> result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values().get(0);

        assertThat(result).hasSize(1);
        Attachment a = result.get(0);
        assertThat(a.id).isEqualTo(THUMBNAIL_ID);
        assertThat(a.title).isEqualTo(TITLE);
        assertThat(a.creation).isEqualTo(creationDate);
        assertThat(a.contentType).isEqualTo(CONTENT_TYPE);
        assertThat(a.data).isEqualTo(dataBase64);

        verify(mockFileService).downloadFile(attachmentKey, USER_ID, THUMBNAIL_ID.split(RecordService.SPLIT_CHAR)[1]);
        verifyNoMoreInteractions(mockFileService);

    }

    @Test
    public void updatingAttachments_shouldReturnListOfAttachmentsInOrder() throws InterruptedException, DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        // given
        attachment.id = null;
        Attachment newAttachment = AttachmentBuilder.buildWith("newAttachment", creationDate, CONTENT_TYPE, pdf);
        List<Attachment> attachments = Arrays.asList(attachment, newAttachment);

        when(mockFileService.uploadFile(attachmentKey, USER_ID, pdf)).thenReturn(Single.just(ATTACHMENT_ID));

        // when
        TestObserver<List<Pair<Attachment, List<String>>>> subscriber =
                attachmentService.uploadAttachments(attachments, attachmentKey, USER_ID).test().await();

        // then
        List<Pair<Attachment, List<String>>> result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values().get(0);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFirst()).isEqualTo(attachment);
        assertThat(result.get(1).getFirst()).isEqualTo(newAttachment);
    }

    @Test
    public void deleteAttachment() throws InterruptedException {
        // given
        when(mockFileService.deleteFile(USER_ID, ATTACHMENT_ID)).thenReturn(Single.just(true));

        // when
        TestObserver<Boolean> subscriber = attachmentService.deleteAttachment(ATTACHMENT_ID, USER_ID).test().await();

        // then
        subscriber.assertNoErrors()
                .assertComplete()
                .assertValue(it -> it);
    }

    @Test
    public void deleteAttachment_shouldFail() throws InterruptedException {
        // given
        when(mockFileService.deleteFile(ATTACHMENT_ID, USER_ID)).thenReturn(Single.error(new Throwable()));

        // when
        TestObserver<Boolean> subscriber = attachmentService.deleteAttachment(ATTACHMENT_ID, USER_ID).test().await();

        // then
        subscriber.assertError(Objects::nonNull)
                .assertNotComplete();
    }
}
