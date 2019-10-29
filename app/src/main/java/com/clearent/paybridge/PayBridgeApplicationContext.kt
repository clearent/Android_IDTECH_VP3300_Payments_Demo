package com.clearent.paybridge

import android.content.Context
import com.clearent.idtech.android.PublicOnReceiverListener
import com.clearent.idtech.android.family.ApplicationContext
import com.idtechproducts.device.ReaderInfo

data class PayBridgeApplicationContext(
    private val deviceType: ReaderInfo.DEVICE_TYPE,
    private val publicOnReceiverListener: PublicOnReceiverListener,
    private val context: Context,
    private val paymentsBaseUrl: String,
    private val paymentsPublicKey: String,
    private val idTechXmlConfigurationFileLocation: String?
) : ApplicationContext {

    private var autoConfiguration: Boolean = false

    override fun getAndroidContext(): Context {
        return context
    }

    override fun getPublicOnReceiverListener(): PublicOnReceiverListener {
        return publicOnReceiverListener
    }

    override fun getPaymentsPublicKey(): String {
        return paymentsPublicKey
    }

    override fun getIdTechXmlConfigurationFileLocation(): String? {
        return idTechXmlConfigurationFileLocation
    }

    override fun getPaymentsBaseUrl(): String {
        return paymentsBaseUrl
    }

    override fun getDeviceType(): ReaderInfo.DEVICE_TYPE {
        return deviceType
    }

    override fun disableAutoConfiguration(): Boolean {
        return autoConfiguration
    }
}
