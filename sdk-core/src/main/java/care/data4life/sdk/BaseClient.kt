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
package care.data4life.sdk

import care.data4life.sdk.SdkContract.LegacyClient
import care.data4life.sdk.call.CallHandler
import care.data4life.sdk.log.Log
import care.data4life.sdk.log.Logger

internal abstract class BaseClient(
        protected var alias: String,
        protected var userService: UserService,
        protected var recordService: RecordService,
        protected var handler: CallHandler,

        private val legacyClient: care.data4life.sdk.LegacyClient = createLegacyClient(alias, userService, recordService, handler)
) : LegacyClient by legacyClient {

    companion object {

        fun createLegacyClient(
                alias: String,
                userService: UserService,
                recordService: RecordService,
                handler: CallHandler
        ): care.data4life.sdk.LegacyClient {
            return LegacyClient(alias, userService, recordService, handler)
        }

        // FIXME refactor into own tool
        const val CLIENT_ID_SPLIT_CHAR = "#"

        // FIXME refactor into own tool
        const val PARTNER_ID_INDEX = 0

        // FIXME is this the right place?
        fun setLogger(logger: Logger) {
            Log.logger = logger
        }
    }
}
