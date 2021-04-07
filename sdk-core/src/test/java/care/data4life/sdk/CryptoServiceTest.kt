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
package care.data4life.sdk

import care.data4life.crypto.ExchangeKey
import care.data4life.crypto.ExchangeKeyFactory.createKey
import care.data4life.crypto.GCAESKeyAlgorithm
import care.data4life.crypto.GCAESKeyAlgorithm.Companion.createDataAlgorithm
import care.data4life.crypto.GCAsymmetricKey
import care.data4life.crypto.GCKey
import care.data4life.crypto.GCKeyPair
import care.data4life.crypto.GCRSAKeyAlgorithm
import care.data4life.crypto.GCSymmetricKey
import care.data4life.crypto.KeyType
import care.data4life.crypto.KeyVersion
import care.data4life.crypto.error.CryptoException.InvalidKeyVersion
import care.data4life.crypto.error.CryptoException.KeyDecryptionFailed
import care.data4life.crypto.error.CryptoException.KeyFetchingFailed
import care.data4life.sdk.crypto.CommonKeyService
import care.data4life.sdk.crypto.KeyFactory
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.test.util.TestSchedulerRule
import care.data4life.sdk.util.Base64
import care.data4life.sdk.util.Base64.decode
import care.data4life.securestore.toBytes
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.Key
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class CryptoServiceTest {

    var schedulerRule = TestSchedulerRule()
    private lateinit var cryptoService: CryptoService
    private lateinit var gcKey: GCKey
    private lateinit var mockStorage: CryptoSecureStore
    private lateinit var keyPair: GCKeyPair
    private lateinit var rnd: SecureRandom
    private lateinit var mockMoshi: Moshi
    private lateinit var adapter: JsonAdapter<ExchangeKey>
    private lateinit var mockBase64: Base64
    private lateinit var mockKeyFactory: KeyFactory
    private lateinit var mockCommonKeyService: CommonKeyService


    @Before
    fun setUp() {
        mockStorage = mockk()
        mockMoshi = mockk()
        mockBase64 = mockk()
        mockKeyFactory = mockk()
        mockCommonKeyService = mockk()
        adapter = mockk()
        val cipher = mockk<Cipher>()
        every { cipher.iv } returns ByteArray(1)
        every { cipher.doFinal(any<ByteArray>()) } returns ByteArray(1)
        every { mockBase64.decode(any<ByteArray>()) } returns ByteArray(16)
        every { mockBase64.decode(any<String>()) } returns ByteArray(16)
        every { mockBase64.encodeToString(any<ByteArray>()) } returns "encoded"
        every { mockBase64.encodeToString(any<String>()) } returns "encoded"

        val keyGenerator = mockk<KeyGenerator>()
        every { keyGenerator.generateKey() } returns mockk<SecretKey>()
        every { keyGenerator.init(256) } just runs
        val algorithm = createDataAlgorithm()
        val key = GCSymmetricKey(mockk<SecretKey>())
        gcKey = GCKey(algorithm, key, 256)
        val rsaAlgorithm = mockk<GCRSAKeyAlgorithm>()
        val privateKey = mockk<GCAsymmetricKey>()
        val publicKey = mockk<GCAsymmetricKey>()
        val keysize = 2048
        keyPair = GCKeyPair(rsaAlgorithm, privateKey, publicKey, keysize)
        val mockKey = mockk<Key>()
        every { mockKey.encoded } returns ByteArray(1)
        every { privateKey.value } returns mockKey
        every { publicKey.value } returns mockKey
        every { rsaAlgorithm.transformation } returns ""
        every { cipher.init(1, gcKey.getSymmetricKey().value, any<IvParameterSpec>()) } just runs
        every { cipher.init(2, keyPair.privateKey?.value) } just runs
        every { mockStorage.deleteSecret(any<String>()) } just runs
        rnd = SecureRandom()
        adapter = mockk()
        every { mockMoshi.adapter(ExchangeKey::class.java) } returns adapter
        cryptoService = MockCryptoService(
                ALIAS,
                mockStorage,
                mockMoshi,
                rnd,
                mockBase64,
                cipher,
                keyGenerator,
                mockKeyFactory,
                mockCommonKeyService
        )
    }

    @Test
    fun encrypt_shouldCompleteWithoutErrors() {
        // given
        val data = byteArrayOf(0xA1.toByte())

        // when
        val testSubscriber = cryptoService
                .encrypt(gcKey, data)
                .test()

        // then
        testSubscriber
                .assertNoErrors()
                .assertComplete()
    }

    @Test
    fun decrypt_shouldCompleteWithoutErrors() {
        // given
        val data = ByteArray(16)

        // when
        val testSubscriber = cryptoService
                .decrypt(gcKey, data)
                .test()

        // then
        testSubscriber
                .assertNoErrors()
                .assertComplete()
    }

    @Test
    fun encryptString_shouldCompleteWithoutErrors() {
        // given
        val input = "data"

        // when
        val testSubscriber = cryptoService.encryptString(gcKey, input)
                .test()

        // then
        testSubscriber
                .assertNoErrors()
                .assertComplete()
    }

    @Test
    fun decryptString_shouldCompleteWithoutErrors() {
        // given
        val input = "data"

        // when
        val testSubscriber = cryptoService
                .decryptString(gcKey, input)
                .test()

        // then
        testSubscriber
                .assertNoErrors()
                .assertComplete()
    }

    @Test
    fun generateGCKey_shouldCompleteWithoutErrors() {
        // when
        val testSubscriber = cryptoService
                .generateGCKey()
                .test()
        // then
        testSubscriber
                .assertNoErrors()
                .assertComplete()
    }

    @Test
    fun generateGCKeyPair() {
        // when
        val testSubscriber = cryptoService.generateGCKeyPair()
                .test()

        // then
        testSubscriber
                .onComplete()
    }

    @Ignore
    @Test
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun setGCKeyPairFromPemPrivateKey_shouldStoreCorrectPrivateKey() {
        // given
        val base64TestKeyWord = """MIIEowIBAAKCAQEArosQh7LO8HhpJxeHBwQpi12uxOKQsYYBzMOUeoerijGOwtQG
            sz656ZmQJNR62BIK2mHSSjIcpmu3ydE5fJkUxtF+dpTdmPtZ2URGEyt3/dXFe5RR
            i9hiIVwgWzjZAiVrWfIx4MtPEk9fMV2WKMLOa+o2ZEWqDLfiDjU8ixSaUc/Vtd5K
            msSun587+iBPTR33pGAG1u3If3GSkQPKkW3elRNLxL4twSL45+pmSIgkcTIrxo71
            0xqN5hWRr0dv19RSYHapJdgIPXDQkJiKULpF3/OWst8/OjtBhP3G8ne0FDoP4xId
            RDXCUat1R6MuJVDx+sr/GkHjFlMjKjFUHrXQqQIDAQABAoIBAAHQTh6q2/2hsq4G
            T4/iGjBpi8xd8lT16IThL2TKjhzEgRBDNcKdDz9/KgFH9/LQ1S4JwC6nMKcGDYXa
            V7eUu6OJP8ApsdfKHNfmHrhKRlfr5b5v/xzt5a8lDu0DvTWJgAESRDRqyGqPSpTv
            vQS1aYGzkFcgZjD1pDKzmOp1D1l0RAZdGxa5cTsZiovmX27nOg16qKdmUIRbMyQC
            CQqnX+uWvfAJrxmu+DUQthsVfm599TvZkuqBCCdUvri7dEFLPrV08kn7/ZnqlBf9
            ms/OQjCGI+41LXHyxrriG8noBlklQ/QrXe8kZPFAv0YWCTYEo2pDuowIZrByDBZc
            d8KM5nUCgYEA19rwq+PS0UM2reVkM3JHMXxZrxp5yADjUUYfsiMom0BRq4T8HBao
            eD5wMiTBjRhLuDzwnFF/o38Z/DFgsyhIjNlI3mdrQV1iroiHK/QDqkKk53xQofFl
            Aro7JtkyVMcT0htjZzVDobBEzH4c/BcstYrlXt/r27GMeH+D6nZaAJcCgYEAzwE5
            AmWMAf9EOVVsSf0s4DSJ3So7pUUlkCg9i5QU7gLu6gMAa19EyZO0aBflmTtuovm2
            302/giL6z/SgEbzkHHomvr2RZYwocYMbN30EDHi4JX8RkHS8bYioz3Vw4g6ELvSs
            CnLRA9FCX4vB2tJtYI/LwiZukWnDFUYZQW13oL8CgYBhLp9gpEfME1jQ3hBI4VCQ
            RQ4TufXOSCgP9WRbzVyA2WprsInZE5Jx4Jqe2NGTdrbQkg86Ma8nqxfF5W1F/AL9
            9u3JxAIUAblmHu3MqiXkR/D6j4u1/Xqeyb3L9cmlRaP02oPceayjZTr0XmsqTDzC
            13ABUQtddAhsT+zSaMqIrQKBgQCGAaqgTJC4ckH+Q7iYpVc5xYlCLabzNLI+gm5l
            P3XVJvz3bP4GhGQJgp8Vi/LModbbloC2SqShYHexzBEbqoaZkNIoRJwtevBrm44w
            +7N1R2kejQYX2BprZj6yHrr2/KLBqw78rJt2ty8an2Tdfb/k9PHZO/v0Et2BliGf
            Y3hADQKBgCdHaK1A3cxTQK4+EE3DsUM4ZnB+vUS78IiHTSpJvUY/gX8RcWxTJVvc
            nnxSlrzV/rxA7uI40DFiPFGVrVoXW1w0C1ASeL3siqv1aoZ5QiuCUv6ULKrbNBp7
            QyhWfy6A4sU7XdwJfNhQUAoLvvRrECtqMR7Ayn7xoWgeuhqQCHeg
            """.trimIndent()

        val base64TestKey = base64TestKeyWord.toCharArray()
        base64TestKeyWord.replace("\n", "", false)
        val pemTestKeyString = """
            -----BEGIN RSA PRIVATE KEY-----
            $base64TestKeyWord-----END RSA PRIVATE KEY-----
            
            """.trimIndent()

        // when
        cryptoService.setGCKeyPairFromPemPrivateKey(pemTestKeyString)

        //then
        /* The base64 serialized input and out keys may differ due to irrelevant meta-data
        differences and hence cannot be compared directly. Hence, we convert both the original
        and the stored key to Java key objects before comparing them. */
        val keyPairArg = ArgumentCaptor.forClass(GCKeyPair::class.java)
        verify { mockStorage.storeKey(ArgumentMatchers.eq(PREFIX + GC_KEYPAIR), keyPairArg.capture()) }
        val algorithm = GCRSAKeyAlgorithm()
        val keyFactory = java.security.KeyFactory.getInstance(algorithm.cipher)
        val storedPrivateKeyBase64 = keyPairArg.value.getPrivateKeyBase64()
        val storedJavaKey = getPrivateJavaKey(keyFactory, storedPrivateKeyBase64)
        val testKeyNoLinebreaksBase64 = Arrays.toString(base64TestKey).replace("\n", "").toCharArray()
        val testJavaKey = getPrivateJavaKey(keyFactory, testKeyNoLinebreaksBase64)
        Assert.assertEquals(testJavaKey, storedJavaKey)
    }

    /**
     * Convert a base 64 DER format key into a java.sercurity PrivateKey object
     *
     * @param keyFactory       java.security.KeyFactory
     * @param privateKeyBase64 Private key as base 64 string
     * @return Key
     * @throws InvalidKeySpecException
     */
    @Throws(InvalidKeySpecException::class)
    private fun getPrivateJavaKey(keyFactory: java.security.KeyFactory, privateKeyBase64: CharArray): PrivateKey {
        val storedPrivateKey: ByteArray = decode(privateKeyBase64.toBytes())
        val encodedStoredKeySpec = PKCS8EncodedKeySpec(storedPrivateKey)
        return keyFactory.generatePrivate(encodedStoredKeySpec)
    }


    @Test
    fun saveGCKeyPair_shouldStoreKeysAndAlgorithm() {

        every { mockStorage.storeKey(PREFIX + GC_KEYPAIR, any<GCKeyPair>()) } just runs
        // when
        cryptoService.saveGCKeyPair(keyPair)

        // then
        verify { mockStorage.storeKey(PREFIX + GC_KEYPAIR, any<GCKeyPair>()) }
    }


    @Test
    @Throws(Exception::class)
    fun deleteGCKeyPair_shouldDeleteKeyAndAlgorithm() {
        // when
        cryptoService.deleteGCKeyPair()

        // then
        verify { mockStorage.deleteSecret(PREFIX + GC_KEYPAIR) }
    }

    @Test
    @Throws(Exception::class)
    fun fetchingAndSavingCommonKey() {
        //given
        val commonKeyId = "1234-1234-1234-1234"
        val algorithm = mockk<GCAESKeyAlgorithm>()
        val commonKey = mockk<GCKey>()
        every { algorithm.transformation } returns "AES"
        every { mockCommonKeyService.fetchCurrentCommonKeyId() } returns commonKeyId
        every { mockCommonKeyService.fetchCurrentCommonKey() } returns commonKey
        every { mockStorage.getExchangeKey(PREFIX + commonKeyId) } returns ExchangeKey(KeyType.COMMON_KEY, charArrayOf(), charArrayOf(), "keyBase64".toCharArray(), 1)
        every { mockCommonKeyService.storeCommonKey(commonKeyId, commonKey) } just runs

        //when
        cryptoService.storeCommonKey(commonKeyId, commonKey)
        cryptoService.fetchCurrentCommonKey()

        //then
        verify { mockCommonKeyService.storeCommonKey(commonKeyId, commonKey) }
    }


    @Test
    @Throws(Exception::class)
    fun fetchGCKeyPair() {
        // given
        val algorithm = GCRSAKeyAlgorithm()
        val exchangeKey = ExchangeKey(
                KeyType.APP_PRIVATE_KEY,
                "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDT+6yUev3950Schqa6fsqFsCaN7zntBTHstL5g9fsay5K0gASSEoGK9IYXyRe7XjH45XWCwMtpWPIOsmOyGLPBgmfZtDZrQTRKjCHxA624bfWbshe6uJjK0dtSyTSLQYUhQH2ziLPzxTG6w0bA+OO+sWl8VCzAESOWzC4OWwwEkEdT1kKIitadhjFmh9NEHipFgcnYTJOIu3b3Rb+8RSziacktdG06kY+P0hSgF3Nbepoy2jBshRdyEc4qXcSf+dAoDKLvPDU7UtrJvCBUDtKPnLx4o+/2Eor7qISLLzo8Y3ipopufZlXpuFDnOZTSCndqE+PWIBOILsoBKYTHjpVJAgEDAoIBAQCNUnMNp1P+mi29rxnRqdxZIBmz9NFIriFIeH7rTqdnMmHNqq22twEHTa66hg/SPsv7Q6OsgIebkKFfIZfMEHfWVu/meCRHgM2HCBagrR568/kSdrp8exCHNpI3MM2yK64WKv53sHf32MvR14SApe0py5uoOB3VYMJkiB60PLKttE47vgR/z4quV+RcYkMwr6Sczbd5eknj1cMZCdlYUVrB4GpaUbXyxYP+Nfn2jSjCl9rqmd9ulON75/wgdOkV6dku/fTPbZyFFD0qAfqUripvvlpkq5FgtmIGy+Q8U5YEsE2M499JJa411xNKJkNlOJ/jCa3N2WAw+0H1K3ySeoJjAoGBAOz/TW0cRU7C1JozeOH/Xj7d3V8ZvixO7GmYHQzhv0fee4vMBn918jSUDdrY4+f1V2POID8vE/O3qoz7MXys6hcfBm0QQ80/HGqJivbdCoAQhzKx7an2EyGEkTHaR1Z9ndPBd9qc1j95anoZES6Pbi6sAhKqWI6N+vL0oYiZqRmfAoGBAOT6686sjjfVLcCoe4x7uHR8b9eIVvhkDmi5mezWC9zhHZ3Z81zYdxT+c0LVX85CP24E0yIXkc6Ai0b+fOpSMPNCiUan0/00mBSBLjGX/xLXeAIvtOvu7dZs5XxWaoK3vTCU1PIU15Efizne7wEqx1jpg0x3AXSwuvQcxsFSLbgXAoGBAJ3/iPNoLjSB4xF3pev/lCnpPj9mfsg0nZu6vgiWf4U+/QfdWapOoXhis+c7Qpqjj5fewCofYqJ6cbNSIP3InA9qBEi1gojUvZxbsfnosaq1r3chSRv5YhZYYMvm2jmpE+KA+pG95CpQ8aa7YMm09B8dVrccOwmz/KH4a7BmcLu/AoGBAJinR98dtCU4ySsa/QhSevhS9Tpa5KWYCZsmZp3kB+iWE76RTOiQT2NUTNc46omBf56t4ha6YTRVsi9UU0bhdfeBsNnFN/4jEA2rdCEP/2Hk+qwfzfKfSTmd7lLkRwHP03W4jfa4j7YVB3vp9Ktx2jtGV4hPVk3LJ01ohIDhc9APAoGAdal7YmmVfc0RDJXapqbc4D5k1yxEq6q6VFdfm7dpDC+wqRmleF8rY+H08HZrPdb12G2KmDcbaZSosqu8XST7IMPj8DhomCZl1bq8qyFMzyosDbuGk2dwqiXkYaqJDHdwW7FfbSmi04VDsBopPAUUx/M8OYDJnMcvgojJYZPIFJg=".toCharArray(),
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA43uqiWS2xJyNjRT5XUJfyIB8Be0LGQYXKrmgKF77DxohrQz3K1fN+l0AdTZeT7u04f5V8BwrpVG5iRDQxKg8JSWghfjs4YqP8JOrQmheQKbrrsTon2PrAStBsSNoyQlngXex88/lgJfRHx0F+mCDnx9Iz8xdHeeleagKe4kPXEIcKCwL6Ib8sMCSASNqPQLReDML42r0HDzqXDqIVZHXoLjmue+oypk1YpvlWeyU9vXJNe2RKWyscLXGxBIUtRC2XHWAZ3QbebRUhQGbMnhTWYvdXliLhxdNvZTNt+HB1iSpvSLv0aOK3WoebsHIhpzsOAn5ENpDGhNANdUmCTEf1wIDAQAB".toCharArray(),
                charArrayOf(),
                1
        )
        every { mockKeyFactory.createGCKey(exchangeKey) } returns gcKey
        every { mockKeyFactory.createGCKeyPair(any()) } returns keyPair
        val keyFactory = mockk<java.security.KeyFactory>()
        every { keyFactory.generatePrivate(any<KeySpec>()) } returns mockk<PrivateKey>()
        every { keyFactory.generatePublic(any<KeySpec>()) } returns mockk<PublicKey>()
        every { mockStorage.getExchangeKey(PREFIX + GC_KEYPAIR) } returns exchangeKey

        // when
        val observer = cryptoService.fetchGCKeyPair().test()

        // then
        observer.assertNoErrors()
        observer.assertComplete()
        observer.assertValueCount(1)
        val gcKeyPair = observer.values()[0]
    }


    @Test
    fun fetchGCKeyPair_shouldThrowException() {
        // when
        val test = cryptoService.fetchGCKeyPair().test()

        // then
        test.assertError(KeyFetchingFailed::class.java as Class<out Throwable?>)
    }


    @Test
    fun convertAsymmetricKeyToBase64ExchangeKey() {
        // given
        val pk = mockk<PublicKey>()
        every { pk.encoded } returns ByteArray(1)
        val publicKey = GCAsymmetricKey(pk, GCAsymmetricKey.Type.Public)
        every { adapter.toJson(any<ExchangeKey>()) } returns ""

        // when
        val testObserver = cryptoService.convertAsymmetricKeyToBase64ExchangeKey(publicKey)
                .test()

        // then
        testObserver
                .assertValue { s: String -> !s.isEmpty() }
                .assertComplete()
    }

    @Test
    @Throws(IOException::class)
    fun symDecryptSymmetricKey() {
        // given
        val commonKey = mockk<GCKey>()
        val encryptedKey = EncryptedKey("")
        val ek = createKey(KeyVersion.VERSION_1, KeyType.DATA_KEY, CharArray(0))
        every { adapter.fromJson(any<String>()) } returns ek
        every { mockKeyFactory.createGCKey(ek) } returns gcKey

        // when
        val testObserver = cryptoService.symDecryptSymmetricKey(commonKey, encryptedKey)
                .test()

        // then
        testObserver
                .assertValue { o: GCKey? -> Objects.nonNull(o) }
                .assertComplete()
    }

    @Test
    @Throws(IOException::class)
    fun symDecryptSymmetricTagKey() {
        // given
        val commonKey = mockk<GCKey>()
        val encryptedKey = EncryptedKey("")
        val ek = createKey(KeyVersion.VERSION_1, KeyType.TAG_KEY, CharArray(0))
        every { adapter.fromJson(any<String>()) } returns ek
        every { mockKeyFactory.createGCKey(ek) } returns gcKey


        // when
        val testObserver = cryptoService!!.symDecryptSymmetricKey(commonKey, encryptedKey)
                .test()

        // then
        testObserver
                .assertValue { o: GCKey? -> Objects.nonNull(o) }
                .assertComplete()
    }

    @Test
    @Throws(IOException::class)
    fun symDecryptSymmetricTagKey_shouldThrowException() {
        // given
        val commonKey = mockk<GCKey>()
        val encryptedKey = EncryptedKey("")
        val ek = ExchangeKey(KeyType.TAG_KEY, charArrayOf(), charArrayOf(), charArrayOf(), KeyVersion.VERSION_1)
        every { adapter.fromJson(any<String>()) } returns ek

        // when
        val testObserver = cryptoService.symDecryptSymmetricKey(commonKey, encryptedKey)
                .test()

        // then
        testObserver
                .assertError(KeyDecryptionFailed::class.java as Class<out Throwable?>)
                .assertError { throwable: Throwable -> throwable.message == "Failed to decrypt exchange key" }
    }


    @Test
    @Throws(IOException::class)
    fun asymDecryptSymmetricKey() {
        // given
        val encryptedKey = EncryptedKey("")
        val ek = createKey(KeyVersion.VERSION_1, KeyType.DATA_KEY, CharArray(0))
        every { adapter.fromJson(any<String>()) } returns ek
        every { mockKeyFactory.createGCKey(ek) } returns gcKey

        // when
        val testObserver = cryptoService.asymDecryptSymetricKey(keyPair, encryptedKey)
                .test()

        // then
        testObserver
                .assertValue { o: GCKey? -> Objects.nonNull(o) }
                .assertComplete()
    }


    @Test
    @Throws(IOException::class)
    fun asymDecryptSymmetricKey_shouldThrowWrongVersionException() {
        // given
        val encryptedKey = EncryptedKey("")
        val ek = ExchangeKey(KeyType.DATA_KEY, charArrayOf(), charArrayOf(), charArrayOf(), KeyVersion.VERSION_0)
        every { adapter.fromJson(any<String>()) } returns ek

        // when
        val testObserver = cryptoService.asymDecryptSymetricKey(keyPair, encryptedKey)
                .test()

        // then
        testObserver
                .assertError(InvalidKeyVersion::class.java as Class<out Throwable?>)
                .assertError { throwable: Throwable -> throwable.message!!.contains("Key version '" + KeyVersion.VERSION_0.value + "' is not supported") }
    }


    @Test
    @Throws(IOException::class)
    fun asymDecryptSymmetricKey_shouldThrowExceptionWhenExchangeKeyIsAsymmetric() {
        // given
        val encryptedKey = EncryptedKey("")
        val ek = ExchangeKey(KeyType.APP_PRIVATE_KEY, "something".toCharArray(), charArrayOf(), charArrayOf(), KeyVersion.VERSION_1)
        every { adapter.fromJson(any<String>()) } returns ek

        // when
        val testObserver = cryptoService.asymDecryptSymetricKey(keyPair, encryptedKey)
                .test()

        // then
        testObserver
                .assertError(KeyDecryptionFailed::class.java as Class<out Throwable?>)
                .assertError { throwable: Throwable -> throwable.message!!.contains("can't decrypt asymmetric to symmetric key") }
    }


    @Test
    @Throws(Exception::class)
    fun fetchingAndSavingTEK() {
        // given
        val algorithm = mockk<GCAESKeyAlgorithm>()
        every { algorithm.transformation } returns "AES"
        val tekKey = mockk<GCKey>()
        every { mockStorage.getExchangeKey(PREFIX + TEK_KEY) } returns ExchangeKey(KeyType.TAG_KEY, charArrayOf(), charArrayOf(), "tekKeyBase64".toCharArray(), 1)
        every { mockKeyFactory.createGCKey(any<ExchangeKey>()) } returns tekKey
        every { mockStorage.storeKey(PREFIX + TEK_KEY, tekKey, KeyType.TAG_KEY) } just runs

        // when
        cryptoService.storeTagEncryptionKey(tekKey)
        val gcKey = cryptoService.fetchTagEncryptionKey()

        // then
        verify { mockStorage.storeKey(PREFIX + TEK_KEY, tekKey, KeyType.TAG_KEY) }
        verify { mockStorage.getExchangeKey(PREFIX + TEK_KEY) }
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    inner class MockCryptoService(alias: String?,
                                  storage: CryptoSecureStore?,
                                  moshi: Moshi?,
                                  rng: SecureRandom?,
                                  base64: Base64?,
                                  private val mockCipher: Cipher,
                                  private val mockKeyGenerator: KeyGenerator,
                                  keyFactory: KeyFactory?,
                                  commonKeyService: CommonKeyService?) : CryptoService(alias!!, storage!!, moshi!!, rng!!, base64!!, keyFactory!!, commonKeyService!!) {
        @Throws(InvalidAlgorithmParameterException::class, InvalidKeyException::class, NoSuchPaddingException::class, NoSuchAlgorithmException::class, BadPaddingException::class, IllegalBlockSizeException::class, NoSuchProviderException::class)
        override fun symDecrypt(key: GCKey, data: ByteArray, iv: ByteArray): ByteArray {
            return data
        }

        @Throws(NoSuchPaddingException::class, NoSuchAlgorithmException::class, NoSuchProviderException::class)
        override fun createCypher(transformation: String): Cipher {
            return mockCipher
        }

        @Throws(NoSuchProviderException::class, NoSuchAlgorithmException::class)
        override fun createKeyGenerator(algorithm: String?): KeyGenerator {
            return mockKeyGenerator
        }
    }

    companion object {
        private const val ALIAS = "dataAlias"
        private const val PREFIX = ALIAS + "_"
        private const val TEK_KEY = "crypto_tag_encryption_key"
        private const val GC_KEYPAIR = "crypto_gc_keypair"
    }
}
