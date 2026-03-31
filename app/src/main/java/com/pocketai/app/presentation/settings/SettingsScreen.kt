package com.pocketai.app.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.pocketai.app.presentation.components.GlassBox
import com.pocketai.app.presentation.components.GlassCard
import com.pocketai.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isExporting by viewModel.isExporting.collectAsState()
    var showClearDataDialog by remember { mutableStateOf(false) }
    
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()
    
    val userName by viewModel.userName.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }
    
    val themeMode by viewModel.themeMode.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    val useGpu by viewModel.useGpu.collectAsState()

    val visibleState = remember { 
        androidx.compose.animation.core.MutableTransitionState(false).apply { 
            targetState = true 
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
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 120.dp) // Nav bar padding
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 16.dp, start = 24.dp, end = 24.dp),
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
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Spacer(modifier = Modifier.size(44.dp))
                }

                // Privacy Hero Card
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 20 })
                ) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            Column {
                                Text(
                                    text = "Private by Default",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "All LLM analysis happens locally on-device. No receipts are sent to the cloud.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Profile Section
                SettingsGroup(title = "PROFILE", delay = 100) {
                    SettingsItem(
                        icon = Icons.Default.Person,
                        iconColor = MaterialTheme.colorScheme.primary,
                        title = "User Name",
                        value = userName,
                        onClick = { showEditNameDialog = true }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 64.dp))
                    SettingsItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        iconColor = MaterialTheme.colorScheme.secondary,
                        title = "Monthly Budget",
                        value = "€${String.format("%,.0f", monthlyBudget)}",
                        onClick = { showEditBudgetDialog = true }
                    )
                }

                SettingsGroup(title = "PREFERENCES", delay = 200) {
                    SettingsItem(
                        icon = Icons.Default.DarkMode,
                        iconColor = Color(0xFF8B5CF6), // Purple
                        title = "App Theme",
                        value = when(themeMode) {
                            "light" -> "Light"
                            "dark" -> "Dark"
                            else -> "System"
                        },
                        onClick = { showThemeDialog = true }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 64.dp))
                    SettingsToggleItem(
                        icon = Icons.Default.FlashOn,
                        iconColor = Color(0xFFF59E0B), // Amber
                        title = "GPU Acceleration",
                        subtitle = "Use Vulkan for faster AI",
                        checked = useGpu,
                        onCheckedChange = { viewModel.setUseGpu(it) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 64.dp))
                    SettingsItem(
                        icon = Icons.Default.SystemUpdate,
                        iconColor = MaterialTheme.colorScheme.primary,
                        title = "Check for Updates",
                        value = "v${viewModel.appVersion}",
                        isLoading = isCheckingUpdate,
                        onClick = {
                            viewModel.checkForUpdates(
                                onNoUpdate = { scope.launch { snackbarHostState.showSnackbar("You are up to date! \uD83C\uDF89") } },
                                onError = { msg -> scope.launch { snackbarHostState.showSnackbar("Check failed: $msg") } }
                            )
                        }
                    )
                }

                // Data Section
                SettingsGroup(title = "DATA MANAGEMENT", delay = 300) {
                    SettingsItem(
                        icon = Icons.Default.FileDownload,
                        iconColor = MaterialTheme.colorScheme.secondary,
                        title = "Export All Data",
                        value = "CSV",
                        isLoading = isExporting,
                        onClick = {
                            viewModel.exportData { result ->
                                scope.launch {
                                    result.fold(
                                        onSuccess = { path -> snackbarHostState.showSnackbar("✓ Exported to $path") },
                                        onFailure = { error -> snackbarHostState.showSnackbar("Export failed: ${error.message}") }
                                    )
                                }
                            }
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 64.dp))
                    SettingsItem(
                        icon = Icons.Default.DeleteSweep,
                        iconColor = MaterialTheme.colorScheme.error,
                        title = "Clear All Data",
                        value = "",
                        isDestructive = true,
                        onClick = { showClearDataDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
                
                // Footer
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PocketAI Premium",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Version ${viewModel.appVersion}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }

            // Dialogs...
            if (showEditNameDialog) {
                var tempName by remember { mutableStateOf(userName) }
                AlertDialog(
                    onDismissRequest = { showEditNameDialog = false },
                    title = { Text("Display Name") },
                    text = {
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            placeholder = { Text("Enter your name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.updateUserName(tempName)
                            showEditNameDialog = false
                        }) { Text("Save", color = MaterialTheme.colorScheme.primary) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            if (showEditBudgetDialog) {
                var tempBudget by remember { mutableStateOf(if (monthlyBudget > 0) String.format("%.0f", monthlyBudget) else "") }
                AlertDialog(
                    onDismissRequest = { showEditBudgetDialog = false },
                    title = { Text("Monthly Budget") },
                    text = {
                        OutlinedTextField(
                            value = tempBudget,
                            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) tempBudget = it },
                            placeholder = { Text("e.g. 1500") },
                            leadingIcon = { Text("€", modifier = Modifier.padding(start = 8.dp)) },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val budgetVal = tempBudget.toFloatOrNull() ?: monthlyBudget
                            viewModel.updateMonthlyBudget(budgetVal)
                            showEditBudgetDialog = false
                        }) { Text("Save", color = MaterialTheme.colorScheme.primary) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditBudgetDialog = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            if (showClearDataDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDataDialog = false },
                    title = { Text("Erase Everything?") },
                    text = { Text("This will permanently delete all your receipts and expenses. This action cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.clearAllData { scope.launch { snackbarHostState.showSnackbar("All data erased") } }
                                showClearDataDialog = false
                            }
                        ) { Text("Delete All", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDataDialog = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            if (showThemeDialog) {
                AlertDialog(
                    onDismissRequest = { showThemeDialog = false },
                    title = { Text("Select Appearance") },
                    text = {
                        Column {
                            listOf("auto" to "System Default", "light" to "Light", "dark" to "Dark").forEach { (mode, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.setThemeMode(mode)
                                            showThemeDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (themeMode == mode),
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = label, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showThemeDialog = false }) { Text("Close", color = MaterialTheme.colorScheme.primary) }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            if (updateInfo != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissUpdateDialog() },
                    title = { Text("Update Available \uD83D\uDFE2") },
                    text = {
                        Column {
                            Text(text = "New version: ${updateInfo?.versionName}", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = updateInfo?.releaseNotes ?: "No release notes.", style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (updateInfo?.apkUrl != null) {
                                    viewModel.startUpdate(updateInfo!!.apkUrl, updateInfo!!.versionName)
                                    scope.launch { snackbarHostState.showSnackbar("Downloading update...") }
                                }
                                viewModel.dismissUpdateDialog()
                            }
                        ) { Text("Update", color = MaterialTheme.colorScheme.primary) }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissUpdateDialog() }) { Text("Later", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    delay: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    val visibleState = remember { 
        androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } 
    }

    androidx.compose.animation.AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(500, delayMillis = delay)) + 
                slideInVertically(initialOffsetY = { 20 }, animationSpec = androidx.compose.animation.core.tween(500, delayMillis = delay))
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
            )
            
            GlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column {
                    content()
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = iconColor
                )
            } else {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
