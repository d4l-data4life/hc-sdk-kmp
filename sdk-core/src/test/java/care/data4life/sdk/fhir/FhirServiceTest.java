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

package care.data4life.sdk.fhir;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import care.data4life.crypto.GCKey;
import care.data4life.crypto.error.CryptoException;
import care.data4life.fhir.FhirException;
import care.data4life.fhir.FhirParser;
import care.data4life.fhir.stu3.model.DocumentReference;
import care.data4life.fhir.stu3.model.DomainResource;
import care.data4life.sdk.CryptoService;
import io.reactivex.Single;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FhirServiceTest {

    private static final String ENCRYPTED_RESOURCE = "encryptedResource";
    private static final String JSON_RESOURCE = "jsonResource";

    private final FhirException parseException = new FhirException(FhirException.ErrorType.DECODE, FhirException.ErrorCode.FAILED_TO_PARSE_JSON, "");
    private final RuntimeException unkwnownException = new RuntimeException();

    private final Class fhirClass = DocumentReference.class;
    private final String fhirType = DocumentReference.resourceType;
    private final DocumentReference mockDocumentReference = mock(DocumentReference.class);
    private final GCKey dataKey = mock(GCKey.class);

    private FhirParser mockFhirParser;
    private CryptoService mockCryptoService;
    private FhirService fhirService; // SUT


    @Before
    public void setUp() {
        mockCryptoService = mock(CryptoService.class);
        mockFhirParser = mock(FhirParser.class);
        fhirService = new FhirService(mockCryptoService, mockFhirParser);
    }

    @Test
    public void decryptResource_shouldReturnResource() throws FhirException {
        // Given
        when(mockCryptoService.decryptString(dataKey, ENCRYPTED_RESOURCE)).thenReturn(Single.just(JSON_RESOURCE));
        when(mockFhirParser.toFhir(fhirClass, JSON_RESOURCE)).thenReturn(mockDocumentReference);

        // When
        DomainResource resource = fhirService.decryptResource(dataKey, fhirType, ENCRYPTED_RESOURCE);

        // Then
        assertThat(resource).isEqualTo(mockDocumentReference);

        InOrder inOrder = Mockito.inOrder(mockCryptoService, mockFhirParser);
        inOrder.verify(mockCryptoService).decryptString(dataKey, ENCRYPTED_RESOURCE);
        inOrder.verify(mockFhirParser).toFhir(fhirClass, JSON_RESOURCE);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void encryptResource_shouldReturnEncryptedResource() throws FhirException {
        // given
        when(mockFhirParser.fromFhir(mockDocumentReference)).thenReturn(JSON_RESOURCE);
        when(mockCryptoService.encryptString(dataKey, JSON_RESOURCE)).thenReturn(Single.just(ENCRYPTED_RESOURCE));

        // when
        String result = fhirService.encryptResource(dataKey, mockDocumentReference);

        // then
        assertThat(result).isEqualTo(ENCRYPTED_RESOURCE);

        InOrder inOrder = Mockito.inOrder(mockCryptoService, mockFhirParser);
        inOrder.verify(mockFhirParser).fromFhir(mockDocumentReference);
        inOrder.verify(mockCryptoService).encryptString(dataKey, JSON_RESOURCE);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void decryptResource_shouldThrowException_whenDecryptErrorHappens() {
        // given
        when(mockCryptoService.decryptString(dataKey, ENCRYPTED_RESOURCE)).thenThrow(unkwnownException);

        try {
            // when
            fhirService.decryptResource(dataKey, DocumentReference.resourceType, ENCRYPTED_RESOURCE);
            fail("Exception expected");
        } catch (RuntimeException e) {

            //Then
            RuntimeException firstException = (RuntimeException) e.getCause().getCause();
            assertThat(firstException).isEqualTo(unkwnownException);

            assertThat(e.getCause()).isInstanceOf(CryptoException.DecryptionFailed.class);
            assertThat(e.getCause().getMessage()).isEqualTo("Failed to decrypt resource");
        }

        InOrder inOrder = Mockito.inOrder(mockCryptoService, mockFhirParser);
        inOrder.verify(mockCryptoService).decryptString(dataKey, ENCRYPTED_RESOURCE);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void decryptResource_shouldThrowException_whenParseErrorHappens() throws FhirException {
        // given
        when(mockCryptoService.decryptString(dataKey, ENCRYPTED_RESOURCE)).thenReturn(Single.just(JSON_RESOURCE));
        when(mockFhirParser.toFhir(DocumentReference.class, JSON_RESOURCE)).thenThrow(parseException);

        try {
            // when
            fhirService.decryptResource(dataKey, DocumentReference.resourceType, ENCRYPTED_RESOURCE);
            fail("Exception expected");
        } catch (RuntimeException e) {

            //Then
            FhirException firstExc = (FhirException) e.getCause().getCause();
            assertThat(firstExc).isEqualTo(parseException);

            assertThat(e.getCause()).isInstanceOf(CryptoException.DecryptionFailed.class);
            assertThat(e.getCause().getMessage()).isEqualTo("Failed to decrypt resource");
        }

        InOrder inOrder = Mockito.inOrder(mockCryptoService, mockFhirParser);
        inOrder.verify(mockCryptoService).decryptString(dataKey, ENCRYPTED_RESOURCE);
        inOrder.verify(mockFhirParser).toFhir(fhirClass, JSON_RESOURCE);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void encryptResource_shouldThrowException_whenParseErrorHappens() throws FhirException {
        // given
        when(mockFhirParser.fromFhir(mockDocumentReference)).thenThrow(parseException);

        try {
            // when
            fhirService.encryptResource(dataKey, mockDocumentReference);
            fail("Exception expected!");
        } catch (RuntimeException e) {

            // then
            FhirException firstException = (FhirException) e.getCause().getCause();
            assertThat(firstException).isEqualTo(parseException);

            assertThat(e.getCause()).isInstanceOf(CryptoException.EncryptionFailed.class);
            assertThat(e.getCause().getMessage()).isEqualTo("Failed to encrypt resource");
        }

        InOrder inOrder = Mockito.inOrder(mockCryptoService, mockFhirParser);
        inOrder.verify(mockFhirParser).fromFhir(mockDocumentReference);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void encryptResource_shouldThrowException_whenEncryptErrorHappens() throws FhirException {
        // given
        when(mockFhirParser.fromFhir(mockDocumentReference)).thenReturn(JSON_RESOURCE);
        when(mockCryptoService.encryptString(dataKey, JSON_RESOURCE)).thenThrow(unkwnownException);

        try {
            // when
            fhirService.encryptResource(dataKey, mockDocumentReference);
            fail("Exception expected!");
        } catch (RuntimeException e) {

            // then
            RuntimeException firstException = (RuntimeException) e.getCause().getCause();
            assertThat(firstException).isEqualTo(unkwnownException);

            assertThat(e.getCause()).isInstanceOf(CryptoException.EncryptionFailed.class);
            assertThat(e.getCause().getMessage()).isEqualTo("Failed to encrypt resource");
        }

        InOrder inOrder = Mockito.inOrder(mockCryptoService, mockFhirParser);
        inOrder.verify(mockFhirParser).fromFhir(mockDocumentReference);
        inOrder.verify(mockCryptoService).encryptString(dataKey, JSON_RESOURCE);
        inOrder.verifyNoMoreInteractions();
    }
}
