/*
 * Copyright (c) 2022 D4L data4life gGmbH - All rights reserved.
 */

package care.data4life.sdk

import care.data4life.sdk.network.Environment

data class SdkConfig(
    val platform: String,
    val environment: Environment,
    val partnerId: String,
    val clientId: String,
    val clientSecret: String,
    val redirectUrl: String,
    val debug: Boolean = false
)
