package com.clearent.paybridge


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.fragment_card_interaction.*

class CardInteractionFragment : Fragment() {

    companion object {
        fun newInstance() = CardInteractionFragment()
    }

    private lateinit var mainActivity: MainActivity

    private lateinit var viewModel: TransactionViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_card_interaction, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainActivity = activity as MainActivity
        viewModel = mainActivity.run {
            ViewModelProviders.of(this).get(TransactionViewModel::class.java)
        }

        viewModel.clearentMobileRequest.observe(viewLifecycleOwner, Observer {
            showInsertOrSwipe(it?.totalAmount ?: "0.00")
        })

        viewModel.goOnline.observe(viewLifecycleOwner, Observer {
            if (it) {
                showRemoveCard()
            }
        })

        viewModel.processing.observe(viewLifecycleOwner, Observer {
            if (it) {
                showProcessing()
            }
        })

        viewModel.useMagstripe.observe(viewLifecycleOwner, Observer {
            if (it) {
                showPhoneCardReaderImage()
                cardInteractionTextView.text = getString(R.string.swipe_card)
            }
        })

        viewModel.useChipReader.observe(viewLifecycleOwner, Observer {
            if (it) {
                showPhoneCardReaderImage()
                cardInteractionTextView.text = getString(R.string.insert_chip)
                runTransactionButton.isVisible = true
            }
        })

        runTransactionButton.setOnClickListener {
            mainActivity.runTransaction()
        }

        cancelTransactionButton.setOnClickListener {
            mainActivity.cancelTransaction()
        }
    }

    private fun showInsertOrSwipe(totalAmount: String) {
        showPhoneCardReaderImage()
        val text = getString(R.string.insert_or_swipe, totalAmount)
        val styledText = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT)
        cardInteractionTextView.text = styledText
        cancelTransactionButton.isVisible = true
        runTransactionButton.isVisible = false
    }

    private fun showRemoveCard() {
        showPhoneCardReaderImage()
        cardInteractionTextView.text = getString(R.string.remove_card)
        cancelTransactionButton.isVisible = false
        runTransactionButton.isVisible = false
    }

    private fun showPhoneCardReaderImage() {
        cardInteractionImageView.setImageDrawable(activity?.getDrawable(R.drawable.phone_card_reader))
        cardInteractionImageView.clearAnimation()
    }

    private fun showProcessing() {
        cardInteractionImageView.setImageDrawable(activity?.getDrawable(R.drawable.clearent_logo))
        val rotate = AnimationUtils.loadAnimation(context, R.anim.rotate_animation)
        cardInteractionImageView.startAnimation(rotate)
        cardInteractionTextView.text = getString(R.string.processing)
        cancelTransactionButton.isVisible = false
        runTransactionButton.isVisible = false
    }

}
