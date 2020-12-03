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
import java.util.concurrent.Callable;
import java.util.function.Function;

import care.data4life.crypto.GCKey;
import care.data4life.crypto.error.CryptoException;
import care.data4life.sdk.lang.D4LException;
import care.data4life.sdk.tags.TagHelper;
import care.data4life.sdk.util.Base64;
import io.reactivex.Observable;
import io.reactivex.Single;

import static care.data4life.sdk.TaggingService.TAG_DELIMITER;

class TagEncryptionService {
    interface Transformer<T> {
        T run(List<String> decryptedList);
    }

    interface Condition {
        Boolean run(String decryptedItem);
    }


    static final int IV_SIZE = 16;
    private final static String ANNOTATION_KEY = "custom" + TAG_DELIMITER;

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

    private List<String> encryptList(List<String> list, String prefix) throws IOException {
        GCKey tek = cryptoService.fetchTagEncryptionKey();
        return Observable
                .fromIterable(list)
                .map(tag -> encryptTag(tek, prefix+tag).blockingGet())
                .toList()
                .blockingGet();
    }

    private<T extends Object> T decryptList(
            List<String> encryptedList,
            Condition condition,
            Transformer<T> transform
    ) throws IOException {
        GCKey tek = cryptoService.fetchTagEncryptionKey();
        return Observable
                .fromIterable(encryptedList)
                .map(encryptedTag -> decryptTag(tek, encryptedTag).blockingGet())
                .filter(condition::run)
                .toList()
                .map(transform::run)
                .blockingGet();
    }

    List<String> encryptTags(HashMap<String, String> tags) throws IOException {
        return encryptList(TagHelper.convertToTagList(tags), "");
    }

    HashMap<String, String> decryptTags(List<String> encryptedTags) throws IOException {
        return decryptList(
                encryptedTags,
                (String d) -> !d.startsWith(ANNOTATION_KEY) && d.contains(TAG_DELIMITER),
                TagHelper::convertToTagMap
        );
    }

    List<String> encryptAnnotations(List<String> annotations) throws IOException {
        return encryptList(annotations, ANNOTATION_KEY);
    }

    private static List<String> removeAnnotationKey(List<String> list) {
        for(int idx = 0; idx < list.size(); idx++) {
            list.set(idx, list.get(idx).replaceFirst(ANNOTATION_KEY, ""));
        }
        return list;
    }

    List<String> decryptAnnotations(List<String> encryptedAnnotations) throws IOException {
        return decryptList(
                encryptedAnnotations,
                (d) -> d.startsWith(ANNOTATION_KEY),
                TagEncryptionService::removeAnnotationKey
        );
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
