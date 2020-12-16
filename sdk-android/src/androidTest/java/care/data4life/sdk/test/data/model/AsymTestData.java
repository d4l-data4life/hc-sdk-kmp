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

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import care.data4life.crypto.ExchangeKey;
import care.data4life.crypto.GCAsymmetricKey;
import care.data4life.crypto.GCKeyPair;
import care.data4life.crypto.GCRSAKeyAlgorithm;
import care.data4life.sdk.util.Base64;
import care.data4life.sdk.util.CharByteConversionKt;


public class AsymTestData {

    private String test;
    private String input;
    private String output;

    private ExchangeKey privateKey;
    private ExchangeKey publicKey;

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


    public GCKeyPair getKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        GCRSAKeyAlgorithm algorithm = new GCRSAKeyAlgorithm();
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.INSTANCE.decode((privateKey.getPrivateKey().toBytes())));
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(Base64.INSTANCE.decode(publicKey.getPublicKey().toBytes));
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm.getCipher());
        PrivateKey privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
        PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
        GCAsymmetricKey gcPrivateKey = new GCAsymmetricKey(privateKey, GCAsymmetricKey.Type.Private);
        GCAsymmetricKey gcPublicKey = new GCAsymmetricKey(publicKey, GCAsymmetricKey.Type.Public);
        return new GCKeyPair(algorithm, gcPrivateKey, gcPublicKey, 256);
    }
}
