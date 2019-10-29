package com.clearent.paybridge

import android.os.AsyncTask
import com.clearent.idtech.android.family.device.VP3300

class ConnectDeviceTask : AsyncTask<VP3300, Void, Boolean>() {

    override fun doInBackground(vararg devices: VP3300?): Boolean {
        devices.first()?.registerListen()
        return true
    }

}