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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import care.data4life.crypto.GCKey;
import care.data4life.sdk.test.util.TestSchedulerRule;
import care.data4life.sdk.util.Base64;

import static care.data4life.sdk.TaggingService.TAG_DELIMITER;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TagEncryptionServiceTest {

    private final static String ANNOTATION_KEY = "custom" + TAG_DELIMITER;

    @Rule
    public TestSchedulerRule schedulerRule = new TestSchedulerRule();
    private CryptoService mockCryptoService;
    private Base64 mockBase64;


    private TagEncryptionService sut;


    @Before
    public void setUp() {
        mockCryptoService = mock(CryptoService.class);
        mockBase64 = mock(Base64.class);

        sut = new TagEncryptionService(mockCryptoService, mockBase64);
    }


    @Test
    public void encryptTags() throws Exception {
        // given
        doReturn(mock(GCKey.class)).when(mockCryptoService).fetchTagEncryptionKey();
        byte[] symEncrypted = new byte[0];
        doReturn(symEncrypted).when(mockCryptoService).symEncrypt(any(), any(), any());
        String encryptedTag = "encryptedTag";
        doReturn(encryptedTag).when(mockBase64).encodeToString(symEncrypted);
        HashMap<String, String> tags = new HashMap<>();
        tags.put("KEY", "value");

        // when
        List<String> encryptedTags = sut.encryptTags(tags);

        // then
        assertThat(encryptedTags).containsExactly(encryptedTag);

        verify(mockCryptoService).symEncrypt(any(), eq("KEY=value".getBytes()), any());
    }

    @Test(expected = RuntimeException.class)
    public void encryptTags_shouldThrowException() throws Exception {
        // given
        doReturn(mock(GCKey.class)).when(mockCryptoService).fetchTagEncryptionKey();
        byte[] symEncrypted = new byte[0];
        doReturn(symEncrypted).when(mockCryptoService).symEncrypt(any(), any(), any());
        doReturn(null).when(mockBase64).encodeToString(symEncrypted);
        HashMap<String, String> tags = new HashMap<>();
        tags.put("KEY", "value");

        // when
        sut.encryptTags(tags);

    }

    @Test
    public void decryptTags() throws Exception {
        // given
        doReturn(mock(GCKey.class)).when(mockCryptoService).fetchTagEncryptionKey();
        List<String> encryptedTags = new ArrayList<>();
        String eTag = "encryptedTag";
        encryptedTags.add(eTag);
        doReturn(eTag.getBytes()).when(mockBase64).decode(any(String.class));
        String keyValue = "key=value";
        doReturn(keyValue.getBytes()).when(mockCryptoService).symDecrypt(any(), any(), any());

        // when
        HashMap<String, String> decryptedTags = sut.decryptTags(encryptedTags);

        // then
        assertThat(decryptedTags).containsExactly("key", "value");
    }

    @Test
    public void decryptTags_filtersAnnotationKey() throws Exception {
        // given
        doReturn(mock(GCKey.class)).when(mockCryptoService).fetchTagEncryptionKey();
        List<String> encryptedTags = new ArrayList<>();
        String eTag = "encryptedTag";
        encryptedTags.add(eTag);
        doReturn(eTag.getBytes()).when(mockBase64).decode(any(String.class));
        String keyValue = ANNOTATION_KEY +  "something";
        doReturn(keyValue.getBytes()).when(mockCryptoService).symDecrypt(any(), any(), any());

        // when
        HashMap<String, String> decryptedTags = sut.decryptTags(encryptedTags);

        // then
        assertThat(decryptedTags).containsExactly();
    }

    @Test(expected = RuntimeException.class)
    public void decryptTags_shouldThrowException() throws Exception {
        // given
        List<String> encryptedTags = new ArrayList<>();
        encryptedTags.add("ignored");

        // when
        sut.decryptTags(encryptedTags);
    }

    @Test
    public void encryptAnnotations() throws Exception {
        // given
        String expected = "value";
        doReturn(mock(GCKey.class)).when(mockCryptoService).fetchTagEncryptionKey();
        byte[] symEncrypted = new byte[0];
        doReturn(symEncrypted).when(mockCryptoService).symEncrypt(any(), any(), any());
        String encryptedTag = "encryptedTag";
        doReturn(encryptedTag).when(mockBase64).encodeToString(symEncrypted);
        ArrayList<String> tags = new ArrayList<>();
        tags.add(expected);

        // when
        List<String> encryptedAnnotations = sut.encryptAnnotations(tags);

        // then
        assertThat(encryptedAnnotations).containsExactly(encryptedTag);

        verify(mockCryptoService).symEncrypt(any(), eq((ANNOTATION_KEY+expected).getBytes()), any());
    }

    @Test(expected = RuntimeException.class)
    public void encryptAnnotations_shouldThrowException() throws Exception {
        // given
        doReturn(mock(GCKey.class)).when(mockCryptoService).fetchTagEncryptionKey();
        byte[] symEncrypted = new byte[0];
        doReturn(symEncrypted).when(mockCryptoService).symEncrypt(any(), any(), any());
        doReturn(null).when(mockBase64).encodeToString(symEncrypted);
        ArrayList<String> annotations = new ArrayList<>();
        annotations.add("value");

        // when
        sut.encryptAnnotations(annotations);
    }

    @Test
    public void decryptAnnotations() throws Exception {
        // given
        String expected = "value";
        doReturn(mock(GCKey.class)).when(mockCryptoService).fetchTagEncryptionKey();
        List<String> encryptedAnnotation = new ArrayList<>();
        String eTag = "encryptedTag";
        encryptedAnnotation.add(eTag);
        doReturn(eTag.getBytes()).when(mockBase64).decode(any(String.class));
        String keyValue = ANNOTATION_KEY + expected;
        doReturn(keyValue.getBytes()).when(mockCryptoService).symDecrypt(any(), any(), any());

        // when
        List<String> decryptedAnnotations = sut.decryptAnnotations(encryptedAnnotation);

        // then
        assertThat(decryptedAnnotations).containsExactly(expected);
    }

    @Test
    public void decryptAnnotations_filtersNonAnnotationKey() throws Exception {
        // given
        doReturn(mock(GCKey.class)).when(mockCryptoService).fetchTagEncryptionKey();
        List<String> encryptedAnnotation = new ArrayList<>();
        String eTag = "encryptedTag";
        encryptedAnnotation.add(eTag);
        doReturn(eTag.getBytes()).when(mockBase64).decode(any(String.class));
        String keyValue = "key=something";
        doReturn(keyValue.getBytes()).when(mockCryptoService).symDecrypt(any(), any(), any());

        // when
        List<String> decryptedAnnotations = sut.decryptAnnotations(encryptedAnnotation);

        // then
        assertThat(decryptedAnnotations).containsExactly();
    }

    @Test(expected = RuntimeException.class)
    public void decryptAnnotations_shouldThrowException() throws Exception {
        // given
        List<String> encryptedAnnotations = new ArrayList<>();
        encryptedAnnotations.add("ignored");

        // when
        sut.decryptTags(encryptedAnnotations);
    }
}
