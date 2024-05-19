package com.dicoding.asclepius.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem

import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.asclepius.R
import com.dicoding.asclepius.adapter.HistoryAdapter
import com.dicoding.asclepius.database.History
import com.dicoding.asclepius.database.HistoryDatabase
import com.dicoding.asclepius.view.ResultActivity.Companion.REQUEST_HISTORY_UPDATE
import com.google.android.material.bottomnavigation.BottomNavigationView

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HistoryAvticity : AppCompatActivity(),HistoryAdapter.OnDeleteClickListener {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private var historyList: MutableList<History> = mutableListOf()
    private lateinit var tvNotFound: TextView

    companion object{
        const val TAG = "historydata"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        historyRecyclerView = findViewById(R.id.rvHistory)
        tvNotFound = findViewById(R.id.tv_historyNotFound)

        historyAdapter = HistoryAdapter(historyList)
        historyAdapter.setOnDeleteClickListener(this)
        historyRecyclerView.adapter = historyAdapter
        historyRecyclerView.layoutManager = LinearLayoutManager(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        GlobalScope.launch(Dispatchers.Main) {
            loadHistoryFromDatabase()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_HISTORY_UPDATE && resultCode == RESULT_OK) {
            GlobalScope.launch(Dispatchers.Main) {
                loadHistoryFromDatabase()
            }
        }
    }

    private fun loadHistoryFromDatabase() {
        GlobalScope.launch(Dispatchers.Main) {
            val history = HistoryDatabase.getDatabase(this@HistoryAvticity).historyDao().getAllHistory()
            Log.d(TAG, "Number of predictions: ${history.size}")
            historyList.clear()
            historyList.addAll(history)
            historyAdapter.notifyDataSetChanged()
            showOrHideNoHistoryText()
        }
    }

    private fun showOrHideNoHistoryText() {
        if (historyList.isEmpty()) {
            tvNotFound.visibility = View.VISIBLE
            historyRecyclerView.visibility = View.GONE
        } else {
            tvNotFound.visibility = View.GONE
            historyRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDeleteClick(position: Int) {
        val history =historyList[position]
        if (history.result.isNotEmpty()) {
            GlobalScope.launch(Dispatchers.IO) {
               HistoryDatabase.getDatabase(this@HistoryAvticity).historyDao().deleteHistory(history)
            }
            historyList.removeAt(position)
            historyAdapter.notifyDataSetChanged()
            showOrHideNoHistoryText()
        }
    }
}
