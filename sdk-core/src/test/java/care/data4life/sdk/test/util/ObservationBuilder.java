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
import java.util.Arrays;
import java.util.List;

import care.data4life.fhir.stu3.model.Attachment;
import care.data4life.fhir.stu3.model.CodeSystems;
import care.data4life.fhir.stu3.model.CodeableConcept;
import care.data4life.fhir.stu3.model.Observation;
import care.data4life.sdk.util.Base64;

public class ObservationBuilder {

    private static final String ATTACHMENT_ID = "attachmentId";


    public static Observation buildWith(CodeableConcept type, CodeSystems.ObservationStatus status, List<Attachment> attachments) {
        Observation observation = new Observation(type, status);
        observation.valueAttachment = attachments.get(0);
        Observation.ObservationComponent component = buildComponent(null, attachments.get(1));
        observation.component = new ArrayList<>();
        observation.component.add(component);
        return observation;
    }


    public static Observation buildWith(CodeableConcept type, CodeSystems.ObservationStatus status, Attachment attachment) {
        Observation observation = new Observation(type, status);
        observation.valueAttachment = attachment;
        return observation;
    }

    public static Observation buildWith(CodeableConcept type, CodeSystems.ObservationStatus status, Observation.ObservationComponent component) {
        Observation observation = new Observation(type, status);
        observation.component = new ArrayList<>();
        observation.component.add(component);
        return observation;
    }


    public static Observation.ObservationComponent buildComponent(CodeableConcept code, Attachment attachment) {
        Observation.ObservationComponent component = new Observation.ObservationComponent(code);
        component.valueAttachment = attachment;
        return component;
    }

    public static Observation buildObservation() {
        return ObservationBuilder.buildWith(null, null, AttachmentBuilder.buildAttachment(null));
    }

    public static Observation buildObservation(byte[] data) {
        Observation observation = buildObservation();
        observation.valueAttachment.data = Base64.INSTANCE.encodeToString(data);
        return observation;
    }


    public static Observation buildObservationWithComponent() {
        List<Attachment> attachments = new ArrayList<>(Arrays.asList(AttachmentBuilder.buildAttachment(null), AttachmentBuilder.buildAttachment(ATTACHMENT_ID)));
        return ObservationBuilder.buildWith(null, null, attachments);
    }
}
