package com.clearent.paybridge

import org.junit.Test

import org.junit.Assert.*

class TransactionFragmentTest {

    @Test
    fun createAmountString() {
        val transactionFragment = TransactionFragment()
        val amountString = transactionFragment.createAmountString("12.99")
        assertEquals("$12.99", amountString)
    }

    @Test
    fun createAmountStringReturnsZeroDollarsWhenAmountNull() {
        val transactionFragment = TransactionFragment()
        val amountString = transactionFragment.createAmountString(null)
        assertEquals("$0.00", amountString)
    }

}