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

import care.data4life.sdk.auth.AuthClient
import care.data4life.sdk.auth.AuthContract
import care.data4life.sdk.auth.UserService
import care.data4life.sdk.call.CallHandler
import care.data4life.sdk.data.DataRecordClient
import care.data4life.sdk.fhir.Fhir4RecordClient
import care.data4life.sdk.log.Log
import care.data4life.sdk.log.Logger

abstract class BaseClient constructor(
    protected var alias: String,
    protected var userService: AuthContract.UserService,
    protected var recordService: RecordService,
    protected var handler: CallHandler,
    private val authClient: SdkContract.AuthClient = createAuthClient(alias, userService, handler),
    override val data: SdkContract.DataRecordClient = createDataClient(userService, recordService, handler),
    override val fhir4: SdkContract.Fhir4RecordClient = createFhir4Client(userService, recordService, handler),
    private val legacyDataClient: SdkContract.LegacyDataClient = createLegacyDataClient(userService, recordService, handler)
) : SdkContract.Client, SdkContract.LegacyDataClient by legacyDataClient, SdkContract.AuthClient by authClient {
    override val userId: String = userService.userID.blockingGet()

    companion object {

        fun createAuthClient(
            alias: String,
            userService: AuthContract.UserService,
            handler: CallHandler
        ): SdkContract.AuthClient {
            return AuthClient(alias, userService, handler)
        }

        fun createDataClient(
            userService: AuthContract.UserService,
            recordService: RecordService,
            handler: CallHandler
        ): SdkContract.DataRecordClient {
            return DataRecordClient(userService, recordService, handler)
        }

        fun createFhir4Client(
            userService: AuthContract.UserService,
            recordService: RecordService,
            handler: CallHandler
        ): SdkContract.Fhir4RecordClient {
            return Fhir4RecordClient(userService, recordService, handler)
        }

        fun createLegacyDataClient(
            userService: AuthContract.UserService,
            recordService: RecordService,
            handler: CallHandler
        ): SdkContract.LegacyDataClient {
            return LegacyDataClient(userService as UserService, recordService, handler)
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
