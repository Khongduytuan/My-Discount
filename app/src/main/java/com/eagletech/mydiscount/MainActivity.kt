package com.eagletech.mydiscount

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.eagletech.mydiscount.data.ManagerData
import com.eagletech.mydiscount.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var myData: ManagerData
    private var selectedEditText: EditText? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        myData = ManagerData.getInstance(this)
        setContentView(binding.root)

        // Thiết lập Toolbar
        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowTitleEnabled(false)


        binding.edtCost.setOnTouchListener { v, event ->
            v.performClick()
            if (event.action == MotionEvent.ACTION_UP) {
                v.clearFocus()
                v.requestFocus()
                return@setOnTouchListener true
            }
            false
        }

        binding.edtDiscount.setOnTouchListener { v, event ->
            v.performClick()
            if (event.action == MotionEvent.ACTION_UP) {
                v.clearFocus()
                v.requestFocus()
                return@setOnTouchListener true
            }
            false
        }

        binding.edtCost.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) selectedEditText = binding.edtCost
        }

        binding.edtDiscount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) selectedEditText = binding.edtDiscount
        }

        val numberButtons = listOf(
            binding.dot, binding.number0, binding.number1, binding.number2,
            binding.number3, binding.number4, binding.number5, binding.number6,
            binding.number7, binding.number8, binding.number9
        )

        numberButtons.forEach { button ->
            button.setOnClickListener {
                if (selectedEditText == null) {
                    Toast.makeText(this, "Please select an input field first", Toast.LENGTH_SHORT).show()
                } else {
                    appendTextToSelectedEditText(button.text.toString())
                }
            }
        }

        binding.delete.setOnClickListener { deleteLastCharacter() }
        binding.clear.setOnClickListener { clearAllData() }
        binding.equal.setOnClickListener {
            if (myData.isPremium == true){
                calculateResult()
            } else if(myData.getData() > 0){
                calculateResult()
                myData.removeData()
            } else{
                Toast.makeText(this, "Buy to use", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, PaymentScreenActivity::class.java)
                startActivity(intent)
            }

        }

        binding.edtCost.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateAndDisplayReducedPrice()
                validateInput(binding.edtCost)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.edtDiscount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateAndDisplayReducedPrice()
                validateDiscountInput()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun calculateResult() {
        if (binding.edtCost.text.isNullOrEmpty() || binding.edtDiscount.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
        } else {
            calculateAndDisplayLastPrice()
        }
    }

    private fun appendTextToSelectedEditText(text: String) {
        selectedEditText?.let {
            if (it.text.isEmpty() && text == ".") {
                Toast.makeText(this, "Cannot start with a dot", Toast.LENGTH_SHORT).show()
            } else {
                it.append(text)
            }
        }
    }

    private fun deleteLastCharacter() {
        selectedEditText?.let { editText ->
            val text = editText.text.toString()
            if (text.isNotEmpty()) {
                editText.setText(text.substring(0, text.length - 1))
                editText.setSelection(editText.text.length)
            }
        }
    }

    private fun clearAllData() {
        binding.edtCost.text.clear()
        binding.edtDiscount.text.clear()
        binding.tvResult.text = ""
        binding.tvResultReduced.text = ""
    }

    private fun calculateAndDisplayReducedPrice() {
        val cost = binding.edtCost.text.toString().toDoubleOrNull() ?: 0.0
        val discount = binding.edtDiscount.text.toString().toDoubleOrNull() ?: 0.0
        val reducedPrice = cost * (discount / 100)
        binding.tvResult.text = reducedPrice.toString()
    }

    private fun calculateAndDisplayLastPrice() {
        val cost = binding.edtCost.text.toString().toDoubleOrNull() ?: 0.0
        val reducedPrice = binding.tvResult.text.toString().toDoubleOrNull() ?: 0.0
        val lastPrice = cost - reducedPrice
        binding.tvResultReduced.text = lastPrice.toString()
    }

    private fun validateInput(editText: EditText) {
        val text = editText.text.toString()
        if (text.isNotEmpty() && text.startsWith(".")) {
            editText.setText(text.removePrefix("."))
            editText.setSelection(editText.text.length)
            Toast.makeText(this, "Cannot start with a dot", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateDiscountInput() {
        val discountText = binding.edtDiscount.text.toString()
        if (discountText.toDoubleOrNull() ?: 0.0 > 100) {
            binding.edtDiscount.setText("100")
            binding.edtDiscount.setSelection(binding.edtDiscount.text.length)
            Toast.makeText(this, "Discount cannot be more than 100%", Toast.LENGTH_SHORT).show()
        }
        validateInput(binding.edtDiscount)
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_buy -> {
                val intent = Intent(this, PaymentScreenActivity::class.java)
                startActivity(intent)
                true
            }

            R.id.menu_info -> {
                showInfoDialog()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun showInfoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_info, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tvMessage)
        val positiveButton = dialogView.findViewById<Button>(R.id.btnPositive)

        if (myData.isPremium == true) {
            messageTextView.text = "You have successfully registered"
        } else {
            messageTextView.text = "You have ${myData.getData()} use"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        positiveButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
