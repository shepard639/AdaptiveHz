package com.antigravity.adaptivehz.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.antigravity.adaptivehz.R
import com.antigravity.adaptivehz.core.RefreshRateController
import com.antigravity.adaptivehz.data.OperationMode
import com.antigravity.adaptivehz.data.SettingsRepository
import com.antigravity.adaptivehz.service.AdaptiveCoordinator
import com.antigravity.adaptivehz.service.StatusBarIndicatorService
import com.antigravity.adaptivehz.ui.theme.*

class MainActivity : ComponentActivity() {

    private lateinit var controller: RefreshRateController
    private lateinit var repository: SettingsRepository

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            StatusBarIndicatorService.start(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        controller = RefreshRateController.getInstance(this)
        repository = SettingsRepository(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                StatusBarIndicatorService.start(this)
            }
        } else {
            StatusBarIndicatorService.start(this)
        }

        setContent {
            AdaptiveHzTheme {
                val isOnboardingCompleted by repository.isOnboardingCompleted.collectAsStateWithLifecycle()
                var showManualOnboarding by remember { mutableStateOf(false) }

                if (!isOnboardingCompleted || showManualOnboarding) {
                    OnboardingScreen(
                        settingsRepository = repository,
                        refreshRateController = controller,
                        onFinished = { showManualOnboarding = false }
                    )
                } else {
                    MainAppScaffold(
                        controller = controller,
                        repository = repository,
                        onOpenOnboarding = { showManualOnboarding = true }
                    )
                }
            }
        }
    }

    private var lastActivityTouchTime: Long = 0L

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev != null) {
            when (ev.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN,
                android.view.MotionEvent.ACTION_UP -> {
                    val mode = repository.operationMode.value
                    if (mode == OperationMode.ADAPTIVE || mode == OperationMode.ECO) {
                        AdaptiveCoordinator.notifyUserInteraction()
                    }
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val now = System.currentTimeMillis()
                    if (now - lastActivityTouchTime >= 150L) {
                        lastActivityTouchTime = now
                        val mode = repository.operationMode.value
                        if (mode == OperationMode.ADAPTIVE || mode == OperationMode.ECO) {
                            AdaptiveCoordinator.notifyUserInteraction()
                        }
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}

enum class NavigationTab {
    DASHBOARD,
    PROFILES,
    ABOUT
}

@Composable
fun MainAppScaffold(
    controller: RefreshRateController,
    repository: SettingsRepository,
    onOpenOnboarding: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }

    Scaffold(
        containerColor = HealthCraftBg,
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = HealthCraftSurface,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, HealthCraftCardStroke)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Dashboard
                    val isDashboard = selectedTab == NavigationTab.DASHBOARD
                    Column(
                        modifier = Modifier
                            .semantics {
                                role = Role.Tab
                                selected = isDashboard
                            }
                            .clickable { selectedTab = NavigationTab.DASHBOARD }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = null,
                            tint = if (isDashboard) HealthCraftPrimary else TextTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.nav_dashboard),
                            fontSize = 11.sp,
                            fontWeight = if (isDashboard) FontWeight.Bold else FontWeight.Normal,
                            color = if (isDashboard) HealthCraftPrimary else TextSecondary
                        )
                    }

                    // 2. Apps Profiles
                    val isProfiles = selectedTab == NavigationTab.PROFILES
                    Column(
                        modifier = Modifier
                            .semantics {
                                role = Role.Tab
                                selected = isProfiles
                            }
                            .clickable { selectedTab = NavigationTab.PROFILES }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Apps,
                            contentDescription = null,
                            tint = if (isProfiles) HealthCraftPrimary else TextTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.nav_profiles),
                            fontSize = 11.sp,
                            fontWeight = if (isProfiles) FontWeight.Bold else FontWeight.Normal,
                            color = if (isProfiles) HealthCraftPrimary else TextSecondary
                        )
                    }

                    // 3. About Device / Info
                    val isAbout = selectedTab == NavigationTab.ABOUT
                    Column(
                        modifier = Modifier
                            .semantics {
                                role = Role.Tab
                                selected = isAbout
                            }
                            .clickable { selectedTab = NavigationTab.ABOUT }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = if (isAbout) HealthCraftPrimary else TextTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.nav_about),
                            fontSize = 11.sp,
                            fontWeight = if (isAbout) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAbout) HealthCraftPrimary else TextSecondary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                NavigationTab.DASHBOARD -> DashboardScreen(
                    controller = controller,
                    repository = repository
                )
                NavigationTab.PROFILES -> AppProfilesScreen(
                    repository = repository
                )
                NavigationTab.ABOUT -> AboutScreen(
                    controller = controller,
                    onOpenOnboarding = onOpenOnboarding
                )
            }
        }
    }
}

@Composable
fun AboutScreen(
    controller: RefreshRateController,
    onOpenOnboarding: () -> Unit
) {
    val supportedRates = remember { controller.getSupportedRefreshRates() }

    val manufacturer = remember { Build.MANUFACTURER.replaceFirstChar { it.uppercase() } }
    val model = remember { Build.MODEL }
    val displayDeviceName = remember {
        if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
    }
    val hardwareDetail = remember {
        "${Build.DEVICE} • ${Build.HARDWARE} (Android ${Build.VERSION.RELEASE})"
    }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HealthCraftBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.about_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = stringResource(R.string.about_subtitle),
            fontSize = 13.sp,
            color = TextSecondary
        )

        HealthCraftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HealthCraftMintContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Smartphone,
                        contentDescription = null,
                        tint = HealthCraftPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = displayDeviceName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = hardwareDetail,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            HorizontalDivider(
                color = HealthCraftCardStroke,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Text(
                text = stringResource(R.string.about_hardware_modes_title),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            supportedRates.forEach { hz ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.about_mode_item, hz),
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    StatusBadge(
                        text = if (hz >= 120) stringResource(R.string.about_fps_mode_1) else stringResource(R.string.about_fps_mode_2),
                        color = if (hz >= 120) HealthCraftPrimary else MetricAmberText,
                        backgroundColor = if (hz >= 120) HealthCraftMintContainer else MetricAmberBg
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MetricMintBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, MetricMintBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.about_hardware_note_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HealthCraftPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.about_hardware_note_text),
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action to re-launch Onboarding Wizard
            OutlinedButton(
                onClick = onOpenOnboarding,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HealthCraftCardStroke),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = HealthCraftPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Мастер настройки первого запуска",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Updates & GitHub Releases Card
        HealthCraftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(MetricMintBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        tint = HealthCraftPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.updates_card_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.updates_card_version, "v1.0.0"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.updates_card_desc),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.github_releases_url)))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, e.message, android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HealthCraftMintBorder),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = HealthCraftMintContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudDownload,
                    contentDescription = null,
                    tint = HealthCraftPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.updates_btn_check),
                    color = HealthCraftPrimaryDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // HealthCraft Ecosystem Card
        HealthCraftCard(
            backgroundColor = HealthCraftMintContainer,
            borderColor = HealthCraftMintBorder
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(HealthCraftPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Spa,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.healthcraft_about_ecosystem_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "healthcraft.pro",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HealthCraftPrimaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.healthcraft_about_ecosystem_desc),
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
                        android.widget.Toast.makeText(context, e.message, android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HealthCraftPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.healthcraft_promo_btn),
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
