package com.pocketai.app.presentation.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketai.app.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val totalSpendingMonth by viewModel.getTotalSpendingForMonth().collectAsState(initial = 0.0)
    val totalSpendingLast7Days by viewModel.getTotalSpendingLast7Days().collectAsState(initial = 0.0)
    val totalExpenseCount by viewModel.getTotalExpenseCount().collectAsState(initial = 0)
    val categoryTotals by viewModel.getCategoryTotals().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Spending (This Month)", fontWeight = FontWeight.Bold)
                        Text("$${totalSpendingMonth ?: 0.0}")
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Spending (Last 7 Days)", fontWeight = FontWeight.Bold)
                        Text("$${totalSpendingLast7Days ?: 0.0}")
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Number of Expenses", fontWeight = FontWeight.Bold)
                        Text("$totalExpenseCount")
                    }
                }
            }

            item {
                Text("Spending by Category", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            }

            items(categoryTotals) {
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Text(it.category, modifier = Modifier.weight(1f))
                        Text("$${it.total}")
                    }
                }
            }
        }
    }
}