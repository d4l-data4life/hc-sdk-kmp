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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TagHelper {

    private TagHelper() {
    }

    private static final String TAG_DELIMITER = "=";


    public static List<String> convertToTagList(HashMap<String, String> tags) {
        List<String> tagList = new ArrayList<>();
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            tagList.add(entry.getKey() + TAG_DELIMITER + entry.getValue());
        }
        return tagList;
    }


    public static HashMap<String, String> convertToTagMap(List<String> tagList) {
        HashMap<String, String> tags = new HashMap<>();
        for (String entry : tagList) {
            String[] split = entry.split(TAG_DELIMITER);
            if (split.length == 2 && split[0] != null && !split[0].isEmpty()) {
                tags.put(split[0], split[1]);
            }
        }
        return tags;
    }

}
