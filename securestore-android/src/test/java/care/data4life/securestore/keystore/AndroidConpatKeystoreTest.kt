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

package care.data4life.securestore.keystore

class AndroidConpatKeystoreTest {

//    val asymmetricKeyAlias = BaseAndroidKeyStore.ASYMMETRIC_KEY_ALIAS
//    val symmetricKeyAlias = BaseAndroidKeyStore.SYMMETRIC_KEY_ALIAS
//
//    val encryptedSymmetricKey = "encrypted_symmetric_key"
//    val mockSymmetricKey = mockk<SymmetricKey>()
//
//    val mockKeyStore = mockk<KeyStore>()
//    val mockPrivateKey = mockk<PrivateKey>()
//    val mockPublicKey = mockk<PublicKey>()
//    val mockSecreKey = mockk<SecretKey>()
//
//    val mockCertificate = mockk<Certificate>()
//    val mockKeyPairGenerator = mockk<KeyPairGenerator>()
//    val mockKeyPairGeneratorSpec = mockk<KeyPairGeneratorSpec>()
//    val mockKeyPairGeneratorSpecBuilder = mockk<KeyPairGeneratorSpec.Builder>(relaxed = true)
//    val mockKeyPair = mockk<KeyPair>()
//
//    val mockKeyGenerator = mockk<KeyGenerator>()
//
//    val mockContext = mockk<Context>()
//    val mockCryptor = mockk<SecureStoreContract.CryptorLegacy>()
//    val mockStorage = mockk<SecureStoreContract.Storage>()
//
//
//    // SUT
//    lateinit var androidKeyStore: SecureStoreContract.KeyStore
//
//
//    @Before
//    fun setUp() {
//        mockkStatic("java.security.KeyStore")
//        every { KeyStore.getInstance(BaseAndroidKeyStore.KEYSTORE_PROVIDER) } returns mockKeyStore
//        every { mockKeyStore.load(null) } just Runs
//
//        androidKeyStore = AndroidCompatKeyStoreLegacy(mockContext, mockCryptor, mockStorage)
//    }
//
//    @Test
//    fun `getAsymmetricKey() should return key from keystore if present`() {
//        // Given
//        every { mockKeyStore.getKey(asymmetricKeyAlias, null) } returns mockPrivateKey
//        every { mockKeyStore.getCertificate(asymmetricKeyAlias) } returns mockCertificate
//        every { mockCertificate.publicKey } returns mockPublicKey
//
//        // When
//        val result = androidKeyStore.getAsymmetricKey()
//
//        // Then
//        assertEquals(mockPrivateKey, result.getPrivateKey())
//        assertEquals(mockPublicKey, result.getPublicKey())
//    }
//
//    @Test
//    fun `getAsymmetricKey() should generate new key when no PrivateKey present`() {
//        // Given
//        every { mockKeyStore.getKey(asymmetricKeyAlias, null) } returns null
//        every { mockKeyStore.getCertificate(asymmetricKeyAlias) } returns null
//        setupCreateAsymmetricKeyMocks()
//
//        // When
//        val result = androidKeyStore.getAsymmetricKey()
//
//
//        // Then
//        assertEquals(mockPrivateKey, result.getPrivateKey())
//        assertEquals(mockPublicKey, result.getPublicKey())
//    }
//
//    @Test
//    fun `getAsymmetricKey() should generate new key when no Certificate present`() {
//        // Given
//        every { mockKeyStore.getKey(asymmetricKeyAlias, null) } returns mockPrivateKey
//        every { mockKeyStore.getCertificate(asymmetricKeyAlias) } returns null
//        setupCreateAsymmetricKeyMocks()
//
//        // When
//        val result = androidKeyStore.getAsymmetricKey()
//
//
//        // Then
//        assertEquals(mockPrivateKey, result.getPrivateKey())
//        assertEquals(mockPublicKey, result.getPublicKey())
//    }
//
//    @Test
//    fun `getAsymmetricKey() should generate new key when no PublicKey present`() {
//        // Given
//        every { mockKeyStore.getKey(asymmetricKeyAlias, null) } returns mockPrivateKey
//        every { mockKeyStore.getCertificate(asymmetricKeyAlias) } returns mockCertificate
//        every { mockCertificate.publicKey } returns null
//        setupCreateAsymmetricKeyMocks()
//
//        // When
//        val result = androidKeyStore.getAsymmetricKey()
//
//
//        // Then
//        assertEquals(mockPrivateKey, result.getPrivateKey())
//        assertEquals(mockPublicKey, result.getPublicKey())
//    }
//
//
//    @Test
//    fun `getSymmetricKey() should return key from keystore if present`() {
//        // Given
//        every { mockKeyStore.getKey(asymmetricKeyAlias, null) } returns mockPrivateKey
//        every { mockKeyStore.getCertificate(asymmetricKeyAlias) } returns mockCertificate
//        every { mockCertificate.publicKey } returns mockPublicKey
//
//        every { mockStorage.getData(symmetricKeyAlias) } returns encryptedSymmetricKey
//        every { mockCryptor.unwrapKey(encryptedSymmetricKey, any()) } returns mockSymmetricKey
//
//        // When
//        val result = androidKeyStore.getSymmetricKey()
//
//        // Then
//        assertEquals(mockSymmetricKey, result)
//    }
//
//
//    @Test
//    fun `getSymmetricKey() should generate new key when no SymmetricKey present`() {
//        // Given
//        every { mockKeyStore.getKey(asymmetricKeyAlias, null) } returns mockPrivateKey
//        every { mockKeyStore.getCertificate(asymmetricKeyAlias) } returns mockCertificate
//        every { mockCertificate.publicKey } returns mockPublicKey
//
//        every { mockStorage.getData(symmetricKeyAlias) } returns null
//
//        // create
//        mockkStatic("javax.crypto.KeyGenerator")
//        every { KeyGenerator.getInstance(SymmetricKey.KEY_ALGORITHM) } returns mockKeyGenerator
//        every { mockKeyGenerator.init(SymmetricKey.KEY_SIZE) } just Runs
//        every { mockKeyGenerator.generateKey() } returns mockSecreKey
//
//        // store
//        every { mockCryptor.wrapKey(any(), any()) } returns encryptedSymmetricKey
//        every { mockStorage.addData(symmetricKeyAlias, encryptedSymmetricKey) } just Runs
//
//
//        // When
//        val result = androidKeyStore.getSymmetricKey()
//
//        // Then
//        assertEquals(mockSecreKey, result.getKey())
//    }
//
//
//    @Test
//    fun clear() {
//        // Given
//        every { mockStorage.clear() } just Runs
//        every { mockKeyStore.deleteEntry(asymmetricKeyAlias) } just Runs
//        every { mockKeyStore.deleteEntry(symmetricKeyAlias) } just Runs
//
//        // When
//        androidKeyStore.clear()
//
//        // Then
//        verifyAll {
//            mockKeyStore.load(null)
//            mockStorage.clear()
//            mockKeyStore.deleteEntry(asymmetricKeyAlias)
//            mockKeyStore.deleteEntry(symmetricKeyAlias)
//        }
//    }
//
//
//    @After
//    fun cleanUp() {
//        unmockkAll()
//    }
//
//
//    private fun setupCreateAsymmetricKeyMocks() {
//        mockkStatic("java.security.KeyPairGenerator")
//        every { KeyPairGenerator.getInstance(AsymmetricKey.KEY_ALGORITHM, BaseAndroidKeyStore.KEYSTORE_PROVIDER) } returns mockKeyPairGenerator
//        mockkConstructor(KeyPairGeneratorSpec.Builder::class)
//        every { anyConstructed<KeyPairGeneratorSpec.Builder>().setAlias(asymmetricKeyAlias) } returns mockKeyPairGeneratorSpecBuilder
//        every { mockKeyPairGeneratorSpecBuilder.build() } returns mockKeyPairGeneratorSpec
//        every { mockKeyPairGenerator.initialize(any<KeyPairGeneratorSpec>()) } just Runs
//        every { mockKeyPairGenerator.generateKeyPair() } returns mockKeyPair
//        every { mockKeyPair.private } returns mockPrivateKey
//        every { mockKeyPair.public } returns mockPublicKey
//    }
}
