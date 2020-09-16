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

import care.data4life.crypto.GCKey
import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.fhir.stu3.model.Extension
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.listener.ResultListener
import care.data4life.sdk.model.Meta
import care.data4life.sdk.model.Record

//TODO: Replace with basic data record(id,dates,tags etc.)
typealias AppData = String


//TODO: Move to appropriate place
data class AppDataResource(val resource: String?, var id: String? = null)

data class AppDataRecord(val appDataResource: AppDataResource, val meta: Meta)

data class DecryptedAppDataRecord(
        val identifier: String?,
        val appData_: AppDataResource,
        val tags: HashMap<String, String>,
        val customCreationDate: String,
        val updatedDate: String?,
        val dataKey: GCKey,
        val modelVersion: Int
) {
    fun copyWithResource(appData_: AppDataResource) = copy(appData_ = appData_)
}



fun SdkContract.Client.createRecord(vararg data: String, resultListener: ResultListener<AppData>) {
    val records = data.map { data ->
        DomainResource().apply {
            extension = listOf(Extension("appData").apply {
                valueString = data })
        }
    }

    records.forEach {
        createRecord(it, wrapResultListener(resultListener))
    }
}

fun SdkContract.Client.downloadRecord(vararg id: String, resultListener: ResultListener<AppData>) {
    id.forEach {
        downloadRecord(it,wrapResultListener(resultListener))
    }
}

fun SdkContract.Client.updateRecord(vararg data:String, resultListener: ResultListener<AppData>) {
    data.forEach {
        updateRecord(TODO(), wrapResultListener(resultListener))
    }
}

fun SdkContract.Client.deleteRecord(vararg id: String, resultListener: ResultListener<AppData>) {
    id.forEach {
        deleteRecord(it, TODO())
    }
}

private fun wrapResultListener(resultListener: ResultListener<AppData>) =
    object : ResultListener<Record<DomainResource>> {
        override fun onError(exception: D4LException?) {
            resultListener.onError(exception)
        }

        override fun onSuccess(t: Record<DomainResource>?) {
            resultListener.onSuccess(t!!.fhirResource.extension!!.first().valueString)
        }
    }
