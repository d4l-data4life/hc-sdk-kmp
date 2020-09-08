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
import care.data4life.fhir.stu3.model.CodeableConcept;
import care.data4life.fhir.stu3.model.Medication;
import care.data4life.fhir.util.Preconditions;
import care.data4life.sdk.helpers.FhirHelpers;
import care.data4life.sdk.util.Base64;

//TODO move builder to Fhir Helper project
public class MedicationBuilder {


    public static Medication buildWith(String medicationName, String medicationForm, List<Attachment> attachments) {

        Preconditions.checkArgument(!(medicationName.isEmpty()), "medicationName is required");
        Preconditions.checkArgument(!(medicationForm.isEmpty()), "medicationForm is required");

        Medication medication = new Medication();


        CodeableConcept medicationCode = FhirHelpers.buildWith(medicationName);
        medication.code = medicationCode;
        CodeableConcept formCode = FhirHelpers.buildWith(medicationForm);
        medication.form = formCode;

        medication.image = attachments;

        return medication;
    }


    public static Medication buildMedication() {
        List<Attachment> photo = new ArrayList<>();
        photo.add(AttachmentBuilder.buildAttachment(null));
        return MedicationBuilder.buildWith("Ibuprofen-ratiopharm", "tablets", photo);
    }

    public static Medication buildMedication(byte[] data) {
        Medication medication = buildMedication();
        medication.image.get(0).data = Base64.INSTANCE.encodeToString(data);
        return medication;
    }

}
