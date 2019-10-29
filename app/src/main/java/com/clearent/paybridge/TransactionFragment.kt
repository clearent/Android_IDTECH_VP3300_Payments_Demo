package com.clearent.paybridge

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.clearent.paybridge.domain.ClearentMobileRequest
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.fragment_transaction.*


class TransactionFragment : Fragment() {

    companion object {
        fun newInstance() = TransactionFragment()
    }

    private lateinit var mainActivity: MainActivity
    private lateinit var viewModel: TransactionViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transaction, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainActivity = activity as MainActivity
        viewModel = activity?.run {
            ViewModelProviders.of(this).get(TransactionViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
        viewModel.clearentMobileRequest.observe(viewLifecycleOwner, Observer {
            it?.let {
                updateAmountValueTexts(it)
                goToManualButton.isEnabled = true
                if (viewModel.deviceConnected.value == true) {
                    runTransactionButton.isEnabled = true
                }
            }
        })
        viewModel.deviceConnected.observe(this, Observer {
            runTransactionButton.isEnabled = it && viewModel.clearentMobileRequest.value != null
        })

        runTransactionButton.setOnClickListener {
            mainActivity.runTransaction()
        }

        goToManualButton.setOnClickListener {
            goToManualTransaction()
        }
    }

    private fun goToManualTransaction() {
        mainActivity.run {
            val manual = supportFragmentManager.findFragmentByTag("manual")
            if (manual == null) {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_main, ManualEntryFragment.newInstance(), "manual")
                    .commit()
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_main, manual)
                    .commit()
            }
        }
    }

    private fun updateAmountValueTexts(clearentMobileRequest: ClearentMobileRequest) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val requestMap = gson.fromJson<Map<String, Any>>(clearentMobileRequest.request, Map::class.java)
        Log.d("REQUEST MAP", requestMap.toString())
        amountValue.text = createAmountString(requestMap.get("amount") as String)
        totalValue.text = createAmountString(clearentMobileRequest.totalAmount)
        salesTaxValue.text = createAmountString(requestMap["sales-tax-amount"] as? String)
        tipAmountValue.text = createAmountString(requestMap["tip-amount"] as? String)
    }

    fun createAmountString(amount: String?): String {
        return "$${amount ?: "0.00"}"
    }

}
