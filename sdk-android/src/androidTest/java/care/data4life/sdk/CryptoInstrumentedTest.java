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

import android.content.Context;

import com.squareup.moshi.Moshi;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import care.data4life.crypto.ExchangeKey;
import care.data4life.crypto.GCAESKeyAlgorithm;
import care.data4life.crypto.GCKey;
import care.data4life.crypto.GCKeyPair;
import care.data4life.crypto.GCRSAKeyAlgorithm;
import care.data4life.crypto.GCSymmetricKey;
import care.data4life.crypto.KeyOptions;
import care.data4life.crypto.KeyType;
import care.data4life.crypto.KeyVersion;
import care.data4life.crypto.error.CryptoException;
import care.data4life.sdk.network.model.EncryptedKey;
import care.data4life.sdk.test.data.model.AsymTestData;
import care.data4life.sdk.test.data.model.SymTestData;
import care.data4life.sdk.test.util.AssetsHelper;
import care.data4life.sdk.util.Base64;
import care.data4life.securestore.SecureStore;
import care.data4life.securestore.SecureStoreCryptor;
import care.data4life.securestore.SecureStoreStorage;
import io.reactivex.observers.TestObserver;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class CryptoInstrumentedTest {

    private static final String PATH = "design-documents/crypto/test-fixture/v1/";
    private static final String DEFAULT_ALIAS = "defaultAlias";

    private static final String SYM_COMMON_KEY_ENCRYPT_TEST_DATA = "symCommonEncrypt.json";
    private static final String SYM_COMMON_KEY_DECRYPT_TEST_DATA = "symCommonDecrypt.json";

    private static final String ASYM_KEY_ENCRYPT_TEST_DATA = "asymEncrypt.json";
    private static final String ASYM_KEY_DECRYPT_TEST_DATA = "asymDecrypt.json";


    private static final String GC_KEYPAIR = CryptoService.class.getPackage() + "gc_keypair";


    private CryptoService cryptoService;
    private Context context;
    private Moshi moshi = new Moshi.Builder().build();


    @Before
    public void setUp() {
        context = InstrumentationRegistry.getContext();
        CryptoSecureStore storage = new CryptoSecureStore(new SecureStore(new SecureStoreCryptor(context), new SecureStoreStorage(context)));
        cryptoService = new CryptoService(DEFAULT_ALIAS, storage);
    }

    @Test
    public void testSymCommonKeyEncrypt() throws Exception {
        // given
        SymTestData symTestData = loadSymTestData(SYM_COMMON_KEY_ENCRYPT_TEST_DATA);

        // when
        byte[] encryptedData = cryptoService.symEncrypt(symTestData.getGCKey(), symTestData.getInputDecoded(), symTestData.getIvDecoded());
        String data = Base64.INSTANCE.encodeToString(encryptedData);

        // then
        assertEquals(symTestData.getOutput(), data);
    }

    @Test
    public void testSymCommonKeyDecrypt() throws Exception {
        // given
        SymTestData symTestData = loadSymTestData(SYM_COMMON_KEY_DECRYPT_TEST_DATA);

        byte[] data = symTestData.getInputDecoded();
        byte[] iv = symTestData.getIvDecoded();

        // when
        byte[] decryptedData = cryptoService.symDecrypt(symTestData.getGCKey(), data, iv);

        // then
        assertEquals(symTestData.getOutput(), Base64.INSTANCE.encodeToString(decryptedData));
    }

    @Test
    public void verifySymmetricEncryptionFlow() throws Exception {
        // given
        SymTestData symTestData = loadSymTestData(SYM_COMMON_KEY_ENCRYPT_TEST_DATA);
        String inputData = "Hello World";

        // when
        byte[] encryptedData = cryptoService.encrypt(symTestData.getGCKey(), inputData.getBytes("UTF-8")).blockingGet();
        byte[] decryptedData = cryptoService.decrypt(symTestData.getGCKey(), encryptedData).blockingGet();

        String outputData = new String(decryptedData, "UTF-8");

        // then
        assertEquals(inputData, outputData);
    }

    @Test
    public void testAsymmetricDecryption() throws Exception {
        // given
        AsymTestData asymTestData = loadAsymTestData(ASYM_KEY_DECRYPT_TEST_DATA);

        // when
        byte[] decryptedData = cryptoService.asymDecrypt(asymTestData.getKey(), asymTestData.getInputDecoded());
        String data = Base64.INSTANCE.encodeToString(decryptedData);

        // then
        assertEquals(asymTestData.getOutput(), data);
    }

    @Test
    public void testAsymmetricEncryption() throws Exception {
        // given
        AsymTestData asymTestData = AssetsHelper.loadJson(context, PATH + "asymEncrypt.json", AsymTestData.class);

        // when
        byte[] encryptedData = cryptoService.asymEncrypt(asymTestData.getKey(), Base64.INSTANCE.decode(asymTestData.getInput()));
        byte[] decryptedData = cryptoService.asymDecrypt(asymTestData.getKey(), encryptedData);

        // then
        assertEquals(asymTestData.getInput(), Base64.INSTANCE.encodeToString(decryptedData));
    }

    @Test
    public void verifyAsymmetricEncryptionFlow() throws Exception {
        // given
        GCRSAKeyAlgorithm algorithm = new GCRSAKeyAlgorithm();
        KeyOptions options = new KeyOptions(2048, GC_KEYPAIR);
        GCKeyPair gcKeyPair = cryptoService.generateAsymKeyPair(algorithm, options);

        cryptoService.saveGCKeyPair(gcKeyPair);

        GCKeyPair gcKeyPairFromStorage = cryptoService.fetchGCKeyPair().blockingGet();

        String input = "hello world";

        // when
        byte[] encryptedData = cryptoService.asymEncrypt(gcKeyPairFromStorage, input.getBytes("UTF-8"));
        byte[] decryptedData = cryptoService.asymDecrypt(gcKeyPairFromStorage, encryptedData);

        // then
        assertEquals(input, new String(decryptedData, "UTF-8"));
    }

    @Test
    public void verifySymmetricStringEncryption() throws Exception {
        // given
        SymTestData symTestData = loadSymTestData(SYM_COMMON_KEY_ENCRYPT_TEST_DATA);

        String input = "hello world";

        // when
        String encryptedString = cryptoService.encryptString(symTestData.getGCKey(), input).blockingGet();
        String decryptedString = cryptoService.decryptString(symTestData.getGCKey(), encryptedString).blockingGet();

        // then
        assertEquals(input, decryptedString);
    }

    @Test
    public void encryptSymmetricKey_shouldCompleteWithoutErrors() throws Exception {
        // given
        SymTestData symTestData = loadSymTestData(SYM_COMMON_KEY_ENCRYPT_TEST_DATA);

        // when
        List<EncryptedKey> results = cryptoService
                .encryptSymmetricKey(symTestData.getGCKey(), KeyType.COMMON_KEY, symTestData.getGCKey())
                .test()
                .assertNoErrors()
                .assertComplete()
                .values();

        // then
        assertEquals(1, results.size());

        // as Json form is not normalized we need to verify it's parsed representation!
        String resultJson = cryptoService.decryptString(symTestData.getGCKey(), results.get(0).getEncryptedKey()).blockingGet();
        ExchangeKey resultExchangeKey = moshi.adapter(ExchangeKey.class).fromJson(resultJson);

        assertEquals(symTestData.getKey(), resultExchangeKey);
    }

    @Test
    public void decryptSymmetricKey_shouldCompleteWithoutErrors() throws Exception {
        // given
        SymTestData symTestData = loadSymTestData(SYM_COMMON_KEY_ENCRYPT_TEST_DATA);

        ExchangeKey exchangeKey = AssetsHelper.loadJson(context, PATH + "symCommonExchangeKey.json", ExchangeKey.class);

        GCAESKeyAlgorithm algorithm;
        if (exchangeKey.getType() == KeyType.TAG_KEY) {
            algorithm = GCAESKeyAlgorithm.Companion.createTagAlgorithm();
        } else {
            algorithm = GCAESKeyAlgorithm.Companion.createDataAlgorithm();
        }

        GCSymmetricKey symmetricKey = new GCSymmetricKey(new SecretKeySpec(Base64.INSTANCE.decode(exchangeKey.getSymmetricKey()), algorithm.getTransformation()));
        GCKey expected = new GCKey(algorithm, symmetricKey, 256);

        String keyJson = moshi.adapter(ExchangeKey.class).toJson(exchangeKey);
        String encryptedKeyBase64 = cryptoService.encryptString(symTestData.getGCKey(), keyJson).blockingGet();
        EncryptedKey input = new EncryptedKey(encryptedKeyBase64);

        // when
        List<GCKey> results = cryptoService
                .symDecryptSymmetricKey(symTestData.getGCKey(), input)
                .test()
                .assertNoErrors()
                .assertComplete().values();

        // then
        assertEquals(1, results.size());
        assertEquals(expected, results.get(0));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void decryptSymmetricKey_shouldFail_whenVersionMismatch() throws Exception {
        // given
        SymTestData symTestData = loadSymTestData(SYM_COMMON_KEY_DECRYPT_TEST_DATA);
        ExchangeKey exchangeKey = new ExchangeKey(KeyType.COMMON_KEY, null, null, symTestData.getKey().getSymmetricKey(), KeyVersion.VERSION_0);

        String keyJson = moshi.adapter(ExchangeKey.class).toJson(exchangeKey);
        String encryptedKeyBase64 = cryptoService.encryptString(symTestData.getGCKey(), keyJson).blockingGet();

        // when
        TestObserver<GCKey> observer = cryptoService
                .symDecryptSymmetricKey(symTestData.getGCKey(), new EncryptedKey(encryptedKeyBase64))
                .test();

        // then
        observer.assertError((Class<? extends Throwable>) CryptoException.InvalidKeyVersion.class);
        observer.assertError(throwable -> throwable.getMessage().equals("Key version '0' is not supported"));
    }

    @Test
    public void verifySymmetricKeyEncryption() throws Exception {
        // given
        SymTestData symTestData = loadSymTestData(SYM_COMMON_KEY_ENCRYPT_TEST_DATA);

        // when
        List<EncryptedKey> encryptedResults = cryptoService
                .encryptSymmetricKey(symTestData.getGCKey(), KeyType.COMMON_KEY, symTestData.getGCKey())
                .test()
                .assertNoErrors()
                .assertComplete()
                .values();

        List<GCKey> results = cryptoService
                .symDecryptSymmetricKey(symTestData.getGCKey(), encryptedResults.get(0))
                .test()
                .assertNoErrors()
                .assertComplete().values();

        // then
        assertEquals(1, results.size());
        assertEquals(symTestData.getGCKey(), results.get(0));
    }


    @Test
    public void testGenerateSymmetricKey() throws Exception {
        int keySizeBits = 256;

        GCAESKeyAlgorithm algorithm = GCAESKeyAlgorithm.Companion.createDataAlgorithm();
        KeyOptions options = new KeyOptions(keySizeBits);
        GCKey gcKey = cryptoService.generateSymKey(algorithm, options);

        assertEquals(keySizeBits / 8, gcKey.getSymmetricKey().getValue().getEncoded().length);
    }

    @Test
    public void testGenerateAsymmetricKeyPair() throws Exception {
        GCRSAKeyAlgorithm algorithm = new GCRSAKeyAlgorithm();
        KeyOptions options = new KeyOptions(2048, "test");
        GCKeyPair gcKeyPair = cryptoService.generateAsymKeyPair(algorithm, options);

        assertNotNull(gcKeyPair.getPrivateKey().getValue());
        assertNotNull(gcKeyPair.getPublicKey().getValue());
    }


    private SymTestData loadSymTestData(String fileName) throws IOException {
        return AssetsHelper.loadJson(context, PATH + fileName, SymTestData.class);
    }

    private AsymTestData loadAsymTestData(String fileName) throws IOException {
        return AssetsHelper.loadJson(context, PATH + fileName, AsymTestData.class);
    }
}
