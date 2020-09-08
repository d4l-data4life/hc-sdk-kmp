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

import care.data4life.crypto.ExchangeKey;
import care.data4life.crypto.ExchangeKeyFactory;
import care.data4life.crypto.KeyType;
import care.data4life.crypto.KeyVersion;
import care.data4life.sdk.lang.D4LException;

import static org.junit.Assert.assertEquals;

public class ExchangeKeyFactoryTest {

    private static final KeyVersion KEY_VERSION = KeyVersion.VERSION_1;
    private static final KeyType KEY_TYPE_COMMON = KeyType.COMMON_KEY;
    private static final String KEY_BASE_64 = "key_base_64";

    @Test(expected = IllegalArgumentException.class)
    public void createKey_shouldFail_whenVersionUnknown() {
        ExchangeKeyFactory.INSTANCE.createKey(null, KEY_TYPE_COMMON, KEY_BASE_64);
    }


    @Test(expected = D4LException.class)
    public void createKey_shouldFail_whenKeyTypePrivateKey() {
        ExchangeKeyFactory.INSTANCE.createKey(KeyVersion.VERSION_1, KeyType.APP_PRIVATE_KEY, KEY_BASE_64);
    }

    @Test
    public void createKey_shouldReturnValidKey() {
        // given
        ExchangeKey expected = new ExchangeKey(KEY_TYPE_COMMON, null, null, KEY_BASE_64, KEY_VERSION);

        // when
        ExchangeKey actual = ExchangeKeyFactory.INSTANCE.createKey(KeyVersion.VERSION_1, KEY_TYPE_COMMON, KEY_BASE_64);

        // then
        assertEquals(expected, actual);
    }
}
