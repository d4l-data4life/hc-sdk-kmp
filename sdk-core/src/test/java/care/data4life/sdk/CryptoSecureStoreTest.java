///*
// * Copyright (c) 2020 D4L data4life gGmbH / All rights reserved.
// *
// * D4L owns all legal rights, title and interest in and to the Software Development Kit ("SDK"),
// * including any intellectual property rights that subsist in the SDK.
// *
// * The SDK and its documentation may be accessed and used for viewing/review purposes only.
// * Any usage of the SDK for other purposes, including usage for the development of
// * applications/third-party applications shall require the conclusion of a license agreement
// * between you and D4L.
// *
// * If you are interested in licensing the SDK for your own applications/third-party
// * applications and/or if you’d like to contribute to the development of the SDK, please
// * contact D4L by email to help@data4life.care.
// */
//
//package care.data4life.sdk;
//
//import com.squareup.moshi.JsonAdapter;
//import com.squareup.moshi.Moshi;
//
//import org.junit.Before;
//import org.junit.Test;
//
//import java.io.IOException;
//import java.lang.reflect.Type;
//
//import care.data4life.crypto.ExchangeKey;
//import care.data4life.crypto.GCKey;
//import care.data4life.crypto.GCKeyPair;
//import care.data4life.crypto.GCRSAKeyAlgorithm;
//import care.data4life.crypto.KeyType;
//import care.data4life.securestore.SecureStoreContract.SecureStore;
//
//import static org.junit.Assert.assertEquals;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.spy;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.verifyNoMoreInteractions;
//import static org.mockito.Mockito.when;
//
//public class CryptoSecureStoreTest {
//
//    private static final String DATA_ALIAS = "data_alias";
//    private static final String DATA = "data";
//
//
//    private static final String OBJECT_ALIAS = "object_alias";
//    private static final Object OBJECT = new Object();
//    private static final String OBJECT_JSON = "object_json";
//
//    private SecureStore secureStore;
//    private Moshi moshi;
//    private JsonAdapter adapter;
//
//
//    // SUT
//    private CryptoSecureStore cryptoSecureStore;
//
//
//    @Before
//    public void setUp() {
//        secureStore = mock(SecureStore.class);
//        moshi = mock(Moshi.class);
//
//        adapter = mock(JsonAdapter.class);
//        when(moshi.adapter(any())).thenReturn(adapter);
//
//        cryptoSecureStore = spy(new CryptoSecureStore(moshi, secureStore));
//    }
//
//    @Test
//    public void storeSecret_shouldStoreString() {
//        // When
//        cryptoSecureStore.storeSecret(DATA_ALIAS, DATA.toCharArray());
//
//        // Then
//        verify(secureStore, times(1)).addData(DATA_ALIAS, DATA.toCharArray());
//    }
//
//    @Test
//    public void storeSecret_shouldStoreObject() {
//        // Given
//        Object result = new Object();
//        when(moshi.adapter(any(Type.class))).thenReturn(adapter);
//        when(adapter.toJson(any())).thenReturn(OBJECT_JSON);
//
//        // When
//        cryptoSecureStore.storeSecret(OBJECT_ALIAS, result);
//
//        // Then
//        verify(secureStore, times(1)).addData(OBJECT_ALIAS, OBJECT_JSON.toCharArray());
//    }
//
//    @Test
//    public void getSecret_shouldReturnString() throws Exception {
//        // Given
//        when(secureStore.getData(DATA_ALIAS)).thenReturn(DATA.toCharArray());
//
//        // When
//        String result = new String(cryptoSecureStore.getSecret(DATA_ALIAS));
//
//        // Then
//        assertEquals(DATA, result);
//    }
//
//    @Test
//    public void getSecret_shouldReturnObject() throws Exception {
//        // Given
//        when(secureStore.getData(OBJECT_ALIAS)).thenReturn(OBJECT_JSON.toCharArray());
//        when(adapter.fromJson(OBJECT_JSON)).thenReturn(OBJECT);
//
//        // When
//        Object result = cryptoSecureStore.getSecret(OBJECT_ALIAS, Object.class);
//
//        // Then
//        assertEquals(OBJECT, result);
//    }
//
//    @Test
//    public void clearStorage_shoudBeCalledOnce() {
//        // When
//        cryptoSecureStore.clear();
//
//        // Then
//        verify(secureStore, times(1)).clear();
//    }
//
//    @Test
//    public void deleteSecret_shouldSucceed() {
//        // When
//        cryptoSecureStore.deleteSecret(DATA_ALIAS);
//
//        // Then
//        verify(secureStore, times(1)).removeData(DATA_ALIAS);
//    }
//
//    @Test
//    public void storeGCKey() {
//        // given
//        GCKey key = mock(GCKey.class);
//        when(moshi.adapter(any(Type.class))).thenReturn(adapter);
//        when(adapter.toJson(any())).thenReturn(DATA);
//
//        // When
//        cryptoSecureStore.storeKey(DATA_ALIAS, key, KeyType.DATA_KEY);
//
//        // Then
//        verify(adapter).toJson(any(ExchangeKey.class));
//        verify(key).getKeyBase64();
//        verify(key).getKeyVersion();
//        verifyNoMoreInteractions(key);
//        verify(secureStore).addData(eq(DATA_ALIAS), eq(DATA.toCharArray()));
//    }
//
//    @Test
//    public void storeGCKeyPair() {
//        // given
//        GCKeyPair key = mock(GCKeyPair.class);
//        GCRSAKeyAlgorithm algorithm = new GCRSAKeyAlgorithm();
//        when(key.getAlgorithm()).thenReturn(algorithm);
//        when(key.getPublicKeyBase64()).thenReturn(DATA);
//        when(key.getPrivateKeyBase64()).thenReturn(DATA);
//        when(key.getKeyVersion()).thenReturn(1);
//        when(moshi.adapter(any(Type.class))).thenReturn(adapter);
//        when(adapter.toJson(any())).thenReturn(DATA);
//
//        // When
//        cryptoSecureStore.storeKey(DATA_ALIAS, key);
//
//        // Then
//        verify(adapter).toJson(any(ExchangeKey.class));
//        verify(key).getPrivateKeyBase64();
//        verify(key).getPublicKeyBase64();
//        verify(key).getKeyVersion();
//        verifyNoMoreInteractions(key);
//        verify(secureStore).addData(eq(DATA_ALIAS), eq(DATA.toCharArray()));
//    }
//
//    @Test
//    public void getExchangeKey() throws IOException {
//        // Given
//        when(secureStore.getData(DATA_ALIAS)).thenReturn(DATA.toCharArray());
//        when(moshi.adapter(any(Type.class))).thenReturn(adapter);
//        ExchangeKey mockedExchangeKey = mock(ExchangeKey.class);
//        when(adapter.fromJson(DATA)).thenReturn(mockedExchangeKey);
//
//        // When
//        ExchangeKey exchangeKey = cryptoSecureStore.getExchangeKey(DATA_ALIAS);
//
//        // Then
//        assertEquals(mockedExchangeKey, exchangeKey);
//
//    }
//}
