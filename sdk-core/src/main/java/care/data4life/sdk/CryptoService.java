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

import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

import care.data4life.crypto.CryptoProtocol;
import care.data4life.crypto.ExchangeKey;
import care.data4life.crypto.ExchangeKeyFactory;
import care.data4life.crypto.GCAESKeyAlgorithm;
import care.data4life.crypto.GCAsymmetricKey;
import care.data4life.crypto.GCKey;
import care.data4life.crypto.GCKeyPair;
import care.data4life.crypto.GCRSAKeyAlgorithm;
import care.data4life.crypto.KeyOptions;
import care.data4life.crypto.KeyType;
import care.data4life.crypto.KeyVersion;
import care.data4life.crypto.error.CryptoException;
import care.data4life.sdk.crypto.CommonKeyService;
import care.data4life.sdk.crypto.KeyFactory;
import care.data4life.sdk.lang.D4LException;
import care.data4life.sdk.network.model.EncryptedKey;
import care.data4life.sdk.util.Base64;
import io.reactivex.Single;

import static care.data4life.sdk.log.Log.error;

class CryptoService extends CryptoProtocol {

    private static final String TEK_KEY = "crypto_tag_encryption_key";
    private static final String GC_KEYPAIR = "crypto_gc_keypair";

    private static final KeyVersion KEY_VERSION = KeyVersion.VERSION_1;
    private static final int IV_SIZE = 12;


    private final Moshi moshi;
    private String alias;
    private CryptoSecureStore storage;
    private SecureRandom rng;
    private Base64 base64;
    private KeyFactory keyFactory;
    private CommonKeyService commonKeyService;


    // for testing only
    protected CryptoService(
            String alias,
            CryptoSecureStore storage,
            Moshi moshi,
            SecureRandom rng,
            Base64 base64,
            KeyFactory keyFactory,
            CommonKeyService commonKeyService
    ) {
        this.alias = alias;
        this.storage = storage;
        this.moshi = moshi;
        this.rng = rng;
        this.base64 = base64;
        this.keyFactory = keyFactory;
        this.commonKeyService = commonKeyService;
    }

    public CryptoService(
            String alias,
            CryptoSecureStore storage
    ) {
        this.alias = alias;
        this.storage = storage;
        this.moshi = new Moshi.Builder().build();
        this.rng = new SecureRandom();
        this.base64 = Base64.INSTANCE;
        this.keyFactory = new KeyFactory(base64);
        this.commonKeyService = new CommonKeyService(alias, storage, keyFactory);
    }


    public Single<byte[]> encrypt(GCKey key, byte[] data) {
        return Single
                .fromCallable(() -> data)
                .map(d -> {
                    byte[] iv = new byte[IV_SIZE];
                    rng.nextBytes(iv);
                    byte[] ciphertext = symEncrypt(key, d, iv);
                    byte[] result = new byte[iv.length + ciphertext.length];
                    System.arraycopy(iv, 0, result, 0, iv.length);
                    System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
                    return result;
                })
                .onErrorResumeNext(error -> {
                    error(error, "Failed to encrypt data");
                    return Single.error((D4LException) new CryptoException.EncryptionFailed("Failed to encrypt data"));
                });
    }

    public Single<byte[]> decrypt(GCKey key, byte[] data) {
        return Single
                .fromCallable(() -> data)
                .map(d -> {
                    byte[] iv = Arrays.copyOfRange(data, 0, IV_SIZE);
                    byte[] ciphertext = Arrays.copyOfRange(data, IV_SIZE, data.length);
                    return symDecrypt(key, ciphertext, iv);
                })
                .onErrorResumeNext(error -> {
                    error(error, "Failed to decrypt data");
                    return Single.error((D4LException) new CryptoException.DecryptionFailed("Failed to decrypt data"));
                });
    }

    public Single<String> encryptString(GCKey key, String data) {
        return encrypt(key, data.getBytes())
                .map(base64::encodeToString)
                .onErrorResumeNext(error -> {
                    error(error, "Failed to encrypt string");
                    return Single.error((D4LException) new CryptoException.EncryptionFailed("Failed to encrypt string"));
                });
    }

    public Single<String> decryptString(GCKey key, String dataBase64) {
        return Single
                .fromCallable(() -> base64.decode(dataBase64))
                .flatMap(decoded -> decrypt(key, decoded))
                .map(decrypted -> new String(decrypted, "UTF-8"))
                .onErrorResumeNext(error -> {
                    error(error, "Failed to decrypt string");
                    return Single.error((D4LException) new CryptoException.DecryptionFailed("Failed to decrypt string"));
                });
    }

    public Single<String> encryptCharArray(GCKey key, char[] data) {
        byte[] bytes = StandardCharsets.UTF_8.encode(CharBuffer.wrap(data)).array();
        return encrypt(key, bytes)
                .map(base64::encodeToString)
                .onErrorResumeNext(error -> {
                    error(error, "Failed to encrypt string");
                    return Single.error((D4LException) new CryptoException.EncryptionFailed("Failed to encrypt string"));
                });
    }

    public Single<char[]> decryptToCharArray(GCKey key, String dataBase64) {
        return Single
                .fromCallable(() -> base64.decode(dataBase64))
                .flatMap(decoded -> decrypt(key, decoded))
                .map(decrypted -> StandardCharsets.UTF_8.decode(ByteBuffer.wrap(decrypted)).array())
                .onErrorResumeNext(error -> {
                    error(error, "Failed to decrypt string");
                    return Single.error((D4LException) new CryptoException.DecryptionFailed("Failed to decrypt string"));
                });
    }

    public Single<EncryptedKey> encryptSymmetricKey(GCKey key, KeyType keyType, GCKey gckey) {
        return Single.fromCallable(() -> ExchangeKeyFactory.INSTANCE.createKey(KEY_VERSION, keyType, gckey.getKeyBase64()))
                .map(exchangeKey -> moshi.adapter(ExchangeKey.class).toJson(exchangeKey))
                .flatMap(jsonKey -> encryptString(key, jsonKey))
                .map(EncryptedKey::new)
                .onErrorResumeNext(error -> {
                    error(error, "Failed to encrypt GcKey");
                    return Single.error((D4LException) new CryptoException.KeyEncryptionFailed("Failed to encrypt GcKey"));
                });
    }

    public Single<GCKey> symDecryptSymmetricKey(GCKey key, EncryptedKey encryptedKey) {
        return decryptString(key, encryptedKey.getEncryptedKey())
                .map(keyJson -> moshi.adapter(ExchangeKey.class).fromJson(keyJson))
                .flatMap(this::convertExchangeKeyToGCKey);
    }

    Single<GCKey> asymDecryptSymetricKey(GCKeyPair keyPair, EncryptedKey encryptedKey) {
        return Single.fromCallable(() -> base64.decode(encryptedKey.getEncryptedKey()))
                .map(ck -> asymDecrypt(keyPair, ck))
                .map(keyJson -> moshi.adapter(ExchangeKey.class).fromJson(new String(keyJson, "UTF-8")))
                .flatMap(this::convertExchangeKeyToGCKey);
    }

    Single<GCKey> convertExchangeKeyToGCKey(ExchangeKey exchangeKey) {
        return Single.fromCallable(() -> exchangeKey)
                .map(ek -> {
                    if (ek.getVersion() != KeyVersion.VERSION_1) {
                        throw (D4LException) new CryptoException.InvalidKeyVersion(exchangeKey.getVersion().getValue());
                    } else if (ek.getType() == KeyType.APP_PUBLIC_KEY || ek.getType() == KeyType.APP_PRIVATE_KEY) {
                        throw (D4LException) new CryptoException.KeyDecryptionFailed("can't decrypt asymmetric to symmetric key");
                    }
                    return keyFactory.createGCKey(exchangeKey);
                })
                .onErrorResumeNext(error -> {
                    error(error, "Failed to decrypt exchangeKey");
                    if (error instanceof D4LException) {
                        return Single.error(error);
                    }
                    return Single.error((D4LException) new CryptoException.KeyDecryptionFailed("Failed to decrypt exchange key"));
                });
    }

    public Single<String> convertAsymmetricKeyToBase64ExchangeKey(GCAsymmetricKey gcAsymmetricKey) {
        return Single.fromCallable(() -> gcAsymmetricKey.getValue().getEncoded()) //SPKI
                .map(base64::encodeToString)
                .map(encodedKey -> ExchangeKeyFactory.INSTANCE.createKey(KEY_VERSION, KeyType.APP_PUBLIC_KEY, encodedKey))
                .map(exchangeKey -> moshi.adapter(ExchangeKey.class).toJson(exchangeKey))
                .map(base64::encodeToString);
    }

    public Single<GCKey> generateGCKey() {
        return Single
                .fromCallable(GCAESKeyAlgorithm.Companion::createDataAlgorithm)
                .map(algorithm -> {
                    KeyOptions options = new KeyOptions(KEY_VERSION.getSymmetricKeySize());
                    return generateSymKey(algorithm, options);
                })
                .onErrorResumeNext(error -> {
                    error(error, "Failed to generate encryption key");
                    return Single.error((D4LException) new CryptoException.KeyGenerationFailed("Failed to generate encryption key"));
                });
    }

    public Single<GCKeyPair> generateGCKeyPair() {
        return Single
                .fromCallable(GCRSAKeyAlgorithm::new)
                .map(algorithm -> {
                    deleteGCKeyPair();
                    KeyOptions options = new KeyOptions(KEY_VERSION.getAsymmetricKeySize(), GC_KEYPAIR);
                    return generateAsymKeyPair(algorithm, options);
                })
                .map(gcKeyPair -> {
                    saveGCKeyPair(gcKeyPair);
                    return gcKeyPair;
                });
    }

    void saveGCKeyPair(GCKeyPair keyPair) {
        storage.storeKey(prefix() + GC_KEYPAIR, keyPair);
    }

    void deleteGCKeyPair() {
        deleteSecret(GC_KEYPAIR);
    }

    Single<GCKeyPair> fetchGCKeyPair() {
        return Single
                .fromCallable(() -> getExchangeKey(GC_KEYPAIR))
                .map(exchangeKey -> keyFactory.createGCKeyPair(exchangeKey))
                .onErrorResumeNext(error -> {
                    error(error, "Failed to fetch encryption key");
                    return Single.error((D4LException) new CryptoException.KeyFetchingFailed("Failed to fetch encryption key"));
                });
    }


    void storeTagEncryptionKey(GCKey tek) {
        storeKey(TEK_KEY, tek, KeyType.TAG_KEY);
    }

    GCKey fetchTagEncryptionKey() throws IOException {
        return getGCKey(TEK_KEY);
    }

    private void storeKey(String key, GCKey value, KeyType keyType) {
        storage.storeKey(prefix() + key, value, keyType);
    }

    private GCKey getGCKey(String key) throws IOException {
        return keyFactory.createGCKey(getExchangeKey(key));
    }

    private ExchangeKey getExchangeKey(String key) throws IOException {
        return storage.getExchangeKey(prefix() + key);
    }

    private void deleteSecret(String key) {
        storage.deleteSecret(prefix() + key);
    }

    private String prefix() {
        return alias + "_";
    }


    // Common Key Handling

    GCKey fetchCurrentCommonKey() throws IOException {
        return commonKeyService.fetchCurrentCommonKey();
    }

    GCKey getCommonKeyById(String commonKeyId) throws IOException {
        return commonKeyService.fetchCommonKey(commonKeyId);
    }

    String getCurrentCommonKeyId() {
        return commonKeyService.fetchCurrentCommonKeyId();
    }

    void storeCurrentCommonKeyId(String commonKeyId) {
        commonKeyService.storeCurrentCommonKeyId(commonKeyId);
    }

    void storeCommonKey(String commonKeyId, GCKey commonKey) {
        commonKeyService.storeCommonKey(commonKeyId, commonKey);
    }

    boolean hasCommonKey(String commonKeyId) {
        return commonKeyService.hasCommonKey(commonKeyId);
    }

}
