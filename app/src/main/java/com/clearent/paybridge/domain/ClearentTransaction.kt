package com.clearent.paybridge.domain

import com.google.gson.annotations.SerializedName

data class ClearentTransaction(
    val result: String,
    @SerializedName("result-code")
    val resultCode: String,
    @SerializedName("display-message")
    val displayMessage: String
) {

    companion object {
        const val payloadType = "transaction"
    }
}