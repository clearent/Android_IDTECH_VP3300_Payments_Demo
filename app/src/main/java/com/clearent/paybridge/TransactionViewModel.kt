package com.clearent.paybridge

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.clearent.paybridge.domain.ClearentMobileRequest

class TransactionViewModel : ViewModel() {
    val clearentMobileRequest = MutableLiveData<ClearentMobileRequest?>()
    val goOnline = MutableLiveData<Boolean>()
    val processing = MutableLiveData<Boolean>()
    val useMagstripe = MutableLiveData<Boolean>()
    val useChipReader = MutableLiveData<Boolean>()
    val deviceConnected = MutableLiveData<Boolean>()
    val isSuccessfulTransaction = MutableLiveData<Boolean?>()
    val isDuplicateTransaction = MutableLiveData<Boolean?>()

    init {
        resetTransaction()
    }

    fun resetTransaction() {
        reloadTransaction()
        clearentMobileRequest.value = null
    }

    fun reloadTransaction() {
        goOnline.value = false
        processing.value = false
        useMagstripe.value = false
        useChipReader.value = false
        isSuccessfulTransaction.value = null
        isDuplicateTransaction.value = null
    }
}
