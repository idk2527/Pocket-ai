@file:OptIn(ExperimentalMaterial3Api::class)

package com.pocketai.app.presentation.add

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.pocketai.app.data.model.Expense
import com.pocketai.app.presentation.components.CategoryPicker
import com.pocketai.app.presentation.components.DatePickerField
import com.pocketai.app.presentation.components.GlassBox
import com.pocketai.app.presentation.components.GlassCard
import com.pocketai.app.viewmodel.AddViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddExpenseScreen(
    navController: NavController,
    viewModel: AddViewModel = hiltViewModel(),
    expenseId: Int? = null,
    passedUri: String? = null
) {
    val isEditMode = expenseId != null
    var storeName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var note by remember { mutableStateOf("") }
    var receiptImagePath by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val activity = context as? Activity

    val parsedExpense by viewModel.parsedExpense.collectAsState()
    val parsedReceiptData by viewModel.parsedReceiptData.collectAsState()
    val isParsing by viewModel.isParsing.collectAsState()

    var hasPrefilled by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    // ML Kit Scanner Setup
    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setPageLimit(3)
            .setGalleryImportAllowed(true)
            .build()
    }
    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }
    val scannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pages?.firstOrNull()?.let { page ->
                val imageUri = page.imageUri
                val privateUri = com.pocketai.app.util.copyToPrivateStorage(context, imageUri)
                if (privateUri != null) {
                    receiptImagePath = privateUri.toString()
                    viewModel.processReceiptImage(privateUri)
                } else {
                    viewModel.processReceiptImage(imageUri)
                }
            }
        }
    }
    fun launchScanner() {
        if (activity != null) {
            scanner.getStartScanIntent(activity).addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }.addOnFailureListener { e ->
                scope.launch { snackbarHostState.showSnackbar("Scanner error: ${e.message}") }
            }
        }
    }

    LaunchedEffect(passedUri) {
        if (passedUri != null && receiptImagePath == null) {
            val uri = Uri.parse(passedUri)
            receiptImagePath = passedUri
            viewModel.processReceiptImage(uri)
        }
    }

    LaunchedEffect(parsedExpense) {
        if (!hasPrefilled) {
            parsedExpense?.let {
                if (storeName.isBlank()) storeName = it.storeName
                if (category.isBlank() || category == "Other") category = it.category
                if (amount.isBlank()) amount = it.amount.toString()
                if (it.date.isNotBlank() && it.date != "unknown") date = it.date
                hasPrefilled = true
            }
        }
    }

    val expenseState = if (isEditMode) viewModel.getExpenseById(expenseId!!).collectAsState(initial = null) else remember { mutableStateOf<Expense?>(null) }
    val expense = expenseState.value

    LaunchedEffect(expense) {
        expense?.let {
            if (storeName.isBlank()) storeName = it.storeName
            if (category == "Other") category = it.category
            if (amount.isBlank()) amount = it.amount.toString()
            if (note.isBlank()) note = it.note ?: ""
            if (date.isBlank() || date == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) date = it.date
            if (receiptImagePath == null) receiptImagePath = it.receiptImagePath
        }
    }

    val categories = listOf("Groceries", "Electronics", "Health & Beauty", "Fashion", "Home", "Food & Dining", "Transport", "Entertainment", "Other")

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            if (message != null) {
                snackbarHostState.showSnackbar(message)
                viewModel.dismissSnackbar()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Ambient Header Glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            radius = 800f
                        )
                    )
            )

            if (isParsing) {
                val partialResult by viewModel.partialResult.collectAsState()
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                        .clickable(enabled = false) { },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        // Glowing Orb
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .shadow(32.dp, CircleShape, spotColor = Color(0xFF20FC8F))
                                .background(Color(0xFF20FC8F).copy(alpha = 0.1f), CircleShape)
                                .border(2.dp, Color(0xFF20FC8F).copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(80.dp),
                                color = Color(0xFF20FC8F),
                                strokeWidth = 3.dp
                            )
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF20FC8F),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "Extracting Data...",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (partialResult.isNotEmpty()) {
                            GlassBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = partialResult,
                                    color = Color(0xFF20FC8F).copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState())
                                )
                            }
                        } else {
                            Text(
                                text = "Running local vision model",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(initialOffsetY = { -40 })
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                            }

                            Text(
                                text = if (isEditMode) "Edit Expense" else "New Expense",
                                style = MaterialTheme.typography.titleLarge
                            )

                            if (!isEditMode) {
                                IconButton(
                                    onClick = { launchScanner() },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                ) {
                                    Icon(Icons.Default.DocumentScanner, contentDescription = "Scan", tint = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                Spacer(modifier = Modifier.size(44.dp))
                            }
                        }
                    }

                    // Main Amount Input (Hero)
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = fadeIn(animationSpec = tween(500, delayMillis = 100)) + scaleIn(initialScale = 0.9f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ENTER AMOUNT",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 2.sp
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = amount,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || Regex("^\\d*\\.?\\d{0,2}$").matches(newValue)) {
                                        amount = newValue
                                    }
                                },
                                textStyle = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 56.sp
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                prefix = {
                                    Text(
                                        "€",
                                        style = MaterialTheme.typography.displayLarge.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 56.sp
                                        ),
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Extraction Preview Box
                    parsedReceiptData?.let { receiptData ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(400)) + slideInVertically(initialOffsetY = { 20 })
                        ) {
                            GlassBox(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF20FC8F), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("AI Extracted", fontWeight = FontWeight.Bold, color = Color(0xFF20FC8F), fontSize = 12.sp)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF20FC8F).copy(alpha = 0.1f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("${(receiptData.confidence * 100).toInt()}% conf.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF20FC8F))
                                        }
                                    }
                                    if (receiptData.items.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        Text("${receiptData.items.size} items detected", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // Form Fields
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = fadeIn(animationSpec = tween(500, delayMillis = 200)) + slideInVertically(initialOffsetY = { 40 })
                    ) {
                        Column {
                            // Store TextField
                            OutlinedTextField(
                                value = storeName,
                                onValueChange = { storeName = it },
                                label = { Text("Store Name") },
                                leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )

                            // Category
                            CategoryPicker(
                                selectedCategory = category,
                                onCategorySelected = { category = it },
                                categories = categories,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            )

                            // Date
                            DatePickerField(
                                selectedDate = date,
                                onDateSelected = { date = it },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            )

                            // Note
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                label = { Text("Add Note (Optional)") },
                                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(16.dp),
                                maxLines = 3
                            )

                            // Save Button
                            val canSave = storeName.isNotBlank() && amount.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0
                            Button(
                                onClick = {
                                    if (canSave) {
                                        val amountValue = amount.toDoubleOrNull() ?: 0.0
                                        if (isEditMode) {
                                            expense?.let { existingExpense ->
                                                val updatedExpense = existingExpense.copy(
                                                    storeName = storeName,
                                                    category = category,
                                                    amount = amountValue,
                                                    date = date,
                                                    note = if (note.isBlank()) null else note,
                                                    receiptImagePath = receiptImagePath
                                                )
                                                viewModel.updateExpense(updatedExpense)
                                            }
                                        } else {
                                            viewModel.addExpenseEnriched(
                                                storeName = storeName,
                                                category = category,
                                                amount = amountValue,
                                                date = date,
                                                note = if (note.isBlank()) null else note,
                                                receiptPath = receiptImagePath,
                                                receiptData = parsedReceiptData
                                            )
                                        }
                                        navController.popBackStack()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                enabled = canSave,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(
                                    text = if (isEditMode) "Save Changes" else "Add Expense",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }
}
