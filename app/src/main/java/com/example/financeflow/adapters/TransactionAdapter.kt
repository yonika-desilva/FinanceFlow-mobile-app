package com.example.financeflow.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.financeflow.R
import com.example.financeflow.model.Transaction
import com.example.financeflow.model.TransactionType
class TransactionAdapter(
    private var transactionList: MutableList<Transaction>,
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        val txtAmount: TextView = itemView.findViewById(R.id.txtAmount)
        val txtType: TextView = itemView.findViewById(R.id.txtType)
        val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        val textViewCategory: TextView = itemView.findViewById(R.id.textViewCategory)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactionList[position]

        holder.txtTitle.text = transaction.title
        holder.txtAmount.text = "Rs. %.2f".format(transaction.amount)
        holder.txtType.text = transaction.type.name
        holder.textViewCategory.text = transaction.category
        holder.textViewDate.text = transaction.date




        // Set the amount color based on income/expense
        if (transaction.type == TransactionType.INCOME) {
            holder.txtAmount.setTextColor(holder.itemView.context.getColor(R.color.green_700))
        } else {
            holder.txtAmount.setTextColor(holder.itemView.context.getColor(R.color.red_500))
        }

        // Set click listeners for Edit and Delete buttons
        holder.btnEdit.setOnClickListener { onEditClick(transaction) }
        holder.btnDelete.setOnClickListener { onDeleteClick(transaction) }
    }

    override fun getItemCount(): Int = transactionList.size

    // Update the list and notify the adapter
    fun updateList(newList: List<Transaction>) {
        transactionList = newList.toMutableList()
        notifyDataSetChanged()
    }


}
