package com.example.financeflow.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.financeflow.R
import com.example.financeflow.data.SharedPrefManager
import com.example.financeflow.databinding.ActivityDashboardBinding
import com.example.financeflow.model.TransactionType
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var barChart: BarChart
    private lateinit var budgetPercentage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        barChart = findViewById(R.id.barChart)
        budgetPercentage = findViewById(R.id.budgetPercentage)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> true
                R.id.navigation_transactions -> {
                    startActivity(Intent(this, Transaction::class.java))
                    true
                }
                R.id.navigation_budget -> {
                    startActivity(Intent(this, Budget::class.java))
                    true
                }
                R.id.navigation_settings -> {
                    startActivity(Intent(this, Settings::class.java))
                    true
                }
                else -> false
            }
        }

        updateDashboard()
    }

    override fun onResume() {
        super.onResume()
        updateDashboard()
    }


    //Update Dashboard
    private fun updateDashboard() {
        val sharedPrefManager = SharedPrefManager(this)
        val transactions = sharedPrefManager.getTransactions()

        var totalIncome = 0.0
        var totalExpenses = 0.0

        for (transaction in transactions) {
            when (transaction.type) {
                TransactionType.INCOME -> totalIncome += transaction.amount
                TransactionType.EXPENSE -> totalExpenses += transaction.amount
            }
        }

        val balance = totalIncome - totalExpenses

        binding.balanceCard.text = "Balance\nRs.%.2f".format(balance)
        binding.incomeCard.text = "Income\nRs.%.2f".format(totalIncome)
        binding.expensesCard.text = "Expenses\nRs.%.2f".format(totalExpenses)


        val budget = sharedPrefManager.getBudget()
        val remainingBudget = budget - totalExpenses

        binding.budgetCard.text = "Budget: Rs. %.2f\nRemaining: Rs. %.2f".format(budget, remainingBudget)

        val progressPercent = if (budget > 0f) {
            ((totalExpenses / budget) * 100).toInt().coerceAtMost(100)
        } else 0

        binding.budgetPercentage.text = "$progressPercent%"

        if (budget > 0 && totalExpenses > budget) {
            showOverBudgetAlert()
        }

        binding.budgetProgressBar.progress = progressPercent

        binding.budgetProgressBar.progressDrawable = ContextCompat.getDrawable(
            this,
            if (totalExpenses > budget) R.drawable.budget_progress_over else R.drawable.budget_progress_style
        )

        setupPieChart(totalIncome.toFloat(), totalExpenses.toFloat())
        setupBarChart(balance.toFloat(), totalIncome.toFloat(), totalExpenses.toFloat())
    }


    private fun setupPieChart(income: Float, expenses: Float) {
        val entries = ArrayList<PieEntry>()

        if (income == 0f && expenses == 0f) {
            binding.pieChart.clear()
            binding.pieChart.centerText = "No data to display"
            binding.pieChart.setNoDataText("No data available")
            binding.pieChart.invalidate()
            return
        }

        entries.add(PieEntry(income, "Income"))
        entries.add(PieEntry(expenses, "Expenses"))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            ContextCompat.getColor(this, R.color.blue),
            ContextCompat.getColor(this, R.color.red_500)
        )
        dataSet.valueTextSize = 14f
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f

        with(binding.pieChart) {
            description.isEnabled = false

            legend.isEnabled = true
            legend.textSize = 14f
            legend.formSize = 14f
            legend.formToTextSpace = 10f
            legend.xEntrySpace = 15f
            legend.yEntrySpace = 10f

            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = Legend.LegendOrientation.HORIZONTAL

            setUsePercentValues(true)
            setEntryLabelColor(ContextCompat.getColor(this@DashboardActivity, R.color.white))
            setHoleColor(ContextCompat.getColor(this@DashboardActivity, R.color.white))
            setTransparentCircleAlpha(110)

            data = PieData(dataSet)
            centerText = "Spending Overview"
            animateY(1000)
            invalidate()
        }
    }

    //Bar chart
    private fun setupBarChart(balance: Float, income: Float, expenses: Float) {
        val totalAmount = balance + income + expenses

        if (totalAmount == 0f) {
            barChart.clear()
            barChart.setNoDataText("No data available")
            return
        }

        val balancePercentage = (balance / totalAmount) * 100
        val incomePercentage = (income / totalAmount) * 100
        val expensesPercentage = (expenses / totalAmount) * 100

        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, balancePercentage))
        entries.add(BarEntry(1f, incomePercentage))
        entries.add(BarEntry(2f, expensesPercentage))

        val dataSet = BarDataSet(entries, "Financial Overview")
        dataSet.colors = listOf(
            ContextCompat.getColor(this, R.color.blue),
            ContextCompat.getColor(this, R.color.green_500),
            ContextCompat.getColor(this, R.color.red_500)
        )
        dataSet.valueTextSize = 14f
        dataSet.setDrawValues(true)

        // Format bar labels to show percentages
        dataSet.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getBarLabel(barEntry: BarEntry?): String {
                return String.format("%.1f%%", barEntry?.y ?: 0f)
            }
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.8f

        barChart.data = barData

        // X-axis configuration
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Balance", "Income", "Expenses"))
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)
        xAxis.textSize = 12f
        xAxis.labelCount = entries.size

        // Y-axis configuration (left)
        val leftAxis = barChart.axisLeft
        barChart.axisLeft.setDrawGridLines(false)
        barChart.axisLeft.setDrawAxisLine(false)
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisLeft.axisMaximum = maxOf(100f, balancePercentage + 10f, incomePercentage + 10f, expensesPercentage + 10f)
        leftAxis.setDrawLabels(false)
        barChart.axisLeft.textSize = 12f
        barChart.axisLeft.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}%"
            }
        }

        // Disable right Y-axis
        barChart.axisRight.isEnabled = false

        // Bar chart appearance
        barChart.setFitBars(true)
        barChart.setDrawValueAboveBar(true)
        barChart.setDrawGridBackground(false)
        barChart.setDrawBorders(false)
        barChart.setExtraTopOffset(16f)
        barChart.setPadding(20, 20, 20, 40)

        // Interactions
        barChart.setTouchEnabled(true)
        barChart.setPinchZoom(false)
        barChart.setScaleEnabled(false)
        //barChart.highlightPerTapEnabled = true

        // Description & animation
        barChart.description.isEnabled = false
        barChart.animateY(1000)
        barChart.invalidate()
    }


    private fun showOverBudgetAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Budget Exceeded!")
        builder.setMessage("You've spent more than your budget. Consider adjusting your spending.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setIcon(R.drawable.ic_warning)
        builder.show()
    }





}
