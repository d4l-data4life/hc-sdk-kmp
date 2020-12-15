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

package care.data4life.sdk.network

import care.data4life.crypto.GCKey
import care.data4life.sdk.network.model.DecryptedRecordGuard
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.mockito.Mockito

abstract class DecryptedRecordBuilderTestBase {
    protected lateinit var identifier: String
    protected lateinit var tags: HashMap<String, String>
    protected lateinit var annotations: List<String>
    protected lateinit var creationDate: String
    protected lateinit var updateDate: String
    protected lateinit var dataKey: GCKey
    protected var attachmentKey: GCKey? = null
    protected var modelVersion: Int = 0
    protected lateinit var resource: WrapperContract.Resource

    fun init() {
        identifier = "potato"
        @Suppress("UNCHECKED_CAST")
        tags = Mockito.mock(HashMap::class.java) as HashMap<String, String>
        @Suppress("UNCHECKED_CAST")
        annotations = Mockito.mock(List::class.java) as List<String>
        creationDate = "A Date"
        updateDate = "2020-05-03"
        dataKey = Mockito.mock(GCKey::class.java)
        attachmentKey = Mockito.mock(GCKey::class.java)
        modelVersion = 42
        resource = mockk()

        mockkObject(DecryptedRecordGuard)
        every { DecryptedRecordGuard.checkTagsAndAnnotationsLimits(any(), any()) } returns Unit
        every { DecryptedRecordGuard.checkDataLimit(any()) } returns Unit
    }

    fun stop() {
        unmockkAll()
    }
}
