package com.example.financeflow.data


    import android.content.Context
    import com.example.financeflow.model.Transaction
    import com.example.financeflow.model.TransactionType
    import com.google.gson.Gson
    import com.google.gson.reflect.TypeToken

    class SharedPrefManager(context: Context) {

        private val prefs = context.getSharedPreferences("transactions_prefs", Context.MODE_PRIVATE)
        private val gson = Gson()

        fun saveTransactions(transactions: List<Transaction>) {
            val json = gson.toJson(transactions)
            prefs.edit().putString("transactions", json).apply()
        }

        fun getTransactions(): MutableList<Transaction> {
            val json = prefs.getString("transactions", null)
            val type = object : TypeToken<MutableList<Transaction>>() {}.type
            return gson.fromJson(json, type) ?: mutableListOf()
        }

        fun addTransaction(transaction: Transaction) {
            val transactions = getTransactions()
            transactions.add(transaction)
            saveTransactions(transactions)
        }

        fun updateTransaction(updatedTransaction: Transaction) {
            val transactions = getTransactions()
            val index = transactions.indexOfFirst { it.id == updatedTransaction.id }
            if (index != -1) {
                transactions[index] = updatedTransaction
                saveTransactions(transactions)
            }
        }


        private val BUDGET_KEY = "budget_amount"

        fun saveBudget(amount: Float) {
            val editor = prefs.edit()
            editor.putFloat(BUDGET_KEY, amount)
            editor.apply()
        }

        fun getBudget(): Float {
            return prefs.getFloat(BUDGET_KEY, 0f)
        }



    }

