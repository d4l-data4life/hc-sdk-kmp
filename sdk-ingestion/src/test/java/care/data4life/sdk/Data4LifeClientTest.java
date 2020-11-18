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

import care.data4life.auth.AuthorizationService;
import care.data4life.crypto.GCAsymmetricKey;
import care.data4life.crypto.GCKeyPair;
import care.data4life.sdk.test.util.TestSchedulerRule;
import io.reactivex.Single;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class Data4LifeClientTest {

    private static final boolean IS_LOGGED_IN = true;
    private static final boolean IS_LOGGED_OUT = false;
    private static final String ALIAS = "alias";


    @Rule
    public TestSchedulerRule schedulerRule = new TestSchedulerRule();
    private UserService userService;
    private AuthorizationService authorizationService;
    private CryptoService cryptoService;
    private RecordService recordService;
    private Data4LifeClient instance;
    private SdkContract.ErrorHandler errorHandler;

    @Before
    public void setUp() {
        authorizationService = mock(AuthorizationService.class);
        cryptoService = mock(CryptoService.class);
        userService = mock(UserService.class);
        recordService = mock(RecordService.class);
        errorHandler = mock(SdkContract.ErrorHandler.class);


        instance = new Data4LifeClient(
                ALIAS,
                userService,
                recordService,
                errorHandler);
    }

    // TODO Need to define new tests

}
