package com.pocketai.app.presentation.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pocketai.app.data.dao.CategoryTotal
import com.pocketai.app.viewmodel.ExpenseViewModel
import com.pocketai.app.presentation.home.getCategoryColor
import com.pocketai.app.presentation.home.getCategoryIcon
import com.pocketai.app.presentation.components.GlassCard
import com.pocketai.app.data.model.Expense
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.border

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    navController: NavController,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val totalSpending by viewModel.getTotalSpendingForMonth().collectAsState(initial = 0.0)
    val categoryTotals by viewModel.getCategoryTotalsForMonth().collectAsState(initial = emptyList())
    val topCategory by viewModel.getTopCategoryForMonth().collectAsState(initial = null)
    val expenses by viewModel.allExpenses.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) } // 0 = Month, 1 = Year
    
    // Animation state
    val visibleState = remember { 
        androidx.compose.animation.core.MutableTransitionState(false).apply { 
            targetState = true 
        } 
    }
    
    // Calculate weekly spending for bar chart (filtered to current month)
    val weeklySpending = remember(expenses) {
        val calendar = java.util.Calendar.getInstance()
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

        // Filter to current month
        val monthExpenses = expenses.filter {
            try {
                val parts = it.date.split("-")
                parts.size == 3 && parts[0].toInt() == currentYear && parts[1].toInt() == currentMonth + 1
            } catch (e: Exception) { false }
        }

        // Group by week of month (1-based)
        val weeks = mutableMapOf<Int, Double>()
        monthExpenses.forEach { expense ->
            try {
                val day = expense.date.split("-")[2].toInt()
                val weekOfMonth = (day - 1) / 7  // 0-based week
                weeks[weekOfMonth] = (weeks[weekOfMonth] ?: 0.0) + expense.amount
            } catch (e: Exception) { }
        }
        (0..3).map { weeks[it] ?: 0.0 }
    }
    
    // Get top merchants (filtered to current month)
    val topMerchants = remember(expenses) {
        val calendar = java.util.Calendar.getInstance()
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        val currentYear = calendar.get(java.util.Calendar.YEAR)

        val monthExpenses = expenses.filter {
            try {
                val parts = it.date.split("-")
                parts.size == 3 && parts[0].toInt() == currentYear && parts[1].toInt() == currentMonth + 1
            } catch (e: Exception) { false }
        }

        monthExpenses
            .groupBy { it.storeName }
            .map { (store, list) -> 
                MerchantData(
                    name = store,
                    amount = list.sumOf { it.amount },
                    count = list.size,
                    category = list.firstOrNull()?.category ?: "Other"
                )
            }
            .sortedByDescending { it.amount }
            .take(5)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp) // Adjusted for custom bottom bar
        ) {
            // Header
            item {
                androidx.compose.animation.AnimatedVisibility(
                    visibleState = visibleState,
                    enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) + 
                            androidx.compose.animation.slideInVertically(initialOffsetY = { -40 })
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        
                        Text(
                            text = "Analytics",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        IconButton(onClick = { /* More options */ }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
            
            // Period Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    GlassCard(modifier = Modifier.padding(4.dp)) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            listOf("Month", "Year").forEachIndexed { index, label ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (selectedTab == index) MaterialTheme.colorScheme.primary
                                            else Color.Transparent
                                        )
                                        .clickable { selectedTab = index }
                                        .padding(horizontal = 24.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selectedTab == index) Color.White
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Total Spent
            item {
                androidx.compose.animation.AnimatedVisibility(
                    visibleState = visibleState,
                    enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(500, delayMillis = 100)) + 
                            androidx.compose.animation.scaleIn(initialScale = 0.9f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Spent",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "€${String.format("%,.2f", totalSpending ?: 0.0)}",
                            fontSize = 40.sp, // Larger hero text
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
            
            // Donut Chart with Category Breakdown
            item {
                androidx.compose.animation.AnimatedVisibility(
                    visibleState = visibleState,
                    enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(600, delayMillis = 200)) + 
                            androidx.compose.animation.expandVertically()
                ) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        CategoryDonutChart(
                            categoryTotals = categoryTotals,
                            topCategory = topCategory,
                            totalSpending = totalSpending ?: 0.0
                        )
                    }
                }
            }
            
            // Category Legend
            item {
                CategoryLegend(categoryTotals, totalSpending ?: 0.0)
            }
            
            // Spending Intensity Heatmap
            item {
                androidx.compose.animation.AnimatedVisibility(
                    visibleState = visibleState,
                    enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(600, delayMillis = 300))
                ) {
                    SpendingHeatmap(expenses)
                }
            }
            
            // Weekly Trends Card
            item {
                WeeklyTrendsCard(weeklySpending)
            }
            
            // Top Merchants Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Top Merchants",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            
            // Top Merchants List
            items(topMerchants) { merchant ->
                MerchantItem(merchant)
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    categoryTotals: List<CategoryTotal>,
    topCategory: String?,
    totalSpending: Double
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Donut Chart
        Canvas(modifier = Modifier.size(180.dp)) {
            val strokeWidth = 16.dp.toPx() // Made thinner for a premium look
            var startAngle = -90f
            
            if (categoryTotals.isNotEmpty() && totalSpending > 0) {
                categoryTotals.forEach { category ->
                    val sweepAngle = (category.total / totalSpending * 360).toFloat()
                    drawArc(
                        color = getCategoryColor(category.category),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle - 3f, // Slightly larger gap
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            } else {
                // Empty state - gray ring
                drawArc(
                    color = Color.Gray.copy(alpha = 0.3f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    style = Stroke(width = strokeWidth)
                )
            }
        }
        
        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TOP CATEGORY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            
            if (topCategory != null) {
                val topTotal = categoryTotals.find { it.category == topCategory }?.total ?: 0.0
                val percentage = if (totalSpending > 0) (topTotal / totalSpending * 100).toInt() else 0
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        getCategoryIcon(topCategory),
                        contentDescription = null,
                        tint = getCategoryColor(topCategory),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$percentage%",
                        fontSize = 32.sp, // Bigger number
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Text(
                    text = topCategory,
                    fontSize = 12.sp,
                    color = getCategoryColor(topCategory)
                )
            } else {
                Text(
                    text = "No data",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategoryLegend(categoryTotals: List<CategoryTotal>, totalSpending: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        categoryTotals.take(4).forEach { category ->
            val percentage = if (totalSpending > 0) (category.total / totalSpending * 100).toInt() else 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(getCategoryColor(category.category))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = category.category.take(8),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$percentage%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun WeeklyTrendsCard(weeklySpending: List<Double>) {
    val maxSpending = weeklySpending.maxOrNull() ?: 1.0
    val avgSpending = if (weeklySpending.isNotEmpty()) weeklySpending.average() else 0.0
    
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weekly Trends",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Avg. €${String.format("%.0f", avgSpending)} / week",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Trend indicator (computed from real data)
                val trend = remember(weeklySpending) {
                    if (weeklySpending.size >= 2) {
                        val latest = weeklySpending.lastOrNull { it > 0 } ?: 0.0
                        val previous = weeklySpending.dropLast(1).lastOrNull { it > 0 } ?: 0.0
                        if (previous > 0) ((latest - previous) / previous * 100) else 0.0
                    } else 0.0
                }
                val trendDown = trend <= 0
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (trendDown) Color(0xFF14B8A6).copy(alpha = 0.15f)
                            else Color(0xFFEF4444).copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (trendDown) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = if (trendDown) Color(0xFF14B8A6) else Color(0xFFEF4444),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${if (trend > 0) "+" else ""}${String.format("%.0f", trend)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (trendDown) Color(0xFF14B8A6) else Color(0xFFEF4444)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Bar Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklySpending.forEachIndexed { index, spending ->
                    val height = if (maxSpending > 0) (spending / maxSpending * 60).toFloat() else 0f
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(height.dp.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (index == 2) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant // Solid dark, not gray alpha
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "W${index + 1}",
                            fontSize = 12.sp,
                            fontWeight = if (index == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (index == 2) MaterialTheme.colorScheme.onBackground
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MerchantItem(merchant: MerchantData) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Initial Circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(getCategoryColor(merchant.category).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = merchant.name.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = getCategoryColor(merchant.category)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = merchant.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${merchant.count} transaction${if (merchant.count > 1) "s" else ""}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-€${String.format("%.2f", merchant.amount)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = merchant.category,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}



data class MerchantData(
    val name: String,
    val amount: Double,
    val count: Int,
    val category: String
)

@Composable
fun SpendingHeatmap(expenses: List<Expense>) {
    // Current Month Logic
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)
    val monthName = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(calendar.time)
    
    // Filter expenses for this month
    val monthExpenses = remember(expenses) {
        expenses.filter {
            try {
                // Parse "yyyy-MM-dd"
                val parts = it.date.split("-")
                parts.size == 3 && parts[0].toInt() == currentYear && parts[1].toInt() == currentMonth + 1
            } catch (e: Exception) {
                false
            }
        }
    }
    
    // Day -> Total map
    val dailyTotals = remember(monthExpenses) {
        monthExpenses.groupBy {
            try {
                it.date.split("-")[2].toInt()
            } catch (e: Exception) {
                0
            }
        }.mapValues { (_, list) -> list.sumOf { it.amount } }
    }
    
    // Max daily for intensity
    val maxDaily = dailyTotals.values.maxOrNull() ?: 1.0
    
    // Calendar Layout
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Mon=2 -> offset=0. Sun=1 -> offset=6.
    // Calendar.DAY_OF_WEEK: Sun=1, Mon=2, Tue=3, Wed=4, Thu=5, Fri=6, Sat=7
    var startDayOffset = calendar.get(Calendar.DAY_OF_WEEK) - 2
    if (startDayOffset < 0) startDayOffset += 7 // Sun becomes 6
    
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spending Intensity",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = monthName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Header
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                        Text(
                            text = day,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                // Days
                var currentDay = 1
                // Weeks needed
                val totalCells = startDayOffset + daysInMonth
                val rows = (totalCells + 6) / 7
                
                for (row in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            if (cellIndex >= startDayOffset && currentDay <= daysInMonth) {
                                val total = dailyTotals[currentDay] ?: 0.0
                                val intensity = if (total == 0.0) 0 else if (total < maxDaily * 0.3) 1 else if (total < maxDaily * 0.7) 2 else 3
                                
                                val color = when(intensity) {
                                    0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) // None
                                    1 -> MaterialTheme.colorScheme.surfaceVariant // Low (Grayish)
                                    2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) // Mid
                                    3 -> MaterialTheme.colorScheme.primary // High
                                    else -> Color.Transparent
                                }
                                
                                val isToday = currentDay == Calendar.getInstance().get(Calendar.DAY_OF_MONTH) && currentMonth == Calendar.getInstance().get(Calendar.MONTH)
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .then(
                                                if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier
                                            )
                                    )
                                }
                                currentDay++
                            } else {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegentItem(color = MaterialTheme.colorScheme.surfaceVariant, label = "None")
                Spacer(modifier = Modifier.width(16.dp))
                LegentItem(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), label = "Moderate")
                Spacer(modifier = Modifier.width(16.dp))
                LegentItem(color = MaterialTheme.colorScheme.primary, label = "High")
            }
        }
    }
}

@Composable
fun LegentItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
