package com.clearent.paybridge.domain

import com.clearent.idtech.android.token.manual.ManualEntry
import com.clearent.paybridge.BuildConfig
import com.clearent.paybridge.SOFTWARE_TYPE

data class CreditCard(private val _card: String,
                      private val _expirationDateMMYY: String,
                      private val _csc: String): ManualEntry {
    override fun getCard(): String {
        return _card
    }

    override fun getExpirationDateMMYY(): String {
        return _expirationDateMMYY
    }

    override fun getCsc(): String {
        return _csc
    }

    override fun getSoftwareType(): String {
        return SOFTWARE_TYPE
    }

    override fun getSoftwareTypeVersion(): String {
        return BuildConfig.VERSION_NAME
    }
}