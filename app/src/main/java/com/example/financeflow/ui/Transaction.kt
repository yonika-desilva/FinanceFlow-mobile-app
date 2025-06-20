package com.example.financeflow.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.financeflow.R
import com.example.financeflow.data.SharedPrefManager
import com.example.financeflow.model.Transaction
import com.example.financeflow.adapter.TransactionAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class Transaction : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var sharedPrefManager: SharedPrefManager
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)

        // Initialize bottomNavigation
        bottomNavigation = findViewById(R.id.bottom_navigation)

        // Set the selected item to "Transactions" since we are in this activity
        bottomNavigation.selectedItemId = R.id.navigation_transactions

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    overridePendingTransition(0, 0) // Optional: no animation
                    true
                }
                R.id.navigation_transactions -> {
                    // Already on this screen
                    true
                }
                R.id.navigation_budget -> {
                    startActivity(Intent(this, Budget::class.java)) // Ensure this is the correct class name
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_settings -> {
                    startActivity(Intent(this, Settings::class.java)) // Ensure this is the correct class name
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }

        sharedPrefManager = SharedPrefManager(this)

        recyclerView = findViewById(R.id.transactionRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        transactionAdapter = TransactionAdapter(
            mutableListOf(),
            onEditClick = { transaction ->
                val intent = Intent(this, EditTransaction::class.java)
                intent.putExtra("transaction", transaction)
                startActivity(intent)
            },
            onDeleteClick = { transaction ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Transaction")
                    .setMessage("Are you sure you want to delete this transaction?")
                    .setPositiveButton("Yes") { _, _ ->
                        val updatedList = sharedPrefManager.getTransactions()
                            .filter { it.id != transaction.id }
                        sharedPrefManager.saveTransactions(updatedList)
                        transactionAdapter.updateList(updatedList)
                        Toast.makeText(this, "Transaction deleted", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        recyclerView.adapter = transactionAdapter

        val addTransactionButton = findViewById<FloatingActionButton>(R.id.addTransactionButton)
        addTransactionButton.setOnClickListener {
            startActivity(Intent(this, AddTransaction::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    private fun loadTransactions() {
        val transactions: List<Transaction> = sharedPrefManager.getTransactions()
        transactionAdapter.updateList(transactions)
    }
}
