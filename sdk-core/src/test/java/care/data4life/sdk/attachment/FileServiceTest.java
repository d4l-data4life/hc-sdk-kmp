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

package care.data4life.sdk.attachment;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Objects;

import care.data4life.crypto.GCKey;
import care.data4life.sdk.crypto.CryptoService;
import care.data4life.sdk.network.ApiService;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FileServiceTest {

    private static final String ALIAS = "alias";
    private static final String FILE_ID = "fileId";
    private static final String USER_ID = "userId";

    private static final byte[] RESULT = new byte[1];

    private FileService fileService;
    private ApiService apiService;
    private CryptoService cryptoService;

    @Before
    public void setUp() throws Exception {
        apiService = mock(ApiService.class);
        cryptoService = mock(CryptoService.class);

        fileService = spy(new FileService(ALIAS, apiService, cryptoService));
    }

    @Test
    public void downloadFile_shouldReturnSingleWithByteArrayOrError() {
        // given
        when(apiService.downloadDocument(anyString(), anyString(), anyString())).thenReturn(Single.just(RESULT));
        when(cryptoService.decrypt(any(), any(byte[].class))).thenReturn(Single.just(RESULT));
        when(apiService.downloadDocument(anyString(), anyString(), anyString())).thenReturn(Single.error(new IOException()));


        // when
        TestObserver<byte[]> testSubscriber = fileService.downloadFile(mock(GCKey.class), USER_ID, FILE_ID)
                .test();

        // then
        testSubscriber.assertError(Throwable.class);
    }

    @Test
    public void uploadFiles_shouldReturnTrueOrFalse() {
        // given
        byte[] result = new byte[1];

        when(cryptoService.encrypt(any(), any(byte[].class))).thenReturn(Single.just(RESULT));

        when(apiService.uploadDocument(anyString(), anyString(), any(byte[].class))).thenReturn(Single.just(FILE_ID));

        // when
        TestObserver<String> testSubscriber = fileService.uploadFile(mock(GCKey.class), USER_ID, new byte[1])
                .test();

        when(apiService.uploadDocument(anyString(), anyString(), any())).thenReturn(Single.error(new Throwable()));
        TestObserver<String> testSubscriber2 = fileService.uploadFile(mock(GCKey.class), USER_ID, new byte[1])
                .test();

        // then
        verify(apiService, times(2)).uploadDocument(ALIAS, USER_ID, RESULT);
        testSubscriber
                .assertNoErrors()
                .assertComplete();

        testSubscriber2.assertError(Throwable.class);
    }

    @Test
    public void deleteFile() {
        when(apiService.deleteDocument(ALIAS, USER_ID, FILE_ID)).thenReturn(Single.just(true));

        // when
        TestObserver<Boolean> subscriber = fileService.deleteFile(USER_ID, FILE_ID).test();

        // then
        subscriber.assertComplete()
                .assertNoErrors()
                .assertValue(it -> it);
    }

    @Test
    public void deleteFile_shouldFail() {
        when(apiService.deleteDocument(ALIAS, USER_ID, FILE_ID)).thenReturn(Single.error(new Throwable()));

        // when
        TestObserver<Boolean> subscriber = fileService.deleteFile(USER_ID, FILE_ID).test();

        // then
        subscriber.assertNotComplete()
                .assertError(Objects::nonNull);
    }

}
