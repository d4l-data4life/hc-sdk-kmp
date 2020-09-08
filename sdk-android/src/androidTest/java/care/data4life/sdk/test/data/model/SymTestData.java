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

package care.data4life.sdk.test.data.model;

import javax.crypto.spec.SecretKeySpec;

import care.data4life.crypto.ExchangeKey;
import care.data4life.crypto.GCAESKeyAlgorithm;
import care.data4life.crypto.GCKey;
import care.data4life.crypto.GCSymmetricKey;
import care.data4life.crypto.KeyType;
import care.data4life.crypto.KeyVersion;
import care.data4life.sdk.util.Base64;

public class SymTestData {

    private String test;
    private String input;
    private String output;
    private String iv;
    private ExchangeKey key;

    public String getTest() {
        return test;
    }

    public String getInput() {
        return input;
    }

    public byte[] getInputDecoded() {
        return Base64.INSTANCE.decode(input);
    }

    public String getOutput() {
        return output;
    }

    public byte[] getOutputDecoded() {
        return Base64.INSTANCE.decode(input);
    }

    public String getIv() {
        return iv;
    }

    public byte[] getIvDecoded() {
        return Base64.INSTANCE.decode(iv);
    }

    public ExchangeKey getKey() {
        return key;
    }

    public GCKey getGCKey() {
        if (key.getVersion() != KeyVersion.VERSION_1) {
            return null;
        } else if (key.getType() == KeyType.APP_PUBLIC_KEY || key.getType() == KeyType.APP_PRIVATE_KEY) {
            return null;
        }

        GCAESKeyAlgorithm algorithm;
        if (key.getType() == KeyType.TAG_KEY) {
            algorithm = GCAESKeyAlgorithm.Companion.createTagAlgorithm();
        } else {
            algorithm = GCAESKeyAlgorithm.Companion.createDataAlgorithm();
        }

        GCSymmetricKey symmetricKey;
        symmetricKey = new GCSymmetricKey(
                new SecretKeySpec(Base64.INSTANCE.decode(key.getSymmetricKey()),
                        algorithm.getTransformation()
                ));

        return new GCKey(algorithm, symmetricKey, 256);
    }
}
