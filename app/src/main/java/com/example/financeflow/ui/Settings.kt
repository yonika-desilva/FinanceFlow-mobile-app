package com.example.financeflow.ui

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.financeflow.R
import com.example.financeflow.data.SharedPrefManager
import com.example.financeflow.model.Transaction
import com.example.financeflow.model.TransactionType
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

class Settings : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var sharedPrefManager: SharedPrefManager
    private val gson = Gson()

    private val BACKUP_FILE_NAME = "financeflow_backup.json"
    private val NOTIFICATION_CHANNEL_ID = "financeflow_channel"
    private val PERMISSION_REQUEST_CODE = 123
    private val WRITE_STORAGE_REQUEST_CODE = 124

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        sharedPrefManager = SharedPrefManager(this)

        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.navigation_settings

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_transactions -> {
                    startActivity(Intent(this, Transaction::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_budget -> {
                    startActivity(Intent(this, Budget::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.navigation_settings -> true
                else -> false
            }
        }

        setupCurrencySpinner()
        setupNotificationSwitches()
        setupButtons()
        setupReportButton()
        requestNotificationPermission()
        createNotificationChannel()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupCurrencySpinner() {
        val spinner = findViewById<Spinner>(R.id.spinnerCurrency)
        val currencies = arrayOf("USD", "EUR", "LKR", "INR", "JPY")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        val savedCurrency = sharedPreferences.getString("currency", "USD")
        val currencyIndex = currencies.indexOf(savedCurrency)
        if (currencyIndex != -1) {
            spinner.setSelection(currencyIndex)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val selectedCurrency = currencies[position]
                sharedPreferences.edit().putString("currency", selectedCurrency).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    //Generate Report
    private fun setupReportButton() {
        val transactions = sharedPrefManager.getTransactions()

        findViewById<Button>(R.id.btnDownloadReport).setOnClickListener {
            if (hasWriteStoragePermission()) {
                generateAndSavePdfReport(transactions)
            } else {
                requestWriteStoragePermission()
            }
        }
    }

    private fun generateAndSavePdfReport(transactions: List<Transaction>) {
        val reportLines = buildReportLines(transactions)

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            textSize = 16f
            isAntiAlias = true
        }

        val leftMargin = 32f
        val rightMargin = 32f
        val maxWidth = pageInfo.pageWidth - leftMargin - rightMargin
        var yPosition = 50f
        val lineHeight = paint.descent() - paint.ascent() + 8

        // Helper to split long lines for justification
        fun splitLine(line: String): List<String> {
            val words = line.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = ""
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine = testLine
                } else {
                    lines.add(currentLine)
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) lines.add(currentLine)
            return lines
        }

        for (line in reportLines) {
            val justifiedLines = splitLine(line)
            for (justified in justifiedLines) {
                canvas.drawText(justified, leftMargin, yPosition, paint)
                yPosition += lineHeight
            }
        }

        pdfDocument.finishPage(page)

        // Save PDF to Documents folder
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }
        val fileName = "Finance_Report_${System.currentTimeMillis()}.pdf"
        val file = File(documentsDir, fileName)

        try {
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            Toast.makeText(this, "PDF saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Failed to save PDF report", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
    }



    private fun buildReportLines(transactions: List<Transaction>): List<String> {
        val lines = mutableListOf<String>()
        var totalIncome = 0.0
        var totalExpense = 0.0

        lines.add("Finance Flow Report")
        lines.add("")
        lines.add("Income:")
        transactions.filter { it.type == TransactionType.INCOME }.forEach {
            lines.add("Title: ${it.title}, Date: ${it.date}, Amount: +${it.amount}")
            totalIncome += it.amount
        }

        lines.add("")
        lines.add("Expenses:")
        transactions.filter { it.type == TransactionType.EXPENSE }.forEach {
            lines.add("Title: ${it.title}, Date: ${it.date}, Amount: -${it.amount}")
            totalExpense += it.amount
        }

        // Add extra lines for spacing before totals
        lines.add("") // Empty line
        lines.add("") // Empty line

        lines.add("Total Income: $totalIncome")
        lines.add("Total Expenses: $totalExpense")
        lines.add("Balance: ${totalIncome - totalExpense}")

        return lines
    }

    private fun hasWriteStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestWriteStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                WRITE_STORAGE_REQUEST_CODE
            )
        }
    }


    //Notifications
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "FinanceFlow Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for app notifications"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }




    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                ) {
                    AlertDialog.Builder(this)
                        .setTitle("Notification Permission Needed")
                        .setMessage("This app needs notification permission for alerts")
                        .setPositiveButton("OK") { _, _ ->
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                PERMISSION_REQUEST_CODE
                            )
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    private fun setupNotificationSwitches() {
        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)

        val budgetSwitch = findViewById<Switch>(R.id.switchBudgetAlerts)
        val reminderSwitch = findViewById<Switch>(R.id.switchReminders)

        budgetSwitch.isChecked = sharedPreferences.getBoolean("budgetAlerts", false)
        reminderSwitch.isChecked = sharedPreferences.getBoolean("reminders", false)

        budgetSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("budgetAlerts", isChecked).apply()
            scheduleBudgetAlertNotification(isChecked)
            Toast.makeText(this, "Budget alerts ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("reminders", isChecked).apply()
            scheduleReminderNotification(isChecked)
            Toast.makeText(this, "Reminders ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scheduleBudgetAlertNotification(enable: Boolean) {
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return

            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Budget Alert")
                .setContentText("You're nearing your budget limit")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .let { builder ->
                    NotificationManagerCompat.from(this).notify(100, builder.build())
                }
        } else {
            NotificationManagerCompat.from(this).cancel(100)
        }
    }

    private fun scheduleReminderNotification(enable: Boolean) {
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return

            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Monthly Reminder")
                .setContentText("Review your monthly expenses")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .let { builder ->
                    NotificationManagerCompat.from(this).notify(101, builder.build())
                }
        } else {
            NotificationManagerCompat.from(this).cancel(101)
        }
    }



//Backup and recovery
    private fun backupData() {
        val transactions = sharedPrefManager.getTransactions()
        val jsonString = gson.toJson(transactions)
        try {
            val file = File(filesDir, BACKUP_FILE_NAME)
            FileOutputStream(file).use {
                it.write(jsonString.toByteArray())
            }
            Toast.makeText(this, "Backup successful", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Backup failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreData() {
        try {
            val file = File(filesDir, BACKUP_FILE_NAME)
            if (!file.exists()) {
                Toast.makeText(this, "No backup found", Toast.LENGTH_SHORT).show()
                return
            }
            val jsonString = FileReader(file).use { it.readText() }
            val type: Type = object : TypeToken<List<Transaction>>() {}.type
            val transactions: List<Transaction> = gson.fromJson(jsonString, type)
            sharedPrefManager.saveTransactions(transactions)
            Toast.makeText(this, "Data restored", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Restore failed", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupButtons() {
        findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnBackup).setOnClickListener {
            backupData()
        }

        findViewById<Button>(R.id.btnRestore).setOnClickListener {
            restoreData()
        }


        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            clearSession()
            startActivity(Intent(this, SignIn::class.java))
            finish()
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }
            }
            WRITE_STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val transactions = sharedPrefManager.getTransactions()
                    generateAndSavePdfReport(transactions)
                } else {
                    Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun clearSession() {
        getSharedPreferences("user_session", MODE_PRIVATE).edit().clear().apply()
    }
}