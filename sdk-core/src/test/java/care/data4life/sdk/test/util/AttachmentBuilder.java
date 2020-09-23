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

package care.data4life.sdk.test.util;

import care.data4life.fhir.stu3.model.Attachment;

public class AttachmentBuilder {

    private static final String DATA = "data";
    private static final Integer DATA_SIZE = 42;
    private static final String DATA_HASH = "dataHash";


    public static Attachment buildAttachment(String id) {
        Attachment attachment = new Attachment();
        attachment.id = id;
        attachment.data = DATA;
        attachment.size = DATA_SIZE;
        attachment.hash = DATA_HASH;
        return attachment;
    }

}
