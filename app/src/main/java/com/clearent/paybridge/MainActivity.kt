package com.clearent.paybridge

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.clearent.idtech.android.HasManualTokenizingSupport
import com.clearent.idtech.android.PublicOnReceiverListener
import com.clearent.idtech.android.domain.CardProcessingResponse
import com.clearent.idtech.android.family.DeviceFactory
import com.clearent.idtech.android.family.device.VP3300
import com.clearent.idtech.android.token.domain.TransactionToken
import com.clearent.idtech.android.token.manual.ManualCardTokenizer
import com.clearent.idtech.android.token.manual.ManualCardTokenizerImpl
import com.clearent.paybridge.domain.ClearentMobileRequest
import com.clearent.paybridge.domain.ClearentTransaction
import com.clearent.paybridge.domain.CreditCard
import com.google.gson.GsonBuilder
import com.idtechproducts.device.ErrorCode
import com.idtechproducts.device.ErrorCodeInfo
import com.idtechproducts.device.ReaderInfo
import com.idtechproducts.device.StructConfigParameters
import com.idtechproducts.device.bluetooth.BluetoothLEController
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity(), PublicOnReceiverListener, HasManualTokenizingSupport {

    companion object {
        const val DEVICE_ACTIVITY_REQUEST_CODE = 1
        const val ALL_PERMISSIONS_REQUEST_CODE = 2
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS
        )
    }

    private val TAG = MainActivity::class.java.simpleName

    private lateinit var manualCardTokenizer: ManualCardTokenizer

    lateinit var viewModel: TransactionViewModel

    private lateinit var requestMap: MutableMap<String, Any>

    private var applicationContext: PayBridgeApplicationContext? = null

    private var device: VP3300? = null

    private var connectDeviceTask: AsyncTask<VP3300, Void, Boolean>? = null

    private var isBleDeviceSet = false

    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hideSystemUI()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            root_layout.setOnApplyWindowInsetsListener { v, insets ->
                Log.d("INSETS", "apply window insets called!")
                insets.displayCutout?.let {
                    val density = resources.displayMetrics.density

                    val invoiceTextViewParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )
                    invoiceTextViewParams.width = 0
                    invoiceTextViewParams.topToTop = v.id
                    invoiceTextViewParams.topMargin = it.safeInsetTop
                    invoiceTextViewParams.startToStart = v.id
                    invoiceTextViewParams.leftMargin = (16 * density).toInt()
                    invoiceTextViewParams.endToStart = bleConnectionImageView.id
                    invoiceTextViewParams.rightMargin = (16 * density).toInt()
                    invoiceTextView.layoutParams = invoiceTextViewParams

                    val bleConnectionImageViewParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )
                    bleConnectionImageViewParams.topToTop = v.top
                    bleConnectionImageViewParams.topMargin =
                        it.safeInsetTop.plus(8 * density).toInt()
                    bleConnectionImageViewParams.endToStart = R.id.bleConnectionButton
                    bleConnectionImageViewParams.rightMargin = (4 * density).toInt()
                    bleConnectionImageView.handler.post {
                        bleConnectionImageView.layoutParams = bleConnectionImageViewParams
                    }

                    val cancelManualEntryButtonParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )
                    cancelManualEntryButtonParams.baselineToBaseline = invoiceTextView.id
                    cancelManualEntryButtonParams.endToEnd = v.id
                    cancelManualEntryButtonParams.rightMargin = (16 * density).toInt()
                    cancelManualEntryButton.layoutParams = cancelManualEntryButtonParams

                    insets.consumeDisplayCutout()
                }
                insets
            }
        }

        bleConnectionButton.setOnClickListener {
            startDeviceConnectionActivity()
        }

        infoButton.setOnClickListener {
            val intent = Intent(this, InfoActivity::class.java)
            startActivity(intent)
        }

        viewModel = ViewModelProviders.of(this).get(TransactionViewModel::class.java)

        viewModel.clearentMobileRequest.observe(this, Observer {
            if (it != null) {
                val gson = GsonBuilder().create()
                requestMap = gson.fromJson<MutableMap<String, Any>>(it.request, MutableMap::class.java)
                updateInvoiceNumberText(requestMap["invoice"] as? String)
            } else {
                requestMap = emptyMap<String, Any>().toMutableMap()
                updateInvoiceNumberText(null)
            }
        })

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_main, TransactionFragment.newInstance())
                .commit()
        }

        if (intent?.action == Intent.ACTION_VIEW) {
            initializeLaunchDetails(intent)
        }

        manualCardTokenizer = ManualCardTokenizerImpl(this)

//        connectButton.setOnClickListener {
//            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
//            textView.append(audioManager.isWiredHeadsetOn.toString())
//            device.registerListen()
//
//            device.device_configurePeripheralAndConnect()
//        }
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, ALL_PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DEVICE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.i(TAG, requestCode.toString())
            Log.i(TAG, resultCode.toString())
            Log.i(TAG, data.toString())
            val bluetoothDevice = data?.getParcelableExtra("device") as BluetoothDevice
            BluetoothLEController.setBluetoothDevice(bluetoothDevice)
            isBleDeviceSet = true

            val loadingAnimation = getDrawable(R.drawable.loading) as AnimationDrawable
            bleConnectionImageView.setImageDrawable(loadingAnimation)
            loadingAnimation.start()

            device?.run {
                connectDevice()
            }

            showTryingToConnectBleButtonAndImage()
        }
    }

    fun cancelTransaction() {
        device!!.device_cancelTransaction()
        if (viewModel.clearentMobileRequest.value?.redirectCancelUrl != null) {
            val uri = Uri.parse(viewModel.clearentMobileRequest.value!!.redirectCancelUrl)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
            handler.postDelayed({
                viewModel.resetTransaction()
                resetToInitialView()
            }, 500L)

        } else {
            viewModel.resetTransaction()
            resetToInitialView()
        }
    }

    private fun connectDevice() {
        if (connectDeviceTask == null) {
            connectDeviceTask = ConnectDeviceTask()
        } else {
            if (connectDeviceTask!!.status != AsyncTask.Status.RUNNING) {
                connectDeviceTask = ConnectDeviceTask()
            }
        }
        connectDeviceTask!!.execute(device)
    }

    private fun startDeviceConnectionActivity() {
        val intent = Intent(this, DeviceConnectionActivity::class.java)
        startActivityForResult(intent, DEVICE_ACTIVITY_REQUEST_CODE)
    }

    private fun showTryingToConnectBleButtonAndImage() {
        bleConnectionButton.text = getString(R.string.trying_to_connect)
        bleConnectionButton.setOnClickListener {
            disconnectBleDevice()
        }
        val loadingAnimation = getDrawable(R.drawable.loading) as AnimationDrawable
        bleConnectionImageView.setImageDrawable(loadingAnimation)
        loadingAnimation.start()
    }

    private fun disconnectBleDevice() {
        device?.unregisterListen()
        device?.release()
        isBleDeviceSet = false
    }

    private fun hideSystemUI() {
        window.decorView.run {
            this.setOnSystemUiVisibilityChangeListener {
                if (it.and(View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    this.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == Intent.ACTION_VIEW) {
            initializeLaunchDetails(intent)
        }
    }

    private fun initializeLaunchDetails(intent: Intent) {
        val clearentMobileRequest =
            createClearentMobileRequestFromIntentParameters(intent.data)

        Log.d(TAG, clearentMobileRequest.toString())

        clearentMobileRequest?.let {
            viewModel.clearentMobileRequest.postValue(it)
            val incomingContext = PayBridgeApplicationContext(
                ReaderInfo.DEVICE_TYPE.DEVICE_VP3300_BT,
                this,
                this,
                it.baseUrl,
                it.publicKey,
                null
            )
            if (applicationContext == null || (applicationContext != null && applicationContext!! != incomingContext)) {
                applicationContext = incomingContext
                device = DeviceFactory.getVP3300(applicationContext)
                device!!.setAutoConfiguration(false)
                device!!.device_setDeviceType(ReaderInfo.DEVICE_TYPE.DEVICE_VP3300_BT)
            }

            if (!device!!.device_isConnected() && isBleDeviceSet) {
                connectDevice()
                showTryingToConnectBleButtonAndImage()
            }
        }
    }

    fun resetToInitialView() {
        cancelManualEntryButton.isVisible = false
        bleConnectionImageView.isVisible = true
        bleConnectionButton.isVisible = true
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_main, TransactionFragment.newInstance())
            .commit()
    }

    fun createClearentMobileRequestFromIntentParameters(uri: Uri?): ClearentMobileRequest? {
        val parameters: MutableMap<String, String?> = HashMap()
        uri?.queryParameterNames?.forEach {
            parameters[it] = uri.getQueryParameter(it)
        }
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonFromParameters = gson.toJson(parameters)
        return gson.fromJson(
            jsonFromParameters,
            ClearentMobileRequest::class.java
        )
    }

    private fun updateInvoiceNumberText(invoice: String?) {
        invoiceTextView.text = createInvoiceNumberText(invoice)
    }

    fun createInvoiceNumberText(invoice: String?) : String? {
        return if (invoice != null) "Invoice #$invoice" else null
    }

    private fun updateConnectBleButtonAndImage(connected: Boolean) {
        if (connected) {
            bleConnectionButton.text = getString(R.string.connected)
            bleConnectionButton.setOnClickListener {
                disconnectBleDevice()
            }
            bleConnectionImageView.setImageDrawable(getDrawable(R.drawable.connected))
        } else {
            bleConnectionButton.text = getString(R.string.connect_now)
            bleConnectionButton.setOnClickListener {
                startDeviceConnectionActivity()
            }
            bleConnectionImageView.setImageDrawable(getDrawable(R.drawable.disconnected))
        }
    }

    fun runTransaction() {
        Log.d(TAG, "Getting Mobile Jwt")

        val ret = device!!.device_startTransaction(0.0, 0.0, 0, 60, null)
        Log.d(TAG, "$ret")
        if (ret == ErrorCode.SUCCESS || ret == ErrorCode.RETURN_CODE_OK_NEXT_COMMAND) {
            Log.d(TAG, "Insert or swipe card")
            showCardInteractionFragment()
        }
    }

    fun runManualTransaction(card: String, expDate: String, securityCode: String) {
        val creditCard = CreditCard(card, expDate, securityCode)
        manualCardTokenizer.createTransactionToken(creditCard)
        showCardInteractionFragment()
        viewModel.processing.postValue(true)
    }

    private fun showCardInteractionFragment() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_main, CardInteractionFragment.newInstance())
            .commit()
    }

    private fun runPayment(jwt: String) {
        val queue = Volley.newRequestQueue(this)
        val clearentMobileRequest = viewModel.clearentMobileRequest.value!!
        val url =
            "${clearentMobileRequest.baseUrl}/rest/v2/mobile/transactions/${(requestMap["type"] as String).toLowerCase(Locale.ENGLISH)}"

        requestMap["software-type"] = SOFTWARE_TYPE
        requestMap["software-type-version"] = BuildConfig.VERSION_NAME

        val jsonObject = JSONObject(requestMap)

        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, url, jsonObject,
            Response.Listener {
                handleResponse(it)
            },
            Response.ErrorListener {
                Log.d("VOLLEY ERROR", it.toString())
                viewModel.isSuccessfulTransaction.postValue(false)
                showTransactionResultFragment()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(
                    Pair("Authorization", "Bearer ${clearentMobileRequest.bearerToken}"),
                    Pair("mobilejwt", jwt),
                    Pair("Accept", "application/json")
                )
            }
        }

        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        queue.add(jsonObjectRequest)
    }

    private fun showTransactionResultFragment() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_main, TransactionResultFragment.newInstance())
            .commit()
    }

    private fun handleResponse(response: JSONObject) {
        val responsePayload = response["payload"] as JSONObject
        val payloadType = responsePayload["payloadType"] as String
        when (payloadType.toLowerCase(Locale.ENGLISH)) {
            ClearentTransaction.payloadType -> handleTransactionPayload(responsePayload)
        }
        Log.i(TAG, "RESPONSE: $response")
    }

    private fun handleTransactionPayload(payload: JSONObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val transaction =
            gson.fromJson<ClearentTransaction>(
                payload[ClearentTransaction.payloadType].toString(),
                ClearentTransaction::class.java
            )
        if (transaction.result.equals("APPROVED", true) && transaction.resultCode == "000") {
            if (transaction.displayMessage == "Transaction previously approved") {
                viewModel.isDuplicateTransaction.postValue(true)
                showTransactionResultFragment()
            }
            else if (viewModel.clearentMobileRequest.value?.redirectSuccessUrl != null) {
                val uri = Uri.parse(viewModel.clearentMobileRequest.value!!.redirectSuccessUrl)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
                handler.postDelayed({
                    viewModel.resetTransaction()
                    resetToInitialView()
                }, 500L)
            } else {
                viewModel.isSuccessfulTransaction.postValue(true)
                showTransactionResultFragment()
            }
        }
    }

    //publicOnReceiverListener methods
    override fun successfulTransactionToken(transactionToken: TransactionToken) {
        Log.d(TAG, "SUCCESS! Token returned. Running Transaction")
        viewModel.processing.takeIf { it.value == false }.apply {
            this?.postValue(true)
        }
        val jwt = transactionToken.transactionToken
        Log.d(TAG, "Transaction token value: $jwt")
        runPayment(jwt)
    }

    override fun LoadXMLConfigFailureInfo(index: Int, strMessage: String) {
        Log.d("XML Configuration", "Index: $index, message: $strMessage")
    }

    override fun deviceConfigured() {
        Log.d(TAG, "DEVICE CONFIGURED")
    }

    override fun timeout(errorCode: Int) {
        val errorCodeDescription = ErrorCodeInfo.getErrorCodeDescription(errorCode)
        if (errorCodeDescription.contains("Timeout")) {
            viewModel.isSuccessfulTransaction.postValue(false)
            showTransactionResultFragment()
        }
        Log.d(TAG, "errorCode: $errorCode, description: $errorCodeDescription")
    }

    override fun deviceConnected() {
        Log.d(TAG, "DEVICE CONNECTED")
        viewModel.deviceConnected.postValue(device!!.device_isConnected())
        runOnUiThread {
            updateConnectBleButtonAndImage(device!!.device_isConnected())
        }
    }

    override fun msgAudioVolumeAdjustFailed() {
        Log.d(TAG, "Audio Volume Adjust Failed")
    }

    override fun dataInOutMonitor(data: ByteArray?, isIncoming: Boolean) {
        Log.d(TAG, "data: $data, isIncoming: $isIncoming")
    }

    override fun autoConfigProgress(i: Int) {
        //not used
    }

    override fun deviceDisconnected() {
        Log.d(TAG, "DEVICE DISCONNECTED")
        viewModel.deviceConnected.postValue(device!!.device_isConnected())
        runOnUiThread {
            updateConnectBleButtonAndImage(device!!.device_isConnected())
        }
    }

    override fun handleCardProcessingResponse(cardProcessingResponse: CardProcessingResponse) {
        Log.d(TAG, "Handle Card Processing Reponse: ${cardProcessingResponse.displayMessage}")

        when(cardProcessingResponse.displayMessage) {
            "USE MAGSTRIPE" -> viewModel.useMagstripe.postValue(true)
            "USE CHIP READER" -> viewModel.useChipReader.postValue(true)
            "Invalid swipe" -> {
                viewModel.isSuccessfulTransaction.postValue(false)
                showTransactionResultFragment()
            }
        }
    }

    override fun msgToConnectDevice() {
        Log.d(TAG, "To Connect Device")
    }

    override fun autoConfigCompleted(structConfigParameters: StructConfigParameters) {
        //not used
    }

    override fun isReady() {
        Log.d(TAG, "IS READY")
        runOnUiThread {
            updateConnectBleButtonAndImage(device!!.device_isConnected())
        }
    }

    override fun handleConfigurationErrors(message: String?) {
        Log.d(TAG, "CONFIGURATION ERROR: $message")
    }

    override fun ICCNotifyInfo(dataNotify: ByteArray, strMessage: String) {
        //not used
    }

    override fun msgBatteryLow() {
        Log.d(TAG, "BATTERY LOW")
    }

    override fun lcdDisplay(mode: Int, lines: Array<String>, timeout: Int) {
        for (line in lines) {
            Log.d(TAG, "LCDDisplay $line")
            when (line) {
                "GO ONLINE" -> {
                    viewModel.goOnline.postValue(true)
                    viewModel.processing.postValue(false)
                }
                "PROCESSING..." ->
                    viewModel.processing.takeIf { it.value == false }
                        .apply {
                            this?.postValue(true)
                        }
                "Failed to read card" -> {
                    viewModel.isSuccessfulTransaction.postValue(false)
                    showTransactionResultFragment()
                }
            }
        }
    }

    override fun lcdDisplay(
        mode: Int,
        lines: Array<String>,
        timeout: Int,
        languageCode: ByteArray,
        messageId: Byte
    ) {
        for (line in lines) {
            Log.d(TAG, "LCDDisplay2 $line")
        }
    }

    //Manual Entry Methods
    override fun getPaymentsPublicKey(): String {
        return viewModel.clearentMobileRequest.value!!.publicKey
    }

    override fun getPaymentsBaseUrl(): String {
        return viewModel.clearentMobileRequest.value!!.baseUrl
    }

    override fun handleManualEntryError(message: String?) {
        Log.d(TAG, "$message")
    }

}
