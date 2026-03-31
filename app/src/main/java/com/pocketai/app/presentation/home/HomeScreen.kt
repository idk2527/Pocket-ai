@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
package com.pocketai.app.presentation.home
    
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pocketai.app.data.model.Expense
import com.pocketai.app.presentation.components.GlassCard
import com.pocketai.app.ui.theme.*
import com.pocketai.app.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val userName by viewModel.userName.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()

    val totalSpending by viewModel.getTotalSpendingForMonth().collectAsState(initial = 0.0)
    val lastMonthSpending by viewModel.getTotalSpendingLastMonth().collectAsState(initial = 0.0)
    val recentExpenses by viewModel.allExpenses.collectAsState()
    val expenseCount by viewModel.getTotalExpenseCount().collectAsState(initial = 0)
    val topCategory by viewModel.getTopCategoryForMonth().collectAsState(initial = null)
    
    val percentChange = remember(totalSpending, lastMonthSpending) {
        if (lastMonthSpending != null && lastMonthSpending!! > 0) {
            ((totalSpending ?: 0.0) - lastMonthSpending!!) / lastMonthSpending!! * 100
        } else 0.0
    }

    val visibleState = remember { 
        MutableTransitionState(false).apply { targetState = true } 
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Removed the distracting radial gradient glow. The background is now solid near-black for a premium OLED feel.

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp) // Space for floating nav bar
        ) {
            item {
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = fadeIn(animationSpec = tween(600)) + slideInVertically(initialOffsetY = { -40 })
                ) {
                    HomeHeader(navController, userName)
                }
            }
            
            item {
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 100)) + slideInVertically(initialOffsetY = { 40 })
                ) {
                    MonthlySpendCard(
                        totalSpending = totalSpending ?: 0.0,
                        percentChange = percentChange,
                        budget = monthlyBudget
                    )
                }
            }
            
            item {
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) + expandVertically()
                ) {
                    SpendingChart(recentExpenses.take(30))
                }
            }
            
            item {
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 300))
                ) {
                    HighlightsSection(
                        topCategory = topCategory,
                        topCategoryTotal = topCategory?.let { cat ->
                            recentExpenses.filter { it.category == cat }.sumOf { it.amount }
                        } ?: 0.0,
                        receiptCount = expenseCount ?: 0
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { navController.navigate("receipts") }
                    )
                }
            }
            
            items(
                items = recentExpenses.take(8),
                key = { it.id ?: it.hashCode() }
            ) { expense ->
                Box(modifier = Modifier.animateItemPlacement(animationSpec = tween(400))) {
                    TransactionItem(expense) {
                        navController.navigate("expense_detail/${expense.id}")
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(navController: NavController, userName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(12.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.08f))
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clickable { navController.navigate("settings") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(8.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.05f))
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { navController.navigate("settings") },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.NotificationsNone,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun MonthlySpendCard(totalSpending: Double, percentChange: Double, budget: Float) {
    val progress = if (budget > 0) (totalSpending / budget).coerceIn(0.0, 1.0).toFloat() else 0f
    val remaining = (budget - totalSpending).coerceAtLeast(0.0)
    
    val isOverBudget = progress >= 0.9f
    val progressColor = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Spent This Month",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "€${String.format("%,.2f", totalSpending)}",
                style = MaterialTheme.typography.displayLarge, // Much bigger for hero effect
                fontWeight = FontWeight.Black
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Premium thin progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(progressColor)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Budget",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "€${String.format("%,.0f", budget)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (percentChange <= 0) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (percentChange <= 0) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = if (percentChange <= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${if (percentChange > 0) "+" else ""}${String.format("%.1f", percentChange)}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (percentChange <= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "€${String.format("%,.0f", remaining)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun SpendingChart(expenses: List<Expense>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        if (expenses.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val maxAmount = expenses.maxOfOrNull { it.amount } ?: 1.0
                
                val path = Path()
                val fillPath = Path()
                val points = mutableListOf<Offset>()
                
                expenses.reversed().forEachIndexed { index, expense ->
                    val x = (index.toFloat() / (expenses.size - 1).coerceAtLeast(1)) * width
                    val y = height - (expense.amount / maxAmount * height * 0.7).toFloat() - 20f
                    points.add(Offset(x, y))
                }
                
                if (points.isNotEmpty()) {
                    path.moveTo(points.first().x, points.first().y)
                    fillPath.moveTo(points.first().x, height)
                    fillPath.lineTo(points.first().x, points.first().y)
                    
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val controlX = (prev.x + curr.x) / 2
                        path.cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
                        fillPath.cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
                    }
                    
                    fillPath.lineTo(points.last().x, height)
                    fillPath.close()
                    
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = height
                        )
                    )
                    
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(primaryColor, secondaryColor)
                        ),
                        style = Stroke(width = 6f)
                    )
                }
            }
        }
    }
}

@Composable
fun HighlightsSection(
    topCategory: String?,
    topCategoryTotal: Double,
    receiptCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HighlightCard(
            modifier = Modifier.weight(1f),
            label = "Top Category",
            value = topCategory ?: "None",
            subValue = "€${String.format("%.2f", topCategoryTotal)}",
            icon = Icons.Default.Stars,
            iconTint = MaterialTheme.colorScheme.secondary
        )
        
        HighlightCard(
            modifier = Modifier.weight(1f),
            label = "Activity",
            value = "$receiptCount",
            subValue = "Receipts Scanned",
            icon = Icons.Default.ReceiptLong,
            iconTint = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
fun HighlightCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    iconTint: Color
) {
    Box(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.04f))
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface) // White floating card
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TransactionItem(expense: Expense, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.pocketai.app.presentation.components.BrandLogo(
            storeName = expense.storeName,
            size = 48.dp
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.storeName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${expense.category} • ${formatDateShort(expense.date)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = "-€${String.format("%.2f", expense.amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}


// Helpers
fun getCategoryColor(category: String): Color {
    // Rely on theme colors now rather than hardcoded hexes
    return Color(0xFF14B8A6)
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Groceries" -> Icons.Default.ShoppingCart
        "Transport" -> Icons.Default.DirectionsCar
        "Food & Dining" -> Icons.Default.Restaurant
        "Electronics" -> Icons.Default.Devices
        "Entertainment" -> Icons.Default.Movie
        "Shopping" -> Icons.Default.ShoppingBag
        else -> Icons.Default.Category
    }
}

fun formatDateShort(dateStr: String): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
        val today = Date()
        val diff = today.time - (date?.time ?: 0)
        val days = diff / (1000 * 60 * 60 * 24)
        
        when (days) {
            0L -> "Today"
            1L -> "Yesterday"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date!!)
        }
    } catch (e: Exception) {
        dateStr
    }
}