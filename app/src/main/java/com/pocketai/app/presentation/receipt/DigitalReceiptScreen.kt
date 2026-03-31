@file:OptIn(ExperimentalMaterial3Api::class)

package com.pocketai.app.presentation.receipt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pocketai.app.data.model.ReceiptData
import com.pocketai.app.presentation.components.GlassCard
import com.pocketai.app.viewmodel.ExpenseViewModel

/**
 * Phase 17: Veryfi-style Digital Receipt Display
 * Shows the fully digitalized receipt with items, VAT, payment breakdown.
 */
@Composable
fun DigitalReceiptScreen(
    navController: NavController,
    expenseId: Int,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val expense by viewModel.getExpenseById(expenseId).collectAsState(initial = null)

    val teal = Color(0xFF20FC8F)
    val navy = Color(0xFF0B1221)
    val darkSurface = Color(0xFF141B2D)

    Scaffold(
        containerColor = navy,
        topBar = {
            TopAppBar(
                title = { Text("Digital Receipt", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        val exp = expense
        if (exp == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = teal)
            }
            return@Scaffold
        }

        // Parse items from JSON
        val items = remember(exp.itemsJson) { ReceiptData.parseItemsJson(exp.itemsJson) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // --- Merchant Header ---
            GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Store name
                    Text(
                        text = exp.storeName.uppercase(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )

                    // Address
                    exp.merchantAddress?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = it,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(Modifier.height(12.dp))

                    // Receipt metadata row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            MetaLabel("DATE")
                            MetaValue(exp.date)
                        }
                        exp.receiptNumber?.let {
                            Column(horizontalAlignment = Alignment.End) {
                                MetaLabel("RECEIPT #")
                                MetaValue(it)
                            }
                        }
                    }

                    // Second row
                    if (exp.cashier != null || exp.vatId != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            exp.cashier?.let {
                                Column {
                                    MetaLabel("CASHIER")
                                    MetaValue(it)
                                }
                            }
                            exp.vatId?.let {
                                Column(horizontalAlignment = Alignment.End) {
                                    MetaLabel("VAT ID")
                                    MetaValue(it)
                                }
                            }
                        }
                    }
                }
            }

            // --- Items Table ---
            if (items.isNotEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "ITEMS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = teal,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        // Column headers
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Item", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            Text("Qty", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.width(36.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                            Text("Price", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.width(70.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Medium)
                        }

                        Divider(color = Color.White.copy(alpha = 0.08f))

                        items.forEachIndexed { index, item ->
                            val isDiscount = item.totalPrice < 0 || item.discount != null && item.discount > 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (index % 2 == 0) Color.Transparent else Color.White.copy(alpha = 0.03f))
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontSize = 14.sp,
                                        color = if (isDiscount) Color(0xFFFF6B6B) else Color.White,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    item.vatRate?.let {
                                        Text(
                                            text = "MwSt $it",
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                                Text(
                                    text = if (item.quantity > 1) "${item.quantity}x" else "",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.width(36.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "€${String.format("%.2f", item.totalPrice)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDiscount) Color(0xFFFF6B6B) else Color.White,
                                    modifier = Modifier.width(70.dp),
                                    textAlign = TextAlign.End
                                )
                            }

                            if (index < items.size - 1) {
                                Divider(color = Color.White.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            }

            // --- Totals Section ---
            GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Subtotal
                    exp.subtotal?.let {
                        TotalRow("Subtotal", "€${String.format("%.2f", it)}", Color.White.copy(alpha = 0.7f))
                        Spacer(Modifier.height(8.dp))
                    }

                    // VAT
                    if (exp.vatAmount != null && exp.vatAmount > 0) {
                        TotalRow(
                            label = "MwSt${exp.vatRate?.let { " ($it)" } ?: ""}",
                            value = "€${String.format("%.2f", exp.vatAmount)}",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    Divider(color = teal.copy(alpha = 0.3f), thickness = 2.dp)
                    Spacer(Modifier.height(12.dp))

                    // Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TOTAL",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = teal,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "€${String.format("%.2f", exp.amount)} ${exp.currency ?: "EUR"}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = teal
                        )
                    }
                }
            }

            // --- Payment Section ---
            exp.paymentMethod?.let { method ->
                GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "PAYMENT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = teal,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = when {
                                method.contains("Bar", ignoreCase = true) || method.contains("Cash", ignoreCase = true) -> Icons.Default.Money
                                method.contains("Karte", ignoreCase = true) || method.contains("Card", ignoreCase = true) -> Icons.Default.CreditCard
                                else -> Icons.Default.Payment
                            }
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = teal.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(icon, null, tint = teal, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(method, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text(
                                    "€${String.format("%.2f", exp.amount)}",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // --- AI Badge ---
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.05f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        null,
                        tint = teal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Extracted by Pocket AI",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            "On-device Vision AI • ${exp.note?.let { it } ?: ""}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF20FC8F).copy(alpha = 0.7f),
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun MetaValue(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White
    )
}

@Composable
private fun TotalRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = color)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
