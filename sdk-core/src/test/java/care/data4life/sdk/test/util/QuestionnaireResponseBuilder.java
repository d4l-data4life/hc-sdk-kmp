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


import java.util.ArrayList;
import java.util.List;

import care.data4life.fhir.stu3.model.Attachment;
import care.data4life.fhir.stu3.model.CodeSystems;
import care.data4life.fhir.stu3.model.QuestionnaireResponse;
import care.data4life.sdk.util.Base64;

public class QuestionnaireResponseBuilder {

    public static QuestionnaireResponse buildWith(CodeSystems.QuestionnaireResponseStatus status, List<Attachment> attachments) {

        QuestionnaireResponse questionnaire = new QuestionnaireResponse(status);
        QuestionnaireResponse.QuestionnaireResponseItem questionnaireResponseItem = buildItem("", attachments.get(0));
        questionnaire.item = new ArrayList<>();
        questionnaire.item.add(questionnaireResponseItem);

        return questionnaire;
    }


    public static QuestionnaireResponse.QuestionnaireResponseItem buildItem(String linkId, Attachment attachment) {

        QuestionnaireResponse.QuestionnaireResponseItem item = new QuestionnaireResponse.QuestionnaireResponseItem(linkId);
        QuestionnaireResponse.QuestionnaireResponseItemAnswer itemAnswer = new QuestionnaireResponse.QuestionnaireResponseItemAnswer();
        itemAnswer.valueAttachment = attachment;
        item.answer = new ArrayList<>();
        item.answer.add(itemAnswer);
        return item;
    }

    public static QuestionnaireResponse buildQuestionnaireResponse() {
        List<Attachment> valueAttachment = new ArrayList<>();
        valueAttachment.add(AttachmentBuilder.buildAttachment(null));
        return QuestionnaireResponseBuilder.buildWith(null, valueAttachment);
    }


    public static QuestionnaireResponse buildQuestionnaireResponse(byte[] data) {
        QuestionnaireResponse questionnaireResponse = buildQuestionnaireResponse();
        questionnaireResponse.item.get(0).answer.get(0).valueAttachment.data = Base64.INSTANCE.encodeToString(data);
        return questionnaireResponse;
    }

}
