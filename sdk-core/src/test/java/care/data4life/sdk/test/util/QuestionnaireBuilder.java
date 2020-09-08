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
import care.data4life.fhir.stu3.model.Questionnaire;

public class QuestionnaireBuilder {


    public static Questionnaire buildWith(CodeSystems.PublicationStatus status, List<Attachment> attachments) {

        Questionnaire questionnaire = new Questionnaire(status);
        Questionnaire.QuestionnaireItem questionnaireItem = buildItem("", null, attachments.get(0));
        questionnaire.item = new ArrayList<>();
        questionnaire.item.add(questionnaireItem);

        return questionnaire;
    }


    public static Questionnaire.QuestionnaireItem buildItem(String linkId, CodeSystems.QuestionnaireItemType type, Attachment attachment) {

        Questionnaire.QuestionnaireItem item = new Questionnaire.QuestionnaireItem(linkId, type);
        item.initialAttachment = attachment;
        return item;
    }

    public static Questionnaire buildQuestionnaire() {
        List<Attachment> initialAttachment = new ArrayList<>();
        initialAttachment.add(AttachmentBuilder.buildAttachment(null));
        return QuestionnaireBuilder.buildWith(null, initialAttachment);
    }

}
