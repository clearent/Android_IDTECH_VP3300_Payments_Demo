package com.clearent.paybridge


import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.fragment_manual_entry.*

class ManualEntryFragment : Fragment() {

    companion object {
        fun newInstance() = ManualEntryFragment()
    }

    private lateinit var mainActivity: MainActivity
    private lateinit var viewModel: TransactionViewModel
    private var hasFullCardNumber = false
    private var hasFullExpDate = false
    private var hasSecurityCode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_manual_entry, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainActivity = activity as MainActivity

        mainActivity.run {
            bleConnectionImageView.isVisible = false
            bleConnectionButton.isVisible = false
            cancelManualEntryButton.isVisible = true
            cancelManualEntryButton.setOnClickListener {
                resetToInitialView()
            }
        }

        viewModel = activity?.run {
            ViewModelProviders.of(this).get(TransactionViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        viewModel.clearentMobileRequest.observe(viewLifecycleOwner, Observer {
            val text = getString(R.string.manual_entry_for, it?.totalAmount)
            val stylizedText = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
            manualEntryForTextView.text = stylizedText
        })

        runManualTransactionButton.setOnClickListener {
            val card = cardNumberEditText.text.replace(Regex(" "), "")
            val expDate = expDateEditText.text.toString().replace(Regex("/"), "")
            mainActivity.runManualTransaction(card, expDate, securityCodeEditText.text.toString())
            mainActivity.cancelManualEntryButton.isVisible = false
        }

        cardNumberEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    var formattedCard = ""
                    var card = it.replace(Regex(" "), "")
                    val is465format = card.startsWith('3')
                    if (is465format) {
                        if (card.length > 15) {
                            card = card.substring(0, 15)
                        }
                        hasFullCardNumber = card.length == 15
                        if (card.length <= 10) {
                            formattedCard = card.replace(Regex("(\\d{4})(\\d+)"), "$1 $2").trim()
                        } else if (card.length <= 15) {
                            formattedCard = card.replace(Regex("(\\d{4})(\\d{6})(\\d+)"), "$1 $2 $3").trim()
                        }
                    } else {
                        if (card.length > 16) {
                            card = card.substring(0, 16)
                        }
                        hasFullCardNumber = card.length == 16
                        formattedCard = card.replace(Regex("(....)"), "$1 ").trim()
                    }
                    if (it.toString() != formattedCard) {
                        it.replace(0, it.length, formattedCard)
                    }
                }
                runManualTransactionButton.isEnabled = hasAllRequiredFields()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        expDateEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    var date = it.replace(Regex("/"), "")
                    if (date.length > 4) {
                        date = date.substring(0, 4)
                    }
                    hasFullExpDate = date.length == 4
                    val formattedDate = date.replace(Regex("(\\d{2})(\\d+)"), "$1/$2")
                    if (it.toString() != formattedDate) {
                        it.replace(0, it.length, formattedDate)
                    }
                }
                runManualTransactionButton.isEnabled = hasAllRequiredFields()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })

        securityCodeEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.let {securityCode ->
                    hasSecurityCode = securityCode.length >= 3
                }
                runManualTransactionButton.isEnabled = hasAllRequiredFields()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })
    }

    private fun hasAllRequiredFields(): Boolean = hasFullCardNumber && hasFullExpDate && hasSecurityCode

}
