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
import com.squareup.moshi.Types;

import java.io.IOException;
import java.lang.reflect.Type;

import care.data4life.crypto.ExchangeKey;
import care.data4life.crypto.GCKey;
import care.data4life.crypto.GCKeyPair;
import care.data4life.crypto.KeyType;
import care.data4life.crypto.error.CryptoException;
import care.data4life.sdk.lang.D4LException;
import care.data4life.securestore.SecureStoreContract;

public final class CryptoSecureStore {


    private final SecureStoreContract.SecureStore secureStore;
    private final Moshi moshi;


    public CryptoSecureStore(SecureStoreContract.SecureStore secureStore) {
        this.secureStore = secureStore;
        this.moshi = new Moshi.Builder().build();
    }

    public CryptoSecureStore(Moshi moshi, SecureStoreContract.SecureStore secureStore) {
        this.secureStore = secureStore;
        this.moshi = moshi;
    }


    public void clear() {
        secureStore.clear();
    }

    public void storeSecret(String alias, char[] secret) {
        secureStore.addData(alias, secret);
    }

    public char[] getSecret(String alias) throws D4LException {
        char[] key = secureStore.getData(alias);

        if (key != null)
            return key;
        else
            throw (D4LException) new CryptoException.DecryptionFailed("Failed to decrypt data");
    }

    public <T> void storeSecret(String alias, T object) {
        System.out.println(object);
        Type type = Types.getRawType(object.getClass());
        String jsonData = moshi.adapter(type).toJson(object);
        storeSecret(alias, jsonData.toCharArray());
    }

    public <T> T getSecret(String alias, Class<T> type) throws D4LException {
        String jsonData = secureStore.getData(alias) == null ? null : new String(secureStore.getData(alias));

        if (jsonData != null) {
            try {
                return moshi.adapter(type).fromJson(jsonData);
            } catch (IOException e) {
                throw (D4LException) new CryptoException.DecryptionFailed("Failed to decrypt data");
            }
        } else {
            throw (D4LException) new CryptoException.DecryptionFailed("Failed to decrypt data");
        }
    }

    public void deleteSecret(String alias) {
        secureStore.removeData(alias);
    }

    public void storeKey(String alias, GCKey key, KeyType keyType) {
        ExchangeKey exchangeKey = new ExchangeKey(keyType,
                new char[0],
                new char[0],
                key.getKeyBase64(),
                key.getKeyVersion());
        String json = moshi.adapter(ExchangeKey.class).toJson(exchangeKey);
        secureStore.addData(alias, json.toCharArray());
    }

    public void storeKey(String alias, GCKeyPair key) {
        ExchangeKey exchangeKey = new ExchangeKey(KeyType.APP_PRIVATE_KEY,
                key.getPrivateKeyBase64(),
                key.getPublicKeyBase64(),
                new char[0],
                key.getKeyVersion());
        String json = moshi.adapter(ExchangeKey.class).toJson(exchangeKey);
        secureStore.addData(alias, json.toCharArray());
    }

    public ExchangeKey getExchangeKey(String alias) throws IOException {
        String json = secureStore.getData(alias) == null ? null : new String(secureStore.getData(alias));

        return moshi.adapter(ExchangeKey.class).fromJson(json);
    }

    public boolean contains(String alias) {
        return secureStore.containsData(alias);
    }
}
