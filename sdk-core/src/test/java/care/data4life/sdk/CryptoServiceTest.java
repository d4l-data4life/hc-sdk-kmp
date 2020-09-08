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

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import care.data4life.crypto.ExchangeKey;
import care.data4life.crypto.ExchangeKeyFactory;
import care.data4life.crypto.GCAESKeyAlgorithm;
import care.data4life.crypto.GCAsymmetricKey;
import care.data4life.crypto.GCKey;
import care.data4life.crypto.GCKeyPair;
import care.data4life.crypto.GCRSAKeyAlgorithm;
import care.data4life.crypto.GCSymmetricKey;
import care.data4life.crypto.KeyType;
import care.data4life.crypto.KeyVersion;
import care.data4life.crypto.error.CryptoException;
import care.data4life.sdk.crypto.CommonKeyService;
import care.data4life.sdk.crypto.KeyFactory;
import care.data4life.sdk.network.model.EncryptedKey;
import care.data4life.sdk.test.util.TestSchedulerRule;
import care.data4life.sdk.util.Base64;
import io.reactivex.observers.TestObserver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CryptoServiceTest {


    private static final String ALIAS = "dataAlias";
    private static final String PREFIX = ALIAS + "_";


    private static final String TEK_KEY = "crypto_tag_encryption_key";
    private static final String GC_KEYPAIR = "crypto_gc_keypair";


    @Rule
    public TestSchedulerRule schedulerRule = new TestSchedulerRule();


    private CryptoService cryptoService;
    private GCKey gcKey;
    private CryptoSecureStore mockStorage;
    private GCKeyPair keyPair;
    private SecureRandom rnd;
    private Moshi mockMoshi;
    private JsonAdapter adapter;
    private Base64 mockBase64;
    private KeyFactory mockKeyFactory;
    private CommonKeyService mockCommonKeyService;


    @Before
    public void setUp() throws Exception {
        mockStorage = mock(CryptoSecureStore.class);
        mockMoshi = mock(Moshi.class);
        mockBase64 = mock(Base64.class);
        mockKeyFactory = mock(KeyFactory.class);
        mockCommonKeyService = mock(CommonKeyService.class);

        Cipher cipher = mock(Cipher.class);
        when(cipher.getIV()).thenReturn(new byte[1]);
        when(cipher.doFinal(any(byte[].class))).thenReturn(new byte[1]);

        when(mockBase64.decode(any(byte[].class))).thenReturn(new byte[16]);
        when(mockBase64.decode(anyString())).thenReturn(new byte[16]);
        when(mockBase64.encodeToString(any(byte[].class))).thenReturn("encoded");
        when(mockBase64.encodeToString(anyString())).thenReturn("encoded");

        KeyGenerator keyGenerator = mock(KeyGenerator.class);
        when(keyGenerator.generateKey()).thenReturn(mock(SecretKey.class));

        GCAESKeyAlgorithm algorithm = GCAESKeyAlgorithm.Companion.createDataAlgorithm();

        GCSymmetricKey key = new GCSymmetricKey(Mockito.<SecretKey>mock(SecretKey.class));
        gcKey = new GCKey(algorithm, key, 256);

        GCRSAKeyAlgorithm rsaAlgorithm = mock(GCRSAKeyAlgorithm.class);
        GCAsymmetricKey privateKey = mock(GCAsymmetricKey.class);
        GCAsymmetricKey publicKey = mock(GCAsymmetricKey.class);
        int keysize = 2048;
        keyPair = new GCKeyPair(rsaAlgorithm, privateKey, publicKey, keysize);
        Key mockKey = mock(Key.class);
        when(mockKey.getEncoded()).thenReturn(new byte[1]);
        when(privateKey.getValue()).thenReturn(mockKey);
        when(publicKey.getValue()).thenReturn(mockKey);
        when(rsaAlgorithm.getTransformation()).thenReturn("");
        rnd = mock(SecureRandom.class);

        adapter = mock(JsonAdapter.class);
        when(mockMoshi.adapter(any())).thenReturn(adapter);

        cryptoService = new MockCryptoService(
                ALIAS,
                mockStorage,
                mockMoshi,
                rnd,
                mockBase64,
                cipher,
                keyGenerator,
                mockKeyFactory,
                mockCommonKeyService
        );
    }

    @Test
    public void encrypt_shouldCompleteWithoutErrors() {
        // given
        byte[] data = new byte[1];

        // when
        TestObserver<byte[]> testSubscriber = cryptoService
                .encrypt(gcKey, data)
                .test();

        // then
        testSubscriber
                .assertNoErrors()
                .assertComplete();
    }

    @Test
    public void decrypt_shouldCompleteWithoutErrors() {
        // given
        byte[] data = new byte[16];

        // when
        TestObserver<byte[]> testSubscriber = cryptoService
                .decrypt(gcKey, data)
                .test();

        // then
        testSubscriber
                .assertNoErrors()
                .assertComplete();
    }

    @Test
    public void decrypt_shouldThrowErrors() {
        // when
        TestObserver<byte[]> testSubscriber = cryptoService.decrypt(gcKey, null).test();

        // then
        testSubscriber.assertError((Class<? extends Throwable>) CryptoException.DecryptionFailed.class);
    }

    @Test
    public void encryptString_shouldCompleteWithoutErrors() {
        // given
        String input = "data";

        // when
        TestObserver<String> testSubscriber = cryptoService
                .encryptString(gcKey, input)
                .test();

        // then
        testSubscriber
                .assertNoErrors()
                .assertComplete();
    }

    @Test
    public void encryptString_shouldThrowError() {
        // when
        TestObserver<String> observer = cryptoService.encryptString(null, "").test();

        // then
        observer.assertError((Class<? extends Throwable>) CryptoException.EncryptionFailed.class);
    }

    @Test
    public void decryptString_shouldCompleteWithoutErrors() {
        // given
        String input = "data";

        // when
        TestObserver<String> testSubscriber = cryptoService
                .decryptString(gcKey, "data")
                .test();

        // then
        testSubscriber
                .assertNoErrors()
                .assertComplete();
    }

    @Test
    public void decryptSring_shouldThrowError() {
        // when
        TestObserver<String> observer = cryptoService.decryptString(gcKey, null).test();

        // then
        observer.assertError((Class<? extends Throwable>) CryptoException.DecryptionFailed.class);
    }

    @Test
    public void generateGCKey_shouldCompleteWithoutErrors() {
        // when
        TestObserver<GCKey> testSubscriber = cryptoService
                .generateGCKey()
                .test();
        // then
        testSubscriber
                .assertNoErrors()
                .assertComplete();
    }

    @Test
    public void generateGCKeyPair() {
        // when
        TestObserver<GCKeyPair> testSubscriber = cryptoService.generateGCKeyPair()
                .test();

        // then
        testSubscriber
                .onComplete();
    }

    @Test
    public void saveGCKeyPair_shouldStoreKeysAndAlgorithm() {
        // when
        cryptoService.saveGCKeyPair(keyPair);

        // then
        verify(mockStorage).storeKey(eq(PREFIX + GC_KEYPAIR), any(GCKeyPair.class));
    }

    @Test
    public void deleteGCKeyPair_shouldDeleteKeyAndAlgorithm() throws Exception {
        // when
        cryptoService.deleteGCKeyPair();

        // then
        verify(mockStorage).deleteSecret(PREFIX + GC_KEYPAIR);

    }

    @Test
    public void fetchingAndSavingCommonKey() throws Exception {
        //given
        String commonKeyId = "1234-1234-1234-1234";
        GCAESKeyAlgorithm algorithm = mock(GCAESKeyAlgorithm.class);
        when(algorithm.getTransformation()).thenReturn("AES");
        GCKey commonKey = mock(GCKey.class);
        when(mockCommonKeyService.fetchCurrentCommonKeyId()).thenReturn(commonKeyId);
        when(mockStorage.getExchangeKey(PREFIX + commonKeyId))
                .thenReturn(new ExchangeKey(KeyType.COMMON_KEY, null, null, "keyBase64", 1));

        //when
        cryptoService.storeCommonKey(commonKeyId, commonKey);
        cryptoService.fetchCurrentCommonKey();

        //then
        verify(mockCommonKeyService).storeCommonKey(commonKeyId, commonKey);
    }

    @Test
    public void fetchGCKeyPair() throws Exception {
        // given
        GCRSAKeyAlgorithm algorithm = new GCRSAKeyAlgorithm();
        ExchangeKey exchangeKey = new ExchangeKey(
                KeyType.APP_PRIVATE_KEY,
                "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDT+6yUev3950Schqa6fsqFsCaN7zntBTHstL5g9fsay5K0gASSEoGK9IYXyRe7XjH45XWCwMtpWPIOsmOyGLPBgmfZtDZrQTRKjCHxA624bfWbshe6uJjK0dtSyTSLQYUhQH2ziLPzxTG6w0bA+OO+sWl8VCzAESOWzC4OWwwEkEdT1kKIitadhjFmh9NEHipFgcnYTJOIu3b3Rb+8RSziacktdG06kY+P0hSgF3Nbepoy2jBshRdyEc4qXcSf+dAoDKLvPDU7UtrJvCBUDtKPnLx4o+/2Eor7qISLLzo8Y3ipopufZlXpuFDnOZTSCndqE+PWIBOILsoBKYTHjpVJAgEDAoIBAQCNUnMNp1P+mi29rxnRqdxZIBmz9NFIriFIeH7rTqdnMmHNqq22twEHTa66hg/SPsv7Q6OsgIebkKFfIZfMEHfWVu/meCRHgM2HCBagrR568/kSdrp8exCHNpI3MM2yK64WKv53sHf32MvR14SApe0py5uoOB3VYMJkiB60PLKttE47vgR/z4quV+RcYkMwr6Sczbd5eknj1cMZCdlYUVrB4GpaUbXyxYP+Nfn2jSjCl9rqmd9ulON75/wgdOkV6dku/fTPbZyFFD0qAfqUripvvlpkq5FgtmIGy+Q8U5YEsE2M499JJa411xNKJkNlOJ/jCa3N2WAw+0H1K3ySeoJjAoGBAOz/TW0cRU7C1JozeOH/Xj7d3V8ZvixO7GmYHQzhv0fee4vMBn918jSUDdrY4+f1V2POID8vE/O3qoz7MXys6hcfBm0QQ80/HGqJivbdCoAQhzKx7an2EyGEkTHaR1Z9ndPBd9qc1j95anoZES6Pbi6sAhKqWI6N+vL0oYiZqRmfAoGBAOT6686sjjfVLcCoe4x7uHR8b9eIVvhkDmi5mezWC9zhHZ3Z81zYdxT+c0LVX85CP24E0yIXkc6Ai0b+fOpSMPNCiUan0/00mBSBLjGX/xLXeAIvtOvu7dZs5XxWaoK3vTCU1PIU15Efizne7wEqx1jpg0x3AXSwuvQcxsFSLbgXAoGBAJ3/iPNoLjSB4xF3pev/lCnpPj9mfsg0nZu6vgiWf4U+/QfdWapOoXhis+c7Qpqjj5fewCofYqJ6cbNSIP3InA9qBEi1gojUvZxbsfnosaq1r3chSRv5YhZYYMvm2jmpE+KA+pG95CpQ8aa7YMm09B8dVrccOwmz/KH4a7BmcLu/AoGBAJinR98dtCU4ySsa/QhSevhS9Tpa5KWYCZsmZp3kB+iWE76RTOiQT2NUTNc46omBf56t4ha6YTRVsi9UU0bhdfeBsNnFN/4jEA2rdCEP/2Hk+qwfzfKfSTmd7lLkRwHP03W4jfa4j7YVB3vp9Ktx2jtGV4hPVk3LJ01ohIDhc9APAoGAdal7YmmVfc0RDJXapqbc4D5k1yxEq6q6VFdfm7dpDC+wqRmleF8rY+H08HZrPdb12G2KmDcbaZSosqu8XST7IMPj8DhomCZl1bq8qyFMzyosDbuGk2dwqiXkYaqJDHdwW7FfbSmi04VDsBopPAUUx/M8OYDJnMcvgojJYZPIFJg=",
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA43uqiWS2xJyNjRT5XUJfyIB8Be0LGQYXKrmgKF77DxohrQz3K1fN+l0AdTZeT7u04f5V8BwrpVG5iRDQxKg8JSWghfjs4YqP8JOrQmheQKbrrsTon2PrAStBsSNoyQlngXex88/lgJfRHx0F+mCDnx9Iz8xdHeeleagKe4kPXEIcKCwL6Ib8sMCSASNqPQLReDML42r0HDzqXDqIVZHXoLjmue+oypk1YpvlWeyU9vXJNe2RKWyscLXGxBIUtRC2XHWAZ3QbebRUhQGbMnhTWYvdXliLhxdNvZTNt+HB1iSpvSLv0aOK3WoebsHIhpzsOAn5ENpDGhNANdUmCTEf1wIDAQAB",
                null,
                1
        );

        when(mockKeyFactory.createGCKey(exchangeKey)).thenReturn(gcKey);
        when(mockKeyFactory.createGCKeyPair(any())).thenReturn(keyPair);

        java.security.KeyFactory keyFactory = mock(java.security.KeyFactory.class);
        when(keyFactory.generatePrivate(any())).thenReturn(mock(PrivateKey.class));
        when(keyFactory.generatePublic(any())).thenReturn(mock(PublicKey.class));
        when(mockStorage.getExchangeKey(PREFIX + GC_KEYPAIR)).thenReturn(exchangeKey);

        // when
        TestObserver<GCKeyPair> observer = cryptoService.fetchGCKeyPair().test();

        // then
        observer.assertNoErrors();
        observer.assertComplete();

        observer.assertValueCount(1);
        GCKeyPair gcKeyPair = observer.values().get(0);
    }

    @Test
    public void fetchGCKeyPair_shouldThrowException() {
        // when
        TestObserver<GCKeyPair> test = cryptoService.fetchGCKeyPair().test();

        // then
        test.assertError((Class<? extends Throwable>) CryptoException.KeyFetchingFailed.class);
    }


    @Test
    public void convertAsymmetricKeyToBase64ExchangeKey() {
        // given
        PublicKey pk = mock(PublicKey.class);
        when(pk.getEncoded()).thenReturn(new byte[1]);
        GCAsymmetricKey publicKey = new GCAsymmetricKey(pk, GCAsymmetricKey.Type.Public);
        when(adapter.toJson(any(Object.class))).thenReturn("");

        // when
        TestObserver<String> testObserver = cryptoService.convertAsymmetricKeyToBase64ExchangeKey(publicKey)
                .test();

        // then
        testObserver
                .assertValue(s -> !s.isEmpty())
                .assertComplete();
    }

    @Test
    public void symDecryptSymmetricKey() throws IOException {
        // given
        GCKey commonKey = mock(GCKey.class);
        EncryptedKey encryptedKey = new EncryptedKey("");
        ExchangeKey ek = ExchangeKeyFactory.INSTANCE.createKey(KeyVersion.VERSION_1, KeyType.DATA_KEY, "");
        when(adapter.fromJson(anyString())).thenReturn(ek);
        when(mockKeyFactory.createGCKey(ek)).thenReturn(gcKey);

        // when
        TestObserver<GCKey> testObserver = cryptoService.symDecryptSymmetricKey(commonKey, encryptedKey)
                .test();

        // then
        testObserver
                .assertValue(Objects::nonNull)
                .assertComplete();

    }

    @Test
    public void symDecryptSymmetricTagKey() throws IOException {
        // given
        GCKey commonKey = mock(GCKey.class);
        EncryptedKey encryptedKey = new EncryptedKey("");
        ExchangeKey ek = ExchangeKeyFactory.INSTANCE.createKey(KeyVersion.VERSION_1, KeyType.TAG_KEY, "");
        when(adapter.fromJson(anyString())).thenReturn(ek);
        when(mockKeyFactory.createGCKey(ek)).thenReturn(gcKey);


        // when
        TestObserver<GCKey> testObserver = cryptoService.symDecryptSymmetricKey(commonKey, encryptedKey)
                .test();

        // then
        testObserver
                .assertValue(Objects::nonNull)
                .assertComplete();

    }

    @Test
    public void symDecryptSymmetricTagKey_shouldThrowException() throws IOException {
        // given
        GCKey commonKey = mock(GCKey.class);
        EncryptedKey encryptedKey = new EncryptedKey("");
        ExchangeKey ek = new ExchangeKey(KeyType.TAG_KEY, null, null, null, KeyVersion.VERSION_1);
        when(adapter.fromJson(anyString())).thenReturn(ek);

        // when
        TestObserver<GCKey> testObserver = cryptoService.symDecryptSymmetricKey(commonKey, encryptedKey)
                .test();

        // then
        testObserver
                .assertError((Class<? extends Throwable>) CryptoException.KeyDecryptionFailed.class)
                .assertError(throwable -> throwable.getMessage().equals("Failed to decrypt exchange key"));

    }

    @Test
    public void asymDecryptSymmetricKey() throws IOException {
        // given
        EncryptedKey encryptedKey = new EncryptedKey("");
        ExchangeKey ek = ExchangeKeyFactory.INSTANCE.createKey(KeyVersion.VERSION_1, KeyType.DATA_KEY, "");
        when(adapter.fromJson(anyString())).thenReturn(ek);
        when(mockKeyFactory.createGCKey(ek)).thenReturn(gcKey);

        // when
        TestObserver<GCKey> testObserver = cryptoService.asymDecryptSymetricKey(keyPair, encryptedKey)
                .test();

        // then
        testObserver
                .assertValue(Objects::nonNull)
                .assertComplete();

    }

    @Test
    public void asymDecryptSymmetricKey_shouldThrowWrongVersionException() throws IOException {
        // given
        EncryptedKey encryptedKey = new EncryptedKey("");
        ExchangeKey ek = new ExchangeKey(KeyType.DATA_KEY, null, null, "", KeyVersion.VERSION_0);
        when(adapter.fromJson(anyString())).thenReturn(ek);

        // when
        TestObserver<GCKey> testObserver = cryptoService.asymDecryptSymetricKey(keyPair, encryptedKey)
                .test();

        // then
        testObserver
                .assertError((Class<? extends Throwable>) CryptoException.InvalidKeyVersion.class)
                .assertError(throwable -> throwable.getMessage().contains("Key version '" + KeyVersion.VERSION_0.getValue() + "' is not supported"));
    }

    @Test
    public void asymDecryptSymmetricKey_shouldThrowExceptionWhenExchangeKeyIsAsymmetric() throws IOException {
        // given
        EncryptedKey encryptedKey = new EncryptedKey("");
        ExchangeKey ek = new ExchangeKey(KeyType.APP_PRIVATE_KEY, "something", null, null, KeyVersion.VERSION_1);
        when(adapter.fromJson(anyString())).thenReturn(ek);

        // when
        TestObserver<GCKey> testObserver = cryptoService.asymDecryptSymetricKey(keyPair, encryptedKey)
                .test();

        // then
        testObserver
                .assertError((Class<? extends Throwable>) CryptoException.KeyDecryptionFailed.class)
                .assertError(throwable -> throwable.getMessage().contains("can't decrypt asymmetric to symmetric key"));
    }

    @Test
    public void fetchingAndSavingTEK() throws Exception {
        // given
        GCAESKeyAlgorithm algorithm = mock(GCAESKeyAlgorithm.class);
        when(algorithm.getTransformation()).thenReturn("AES");
        GCKey tekKey = mock(GCKey.class);

        when(mockStorage.getExchangeKey(PREFIX + TEK_KEY))
                .thenReturn(new ExchangeKey(KeyType.TAG_KEY, null, null, "tekKeyBase64", 1));

        // when
        cryptoService.storeTagEncryptionKey(tekKey);
        GCKey gcKey = cryptoService.fetchTagEncryptionKey();

        // then
        verify(mockStorage).storeKey(PREFIX + TEK_KEY, tekKey, KeyType.TAG_KEY);
        verify(mockStorage).getExchangeKey(PREFIX + TEK_KEY);
    }


    @After
    public void tearDown() {
        Mockito.reset(
                mockStorage,
                mockMoshi,
                mockBase64,
                mockKeyFactory,
                mockCommonKeyService
        );
    }

    public class MockCryptoService extends CryptoService {

        private Cipher mockCipher;
        private KeyGenerator mockKeyGenerator;

        public MockCryptoService(String alias,
                                 CryptoSecureStore storage,
                                 Moshi moshi,
                                 SecureRandom rng,
                                 Base64 base64,
                                 Cipher cipher,
                                 KeyGenerator keyGenerator,
                                 KeyFactory keyFactory,
                                 CommonKeyService commonKeyService) {
            super(alias, storage, moshi, rng, base64, keyFactory, commonKeyService);
            this.mockCipher = cipher;
            mockKeyGenerator = keyGenerator;
        }


        @Override
        public byte[] symDecrypt(GCKey key, byte[] data, byte[] iv) throws InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, NoSuchProviderException {
            return data;
        }

        @Override
        public Cipher createCypher(String transformation) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
            return mockCipher;
        }

        @Override
        public KeyGenerator createKeyGenerator(String algorithm) throws NoSuchProviderException, NoSuchAlgorithmException {
            return mockKeyGenerator;
        }
    }
}
