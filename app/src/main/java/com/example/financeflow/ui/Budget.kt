package com.example.financeflow.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.financeflow.R
import com.example.financeflow.data.SharedPrefManager
import com.example.financeflow.model.Transaction
import com.example.financeflow.model.TransactionType
import com.example.financeflow.databinding.ActivityBudgetBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate

class Budget : AppCompatActivity() {
    private lateinit var binding: ActivityBudgetBinding
    private lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefManager = SharedPrefManager(this)

        // Bottom Navigation Listener
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    startActivity(Intent(this, com.example.financeflow.ui.DashboardActivity::class.java))
                    true
                }
                R.id.navigation_transactions -> {
                    startActivity(Intent(this, com.example.financeflow.ui.Transaction::class.java))
                    true
                }
                R.id.navigation_budget ->
                    true

                R.id.navigation_settings -> {
                    startActivity(Intent(this, com.example.financeflow.ui.Settings::class.java))
                    true
                }
                else -> false
            }
        }

        val etBudgetAmount = binding.budgetAmount
        val btnSave = binding.btnSaveBudget
        val barChart = binding.barChart

        // Display the category-wise bar chart
        displayCategoryBarChart(barChart)

        // Display the expense summary
        displayExpenseSummary()

        // Save budget button click listener
        btnSave.setOnClickListener {
            val budgetInput = etBudgetAmount.text.toString()

            if (budgetInput.isNotEmpty()) {
                val budgetAmount = budgetInput.toFloat()
                sharedPrefManager.saveBudget(budgetAmount)
                Toast.makeText(this, "Budget Saved!", Toast.LENGTH_SHORT).show()
                finish() // Finish the activity to return to the previous screen
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Bar chart
    private fun displayCategoryBarChart(barChart: BarChart) {
        val transactions = sharedPrefManager.getTransactions()
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }

        // Group by category and sum amounts
        val grouped = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        if (grouped.isEmpty()) {
            barChart.clear()
            barChart.setNoDataText("No expense data to display.")
            return
        }

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        grouped.entries.forEachIndexed { index, entry ->
            entries.add(BarEntry(index.toFloat(), entry.value.toFloat()))
            labels.add(entry.key)
        }

        val dataSet = BarDataSet(entries, "Expenses by Category").apply {
            setColors(*ColorTemplate.MATERIAL_COLORS)
            valueTextSize = 14f
        }

        val data = BarData(dataSet)

        // Check if data already exists
        if (barChart.data != null) {
            // Update the dataset if the chart already has data
            barChart.data.notifyDataChanged()
            barChart.notifyDataSetChanged()
        } else {
            // If the chart is empty, set the data
            barChart.data = data
        }

        // X Axis Setup
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f

        barChart.description.isEnabled = false
        barChart.axisRight.isEnabled = false

        barChart.animateY(1000)
        barChart.invalidate() // Refresh the chart to reflect the new data
    }



    private fun displayExpenseSummary() {
        val container = binding.categorySummaryContainer
        val transactionList = sharedPrefManager.getTransactions()

        // Filter out expenses only
        val expenses = transactionList.filter { it.type == TransactionType.EXPENSE }

        // Group expenses by category
        val expenseCategoryMap = expenses.groupBy { it.category }

        // Clear the previous views in the container
        container.removeAllViews()

        // If no expenses are found, show a default message
        if (expenseCategoryMap.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "No expense records found."
                textSize = 16f
                setTextColor(resources.getColor(android.R.color.darker_gray))
            }
            container.addView(emptyView)
        } else {
            // Display each expense category and its individual expenses
            for ((category, categoryExpenses) in expenseCategoryMap) {

                // Display each individual expense within the category
                categoryExpenses.forEach { expense ->
                    val expenseView = TextView(this).apply {
                        text = "${expense.title} - Rs. %.2f".format(expense.amount)
                        textSize = 16f
                        setTextColor(resources.getColor(android.R.color.black))
                    }
                    container.addView(expenseView)
                }

            }
        }
    }
}
