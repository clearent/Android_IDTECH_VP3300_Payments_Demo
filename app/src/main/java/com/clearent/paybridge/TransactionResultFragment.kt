package com.clearent.paybridge


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.fragment_transaction_result.*

class TransactionResultFragment : Fragment() {

    companion object {
        fun newInstance() = TransactionResultFragment()
    }

    private lateinit var viewModel: TransactionViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_transaction_result, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this).get(TransactionViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        viewModel.isSuccessfulTransaction.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                showSuccess()
            } else {
                showFailure()
            }
        })

        viewModel.isDuplicateTransaction.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                showDuplicateResult()
            }
        })
    }

    private fun showSuccess() {
        resultImageView.setImageResource(R.drawable.success)
        resultImageView.contentDescription = getString(R.string.success_description)
        messageTextView.text = getString(R.string.perfect)
        resultTextView.text = getString(R.string.success_result, viewModel.clearentMobileRequest.value!!.totalAmount)
        goToManualButton.isVisible = false
        setUpFinishAndReturnButton()
    }

    private fun showFailure() {
        resultImageView.setImageResource(R.drawable.fail)
        resultImageView.contentDescription = getString(R.string.fail_description)
        messageTextView.text = getString(R.string.oops)
        resultTextView.text = getString(R.string.failure_result)
        goToManualButton.isVisible = true
        goToManualButton.setOnClickListener {
            activity!!.supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_main, ManualEntryFragment.newInstance())
                .commit()
            viewModel.reloadTransaction()
        }
        finishOrTryAgainButton.text = getString(R.string.try_again)
        finishOrTryAgainButton.setOnClickListener {
            reloadTransaction()
        }
    }

    private fun showDuplicateResult() {
        resultImageView.setImageResource(R.drawable.warning)
        resultImageView.contentDescription = "an exclamation point surrounded by a triangle"
        messageTextView.text = getString(R.string.duplicate)
        resultTextView.text = getString(R.string.transaction_previously_approved_for, viewModel.clearentMobileRequest.value!!.totalAmount)
        goToManualButton.isVisible = false
        setUpFinishAndReturnButton()
    }

    private fun setUpFinishAndReturnButton() {
        finishOrTryAgainButton.text = getString(R.string.finish_and_return)
        finishOrTryAgainButton.setOnClickListener {
            resetTransaction()
        }
    }

    private fun resetTransaction() {
        viewModel.resetTransaction()
        showInitialTransactionFragment()
    }

    private fun reloadTransaction() {
        viewModel.reloadTransaction()
        showInitialTransactionFragment()
    }

    private fun showInitialTransactionFragment() {
        val mainActivity = activity as MainActivity
        mainActivity.resetToInitialView()
    }

}
