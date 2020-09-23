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

import care.data4life.crypto.GCKey;
import care.data4life.crypto.error.CryptoException;
import care.data4life.fhir.Fhir;
import care.data4life.fhir.FhirParser;
import care.data4life.fhir.stu3.model.DomainResource;
import care.data4life.fhir.stu3.model.FhirElementFactory;
import care.data4life.sdk.lang.D4LException;
import io.reactivex.Single;

class FhirService {

    private final FhirParser parser;
    private final CryptoService cryptoService;


    FhirService(CryptoService cryptoService) {
        this(cryptoService, new Fhir().createStu3Parser());
    }

    FhirService(CryptoService cryptoService, FhirParser parser) {
        this.parser = parser;
        this.cryptoService = cryptoService;
    }

    @SuppressWarnings("unchecked")
    <T extends DomainResource> T decryptResource(GCKey dataKey, String resourceType, String encryptedResource) {
        return (T) Single
                .just(encryptedResource)
                .filter(er -> er != null && !er.isEmpty())
                .map(er -> cryptoService.decryptString(dataKey, er).blockingGet())
                .map(decryptedResourceJson -> parser.toFhir(FhirElementFactory.getClassForFhirType(resourceType), decryptedResourceJson))
                .toSingle()
                .onErrorResumeNext(error -> Single.error(
                        (D4LException) new CryptoException.DecryptionFailed("Failed to decrypt resource", error)))

                .blockingGet();
    }

    <T extends DomainResource> String encryptResource(GCKey dataKey, T resource) {
        return Single
                .just(resource)
                .map(parser::fromFhir)
                .flatMap(json -> cryptoService.encryptString(dataKey, json))
                .onErrorResumeNext(error ->
                        Single.error(
                                (D4LException) new CryptoException.EncryptionFailed("Failed to encrypt resource", error))
                )
                .blockingGet();
    }
}
