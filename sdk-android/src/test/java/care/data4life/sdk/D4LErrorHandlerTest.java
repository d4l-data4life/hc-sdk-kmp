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
import org.junit.Test;

import care.data4life.sdk.lang.CoreRuntimeException;
import care.data4life.sdk.lang.D4LRuntimeException;

import static com.google.common.truth.Truth.assertThat;

public class D4LErrorHandlerTest {

    //SUT
    private D4LErrorHandler errorHandler;

    @Before
    public void setUp() {
        errorHandler = new D4LErrorHandler();
    }

    @Test
    public void handleError_shouldReturnExceptionComposition_withoutChange() {
        //given
        D4LRuntimeException firstExc = new CoreRuntimeException.Default("firstExc");
        D4LRuntimeException secondExc = new CoreRuntimeException.Default("secondExc", firstExc);

        //when
        Throwable exception = errorHandler.handleError(secondExc);

        //then
        assertThat(exception).hasMessageThat().isEqualTo(secondExc.toString());
        assertThat(exception).hasCauseThat().isEqualTo(secondExc);

        assertThat(exception.getCause()).hasMessageThat().isEqualTo("secondExc");
        assertThat(exception.getCause()).hasCauseThat().isEqualTo(firstExc);

        assertThat(exception.getCause().getCause()).hasMessageThat().isEqualTo("firstExc");
        assertThat(exception.getCause().getCause()).hasCauseThat().isEqualTo(null);
    }

    @Test
    public void handleError_shouldModifyExceptionComposition() {
        //given
        final String EMPTY_MESSAGE = "";
        NullPointerException firstExc = new NullPointerException("Nulls are dangerous!");
        D4LRuntimeException secondExc = new CoreRuntimeException.Default("secondExc", firstExc);

        //when
        Throwable exception = errorHandler.handleError(secondExc);

        //then
        assertThat(exception).hasMessageThat().isEqualTo(secondExc.toString());
        assertThat(exception).hasCauseThat().isEqualTo(secondExc);

        assertThat(exception.getCause()).hasMessageThat().isEqualTo("secondExc");
        assertThat(exception.getCause()).hasCauseThat().isEqualTo(firstExc);

        assertThat(exception.getCause().getCause()).hasMessageThat().isEqualTo(EMPTY_MESSAGE);
        assertThat(exception.getCause().getCause()).hasCauseThat().isEqualTo(null);
    }
}
