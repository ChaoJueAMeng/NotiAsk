package com.notiask

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notiask.data.AiProfile
import com.notiask.data.ProviderKind
import com.notiask.notification.QuestionService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var notificationsEnabled by mutableStateOf(false)

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsEnabled = granted
        if (granted) QuestionService.start(this)
        else Toast.makeText(this, "未授权通知；前台服务通知不会显示在通知栏", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationsEnabled = notificationsAllowed()
        setContent {
            MaterialTheme {
                SettingsScreen(
                    container = appContainer(),
                    notificationsEnabled = notificationsEnabled,
                    onEnable = { enableNotificationAssistant() },
                    onBatterySettings = { openBatterySettings() }
                )
            }
        }
        if (appContainer().profiles.defaultProfile() != null && notificationsAllowed()) {
            QuestionService.start(this)
        }
    }

    override fun onResume() {
        super.onResume()
        notificationsEnabled = notificationsAllowed()
        // 误滑常驻通知后，回到设置页时再走 startForeground 挂回（DeleteIntent 为主路径）。
        if (appContainer().profiles.defaultProfile() != null && notificationsAllowed()) {
            QuestionService.start(this)
        }
    }

    private fun enableNotificationAssistant() {
        if (!notificationsAllowed()) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else QuestionService.start(this)
    }

    private fun notificationsAllowed() = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openBatterySettings() = startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    container: AppContainer,
    notificationsEnabled: Boolean,
    onEnable: () -> Unit,
    onBatterySettings: () -> Unit
) {
    val context = LocalContext.current
    val profiles by container.profiles.profiles.collectAsStateWithLifecycle()
    val defaultId by container.profiles.defaultId.collectAsStateWithLifecycle()
    val orderedProfiles = remember(profiles, defaultId) {
        if (defaultId == null) profiles
        else profiles.sortedByDescending { it.id == defaultId }
    }
    var editing by remember { mutableStateOf<AiProfile?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<AiProfile?>(null) }
    var showAlreadyEnabled by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("NotiAsk 设置") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("通知栏 AI 问答", style = MaterialTheme.typography.headlineSmall)
            Text("保存配置后启用服务。下拉系统通知栏，展开 NotiAsk 可直接输入问题，或点「截屏搜索」；截屏后会在通知栏提示已截图，可继续输入问题。")
            Button(
                onClick = {
                    if (notificationsEnabled) showAlreadyEnabled = true
                    else onEnable()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (notificationsEnabled) "已启用" else "启用通知栏问答") }
            OutlinedButton(onClick = onBatterySettings, modifier = Modifier.fillMaxWidth()) { Text("查看电池优化设置（可选）") }
            Text("为保证不同厂商系统的稳定性，可在系统设置中允许 NotiAsk 不受电池优化限制。", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("AI 配置", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Button(onClick = { editing = null; showEditor = true }) { Text("添加") }
            }
            if (orderedProfiles.isEmpty()) {
                Text("尚无配置。请添加 OpenAI、Claude、通义、DeepSeek、Kimi 或任意 OpenAI 兼容服务。")
            }
            orderedProfiles.forEach { profile ->
                key(profile.id) {
                    ProfileRow(
                        modifier = Modifier
                            .zIndex(if (profile.id == defaultId) 1f else 0f)
                            .animatePlacement(),
                        profile = profile,
                        isDefault = profile.id == defaultId,
                        onSelect = { container.profiles.selectDefault(profile.id) },
                        onEdit = { editing = profile; showEditor = true },
                        onDelete = { confirmDelete = profile }
                    )
                }
            }
        }
    }
    if (showEditor) {
        ProfileEditor(
            initial = editing,
            existingKey = editing?.let { container.profiles.apiKeyFor(it.id) }.orEmpty(),
            onDismiss = { showEditor = false },
            onSave = { profile, key, makeDefault ->
                runCatching { container.profiles.save(profile, key, makeDefault) }
                    .onSuccess { showEditor = false; Toast.makeText(context, "配置已加密保存", Toast.LENGTH_SHORT).show() }
                    .onFailure { Toast.makeText(context, it.message ?: "无法保存配置", Toast.LENGTH_LONG).show() }
            }
        )
    }
    if (showAlreadyEnabled) {
        AlertDialog(
            onDismissRequest = { showAlreadyEnabled = false },
            title = { Text("已启用") },
            text = { Text("通知栏问答已启用。下拉展开 NotiAsk 通知即可提问或截屏搜索。") },
            confirmButton = { Button(onClick = { showAlreadyEnabled = false }) { Text("确定") } }
        )
    }
    confirmDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("删除配置？") },
            text = { Text("将删除 ${profile.name} 及其加密保存的 API Key。") },
            confirmButton = { Button(onClick = { container.profiles.delete(profile.id); confirmDelete = null }) { Text("删除") } },
            dismissButton = { OutlinedButton(onClick = { confirmDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun ProfileRow(
    profile: AiProfile,
    isDefault: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onEdit)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(profile.name, style = MaterialTheme.typography.titleMedium)
                Text("${profile.provider.displayName} · ${profile.model}", style = MaterialTheme.typography.bodySmall)
            }
            Box(contentAlignment = Alignment.CenterEnd) {
                Text(
                    "当前使用",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.graphicsLayer { alpha = if (isDefault) 1f else 0f }
                )
                OutlinedButton(
                    onClick = onSelect,
                    enabled = !isDefault,
                    modifier = Modifier.graphicsLayer { alpha = if (isDefault) 0f else 1f }
                ) { Text("使用该模型") }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "删除配置") }
        }
    }
}

private val placementSpec = tween<IntOffset>(durationMillis = 280, easing = FastOutSlowInEasing)

private fun Modifier.animatePlacement(
    animationSpec: FiniteAnimationSpec<IntOffset> = placementSpec
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val translation = remember { Animatable(IntOffset.Zero, IntOffset.VectorConverter) }
    val lastLayoutPos = remember { arrayOfNulls<IntOffset>(1) }
    onPlaced { coordinates ->
        val newPos = coordinates.positionInParent().round()
        val previous = lastLayoutPos[0]
        lastLayoutPos[0] = newPos
        if (previous == null) return@onPlaced
        val layoutDelta = newPos - previous
        if (layoutDelta == IntOffset.Zero) return@onPlaced
        val from = translation.value - layoutDelta
        scope.launch {
            translation.snapTo(from)
            translation.animateTo(IntOffset.Zero, animationSpec)
        }
    }.offset { translation.value }
}

@Composable
private fun ProfileEditor(initial: AiProfile?, existingKey: String, onDismiss: () -> Unit, onSave: (AiProfile, String, Boolean) -> Unit) {
    var provider by remember(initial) { mutableStateOf(initial?.provider ?: ProviderKind.OPENAI) }
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var apiKey by remember(initial) { mutableStateOf(existingKey) }
    var baseUrl by remember(initial) { mutableStateOf(initial?.baseUrl ?: provider.defaultBaseUrl) }
    var model by remember(initial) { mutableStateOf(initial?.model ?: provider.defaultModel) }
    var expanded by remember { mutableStateOf(false) }
    var makeDefault by remember { mutableStateOf(initial == null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "添加 AI 配置" else "编辑 AI 配置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("服务商：${provider.displayName}") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ProviderKind.entries.forEach { option ->
                        DropdownMenuItem(text = { Text(option.displayName) }, onClick = {
                            provider = option; baseUrl = option.defaultBaseUrl; model = option.defaultModel; expanded = false
                        })
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text("配置名称（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(apiKey, { apiKey = it }, label = { Text("API Key") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(model, { model = it }, label = { Text("模型名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { makeDefault = !makeDefault }, modifier = Modifier.fillMaxWidth()) { Text(if (makeDefault) "✓ 保存后使用该模型" else "保存后使用该模型") }
            }
        },
        confirmButton = { Button(onClick = { onSave(AiProfile(id = initial?.id ?: java.util.UUID.randomUUID().toString(), name = name, provider = provider, baseUrl = baseUrl, model = model), apiKey, makeDefault) }) { Text("保存") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("取消") } }
    )
}
