package com.antigravity.adaptivehz.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.adaptivehz.R
import com.antigravity.adaptivehz.core.RefreshRateController
import com.antigravity.adaptivehz.core.ShizukuPermissionHelper
import com.antigravity.adaptivehz.data.OperationMode
import com.antigravity.adaptivehz.data.SettingsRepository
import com.antigravity.adaptivehz.service.AdaptiveAccessibilityService
import com.antigravity.adaptivehz.ui.theme.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    settingsRepository: SettingsRepository,
    refreshRateController: RefreshRateController,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 5

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }

    // Local states
    var hasSecureSettings by remember { mutableStateOf(refreshRateController.hasWriteSecureSettings()) }
    var isAccessibilityActive by remember { mutableStateOf(AdaptiveAccessibilityService.isAccessibilityEnabled(context)) }
    var shizukuState by remember { mutableStateOf(ShizukuPermissionHelper.checkShizukuState()) }
    var isBatteryOptIgnored by remember {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false)
    }
    var selectedMode by remember { mutableStateOf(OperationMode.ADAPTIVE) }

    fun refreshStates() {
        hasSecureSettings = refreshRateController.hasWriteSecureSettings()
        isAccessibilityActive = AdaptiveAccessibilityService.isAccessibilityEnabled(context)
        shizukuState = ShizukuPermissionHelper.checkShizukuState()
        isBatteryOptIgnored = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Periodically re-check permission when step changes
    LaunchedEffect(currentStep) {
        refreshStates()
    }

    Scaffold(
        containerColor = HealthCraftBg,
        bottomBar = {
            Surface(
                color = HealthCraftSurface,
                border = BorderStroke(1.dp, HealthCraftCardStroke),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, HealthCraftCardStroke)
                        ) {
                            Text(stringResource(R.string.onboarding_back_action), color = TextSecondary)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(80.dp))
                    }

                    // Step Dots Indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(totalSteps) { stepIdx ->
                            Box(
                                modifier = Modifier
                                    .size(if (stepIdx == currentStep) 10.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (stepIdx == currentStep) HealthCraftPrimary
                                        else HealthCraftCardStroke
                                    )
                            )
                        }
                    }

                    if (currentStep < totalSteps - 1) {
                        Button(
                            onClick = {
                                hasSecureSettings = refreshRateController.hasWriteSecureSettings()
                                isAccessibilityActive = AdaptiveAccessibilityService.isAccessibilityEnabled(context)
                                currentStep++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HealthCraftPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(stringResource(R.string.onboarding_next_action), color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    settingsRepository.setOperationMode(selectedMode)
                                    settingsRepository.setOnboardingCompleted(true)
                                    onFinished()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HealthCraftPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(stringResource(R.string.onboarding_finish_btn), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = stringResource(R.string.onboarding_title_welcome),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.onboarding_subtitle_welcome),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Language Switcher on Welcome Screen (Step 0)
            if (currentStep == 0) {
                val currentLang by settingsRepository.appLanguage.collectAsState()
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isRu = currentLang == "ru" || (currentLang == "system" && java.util.Locale.getDefault().language.startsWith("ru"))
                    val isEn = currentLang == "en" || (currentLang == "system" && !java.util.Locale.getDefault().language.startsWith("ru"))

                    FilterChip(
                        selected = isRu,
                        onClick = { settingsRepository.setAppLanguage("ru") },
                        label = { Text("🇷🇺 Русский", fontSize = 12.sp, fontWeight = if (isRu) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HealthCraftMintContainer,
                            selectedLabelColor = HealthCraftPrimaryDark,
                            containerColor = HealthCraftSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isRu,
                            borderColor = if (isRu) HealthCraftMintBorder else HealthCraftCardStroke
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = isEn,
                        onClick = { settingsRepository.setAppLanguage("en") },
                        label = { Text("🇬🇧 English", fontSize = 12.sp, fontWeight = if (isEn) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HealthCraftMintContainer,
                            selectedLabelColor = HealthCraftPrimaryDark,
                            containerColor = HealthCraftSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isEn,
                            borderColor = if (isEn) HealthCraftMintBorder else HealthCraftCardStroke
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Step Content
            AnimatedContent(targetState = currentStep, label = "OnboardingStepAnimation") { step ->
                when (step) {
                    0 -> StepDeviceDiagnostics(refreshRateController)
                    1 -> StepPermissions(
                        hasSecureSettings = hasSecureSettings,
                        shizukuState = shizukuState,
                        onCheckAgain = {
                            hasSecureSettings = refreshRateController.hasWriteSecureSettings()
                            shizukuState = ShizukuPermissionHelper.checkShizukuState()
                        },
                        onGrantShizuku = {
                            coroutineScope.launch {
                                val result = ShizukuPermissionHelper.grantWriteSecureSettings(context)
                                if (result.isSuccess) {
                                    Toast.makeText(context, context.getString(R.string.onboarding_toast_granted), Toast.LENGTH_SHORT).show()
                                    hasSecureSettings = refreshRateController.hasWriteSecureSettings()
                                } else {
                                    Toast.makeText(context, "${context.getString(R.string.onboarding_toast_error)}: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                    2 -> StepAccessibility(
                        isActive = isAccessibilityActive,
                        onOpenSettings = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    3 -> StepBatteryOptimization(
                        isIgnored = isBatteryOptIgnored,
                        onRefresh = { refreshStates() }
                    )
                    4 -> StepModeSelection(selectedMode) { selectedMode = it }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Step 0: Diagnostics
// -------------------------------------------------------------------------------------------------
@Composable
private fun StepDeviceDiagnostics(refreshRateController: RefreshRateController) {
    val context = LocalContext.current
    val modes = remember { refreshRateController.getSupportedRefreshRates() }
    val currentHz by refreshRateController.currentRefreshRateHz.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HealthCraftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = HealthCraftPrimary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.onboarding_step_1_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.onboarding_step_1_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = HealthCraftCardStroke)
            Spacer(modifier = Modifier.height(14.dp))

            InfoRow(
                label = stringResource(R.string.about_device_label),
                value = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}"
            )
            InfoRow(
                label = "Android / SDK:",
                value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            )
            InfoRow(
                label = stringResource(R.string.current_hz_label),
                value = "$currentHz ${stringResource(R.string.hz_unit)}"
            )
            InfoRow(
                label = stringResource(R.string.onboarding_matrix_freq_label),
                value = modes.joinToString(", ") { "$it ${context.getString(R.string.hz_unit)}" }
            )

            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = HealthCraftMintContainer,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, HealthCraftMintBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = HealthCraftPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_display_ready_120),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = HealthCraftPrimaryDark
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Step 1: System Permissions (WRITE_SECURE_SETTINGS)
// -------------------------------------------------------------------------------------------------
@Composable
private fun StepPermissions(
    hasSecureSettings: Boolean,
    shizukuState: ShizukuPermissionHelper.ShizukuState,
    onCheckAgain: () -> Unit,
    onGrantShizuku: () -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (hasSecureSettings) {
            HealthCraftCard(backgroundColor = HealthCraftMintContainer, borderColor = HealthCraftMintBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = HealthCraftPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.onboarding_permission_granted_badge),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = HealthCraftPrimaryDark
                        )
                        Text(
                            text = stringResource(R.string.onboarding_secure_settings_active_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            var showShizukuExplainer by remember { mutableStateOf(false) }

            // Human-friendly Explainer Card for average users
            Surface(
                onClick = { showShizukuExplainer = true },
                shape = RoundedCornerShape(16.dp),
                color = HealthCraftMintContainer,
                border = BorderStroke(1.dp, HealthCraftMintBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HealthCraftPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.shizuku_help_pill_btn),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = HealthCraftPrimaryDark
                        )
                        Text(
                            text = stringResource(R.string.onboarding_shizuku_pill_desc),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = HealthCraftPrimary
                    )
                }
            }

            if (showShizukuExplainer) {
                ShizukuExplainerDialog(onDismiss = { showShizukuExplainer = false })
            }

            // Option 1: Shizuku
            HealthCraftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(HealthCraftMintContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = HealthCraftPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.onboarding_shizuku_option_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.onboarding_shizuku_option_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (shizukuState) {
                    ShizukuPermissionHelper.ShizukuState.READY -> {
                        Button(
                            onClick = onGrantShizuku,
                            colors = ButtonDefaults.buttonColors(containerColor = HealthCraftPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.onboarding_shizuku_btn_grant), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    ShizukuPermissionHelper.ShizukuState.PERMISSION_NEEDED -> {
                        Button(
                            onClick = { ShizukuPermissionHelper.requestPermission() },
                            colors = ButtonDefaults.buttonColors(containerColor = HealthCraftPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.onboarding_shizuku_dialog_prompt), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        Text(
                            text = stringResource(R.string.onboarding_shizuku_not_running),
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorWarning
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=moe.shizuku.privileged.api"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app"))
                                        context.startActivity(intent)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.onboarding_shizuku_btn_install), color = HealthCraftPrimary, fontSize = 12.sp)
                            }

                            Button(
                                onClick = onCheckAgain,
                                colors = ButtonDefaults.buttonColors(containerColor = HealthCraftMintContainer),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, HealthCraftMintBorder)
                            ) {
                                Text(stringResource(R.string.btn_check), color = HealthCraftPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Option 2: ADB via PC
            HealthCraftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.onboarding_adb_option_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.onboarding_adb_option_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                val cmd = stringResource(R.string.permission_secure_command)
                Surface(
                    color = HealthCraftBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, HealthCraftCardStroke)
                ) {
                    Text(
                        text = cmd,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = TextPrimary,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", cmd))
                            Toast.makeText(context, context.getString(R.string.permission_command_copied), Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.btn_copy), fontSize = 12.sp, color = TextPrimary)
                    }

                    Button(
                        onClick = onCheckAgain,
                        colors = ButtonDefaults.buttonColors(containerColor = HealthCraftPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.btn_check), fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        // Comprehensive Developer Mode & Vendor Guide Card
        DevModeGuideCard(context)
    }
}

@Composable
private fun ShizukuExplainerDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = HealthCraftPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.shizuku_explainer_close), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(HealthCraftMintContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = HealthCraftPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.shizuku_explainer_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Point 1: Why
                Column {
                    Text(
                        text = "📱 " + stringResource(R.string.shizuku_explainer_why_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.shizuku_explainer_why_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }

                // Point 2: What is Shizuku
                Column {
                    Text(
                        text = "🔑 " + stringResource(R.string.shizuku_explainer_what_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.shizuku_explainer_what_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }

                // Point 3: Safety
                Surface(
                    color = HealthCraftMintContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, HealthCraftMintBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🛡️ " + stringResource(R.string.shizuku_explainer_safety_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = HealthCraftPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = stringResource(R.string.shizuku_explainer_safety_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Point 4: 3 Steps
                Column {
                    Text(
                        text = "⚡ " + stringResource(R.string.shizuku_explainer_steps_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = stringResource(R.string.shizuku_explainer_step1), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(text = stringResource(R.string.shizuku_explainer_step2), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(text = stringResource(R.string.shizuku_explainer_step3), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        },
        containerColor = HealthCraftSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Expandable Guide Card explaining how to unlock Developer Options and USB Debugging across vendors.
 */
@Composable
fun DevModeGuideCard(context: Context) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedBrandTab by remember { mutableIntStateOf(0) } // 0: Samsung, 1: Xiaomi, 2: Other

    HealthCraftCard(
        backgroundColor = if (isExpanded) HealthCraftSurface else HealthCraftMintContainer,
        borderColor = if (isExpanded) HealthCraftCardStroke else HealthCraftMintBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = HealthCraftPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.dev_mode_guide_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.dev_mode_guide_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = HealthCraftPrimary
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(top = 14.dp)) {
                HorizontalDivider(color = HealthCraftCardStroke)
                Spacer(modifier = Modifier.height(12.dp))

                // Brand Chips Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val brands = listOf("Samsung", "Xiaomi / MIUI", stringResource(R.string.brand_other))
                    brands.forEachIndexed { index, name ->
                        val isSelected = selectedBrandTab == index
                        Surface(
                            onClick = { selectedBrandTab = index },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) HealthCraftPrimary else HealthCraftBg,
                            border = BorderStroke(1.dp, if (isSelected) HealthCraftPrimary else HealthCraftCardStroke),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Guide Steps per Brand
                val stepsText = when (selectedBrandTab) {
                    0 -> stringResource(R.string.dev_mode_samsung_steps)
                    1 -> stringResource(R.string.dev_mode_xiaomi_steps)
                    else -> stringResource(R.string.dev_mode_other_steps)
                }

                Surface(
                    color = HealthCraftBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, HealthCraftCardStroke)
                ) {
                    Text(
                        text = stepsText,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Shizuku wireless guide notice
                Text(
                    text = stringResource(R.string.shizuku_wireless_guide),
                    style = MaterialTheme.typography.bodySmall,
                    color = HealthCraftPrimaryDark,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Launch Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(stringResource(R.string.btn_open_device_info), fontSize = 11.sp, color = TextPrimary, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.dev_mode_tap_toast), Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HealthCraftPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(stringResource(R.string.btn_open_dev_settings), fontSize = 11.sp, color = Color.White, maxLines = 1)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Step 2: Touch Tracking (Accessibility)
// -------------------------------------------------------------------------------------------------
@Composable
private fun StepAccessibility(
    isActive: Boolean,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HealthCraftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = HealthCraftPrimary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.onboarding_step_3_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.onboarding_step_3_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isActive) {
                Surface(
                    color = HealthCraftMintContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, HealthCraftMintBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = HealthCraftPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.onboarding_accessibility_granted_badge),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = HealthCraftPrimaryDark
                        )
                    }
                }
            } else {
                // Visual Steps Card
                Surface(
                    color = HealthCraftBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, HealthCraftCardStroke)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.accessibility_guide_header),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.accessibility_guide_step1),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = stringResource(R.string.accessibility_guide_step2),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = HealthCraftPrimaryDark
                        )
                        Text(
                            text = stringResource(R.string.accessibility_guide_step3),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = HealthCraftPrimaryDark
                        )
                        Text(
                            text = stringResource(R.string.accessibility_guide_step4),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = HealthCraftPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.onboarding_open_accessibility), color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Restricted Settings warning for Android 13+ with direct shortcut
                Surface(
                    color = MetricAmberBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MetricAmberBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.onboarding_restricted_warning_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MetricAmberText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.onboarding_restricted_warning_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MetricAmberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.btn_open_app_details), fontSize = 12.sp, color = MetricAmberText)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Step 3: Background Protection (Battery Optimization)
// -------------------------------------------------------------------------------------------------
@Composable
private fun StepBatteryOptimization(
    isIgnored: Boolean,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current

    HealthCraftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.BatteryChargingFull,
                contentDescription = null,
                tint = HealthCraftPrimary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.onboarding_step_4_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = stringResource(R.string.onboarding_step_4_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isIgnored) {
            Surface(
                color = HealthCraftMintContainer,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, HealthCraftMintBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = HealthCraftPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.onboarding_battery_opt_granted),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = HealthCraftPrimaryDark
                        )
                        Text(
                            text = stringResource(R.string.battery_opt_granted_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            Button(
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                        Toast.makeText(context, context.getString(R.string.battery_opt_prompt_toast), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        try {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        } catch (ex: Exception) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HealthCraftPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_disable_battery_opt), color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, HealthCraftCardStroke),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.battery_opt_fallback_action), color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Step 4: Mode Selection
// -------------------------------------------------------------------------------------------------
@Composable
private fun StepModeSelection(
    selectedMode: OperationMode,
    onSelectMode: (OperationMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.onboarding_step_5_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        ModeOptionCard(
            mode = OperationMode.ADAPTIVE,
            title = stringResource(R.string.onboarding_mode_adaptive_title),
            subtitle = stringResource(R.string.onboarding_mode_adaptive_desc),
            isSelected = selectedMode == OperationMode.ADAPTIVE,
            onClick = { onSelectMode(OperationMode.ADAPTIVE) }
        )

        ModeOptionCard(
            mode = OperationMode.ECO,
            title = stringResource(R.string.onboarding_mode_eco_title),
            subtitle = stringResource(R.string.onboarding_mode_eco_desc),
            isSelected = selectedMode == OperationMode.ECO,
            onClick = { onSelectMode(OperationMode.ECO) }
        )

        ModeOptionCard(
            mode = OperationMode.FIXED_120,
            title = stringResource(R.string.onboarding_mode_120_title),
            subtitle = stringResource(R.string.onboarding_mode_120_desc),
            isSelected = selectedMode == OperationMode.FIXED_120,
            onClick = { onSelectMode(OperationMode.FIXED_120) }
        )
    }
}

@Composable
private fun ModeOptionCard(
    mode: OperationMode,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) HealthCraftMintContainer else HealthCraftSurface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) HealthCraftPrimary else HealthCraftCardStroke
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = HealthCraftPrimary)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
    }
}
