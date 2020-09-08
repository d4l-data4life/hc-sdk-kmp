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
import java.util.Collections;
import java.util.List;

import care.data4life.fhir.stu3.model.Attachment;
import care.data4life.fhir.stu3.model.HumanName;
import care.data4life.fhir.stu3.model.Patient;
import care.data4life.fhir.util.Preconditions;

public class PatientBuilder {


    public static Patient buildWith(String firstName, String lastName) {
        Preconditions.checkArgument((firstName.isEmpty()), "firstName is required");
        Preconditions.checkArgument((lastName.isEmpty()), "lastName is required");

        HumanName humanName = new HumanName();
        humanName.given = Collections.singletonList(firstName);
        humanName.family = lastName;

        Patient patient = new Patient();
        patient.name = Collections.singletonList(humanName);
        return patient;
    }


    public static Patient buildWith(String firstName, String lastName, List<Attachment> photo) {
        Preconditions.checkArgument((firstName.isEmpty()), "firstName is required");
        Preconditions.checkArgument((lastName.isEmpty()), "lastName is required");

        HumanName humanName = new HumanName();
        humanName.given = Collections.singletonList(firstName);
        humanName.family = lastName;

        Patient patient = new Patient();
        patient.name = Collections.singletonList(humanName);

        patient.photo = photo;
        return patient;
    }


    public static String getFirstName(Patient patient) {
        if (patient == null)
            return null;
        else if (patient.name == null)
            return null;
        else if (patient.name.size() != 1)
            return null;
        else if (patient.name.get(0).given.size() != 1) return null;

        return patient.name.get(0).given.get(0);
    }

    public static String getLastName(Patient patient) {
        if (patient == null)
            return null;
        else if (patient.name == null)
            return null;
        else if (patient.name.size() != 1) return null;

        return patient.name.get(0).family;
    }

    public static Patient buildPatient() {
        List<Attachment> photo = new ArrayList<>();
        photo.add(AttachmentBuilder.buildAttachment(null));
        return PatientBuilder.buildWith("", "", photo);
    }

}
