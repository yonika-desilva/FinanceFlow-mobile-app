package com.example.financeflow.model


import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date


enum class TransactionType {
    INCOME,
    EXPENSE
}

@Parcelize
data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val date: String,
    val type: TransactionType,
    val category: String
): Parcelable
