package com.healthcraft.adaptivehz.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthcraft.adaptivehz.R
import com.healthcraft.adaptivehz.data.ProfileMode
import com.healthcraft.adaptivehz.data.SettingsRepository
import com.healthcraft.adaptivehz.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppProfilesScreen(
    repository: SettingsRepository
) {
    val context = LocalContext.current
    val profiles by repository.appProfiles.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val list = packages.mapNotNull { appInfo ->
                try {
                    val name = pm.getApplicationLabel(appInfo).toString()
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    AppItem(
                        packageName = appInfo.packageName,
                        appName = name,
                        isSystem = isSystem
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedWith(compareBy({ it.isSystem }, { it.appName.lowercase() }))

            withContext(Dispatchers.Main) {
                installedApps = list
                isLoading = false
            }
        }
    }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) installedApps
        else {
            val q = searchQuery.lowercase()
            installedApps.filter {
                it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HealthCraftBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.profiles_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = stringResource(R.string.profiles_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Preset notice card (HealthCraft soft mint)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = HealthCraftMintContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, HealthCraftMintBorder)
        ) {
            Text(
                text = stringResource(R.string.video_apps_preset_notice),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = HealthCraftPrimaryDark,
                modifier = Modifier.padding(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_apps_hint), color = TextTertiary) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = HealthCraftPrimary) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HealthCraftPrimary,
                unfocusedBorderColor = HealthCraftCardStroke,
                focusedContainerColor = HealthCraftSurface,
                unfocusedContainerColor = HealthCraftSurface,
                cursorColor = HealthCraftPrimary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HealthCraftPrimary)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val currentMode = profiles[app.packageName] ?: repository.getProfileModeForApp(app.packageName)
                    AppProfileItemCard(
                        app = app,
                        currentMode = currentMode,
                        onModeSelected = { mode ->
                            repository.setAppProfileMode(app.packageName, mode)
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(70.dp))
                }
            }
        }
    }
}

@Composable
fun AppProfileItemCard(
    app: AppItem,
    currentMode: ProfileMode,
    onModeSelected: (ProfileMode) -> Unit
) {
    HealthCraftCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyAppIcon(
                packageName = app.packageName,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mode selector chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ProfileMode.entries.forEach { mode ->
                val selected = currentMode == mode
                val label = when (mode) {
                    ProfileMode.ADAPTIVE -> stringResource(R.string.mode_chip_adaptive)
                    ProfileMode.FORCE_60 -> stringResource(R.string.mode_chip_60)
                    ProfileMode.FORCE_120 -> stringResource(R.string.mode_chip_120)
                }

                FilterChip(
                    selected = selected,
                    onClick = { onModeSelected(mode) },
                    label = { Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HealthCraftMintContainer,
                        selectedLabelColor = HealthCraftPrimaryDark,
                        containerColor = HealthCraftBg,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = if (selected) HealthCraftPrimary else HealthCraftCardStroke
                    )
                )
            }
        }
    }
}

private val appIconCache = LruCache<String, ImageBitmap>(150)

/**
 * Loads app icon on demand for visible items with LRU in-memory caching
 * to prevent heavy PackageManager queries during fast LazyColumn scrolling.
 */
@Composable
fun LazyAppIcon(
    packageName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cached = remember(packageName) { appIconCache.get(packageName) }
    val iconBitmap by produceState<ImageBitmap?>(initialValue = cached, key1 = packageName) {
        if (cached == null) {
            value = withContext(Dispatchers.IO) {
                try {
                    val drawable = context.packageManager.getApplicationIcon(packageName)
                    val bmp = drawable.toBitmap(96, 96).asImageBitmap()
                    appIconCache.put(packageName, bmp)
                    bmp
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap!!,
            contentDescription = null,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(HealthCraftMintContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Apps,
                contentDescription = null,
                tint = HealthCraftPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

class AppItem(
    val packageName: String,
    val appName: String,
    val isSystem: Boolean
)
