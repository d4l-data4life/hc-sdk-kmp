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

import care.data4life.sdk.call.CallHandler;
import care.data4life.sdk.listener.Callback;
import care.data4life.sdk.listener.ResultListener;
import io.reactivex.Single;

/**
 * Deprecated with version v1.9.0
 * <p>
 * Will be removed in version v2.0.0
 */
@Deprecated
class LegacyAuthClient implements SdkContract.LegacyAuthClient {

    protected CallHandler handler;
    protected String alias;
    public UserService userService;
    protected RecordService recordService;

    /**
     * Deprecated with version v1.9.0
     * <p>
     * Will be removed in version v2.0.0
     */
    @Deprecated
    LegacyAuthClient(
            String alias,
            UserService userService,
            RecordService recordService,
            CallHandler handler
    ) {
        this.alias = alias;
        this.userService = userService;
        this.recordService = recordService;
        this.handler = handler;
    }

    @Override
    public void getUserSessionToken(ResultListener<String> listener) {
        Single<String> operation = userService.getSessionToken(alias);
        handler.executeSingle(operation, listener);
    }

    @Override
    public void isUserLoggedIn(ResultListener<Boolean> listener) {
        Single<Boolean> operation = userService.isLoggedIn(alias);
        handler.executeSingle(operation, listener);
    }

    @Override
    public void logout(Callback listener) {
        handler.executeCompletable(userService.logout(), listener);
    }

}
