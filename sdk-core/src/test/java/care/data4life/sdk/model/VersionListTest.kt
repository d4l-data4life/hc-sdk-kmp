/*
 * Copyright (c) 2021 D4L data4life gGmbH / All rights reserved.
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

package care.data4life.sdk.model

import care.data4life.sdk.network.model.NetworkModelContract
import care.data4life.sdk.network.model.Version
import care.data4life.sdk.network.model.VersionList
import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Test

class VersionListTest {
    @Test
    fun `Given a VersionList it implements interface `() {
        //Given
        val versions: Any = VersionList(listOf(Version(
                25,
                "1.9.0",
                "supported"
        )))
        Assert.assertTrue(versions is NetworkModelContract.VersionList)
    }

    @Test
    fun `VersionList is serializable `() {
        //Given
        val version = Version(
                25,
                "1.9.0",
                "supported"
        )
        val versions = VersionList(listOf<Version>(version))
        val moshi = Moshi.Builder().build()

        //When
        val actual = moshi.adapter<VersionList>(VersionList::class.java).toJson(versions)
        Assert.assertEquals(
                "{\"status\":\"supported\", \"version_name\":\"1.9.0\", \"version_code\":25}",
                actual
        )
    }
}
