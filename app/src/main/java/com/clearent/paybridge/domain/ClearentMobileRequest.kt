package com.clearent.paybridge.domain

import com.google.gson.annotations.SerializedName
import org.json.JSONObject
import java.io.Serializable

data class ClearentMobileRequest(
    @SerializedName("public-key")
    val publicKey: String,
    @SerializedName("bearer-token")
    val bearerToken: String,
    @SerializedName("base-url")
    val baseUrl: String,
    @SerializedName("request")
    val request: String,
    @SerializedName("total-amount")
    val totalAmount: String,
    @SerializedName("redirect-success-url")
    val redirectSuccessUrl: String?,
    @SerializedName("redirect-cancel-url")
    val redirectCancelUrl: String?
) : Serializable