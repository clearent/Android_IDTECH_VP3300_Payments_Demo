package com.clearent.paybridge

import android.net.Uri
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.android.synthetic.main.content_main.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import java.net.URI

class MainActivityTest {

    private lateinit var mainActivity: MainActivity

    @Mock
    private lateinit var uri: Uri

//    @Mock
//    private lateinit var intent: Intent

    @Rule
    @JvmField
    var rule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        mainActivity = MainActivity()
        mainActivity.viewModel = TransactionViewModel()
    }

    @Test
    fun createClearentMobileRequestFromIntentParameters() {
        val set = setOf("total-amount", "base-url")
        Mockito.`when`(uri.queryParameterNames).thenReturn(set)
        Mockito.`when`(uri.getQueryParameter("total-amount")).thenReturn("2.99")
        Mockito.`when`(uri.getQueryParameter("base-url")).thenReturn("baseUrl")
        val clearentMobileRequest =
            mainActivity.createClearentMobileRequestFromIntentParameters(uri)
        assertEquals("2.99", clearentMobileRequest?.totalAmount)
        assertEquals("baseUrl", clearentMobileRequest?.baseUrl)
        verify(uri, times(1)).queryParameterNames
        verify(uri, times(2)).getQueryParameter(anyString())
    }

    @Test
    fun createInvoiceNumberText() {
        val invoiceNumberText = mainActivity.createInvoiceNumberText("965483")
        assertEquals("Invoice #965483", invoiceNumberText)
    }

    @Test
    fun createInvoiceNumberTextNull() {
        val invoiceNumberText = mainActivity.createInvoiceNumberText(null)
        assertNull(invoiceNumberText)
    }

}