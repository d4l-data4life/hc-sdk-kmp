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


import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.squareup.moshi.Moshi;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import androidx.test.InstrumentationRegistry;
import care.data4life.crypto.GCAESKeyAlgorithm;
import care.data4life.crypto.GCKey;
import care.data4life.crypto.GCSymmetricKey;
import care.data4life.sdk.crypto.KeyFactory;
import care.data4life.sdk.util.Base64;

import static org.junit.Assert.assertEquals;

public class CryptoTest {

    private static final String commonKeyString = "Ak3iW3W6skJGNLj4KVD1y12kRTyupIIpsYXjyX7D2h8=";
    private static final String encryptedKeyString = "oQFZ/GJ++4+Rc/tIl6nraL7KKmGE5vXtJpsXyQXHkhuKduEDAZhmngAum1uTiOfpTYUJmfCSAGediSltbrfuktwiX5WAH2Pp4GHX6S5WOYImG57UwMBNZWPFpeUjKb+CHVuSo53C";
    private static final String expectedKeyJson = "{\"t\":\"tek\",\"v\":1,\"sym\":\"Zvcoyl8eJMOmj9i6KLV4ANrvboYDWT1IXU7igXTV7BU=\"}";
    private static final String expectedKey = "Zvcoyl8eJMOmj9i6KLV4ANrvboYDWT1IXU7igXTV7BU=";
    private static final String DEFAULT_ALIAS = "defaultAlias";


    GCAESKeyAlgorithm algorithm;
    GCSymmetricKey symmetricKey;
    GCKey commonKey;


    // SUT
    CryptoService service;


    @Before
    public void setup() throws Exception {
        algorithm = GCAESKeyAlgorithm.Companion.createDataAlgorithm();
        symmetricKey = new GCSymmetricKey(new SecretKeySpec(Base64.INSTANCE.decode(commonKeyString), algorithm.getTransformation()));
        commonKey = new GCKey(algorithm, symmetricKey, 256);

        service = new CryptoService(
                DEFAULT_ALIAS,
                null,
                new Moshi.Builder().build(),
                new SecureRandom(),
                Base64.INSTANCE,
                new KeyFactory(Base64.INSTANCE),
                null
        );

        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.addProvider(new BouncyCastleProvider());

        try {
            ProviderInstaller.installIfNeeded(InstrumentationRegistry.getContext());
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void decryptTest() throws UnsupportedEncodingException, NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchProviderException {

        byte[] data = Base64.INSTANCE.decode(encryptedKeyString);
        byte[] iv = Arrays.copyOfRange(data, 0, 16);
        byte[] ciphertext = Arrays.copyOfRange(data, 16, data.length);
        byte[] result = symDecrypt(commonKey, ciphertext, iv);

        String keyJson = new String(result, "UTF-8");

        assertEquals(keyJson, expectedKeyJson);
    }


    byte[] symDecrypt(GCKey key, byte[] data, byte[] iv) throws InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
        IvParameterSpec spec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, key.getSymmetricKey().getValue(), spec);
        return cipher.doFinal(data);
    }
}
