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


import java.util.ArrayList;
import java.util.List;

import care.data4life.crypto.GCKey;
import care.data4life.fhir.stu3.model.Attachment;
import care.data4life.fhir.stu3.model.FhirDateTime;
import care.data4life.fhir.stu3.util.FhirDateTimeParser;
import care.data4life.sdk.lang.DataValidationException;
import care.data4life.sdk.lang.ImageResizeException;
import care.data4life.sdk.log.Log;
import care.data4life.sdk.util.Base64;
import care.data4life.sdk.util.HashUtil;
import io.reactivex.Observable;
import io.reactivex.Single;
import kotlin.Pair;

import static care.data4life.sdk.ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT;
import static care.data4life.sdk.ImageResizer.DEFAULT_PREVIEW_SIZE_PX;
import static care.data4life.sdk.ImageResizer.DEFAULT_THUMBNAIL_SIZE_PX;

class AttachmentService {
    private static final int DOWNSCALED_ATTACHMENT_ID_POS = 1;
    private static final int POSITION_PREVIEW = 0;
    private static final String HASH_VALIDATION_DATE = "2019-09-15";

    private String alias;
    private FileService fileService;
    private ImageResizer imageResizer;

    AttachmentService(String alias, FileService fileService, ImageResizer imageResizer) {
        this.alias = alias;
        this.fileService = fileService;
        this.imageResizer = imageResizer;
    }

    Single<List<Pair<Attachment, List<String>>>> uploadAttachments(List<Attachment> attachments, GCKey attachmentsKey, String userId) {
        List<Attachment> filteredAttachments = Observable
                .fromIterable(attachments)
                .filter(a -> a.data != null)
                .toList().blockingGet();

        return Observable.fromIterable(filteredAttachments)
                .map(attachment -> {
                    byte[] originalData = Base64.INSTANCE.decode(attachment.data);
                    List<String> additionalIds = null;

                    attachment.id = fileService.uploadFile(attachmentsKey, userId, originalData).blockingGet();
                    additionalIds = uploadDownscaledImages(attachmentsKey, userId, attachment, originalData);

                    return new Pair<>(attachment, additionalIds);
                })
                .filter(pair -> pair.getFirst().id != null)
                .toList();
    }

    private List<String> uploadDownscaledImages(GCKey attachmentsKey, String userId, Attachment attachment, byte[] originalData) {
        List<String> additionalIds = null;
        if (imageResizer.isResizable(originalData)) {
            additionalIds = new ArrayList<>();
            String downscaledId;

            for (int position = 0; position < 2; position++) {
                downscaledId = resizeAndUpload(attachmentsKey, userId, attachment, originalData, position == POSITION_PREVIEW ? DEFAULT_PREVIEW_SIZE_PX : DEFAULT_THUMBNAIL_SIZE_PX);
                if (downscaledId == null) return null;

                additionalIds.add(downscaledId);
            }
        }
        return additionalIds;
    }

    private String resizeAndUpload(GCKey attachmentsKey, String userId, Attachment attachment, byte[] originalData, int targetHeight) {
        byte[] downscaledImage;
        try {
            downscaledImage = imageResizer.resizeToHeight(originalData, targetHeight, DEFAULT_JPEG_QUALITY_PERCENT);
        } catch (ImageResizeException.JpegWriterMissing ex) {
            Log.error(ex, ex.getMessage());
            return null;
        }
        if (downscaledImage == null) { //currentSizePx <= targetSizePx
            return attachment.id; //nothing to upload
        }
        String attachmentId = fileService.uploadFile(attachmentsKey, userId, downscaledImage).blockingGet();
        if (attachmentId != null) return attachmentId;
        else return null;
    }

    Single<List<Attachment>> downloadAttachments(List<Attachment> attachments, GCKey attachmentsKey, String userId) throws DataValidationException.InvalidAttachmentPayloadHash {
        return Observable
                .fromCallable(() -> attachments)
                .flatMapIterable(attachmentList -> attachmentList)
                .map(attachment -> {
                    String attachmentId = attachment.id;
                    boolean isPreview = false;
                    if (attachment.id.contains(RecordService.SPLIT_CHAR)) {
                        attachmentId = attachment.id.split(RecordService.SPLIT_CHAR)[DOWNSCALED_ATTACHMENT_ID_POS];
                        isPreview = true;
                    }

                    byte[] data = fileService.downloadFile(attachmentsKey, userId, attachmentId).blockingGet();
                    FhirDateTime validationDate = FhirDateTimeParser.parseDateTime(HASH_VALIDATION_DATE);
                    if (!isPreview && attachment.creation.getDate().toDate().after(validationDate.getDate().toDate()) && !attachment.hash.equals(Base64.INSTANCE.encodeToString(HashUtil.INSTANCE.sha1(data)))) {
                        throw new DataValidationException.InvalidAttachmentPayloadHash(
                                "Attachment.hash is not valid");
                    } else {
                        attachment.hash = Base64.INSTANCE.encodeToString(HashUtil.INSTANCE.sha1(data));
                    }
                    attachment.data = Base64.INSTANCE.encodeToString(data);
                    return attachment;
                })
                .toList();
    }

    Single<Boolean> deleteAttachment(String attachmentId, String userId) {
        return Single.fromCallable(() -> attachmentId)
                .flatMap(id -> fileService.deleteFile(userId, attachmentId));
    }
}
