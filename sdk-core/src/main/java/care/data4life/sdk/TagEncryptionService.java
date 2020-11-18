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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import care.data4life.crypto.GCKey;
import care.data4life.crypto.error.CryptoException;
import care.data4life.sdk.lang.D4LException;
import care.data4life.sdk.tags.TagHelper;
import care.data4life.sdk.util.Base64;
import io.reactivex.Observable;
import io.reactivex.Single;

import static care.data4life.sdk.TaggingService.TAG_DELIMITER;

class TagEncryptionService {

    static final int IV_SIZE = 16;
    private static String ANNOTATION_KEY = "custom=";

    private final byte[] iv;
    private CryptoService cryptoService;
    private Base64 base64;

    TagEncryptionService(CryptoService cryptoService) {
        this(cryptoService, Base64.INSTANCE);
    }

    protected TagEncryptionService(CryptoService cryptoService, Base64 base64) {
        this.cryptoService = cryptoService;
        this.base64 = base64;
        iv = new byte[IV_SIZE];
    }

    List<String> encryptTags(Map<String, String> tags) throws IOException {
        GCKey tek = cryptoService.fetchTagEncryptionKey();
        return Observable
                .fromIterable(TagHelper.convertToTagList(tags))
                .map(tag -> encryptTag(tek, tag).blockingGet())
                .toList()
                .blockingGet();
    }

    HashMap<String, String> decryptTags(List<String> encryptedTags) throws IOException {
        GCKey tek = cryptoService.fetchTagEncryptionKey();
        return Observable
                .fromIterable(encryptedTags)
                .map(encryptedTag -> decryptTag(tek, encryptedTag).blockingGet())
                .filter(decryptedTag -> decryptedTag.contains(TAG_DELIMITER))
                .filter(decryptedTag -> !decryptedTag.contains(ANNOTATION_KEY))
                .toList()
                .map(TagHelper::convertToTagMap)
                .blockingGet();
    }

    List<String> encryptAnnotations(List<String> annotations) throws IOException {
        GCKey tek = cryptoService.fetchTagEncryptionKey();
        return Observable.fromIterable(annotations)
                .map(tag->encryptTag(tek,ANNOTATION_KEY+tag).blockingGet())
                .toList()
                .blockingGet();
    }

    List<String> decryptAnnotations(List<String> annotations) throws IOException {
        GCKey tek = cryptoService.fetchTagEncryptionKey();
        return Observable.fromIterable(annotations)
                .map(tag->decryptTag(tek,tag).blockingGet())
                .filter(decryptedTag->decryptedTag.contains(ANNOTATION_KEY))
                .map(tag->tag.replace(ANNOTATION_KEY,""))
                .toList()
                .blockingGet();
    }

    Single<String> encryptTag(GCKey key, String tag) {
        return Single
                .fromCallable(tag::getBytes)
                .map(d -> cryptoService.symEncrypt(key, d, iv))
                .map(base64::encodeToString)
                .onErrorResumeNext(error -> Single.error(
                        (D4LException) new CryptoException.EncryptionFailed("Failed to encrypt tag")
                ));
    }

    Single<String> decryptTag(GCKey key, String base64tag) {
        return Single
                .fromCallable(() -> base64.decode(base64tag))
                .map(d -> cryptoService.symDecrypt(key, d, iv))
                .map(decrypted -> new String(decrypted, "UTF-8"))
                .onErrorResumeNext(error -> Single.error((D4LException)
                        new CryptoException.DecryptionFailed("Failed to decrypt tag")
                ));
    }
}
