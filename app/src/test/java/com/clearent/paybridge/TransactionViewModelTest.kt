package com.clearent.paybridge

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.clearent.paybridge.domain.ClearentMobileRequest
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule

class TransactionViewModelTest {

    private lateinit var transactionViewModel: TransactionViewModel

    @Rule @JvmField
    var rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        transactionViewModel = TransactionViewModel()
        transactionViewModel.clearentMobileRequest.value = createClearentRequest()
        transactionViewModel.goOnline.value = true
        transactionViewModel.processing.value = true
        transactionViewModel.useMagstripe.value = true
        transactionViewModel.useChipReader.value = true
        transactionViewModel.deviceConnected.value = true
        transactionViewModel.isSuccessfulTransaction.value = true
        transactionViewModel.isDuplicateTransaction.value = true
    }

    private fun createClearentRequest() = ClearentMobileRequest(
        "public-key", "bearerToken", "baseUrl", "{}", "totalAmount", null, null
    )

    @Test
    fun getClearentMobileRequest() {
        val expectedRequest = createClearentRequest()
        assertEquals(expectedRequest, transactionViewModel.clearentMobileRequest.value)
    }

    @Test
    fun getGoOnline() {
        assertTrue(transactionViewModel.goOnline.value!!)
    }

    @Test
    fun getProcessing() {
        assertTrue(transactionViewModel.processing.value!!)
    }

    @Test
    fun getUseMagstripe() {
        assertTrue(transactionViewModel.useMagstripe.value!!)
    }

    @Test
    fun getUseChipReader() {
        assertTrue(transactionViewModel.useChipReader.value!!)
    }

    @Test
    fun getDeviceConnected() {
        assertTrue(transactionViewModel.deviceConnected.value!!)
    }

    @Test
    fun isSuccessfulTransaction() {
        assertTrue(transactionViewModel.isSuccessfulTransaction.value!!)
    }

    @Test
    fun isDuplicateTransaction() {1
        assertTrue(transactionViewModel.isDuplicateTransaction.value!!)
    }

    @Test
    fun resetTransaction() {
        transactionViewModel.resetTransaction()
        assertNull(transactionViewModel.clearentMobileRequest.value)
        assertFalse(transactionViewModel.goOnline.value!!)
        assertFalse(transactionViewModel.processing.value!!)
        assertFalse(transactionViewModel.useMagstripe.value!!)
        assertFalse(transactionViewModel.useChipReader.value!!)
        assertNull(transactionViewModel.isSuccessfulTransaction.value)
        assertNull(transactionViewModel.isDuplicateTransaction.value)
    }

    @Test
    fun reloadTransaction() {
        transactionViewModel.reloadTransaction()
        assertNotNull(transactionViewModel.clearentMobileRequest.value)
        assertFalse(transactionViewModel.goOnline.value!!)
        assertFalse(transactionViewModel.processing.value!!)
        assertFalse(transactionViewModel.useMagstripe.value!!)
        assertFalse(transactionViewModel.useChipReader.value!!)
        assertNull(transactionViewModel.isSuccessfulTransaction.value)
        assertNull(transactionViewModel.isDuplicateTransaction.value)
    }
}