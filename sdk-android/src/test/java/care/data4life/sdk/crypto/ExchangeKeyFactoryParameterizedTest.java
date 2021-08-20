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

package care.data4life.sdk.crypto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import care.data4life.sdk.crypto.ExchangeKey;
import care.data4life.sdk.crypto.ExchangeKeyFactory;
import care.data4life.sdk.crypto.KeyType;
import care.data4life.sdk.crypto.KeyVersion;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ExchangeKeyFactoryParameterizedTest {

    private static final KeyVersion KEY_VERSION = KeyVersion.VERSION_1;
    private static final String KEY = "test_key";
    private KeyType keyType;
    private KeyVersion keyVersion;
    private String key;
    private ExchangeKey expectedExchangeKey;

    public ExchangeKeyFactoryParameterizedTest(KeyType keyType, KeyVersion keyVersion, String key, ExchangeKey expectedExchangeKey) {
        this.keyVersion = keyVersion;
        this.keyType = keyType;
        this.key = key;
        this.expectedExchangeKey = expectedExchangeKey;
    }

    @Parameterized.Parameters(name = "{index}: {0} should create to ExchangeKey")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {KeyType.APP_PUBLIC_KEY, KEY_VERSION, KEY, new ExchangeKey(KeyType.APP_PUBLIC_KEY, null, KEY, null, KEY_VERSION)},
                {KeyType.COMMON_KEY, KEY_VERSION, KEY, new ExchangeKey(KeyType.COMMON_KEY, null, null, KEY, KEY_VERSION)},
                {KeyType.DATA_KEY, KEY_VERSION, KEY, new ExchangeKey(KeyType.DATA_KEY, null, null, KEY, KEY_VERSION)},
                {KeyType.ATTACHMENT_KEY, KEY_VERSION, KEY, new ExchangeKey(KeyType.ATTACHMENT_KEY, null, null, KEY, KEY_VERSION)},
                {KeyType.TAG_KEY, KEY_VERSION, KEY, new ExchangeKey(KeyType.TAG_KEY, null, null, KEY, KEY_VERSION)}
        });
    }

    @Test
    public void test() {
        ExchangeKey exchangeKey = ExchangeKeyFactory.INSTANCE.createKey(keyVersion, keyType, key);
        assertEquals(expectedExchangeKey, exchangeKey);
    }

}
