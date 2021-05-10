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

package care.data4life.sdk.tag;

import android.content.Context;

import androidx.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import care.data4life.sdk.CryptoSecureStore;
import care.data4life.sdk.CryptoService;
import care.data4life.sdk.test.data.model.SymTestData;
import care.data4life.sdk.test.util.AssetsHelper;
import care.data4life.securestore.SecureStore;
import care.data4life.securestore.SecureStoreCryptor;
import care.data4life.securestore.SecureStoreStorage;

import static junit.framework.Assert.assertEquals;

public class TagCryptoServiceInstrumentedTest {

    private static final String PATH = "design-documents/crypto/test-fixture/v1/";
    private static final String DEFAULT_ALIAS = "data4life_android";

    private Context context;
    private TagCryptoService tekService;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getContext();
        SecureStore secureStore = new SecureStore(new SecureStoreCryptor(context), new SecureStoreStorage(context));
        CryptoSecureStore storage = new CryptoSecureStore(secureStore);
        CryptoService cryptoService = new CryptoService(DEFAULT_ALIAS, storage);
        tekService = new TagCryptoService(cryptoService);
    }

    @Test
    public void testTekEncrypt() throws Exception {
        // given
        SymTestData symTestData = loadSymEncryptTestData();
        // when
        String encryptedTag = tekService.encryptTag(symTestData.getGCKey(), symTestData.getInput()).blockingGet();
        // then
        assertEquals(symTestData.getOutput(), encryptedTag);
    }

    @Test
    public void testTekDecrypt() throws Exception {
        // given
        SymTestData symTestData = loadSymDecryptTestData();
        // when
        String decryptedData = tekService.decryptTag(symTestData.getGCKey(), symTestData.getInput()).blockingGet();
        // then
        assertEquals(symTestData.getOutput(), decryptedData);
    }


    @Test
    public void verifyEncryptDecryptTagsFlow() throws Exception {
        // given
        SymTestData symTestData = loadSymEncryptTestData();
        String tag = "tag";

        //when
        String encryptedBase64Tag = tekService.encryptTag(symTestData.getGCKey(), tag).blockingGet();
        String decryptedTag = tekService.decryptTag(symTestData.getGCKey(), encryptedBase64Tag).blockingGet();

        //then
        assertEquals(tag, decryptedTag);
    }

    private SymTestData loadSymEncryptTestData() throws IOException {
        return AssetsHelper.loadJson(context, PATH + "symTagEncrypt.json", SymTestData.class);
    }

    private SymTestData loadSymDecryptTestData() throws IOException {
        return AssetsHelper.loadJson(context, PATH + "symTagDecrypt.json", SymTestData.class);
    }
}
