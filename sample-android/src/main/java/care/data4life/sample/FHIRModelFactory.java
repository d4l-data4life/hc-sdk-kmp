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

package care.data4life.sample;

import android.util.Base64;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import care.data4life.fhir.stu3.model.CodeSystemDiagnosticReportStatus;
import care.data4life.fhir.stu3.model.CodeSystemDocumentReferenceStatus;
import care.data4life.fhir.stu3.model.CodeSystemObservationStatus;
import care.data4life.fhir.stu3.model.Attachment;
import care.data4life.fhir.stu3.model.CarePlan;
import care.data4life.fhir.stu3.model.CodeableConcept;
import care.data4life.fhir.stu3.model.Coding;
import care.data4life.fhir.stu3.model.DiagnosticReport;
import care.data4life.fhir.stu3.model.DocumentReference;
import care.data4life.fhir.stu3.model.Dosage;
import care.data4life.fhir.stu3.model.FhirDateTime;
import care.data4life.fhir.stu3.model.FhirDecimal;
import care.data4life.fhir.stu3.model.FhirInstant;
import care.data4life.fhir.stu3.model.Medication;
import care.data4life.fhir.stu3.model.MedicationRequest;
import care.data4life.fhir.stu3.model.Observation;
import care.data4life.fhir.stu3.model.Patient;
import care.data4life.fhir.stu3.model.Practitioner;
import care.data4life.fhir.stu3.model.Quantity;
import care.data4life.fhir.stu3.model.SampledData;
import care.data4life.fhir.stu3.util.FhirDateTimeParser;
import care.data4life.sdk.helpers.stu3.CarePlanBuilder;
import care.data4life.sdk.helpers.stu3.DiagnosticReportBuilder;
import care.data4life.sdk.helpers.stu3.DocumentReferenceBuilder;
import care.data4life.sdk.helpers.stu3.DosageHelper;
import care.data4life.sdk.helpers.stu3.MedicationHelper;
import care.data4life.sdk.helpers.stu3.MedicationRequestHelper;
import care.data4life.sdk.helpers.stu3.ObservationBuilder;
import care.data4life.sdk.helpers.stu3.PatientHelper;
import care.data4life.sdk.helpers.stu3.PractitionerBuilder;
import care.data4life.sdk.util.MimeType;

public final class FHIRModelFactory {

    private FHIRModelFactory() {
        //empty
    }

    public static Observation getTestObservation() {
        Coding observationCoding = new Coding();
        observationCoding.code = "9279-1";
        observationCoding.display = "Respiratory rate";
        observationCoding.system = "http://loinc.org";
        CodeableConcept observationCode = new CodeableConcept();
        observationCode.text = "Respiratory rate";
        observationCode.coding = Arrays.asList(observationCoding);

        Coding categoryCoding = new Coding();
        categoryCoding.code = "vital-signs";
        categoryCoding.display = "Vital Signs";
        categoryCoding.system = "http://loinc.org";
        CodeableConcept categoryCode = new CodeableConcept();
        categoryCode.text = "Vital Signs";
        categoryCode.coding = Arrays.asList(categoryCoding);

        FhirInstant issuedDate = FhirDateTimeParser.parseInstant("2013-04-03T15:30:10+01:00");
        FhirDateTime effectiveDate = FhirDateTimeParser.parseDateTime("2013-04-03");

        return ObservationBuilder.buildWith(
                observationCode,
                26f,
                "breaths/minute",
                CodeSystemObservationStatus.FINAL,
                issuedDate,
                effectiveDate,
                categoryCode,
                null);
    }

    public static Observation getTestObservationSampledData() {
        Coding observationCoding = new Coding();
        observationCoding.code = "9279-1";
        observationCoding.display = "Respiratory rate";
        observationCoding.system = "http://loinc.org";
        CodeableConcept observationCode = new CodeableConcept();
        observationCode.text = "Respiratory rate";
        observationCode.coding = Arrays.asList(observationCoding);

        Coding categoryCoding = new Coding();
        categoryCoding.code = "vital-signs";
        categoryCoding.display = "Vital Signs";
        categoryCoding.system = "http://loinc.org";
        CodeableConcept categoryCode = new CodeableConcept();
        categoryCode.text = "Vital Signs";
        categoryCode.coding = Arrays.asList(categoryCoding);

        FhirInstant issuedDate = FhirDateTimeParser.parseInstant("2013-04-03T15:30:10+01:00");
        FhirDateTime effectiveDate = FhirDateTimeParser.parseDateTime("2013-04-03");


        Quantity quantity = new Quantity();
        FhirDecimal period = new FhirDecimal(new BigDecimal(1));

        DecimalFormat decimalFormat = new DecimalFormat("######");
        int dataPoints = 1000;
        List<String> data = new ArrayList<>();
        for (int i = 0; i < dataPoints; i++) {
            data.add(decimalFormat.format(i));
        }


        StringBuilder stringBuilder = new StringBuilder();
        for (String string : data) {
            stringBuilder.append(string).append(" ");
        }

        SampledData sampledData = new SampledData(quantity, period, 1, stringBuilder.toString());

        return ObservationBuilder.buildWith(
                observationCode,
                sampledData,
                "breaths/minute",
                CodeSystemObservationStatus.FINAL,
                issuedDate,
                effectiveDate,
                categoryCode,
                null);
    }

    public static CarePlan getTestCarePlan() {
        Patient patient = PatientHelper.INSTANCE.buildWith("John", "Doe");
        Practitioner practitioner = PractitionerBuilder.buildWith("Dr. Bruce Banner, Praxis fuer Allgemeinmedizin");
        Medication medication = MedicationHelper.buildWith("Ibuprofen-ratiopharm", "tablets");
        MedicationHelper.addIngredient(medication, "Ibuprofen", 40f, "mg");
        Dosage morningDosage = DosageHelper.buildWith(2f, "Stueck", "morning");
        Dosage eveningDosage = DosageHelper.buildWith(2f, "Stueck", "evening");

        MedicationRequest medicationRequest = MedicationRequestHelper.buildWith(
                patient,
                medication,
                Arrays.asList(morningDosage, eveningDosage),
                "zur Oralen Einnahme",
                "Erkaeltungsbeschwerden bekaempfen");

        return CarePlanBuilder.buildWith(patient, practitioner, Arrays.asList(medicationRequest));
    }

    public static DocumentReference getTestDocumentReference() {
        FhirInstant indexed = FhirDateTimeParser.parseInstant("2013-04-03T15:30:10+01:00");

        byte[] pdf = new byte[512 * 1024]; //0.5MB
        System.arraycopy(MimeType.PDF.byteSignature()[0], 0, pdf, 0, MimeType.PDF.byteSignature()[0].length);
        Attachment attachment = new Attachment();
        attachment.data = Base64.encodeToString(pdf, android.util.Base64.NO_WRAP);

        Coding docTypeCoding = new Coding();
        docTypeCoding.code = "34108-1";
        docTypeCoding.display = "Outpatient Note";
        docTypeCoding.system = "http://loinc.org";
        CodeableConcept docTypeCode = new CodeableConcept();
        docTypeCode.coding = Arrays.asList(docTypeCoding);

        Practitioner author = PractitionerBuilder.buildWith("Dr. Bruce Banner, Praxis fuer Allgemeinmedizin");

        Coding practiceSpecialityCoding = new Coding();
        practiceSpecialityCoding.code = "General Medicine";
        practiceSpecialityCoding.display = "General Medicine";
        practiceSpecialityCoding.system = "http://www.ihe.net/xds/connectathon/practiceSettingCodes";
        CodeableConcept practiceSpecialityCode = new CodeableConcept();
        practiceSpecialityCode.coding = Arrays.asList(practiceSpecialityCoding);

        return DocumentReferenceBuilder.buildWith(
                "Physical",
                indexed,
                CodeSystemDocumentReferenceStatus.CURRENT,
                Arrays.asList(attachment),
                docTypeCode,
                author,
                practiceSpecialityCode);
    }

    public static DiagnosticReport getTestDiagnosticReport() {
        Coding reportCoding = new Coding();
        reportCoding.code = "GHP";
        reportCoding.display = "General Health Profile";
        reportCoding.system = "http://acme.com/labs/reports";

        CodeableConcept reportCode = new CodeableConcept();
        reportCode.coding = Arrays.asList(reportCoding);

        FhirInstant issuedDate = FhirDateTimeParser.parseInstant("2013-04-03T15:30:10+01:00");

        List<Observation> observations = new ArrayList<>();
        for (int i = 0; i < 5; i++) observations.add(getTestObservation());

        return DiagnosticReportBuilder.buildWith(
                reportCode,
                CodeSystemDiagnosticReportStatus.FINAL,
                "Acme Laboratory, Inc",
                issuedDate,
                observations);
    }
}
