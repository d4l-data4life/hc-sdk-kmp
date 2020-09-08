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

import java.lang.reflect.Field;

import care.data4life.sdk.lang.D4LException;
import care.data4life.sdk.lang.D4LRuntimeException;
import care.data4life.sdk.log.Log;

class D4LErrorHandler implements SdkContract.ErrorHandler {

    private static final String DETAIL_MESSAGE_FIELD_NAME = "detailMessage";
    private static final String EMPTY_MESSAGE = "";

    D4LErrorHandler() {
        //empty
    }

    @Override
    public D4LException handleError(Throwable error) {
        removeAnySecret(error);
        if (error instanceof D4LException) {
            return (D4LException) error;
        }
        return new D4LException(error);
    }

    private void removeAnySecret(Throwable error) {
        do {
            if (error instanceof D4LRuntimeException || error instanceof D4LException)
                continue;

            try {
                Field detailMessageField = Throwable.class.getDeclaredField(DETAIL_MESSAGE_FIELD_NAME);
                detailMessageField.setAccessible(true);
                detailMessageField.set(error, EMPTY_MESSAGE);
                detailMessageField.setAccessible(false);
            } catch (SecurityException | NoSuchFieldException | IllegalAccessException exception) {
                Log.error(exception, "Failed to erase detailMessage");
            }

        } while ((error = error.getCause()) != null);
    }
}
