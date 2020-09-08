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

package care.data4life.sdk.tags;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class TagHelperTest {

    private static final String TAG_DELIMITER = "=";

    private static final String KEY_1 = "key_1";
    private static final String VALUE_1 = "value_1";

    private static final String KEY_2 = "key_2";
    private static final String VALUE_2 = "value_2";

    private static final String KEY_3 = "key_3";
    private static final String VALUE_3 = "value_3";


    @Test
    public void convertToTagList_shouldIncludeAllTagsFromMap() {
        // Given
        HashMap<String, String> tags = new HashMap<>();
        tags.put(KEY_1, VALUE_1);
        tags.put(KEY_2, VALUE_2);

        // When
        List<String> result = TagHelper.convertToTagList(tags);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains(KEY_1 + TAG_DELIMITER + VALUE_1));
        assertTrue(result.contains(KEY_2 + TAG_DELIMITER + VALUE_2));
    }

    @Test
    public void convertToTagList_shouldReturnEmptyList_whenEmptyTags() {
        // Given
        HashMap<String, String> tags = new HashMap<>();

        // When
        List<String> result = TagHelper.convertToTagList(tags);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    public void convertToTagMap_shouldReturnAllTagsInMap() {
        // Given
        List<String> tagList = new ArrayList<>();
        tagList.add(KEY_1 + TAG_DELIMITER + VALUE_1);
        tagList.add(KEY_2 + TAG_DELIMITER + VALUE_2);

        // When
        HashMap<String, String> result = TagHelper.convertToTagMap(tagList);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.containsKey(KEY_1));
        assertEquals(VALUE_1, result.get(KEY_1));
        assertTrue(result.containsKey(KEY_2));
        assertEquals(VALUE_2, result.get(KEY_2));
    }

    @Test
    public void convertToTagMap_shouldReturnOnlyCorrectTags() {
        // Given
        List<String> tagList = new ArrayList<>();
        tagList.add(TAG_DELIMITER + VALUE_1);
        tagList.add(KEY_2 + TAG_DELIMITER);
        tagList.add(KEY_3 + TAG_DELIMITER + VALUE_3);


        // When
        HashMap<String, String> result = TagHelper.convertToTagMap(tagList);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.containsKey(KEY_3));
        assertEquals(VALUE_3, result.get(KEY_3));
    }

    @Test
    public void convertToTagMap_shouldReturnEmptyMap_whenEmptyList() {
        // Given
        List<String> tagList = new ArrayList<>();

        // When
        HashMap<String, String> result = TagHelper.convertToTagMap(tagList);

        // Then
        assertTrue(result.isEmpty());
    }
}
