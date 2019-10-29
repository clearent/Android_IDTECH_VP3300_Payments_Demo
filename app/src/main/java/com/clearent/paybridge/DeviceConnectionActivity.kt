package com.clearent.paybridge

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_device_connection.*

class DeviceConnectionActivity : AppCompatActivity() {

    private lateinit var deviceListAdapter: DeviceListAdapter

    private var devices = ArrayList<BluetoothDevice>()

    private val handler = Handler()

    private val scanCallBack = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let {
                if (!devices.contains(it) && it.name != null && it.name.contains("IDTECH")) {
                    devices.add(it)
                    deviceListAdapter.notifyItemInserted(devices.size)
                }
            }
        }
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_connection)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            device_connection_screen.setOnApplyWindowInsetsListener { v, insets ->
                insets.displayCutout?.let {
                    val density = resources.displayMetrics.density

                    val cancelButtonParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )
                    cancelButtonParams.topToTop = v.id
                    cancelButtonParams.endToEnd = v.id
                    cancelButtonParams.topMargin = it.safeInsetTop
                    cancelButtonParams.rightMargin = (16 * density).toInt()

                    cancelButton.layoutParams = cancelButtonParams

                    insets.consumeDisplayCutout()
                }
                insets
            }
        }

        deviceRecyclerView.layoutManager = LinearLayoutManager(this)
        val divider = getDrawable(R.drawable.divider)
        val inset = resources.getDimensionPixelSize(R.dimen.deviceNameLeftMargin)
        val insetDivider = InsetDrawable(divider, inset, 0, 0, 0)
        val dividerDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        dividerDecoration.setDrawable(insetDivider)
        deviceRecyclerView.addItemDecoration(dividerDecoration)
        deviceListAdapter = DeviceListAdapter(devices)
        deviceRecyclerView.adapter = deviceListAdapter

        cancelButton.setOnClickListener {
            startLeScan(false)
            finish()
        }

        connectBluetoothDevice()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    internal inner class DeviceListAdapter(private val devices: List<BluetoothDevice>) : RecyclerView.Adapter<DeviceViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view =
                layoutInflater.inflate(R.layout.device_recyclerview_item_row, parent, false)
            return DeviceViewHolder(view)
        }

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            holder.setDevice(devices[position])
        }

        override fun getItemCount() = devices.count()

    }

    internal inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private var textView: TextView = itemView.findViewById(R.id.deviceName)
        private var device: BluetoothDevice? = null

        init {
            itemView.setOnClickListener(this)
        }

        fun setDevice(device: BluetoothDevice?) {
            this.device = device
            textView.text = device?.name
        }

        override fun onClick(v: View?) {
            device?.let {
                val returnIntent = Intent().putExtra("device", it)
                setResult(Activity.RESULT_OK, returnIntent)
                startLeScan(false)
                finish()
            }
        }
    }

    private fun connectBluetoothDevice() {
        bluetoothAdapter.takeIf { !it.isEnabled }.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        startLeScan(true)
    }

    private fun startLeScan(enable: Boolean) {
        when (enable) {
            true -> {
                handler.postDelayed({
                    bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallBack)
                }, 10000L)
                val scanSettings =
                    ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setReportDelay(0).build()
                bluetoothAdapter.bluetoothLeScanner.startScan(
                    null,
                    scanSettings,
                    scanCallBack
                )
            }
            false -> {
                bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallBack)
            }
        }
    }
}
