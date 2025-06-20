package com.example.financeflow.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.financeflow.R
import com.example.financeflow.data.SharedPrefManager
import com.example.financeflow.model.Transaction
import com.example.financeflow.model.TransactionType
import java.util.*

class EditTransaction : AppCompatActivity() {

    private lateinit var sharedPrefManager: SharedPrefManager
    private var existingTransaction: Transaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_transaction)

        sharedPrefManager = SharedPrefManager(this)

        val editTextTitle = findViewById<EditText>(R.id.editTextTitle)
        val editTextAmount = findViewById<EditText>(R.id.editTextAmount)
        val radioGroupType = findViewById<RadioGroup>(R.id.radioGroupType)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val editTextDate = findViewById<EditText>(R.id.editTextDate)
        val btnSave = findViewById<Button>(R.id.btnSaveTransaction)

        // Disable manual input for date
        editTextDate.inputType = InputType.TYPE_NULL
        editTextDate.keyListener = null

        // Setup categories spinner
        val categories = listOf("Food", "Transport", "Entertainment", "Salary", "Investment", "Shopping", "Health", "Education", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // Date picker dialog
        editTextDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                editTextDate.setText(formattedDate)
            }, year, month, day).show()
        }

        // Get transaction from intent
        existingTransaction = intent.getParcelableExtra("transaction")
        if (existingTransaction == null) {
            Toast.makeText(this, "No transaction data found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Populate fields with existing transaction data
        editTextTitle.setText(existingTransaction!!.title)
        editTextAmount.setText(existingTransaction!!.amount.toString())
        editTextDate.setText(existingTransaction!!.date)

        if (existingTransaction!!.type == TransactionType.INCOME) {
            radioGroupType.check(R.id.radioIncome)
        } else {
            radioGroupType.check(R.id.radioExpense)
        }

        val position = categories.indexOf(existingTransaction!!.category)
        if (position >= 0) spinnerCategory.setSelection(position)

        btnSave.setOnClickListener {
            val title = editTextTitle.text.toString().trim()
            val amountText = editTextAmount.text.toString().trim()
            val selectedTypeId = radioGroupType.checkedRadioButtonId
            val selectedCategory = spinnerCategory.selectedItem.toString()
            val date = editTextDate.text.toString().trim()

            // Validation (reuse your validation logic)
            if (title.isEmpty() || amountText.isEmpty() || selectedTypeId == -1 || date.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount < 0) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val titlePattern = Regex("^[A-Za-z]+\\d*\$")

            if (!titlePattern.matches(title)) {
                Toast.makeText(this, "Title must start with letters and may end with numbers (e.g., Income1)", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val type = when (selectedTypeId) {
                R.id.radioIncome -> TransactionType.INCOME
                R.id.radioExpense -> TransactionType.EXPENSE
                else -> TransactionType.EXPENSE
            }

            val updatedTransaction = Transaction(
                id = existingTransaction!!.id,
                title = title,
                amount = amount,
                date = date,
                type = type,
                category = selectedCategory
            )

            sharedPrefManager.updateTransaction(updatedTransaction)
            Toast.makeText(this, "Transaction updated", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
