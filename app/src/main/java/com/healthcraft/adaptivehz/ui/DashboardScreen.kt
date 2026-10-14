package com.healthcraft.adaptivehz.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthcraft.adaptivehz.R
import com.healthcraft.adaptivehz.core.RefreshRateController
import com.healthcraft.adaptivehz.data.OperationMode
import com.healthcraft.adaptivehz.data.SettingsRepository
import com.healthcraft.adaptivehz.service.AdaptiveCoordinator
import com.healthcraft.adaptivehz.service.AdaptiveState
import com.healthcraft.adaptivehz.service.FloatingHudService
import com.healthcraft.adaptivehz.service.StatusBarIndicatorService
import com.healthcraft.adaptivehz.ui.theme.*
import kotlinx.coroutines.flow.StateFlow

@Composable
fun DashboardScreen(
    controller: RefreshRateController,
    repository: SettingsRepository
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    val operationMode by repository.operationMode.collectAsStateWithLifecycle()
    val idleTimeoutSeconds by repository.idleTimeoutSeconds.collectAsStateWithLifecycle()
    val isPsmBypassEnabled by repository.isPsmBypassEnabled.collectAsStateWithLifecycle()
    val isSmartChargingEnabled by repository.isSmartChargingEnabled.collectAsStateWithLifecycle()
    val isLowBatteryDefenderEnabled by repository.isLowBatteryDefenderEnabled.collectAsStateWithLifecycle()
    val isThermalProtectionEnabled by repository.isThermalProtectionEnabled.collectAsStateWithLifecycle()
    val isStatusBarIndicatorEnabled by repository.isStatusBarIndicatorEnabled.collectAsStateWithLifecycle()
    val isHudEnabled by repository.isHudEnabled.collectAsStateWithLifecycle()

    val adaptiveState by AdaptiveCoordinator.adaptiveState.collectAsStateWithLifecycle()
    val isServiceActive by AdaptiveCoordinator.isServiceActive.collectAsStateWithLifecycle()

    var hasAdbPermission by remember { mutableStateOf(controller.hasWriteSecureSettings()) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasNotificationPermission by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    var showOverlayDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showAuditSheet by remember { mutableStateOf(false) }

    val deviceModel = remember { Build.MODEL }
    val isLtpo = remember { controller.isLtpoPanel() }
    val isSamsung = remember { Build.MANUFACTURER.contains("Samsung", ignoreCase = true) }
    val (compatScore, compatLabelRes) = remember(isLtpo, isSamsung, hasAdbPermission) {
        when {
            !isSamsung -> Pair(85, R.string.audit_score_vendor)
            isLtpo -> Pair(75, R.string.audit_score_ltpo)
            else -> Pair(98, R.string.audit_score_optimal)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAdbPermission = controller.hasWriteSecureSettings()
                hasOverlayPermission = Settings.canDrawOverlays(context)
                hasNotificationPermission = NotificationManagerCompat.from(context).areNotificationsEnabled()

                // Keep services in sync on return from system settings
                if (isHudEnabled && hasOverlayPermission) {
                    FloatingHudService.start(context)
                }
                if (isStatusBarIndicatorEnabled && hasNotificationPermission) {
                    StatusBarIndicatorService.start(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isHudEnabled) {
        if (isHudEnabled && Settings.canDrawOverlays(context)) {
            FloatingHudService.start(context)
        } else if (!isHudEnabled) {
            FloatingHudService.stop(context)
        }
    }

    LaunchedEffect(isStatusBarIndicatorEnabled) {
        if (isStatusBarIndicatorEnabled && NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            StatusBarIndicatorService.start(context)
        } else if (!isStatusBarIndicatorEnabled) {
            StatusBarIndicatorService.stop(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HealthCraftBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. HealthCraft Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Squircle logo emblem
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(HealthCraftMintContainer)
                        .border(1.dp, HealthCraftMintBorder, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = HealthCraftPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                // Middle Pill: Mode status
                val modeLabel = when (operationMode) {
                    OperationMode.ADAPTIVE -> stringResource(R.string.mode_adaptive_short)
                    OperationMode.FIXED_60 -> stringResource(R.string.mode_fixed_60_short)
                    OperationMode.FIXED_120 -> stringResource(R.string.mode_fixed_120_short)
                    OperationMode.ECO -> stringResource(R.string.mode_eco_short)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (operationMode == OperationMode.FIXED_120) MetricAmberBg else HealthCraftMintContainer)
                        .border(1.dp, if (operationMode == OperationMode.FIXED_120) MetricAmberBorder else HealthCraftMintBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FlashOn,
                        contentDescription = null,
                        tint = if (operationMode == OperationMode.FIXED_120) MetricAmberText else HealthCraftPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "AdaptiveHz",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (operationMode == OperationMode.FIXED_120) MetricAmberText else HealthCraftPrimary)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = modeLabel.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Right: Dynamic Device pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(HealthCraftMintContainer)
                    .border(1.dp, HealthCraftMintBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhoneAndroid,
                    contentDescription = null,
                    tint = HealthCraftPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = deviceModel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HealthCraftPrimary
                )
            }
        }

        // 2. Main Metric Card (Частота обновления)
        HealthCraftCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(HealthCraftMintContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            tint = HealthCraftPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.current_hz_label),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(HealthCraftBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isServiceActive) stringResource(R.string.service_status_active) else stringResource(R.string.service_status_idle),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                            }
                        }
                        val statusDesc = when (operationMode) {
                            OperationMode.FIXED_120 -> stringResource(R.string.state_desc_fixed_120)
                            OperationMode.FIXED_60 -> stringResource(R.string.state_desc_fixed_60)
                            OperationMode.ECO -> when (adaptiveState) {
                                AdaptiveState.ACTIVE_TOUCH -> stringResource(R.string.state_desc_eco_touch)
                                AdaptiveState.IDLE_TIMEOUT -> stringResource(R.string.state_desc_eco_idle)
                                else -> stringResource(R.string.state_desc_eco_default)
                            }
                            OperationMode.ADAPTIVE -> when (adaptiveState) {
                                AdaptiveState.ACTIVE_TOUCH -> stringResource(R.string.state_desc_adaptive_touch)
                                AdaptiveState.IDLE_TIMEOUT -> stringResource(R.string.state_desc_adaptive_idle)
                                AdaptiveState.LOCKED_APP -> stringResource(R.string.state_desc_locked_app)
                                AdaptiveState.DISABLED -> stringResource(R.string.state_desc_disabled)
                            }
                        }
                        Text(
                            text = statusDesc,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Right Live Metric Badge (Isolated to avoid recomposing DashboardScreen on Hz changes)
                LiveRefreshRateBadge(refreshRateFlow = controller.currentRefreshRateHz)
            }

            HorizontalDivider(
                color = HealthCraftCardStroke,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 14.dp)
            )

            // 2x2 Metric Matrix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricBox(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Speed,
                    title = stringResource(R.string.metric_mode_title),
                    value = when (operationMode) {
                        OperationMode.ADAPTIVE -> stringResource(R.string.mode_adaptive_short)
                        OperationMode.FIXED_60 -> stringResource(R.string.mode_fixed_60_short)
                        OperationMode.FIXED_120 -> stringResource(R.string.mode_fixed_120_short)
                        OperationMode.ECO -> stringResource(R.string.mode_eco_short)
                    },
                    subValue = when (operationMode) {
                        OperationMode.ADAPTIVE -> stringResource(R.string.metric_mode_sub_adaptive)
                        OperationMode.FIXED_60 -> stringResource(R.string.metric_mode_sub_fixed_60)
                        OperationMode.FIXED_120 -> stringResource(R.string.metric_mode_sub_fixed_120)
                        OperationMode.ECO -> stringResource(R.string.metric_mode_sub_eco)
                    },
                    backgroundColor = MetricMintBg,
                    borderColor = MetricMintBorder,
                    accentColor = MetricMintText
                )
                MetricBox(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.BatteryChargingFull,
                    title = stringResource(R.string.metric_power_title),
                    value = if (isSmartChargingEnabled) stringResource(R.string.metric_power_charging) else stringResource(R.string.metric_power_battery),
                    subValue = if (isSmartChargingEnabled) stringResource(R.string.metric_power_sub_charging) else stringResource(R.string.metric_power_sub_battery),
                    backgroundColor = MetricAmberBg,
                    borderColor = MetricAmberBorder,
                    accentColor = MetricAmberText
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricBox(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Timer,
                    title = stringResource(R.string.metric_timeout_title),
                    value = when (operationMode) {
                        OperationMode.ECO -> stringResource(R.string.metric_timeout_eco)
                        OperationMode.ADAPTIVE -> stringResource(R.string.metric_timeout_seconds, idleTimeoutSeconds)
                        OperationMode.FIXED_60, OperationMode.FIXED_120 -> "—"
                    },
                    subValue = if (operationMode == OperationMode.ADAPTIVE || operationMode == OperationMode.ECO) stringResource(R.string.metric_timeout_sub_adaptive) else stringResource(R.string.metric_timeout_sub_fixed),
                    backgroundColor = MetricLavenderBg,
                    borderColor = MetricLavenderBorder,
                    accentColor = MetricLavenderText
                )
                MetricBox(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Spa,
                    title = stringResource(R.string.metric_savings_title),
                    value = when (operationMode) {
                        OperationMode.FIXED_60 -> "~50%"
                        OperationMode.ECO -> "~45%"
                        OperationMode.ADAPTIVE -> stringResource(R.string.metric_savings_value)
                        OperationMode.FIXED_120 -> "0%"
                    },
                    subValue = stringResource(R.string.metric_savings_sub),
                    backgroundColor = MetricTealBg,
                    borderColor = MetricTealBorder,
                    accentColor = MetricTealText
                )
            }

            if (isLtpo) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MetricAmberBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MetricAmberBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MetricAmberText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.audit_ltpo_warning_title) + " (LTPO VRR)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MetricAmberText
                        )
                    }
                }
            }
        }

        // 3. One UI Compatibility Audit Banner
        OneUiAuditBanner(
            compatScore = compatScore,
            compatLabel = stringResource(compatLabelRes),
            onOpenAuditClick = { showAuditSheet = true }
        )

        // 4. Interactive Touch & Scroll Testing Zone
        TouchTestZoneCard(
            controller = controller,
            repository = repository
        )

        // 5. HealthCraft Mode Selection Chips
        HealthCraftCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.quick_modes_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.quick_modes_desc),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 1: Adaptive & Fixed 60
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chip 1: Adaptive
                val isAdaptiveActive = operationMode == OperationMode.ADAPTIVE
                Surface(
                    onClick = {
                        repository.setOperationMode(OperationMode.ADAPTIVE)
                        AdaptiveCoordinator.notifyUserInteraction()
                        Toast.makeText(context, context.getString(R.string.toast_mode_adaptive), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            role = Role.Button
                            selected = isAdaptiveActive
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isAdaptiveActive) HealthCraftMintContainer else HealthCraftBg,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isAdaptiveActive) HealthCraftPrimary else HealthCraftCardStroke
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.mode_chip_adaptive_label),
                            fontSize = 13.sp,
                            fontWeight = if (isAdaptiveActive) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isAdaptiveActive) HealthCraftPrimaryDark else TextPrimary
                        )
                    }
                }

                // Chip 2: Fixed 60
                val is60Active = operationMode == OperationMode.FIXED_60
                Surface(
                    onClick = {
                        repository.setOperationMode(OperationMode.FIXED_60)
                        controller.setFixed60()
                        Toast.makeText(context, context.getString(R.string.toast_mode_60), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            role = Role.Button
                            selected = is60Active
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (is60Active) MetricMintBg else HealthCraftBg,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (is60Active) MetricMintText else HealthCraftCardStroke
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.mode_chip_60_label),
                            fontSize = 13.sp,
                            fontWeight = if (is60Active) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (is60Active) MetricMintText else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Fixed 120 & ECO
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chip 3: Fixed 120
                val is120Active = operationMode == OperationMode.FIXED_120
                Surface(
                    onClick = {
                        repository.setOperationMode(OperationMode.FIXED_120)
                        controller.setFixed120()
                        Toast.makeText(context, context.getString(R.string.toast_mode_120), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            role = Role.Button
                            selected = is120Active
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (is120Active) MetricAmberBg else HealthCraftBg,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (is120Active) MetricAmberText else HealthCraftCardStroke
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.mode_chip_120_label),
                            fontSize = 13.sp,
                            fontWeight = if (is120Active) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (is120Active) MetricAmberText else TextPrimary
                        )
                    }
                }

                // Chip 4: ECO (1.5s timeout)
                val isEcoActive = operationMode == OperationMode.ECO
                Surface(
                    onClick = {
                        repository.setOperationMode(OperationMode.ECO)
                        AdaptiveCoordinator.notifyUserInteraction()
                        Toast.makeText(context, context.getString(R.string.toast_mode_eco), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            role = Role.Button
                            selected = isEcoActive
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isEcoActive) MetricTealBg else HealthCraftBg,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isEcoActive) MetricTealText else HealthCraftCardStroke
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.mode_chip_eco_label),
                            fontSize = 13.sp,
                            fontWeight = if (isEcoActive) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isEcoActive) MetricTealText else TextPrimary
                        )
                    }
                }
            }
        }

        // 4. Display Overlays & Indicators (Плавающий экранчик + Индикатор в строке состояния)
        HealthCraftCard {
            Text(
                text = stringResource(R.string.section_display_hud_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = stringResource(R.string.section_display_hud_desc),
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-item 1: Floating HUD Overlay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isHudEnabled) HealthCraftMintContainer else HealthCraftBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureInPicture,
                            contentDescription = null,
                            tint = if (isHudEnabled) HealthCraftPrimary else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.floating_hud_title),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.floating_hud_desc),
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                PastelSwitch(
                    checked = isHudEnabled,
                    onCheckedChange = { enable ->
                        if (enable) {
                            if (!Settings.canDrawOverlays(context)) {
                                showOverlayDialog = true
                            } else {
                                repository.setHudEnabled(true)
                                FloatingHudService.start(context)
                                Toast.makeText(context, context.getString(R.string.floating_hud_active), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            repository.setHudEnabled(false)
                            FloatingHudService.stop(context)
                        }
                    }
                )
            }

            HorizontalDivider(
                color = HealthCraftCardStroke,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 14.dp)
            )

            // Sub-item 2: Status Bar Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isStatusBarIndicatorEnabled) HealthCraftMintContainer else HealthCraftBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = if (isStatusBarIndicatorEnabled) HealthCraftPrimary else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.status_bar_indicator_title),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.status_bar_indicator_desc),
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                PastelSwitch(
                    checked = isStatusBarIndicatorEnabled,
                    onCheckedChange = { enable ->
                        if (enable) {
                            val notifsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                            if (!notifsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                showNotificationDialog = true
                            } else {
                                repository.setStatusBarIndicatorEnabled(true)
                                StatusBarIndicatorService.start(context)
                                Toast.makeText(context, context.getString(R.string.status_bar_indicator_active), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            repository.setStatusBarIndicatorEnabled(false)
                            StatusBarIndicatorService.stop(context)
                        }
                    }
                )
            }

            if (isStatusBarIndicatorEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MetricMintBg,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MetricMintBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.status_bar_notice_hint),
                        fontSize = 11.sp,
                        color = HealthCraftPrimaryDark,
                        modifier = Modifier.padding(10.dp),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Overlay Permission Dialog
        if (showOverlayDialog) {
            AlertDialog(
                onDismissRequest = { showOverlayDialog = false },
                title = { Text(stringResource(R.string.overlay_permission_dialog_title), fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = { Text(stringResource(R.string.overlay_permission_dialog_desc), color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            showOverlayDialog = false
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HealthCraftPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.overlay_permission_dialog_btn), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOverlayDialog = false }) {
                        Text(stringResource(R.string.btn_cancel), color = TextSecondary)
                    }
                },
                containerColor = HealthCraftSurface,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Notification Permission Dialog
        if (showNotificationDialog) {
            AlertDialog(
                onDismissRequest = { showNotificationDialog = false },
                title = { Text(stringResource(R.string.notification_permission_dialog_title), fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = { Text(stringResource(R.string.notification_permission_dialog_desc), color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            showNotificationDialog = false
                            try {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HealthCraftPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.notification_permission_dialog_btn), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNotificationDialog = false }) {
                        Text(stringResource(R.string.btn_cancel), color = TextSecondary)
                    }
                },
                containerColor = HealthCraftSurface,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // 5. Intelligent Safeguards Card
        HealthCraftCard {
            Text(
                text = stringResource(R.string.section_smart_protection),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Smart Charging
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.smart_charging_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.smart_charging_desc),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                PastelSwitch(
                    checked = isSmartChargingEnabled,
                    onCheckedChange = { repository.setSmartChargingEnabled(it) }
                )
            }

            HorizontalDivider(
                color = HealthCraftCardStroke,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Low Battery Defender
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.battery_defender_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.battery_defender_desc),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                PastelSwitch(
                    checked = isLowBatteryDefenderEnabled,
                    onCheckedChange = { repository.setLowBatteryDefenderEnabled(it) }
                )
            }

            HorizontalDivider(
                color = HealthCraftCardStroke,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Thermal Protection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.thermal_guard_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.thermal_guard_desc),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                PastelSwitch(
                    checked = isThermalProtectionEnabled,
                    onCheckedChange = { repository.setThermalProtectionEnabled(it) }
                )
            }

            HorizontalDivider(
                color = HealthCraftCardStroke,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // PSM Bypass
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.psm_bypass_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.psm_bypass_desc),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                PastelSwitch(
                    checked = isPsmBypassEnabled,
                    onCheckedChange = { repository.setPsmBypassEnabled(it) }
                )
            }
        }

        // 6. Tuning & Idle Timeout
        HealthCraftCard {
            Text(
                text = stringResource(R.string.section_tuning),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.idle_timeout_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = stringResource(R.string.idle_timeout_desc, idleTimeoutSeconds),
                fontSize = 12.sp,
                color = HealthCraftPrimary
            )
            Slider(
                value = idleTimeoutSeconds.toFloat(),
                onValueChange = { repository.setIdleTimeoutSeconds(it.toInt()) },
                valueRange = 1f..15f,
                steps = 14,
                enabled = operationMode == OperationMode.ADAPTIVE,
                colors = SliderDefaults.colors(
                    thumbColor = HealthCraftPrimary,
                    activeTrackColor = HealthCraftPrimary,
                    inactiveTrackColor = HealthCraftMintBorder
                )
            )
        }

        // 7. Permissions Status
        HealthCraftCard {
            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // WRITE_SECURE_SETTINGS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WRITE_SECURE_SETTINGS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (hasAdbPermission) stringResource(R.string.permission_secure_granted)
                        else stringResource(R.string.permission_secure_missing),
                        fontSize = 11.sp,
                        color = if (hasAdbPermission) HealthCraftPrimary else ColorWarning
                    )
                }
                Icon(
                    imageVector = if (hasAdbPermission) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = if (hasAdbPermission) HealthCraftPrimary else ColorWarning
                )
            }

            if (!hasAdbPermission) {
                Spacer(modifier = Modifier.height(8.dp))
                val adbCmd = stringResource(R.string.permission_secure_command)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", adbCmd))
                            Toast.makeText(context, context.getString(R.string.permission_command_copied), Toast.LENGTH_SHORT).show()
                        },
                    color = HealthCraftBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, HealthCraftCardStroke)
                ) {
                    Text(
                        text = adbCmd,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = HealthCraftPrimaryDark,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            HorizontalDivider(
                color = HealthCraftCardStroke,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Accessibility Service
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.permission_accessibility_action),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isServiceActive) stringResource(R.string.permission_accessibility_granted)
                        else stringResource(R.string.permission_accessibility_missing),
                        fontSize = 11.sp,
                        color = if (isServiceActive) HealthCraftPrimary else ColorWarning
                    )
                }
                if (!isServiceActive) {
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HealthCraftPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(stringResource(R.string.permission_accessibility_action), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = HealthCraftPrimary
                    )
                }
            }
        }

        // 7. HealthCraft Ecosystem Synergy Card
        HealthCraftCard(
            backgroundColor = HealthCraftMintContainer,
            borderColor = HealthCraftMintBorder
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HealthCraftPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Spa,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.healthcraft_promo_card_subtitle).uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HealthCraftPrimaryDark,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = stringResource(R.string.healthcraft_promo_card_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.healthcraft_promo_card_desc),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.healthcraft_website_url)))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HealthCraftPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.healthcraft_promo_btn),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(70.dp))
    }

    if (showAuditSheet) {
        AuditDetailsBottomSheet(
            controller = controller,
            repository = repository,
            onDismiss = { showAuditSheet = false }
        )
    }
}

/**
 * Isolated Composable for displaying current refresh rate.
 * Recomposes independently without causing the entire DashboardScreen to recompose.
 */
@Composable
fun LiveRefreshRateBadge(
    refreshRateFlow: StateFlow<Int>,
    modifier: Modifier = Modifier
) {
    val currentHz by refreshRateFlow.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = HealthCraftMintContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, HealthCraftMintBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.badge_current),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = HealthCraftPrimary
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$currentHz",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HealthCraftPrimaryDark
                )
                Text(
                    text = " " + stringResource(R.string.hz_unit),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HealthCraftPrimary
                )
            }
        }
    }
}

/**
 * Compact One UI Compatibility Audit Banner.
 * Clicking opens AuditDetailsBottomSheet with human-readable hardware info.
 */
@Composable
fun OneUiAuditBanner(
    compatScore: Int,
    compatLabel: String,
    onOpenAuditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onOpenAuditClick() },
        color = HealthCraftSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HealthCraftCardStroke),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HealthCraftMintContainer)
                        .border(1.dp, HealthCraftMintBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = HealthCraftPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.audit_banner_title),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MetricMintBg)
                                .border(1.dp, MetricMintBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$compatScore% $compatLabel",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MetricMintText
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.audit_banner_desc),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                onClick = onOpenAuditClick,
                shape = RoundedCornerShape(10.dp),
                color = HealthCraftMintContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, HealthCraftMintBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.audit_banner_action),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HealthCraftPrimaryDark
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = HealthCraftPrimaryDark,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Interactive Touch and Scroll Testing Zone Card.
 * Allows users to touch/swipe, shows dynamic Hz badge, live countdown timer, and progress bar.
 */
@Composable
fun TouchTestZoneCard(
    controller: RefreshRateController,
    repository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    val lastInteraction by AdaptiveCoordinator.lastInteractionTimestamp.collectAsStateWithLifecycle()
    val idleTimeoutMs by AdaptiveCoordinator.idleTimeoutDurationMs.collectAsStateWithLifecycle()
    val currentHz by controller.currentRefreshRateHz.collectAsStateWithLifecycle()
    val adaptiveState by AdaptiveCoordinator.adaptiveState.collectAsStateWithLifecycle()
    val operationMode by repository.operationMode.collectAsStateWithLifecycle()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(lastInteraction, idleTimeoutMs, adaptiveState, operationMode) {
        if (operationMode == OperationMode.ADAPTIVE || operationMode == OperationMode.ECO) {
            while (true) {
                now = System.currentTimeMillis()
                val elapsed = now - lastInteraction
                if (elapsed > idleTimeoutMs + 400L) {
                    break
                }
                kotlinx.coroutines.delay(50L)
            }
        }
    }

    val elapsed = (now - lastInteraction).coerceAtLeast(0L)
    val remainingMs = (idleTimeoutMs - elapsed).coerceAtLeast(0L)
    val isTimerActive = remainingMs > 0 && lastInteraction > 0 &&
        (operationMode == OperationMode.ADAPTIVE || operationMode == OperationMode.ECO) &&
        (adaptiveState == AdaptiveState.ACTIVE_TOUCH || currentHz >= 90)

    val progress = if (isTimerActive && idleTimeoutMs > 0) {
        (remainingMs.toFloat() / idleTimeoutMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val isHighHz = currentHz >= 90

    HealthCraftCard(
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.any { it.pressed }) {
                        AdaptiveCoordinator.notifyUserInteraction()
                    }
                }
            }
        }
    ) {
        // Header Row: Title & Hz Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.test_zone_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isHighHz) MetricMintBg else HealthCraftBg)
                    .border(
                        1.dp,
                        if (isHighHz) MetricMintBorder else HealthCraftCardStroke,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isHighHz) "⚡ $currentHz Hz" else "🔒 $currentHz Hz",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighHz) MetricMintText else TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = stringResource(R.string.test_zone_subtitle),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = HealthCraftPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.test_zone_desc),
            fontSize = 11.sp,
            color = TextSecondary,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Countdown Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = if (isTimerActive) HealthCraftPrimary else TextTertiary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            val statusText = when {
                operationMode == OperationMode.FIXED_120 -> stringResource(R.string.state_desc_fixed_120)
                operationMode == OperationMode.FIXED_60 -> stringResource(R.string.state_desc_fixed_60)
                isTimerActive -> stringResource(R.string.test_zone_countdown_label, remainingMs / 1000f)
                else -> stringResource(R.string.test_zone_idle_label)
            }
            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = if (isTimerActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isTimerActive) HealthCraftPrimaryDark else TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Animated Progress Bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = HealthCraftPrimary,
            trackColor = HealthCraftCardStroke
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Touch Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            color = if (isTimerActive) HealthCraftMintContainer else HealthCraftBg,
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isTimerActive) HealthCraftMintBorder else HealthCraftCardStroke
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isTimerActive) Icons.Rounded.Bolt else Icons.Rounded.TouchApp,
                        contentDescription = null,
                        tint = if (isTimerActive) HealthCraftPrimaryDark else TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTimerActive) {
                            stringResource(R.string.test_zone_active_hint)
                        } else {
                            stringResource(R.string.test_zone_touch_hint)
                        },
                        fontSize = 12.sp,
                        fontWeight = if (isTimerActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isTimerActive) HealthCraftPrimaryDark else TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Human-readable Audit Details Bottom Sheet.
 * Explains device hardware compatibility, display panel architecture, and automated tuning.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditDetailsBottomSheet(
    controller: RefreshRateController,
    repository: SettingsRepository,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isLtpo = remember { controller.isLtpoPanel() }
    val supportedRates = remember { controller.getSupportedRefreshRates() }
    val highTarget by repository.highHzTarget.collectAsStateWithLifecycle()
    val lowTarget by repository.lowHzTarget.collectAsStateWithLifecycle()
    val manufacturer = remember { Build.MANUFACTURER.replaceFirstChar { it.uppercase() } }
    val model = remember { Build.MODEL }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HealthCraftBg,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                color = HealthCraftCardStroke,
                shape = RoundedCornerShape(2.dp)
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(HealthCraftMintContainer)
                            .border(1.dp, HealthCraftMintBorder, RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = HealthCraftPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.audit_sheet_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.audit_sheet_subtitle),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 1. Device Info & Compatibility Card
            HealthCraftCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.audit_sheet_device_label).uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiary
                        )
                        Text(
                            text = "$manufacturer $model",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Android ${Build.VERSION.RELEASE} • One UI",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MetricMintBg)
                            .border(1.dp, MetricMintBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.audit_sheet_status_badge),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MetricMintText
                        )
                    }
                }
            }

            // 2. Matrix Architecture Card
            HealthCraftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Smartphone,
                        contentDescription = null,
                        tint = HealthCraftPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.audit_sheet_matrix_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isLtpo) "Samsung Dynamic AMOLED 2X (LTPO VRR)" else "Samsung Super AMOLED (Non-LTPO)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HealthCraftPrimaryDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.audit_sheet_matrix_desc),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(HealthCraftBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.about_hardware_modes_title),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = supportedRates.joinToString(" • ") { "$it Hz" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // 3. Automated Calibration Card
            HealthCraftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = HealthCraftPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.audit_sheet_auto_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MetricMintBg)
                            .border(1.dp, MetricMintBorder, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "High Target", fontSize = 10.sp, color = TextSecondary)
                            Text(text = "$highTarget Hz", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MetricMintText)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MetricLavenderBg)
                            .border(1.dp, MetricLavenderBorder, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Low Target", fontSize = 10.sp, color = TextSecondary)
                            Text(text = "$lowTarget Hz", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MetricLavenderText)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.audit_sheet_auto_desc),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )
            }

            // 4. Power Saving Mode Bypass Card
            HealthCraftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.BatteryChargingFull,
                        contentDescription = null,
                        tint = MetricAmberText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.audit_sheet_psm_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.audit_sheet_psm_desc),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )
            }

            // Close Button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = HealthCraftPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.audit_sheet_close_btn),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
