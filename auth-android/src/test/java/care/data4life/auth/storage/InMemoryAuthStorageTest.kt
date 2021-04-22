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

package care.data4life.auth.storage

import org.junit.Test

class InMemoryAuthStorageTest {

    private val sut = InMemoryAuthStorage()

    @Test
    fun fullInMemoryStorageTestCase() {
        // empty storage
        val actualInitialContains = sut.containsAuthState(ALIAS)
        kotlin.test.assertFalse(actualInitialContains, "Initial storage should be empty")

        // store AuthState
        sut.writeAuthState(ALIAS, AUTH_STATE)

        val actualShouldContain = sut.containsAuthState(ALIAS)
        kotlin.test.assertTrue(actualShouldContain, "AuthState should be present for the alias")

        // Read AuthState
        val actualAuthState = sut.readAuthState(ALIAS)
        kotlin.test.assertEquals(AUTH_STATE, actualAuthState, "AuthStates should match")

        // remove AuthState
        sut.removeAuthState(ALIAS)
        val actualContainAfterRemoval = sut.containsAuthState(ALIAS)
        kotlin.test.assertFalse(actualContainAfterRemoval, "AuthState should be removed")

        // clear storage
        sut.writeAuthState(ALIAS, AUTH_STATE)
        sut.clear()
        kotlin.test.assertFalse { sut.containsAuthState(ALIAS) }
    }

    companion object {
        private const val ALIAS = "ALIAS"
        private const val AUTH_STATE = "expected_authState"
    }
}
