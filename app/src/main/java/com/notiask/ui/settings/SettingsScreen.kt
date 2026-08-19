package com.notiask.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notiask.AppContainer
import com.notiask.data.AiProfile
import com.notiask.data.ProviderKind
import com.notiask.ui.backdrop.ModelMark
import com.notiask.ui.glass.GlassButton
import com.notiask.ui.glass.GlassCard
import com.notiask.ui.glass.GlassIconButton
import com.notiask.ui.glass.GlassStatusChip
import com.notiask.ui.glass.notiGlass
import com.notiask.ui.theme.NotiAccent
import com.notiask.ui.theme.NotiCanvas
import com.notiask.ui.theme.NotiCanvasEnd
import com.notiask.ui.theme.NotiInk
import com.notiask.ui.theme.NotiInkMuted
import com.notiask.ui.theme.NotiSuccess
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    container: AppContainer,
    notificationsEnabled: Boolean,
    onEnable: () -> Unit,
    onBatterySettings: () -> Unit,
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
    var tipsExpandedOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val tipsExpanded = tipsExpandedOverride ?: profiles.isEmpty()
    val chevronRotation by animateFloatAsState(
        targetValue = if (tipsExpanded) 180f else 0f,
        animationSpec = tipsToggleSpec(),
        label = "tipsChevron",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NotiCanvas, NotiCanvasEnd))),
    ) {
        Scaffold(containerColor = Color.Transparent) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.height(8.dp))
                GlassCard {
                    Text("NotiAsk", style = MaterialTheme.typography.headlineSmall)
                    Text("在通知栏完成提问与阅读", style = MaterialTheme.typography.bodyMedium, color = NotiInkMuted)
                }

                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = if (notificationsEnabled) Icons.Filled.NotificationsActive else Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = if (notificationsEnabled) NotiSuccess else NotiAccent,
                        )
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text("通知栏问答", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (notificationsEnabled) "前台服务已在运行" else "保存配置后即可启用",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        GlassStatusChip(
                            text = if (notificationsEnabled) "运行中" else "未开启",
                            tint = if (notificationsEnabled) NotiSuccess else NotiInkMuted,
                        )
                    }
                    GlassButton(
                        onClick = {
                            if (notificationsEnabled) showAlreadyEnabled = true
                            else onEnable()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        prominent = !notificationsEnabled,
                    ) {
                        Text(if (notificationsEnabled) "已启用" else "启用通知栏问答")
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.35f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onBatterySettings)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.BatterySaver, contentDescription = null, tint = NotiAccent)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text("电池优化", style = MaterialTheme.typography.titleMedium)
                            Text("可选，提升部分机型后台稳定性", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = "打开电池优化设置", tint = NotiInkMuted)
                    }
                }

                GlassCard(
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tipsExpandedOverride = !tipsExpanded }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = NotiAccent)
                            Text("如何使用", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp).weight(1f))
                            Icon(
                                imageVector = Icons.Filled.ExpandMore,
                                contentDescription = if (tipsExpanded) "收起说明" else "展开说明",
                                tint = NotiInkMuted,
                                modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
                            )
                        }
                        AnimatedVisibility(
                            visible = tipsExpanded,
                            enter = expandVertically(
                                animationSpec = tipsToggleSpec(),
                                expandFrom = Alignment.Top,
                            ) + fadeIn(animationSpec = tipsToggleSpec()),
                            exit = shrinkVertically(
                                animationSpec = tipsToggleSpec(),
                                shrinkTowards = Alignment.Top,
                            ) + fadeOut(animationSpec = tipsToggleSpec()),
                        ) {
                            Column(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TipLine("1", "添加并保存一组 AI 配置")
                                TipLine("2", "启用通知栏问答，授予通知权限")
                                TipLine("3", "下拉系统通知栏，展开 NotiAsk 即可提问或截屏搜索")
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    Text("AI 配置", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    GlassButton(
                        onClick = { editing = null; showEditor = true },
                        prominent = true,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("添加", modifier = Modifier.padding(start = 4.dp))
                    }
                }
                if (orderedProfiles.isEmpty()) {
                    GlassCard {
                        Text("尚无配置", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "可添加 OpenAI、Claude、通义、DeepSeek、Kimi 或任意 OpenAI 兼容服务。",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                orderedProfiles.forEachIndexed { index, profile ->
                    key(profile.id) {
                        ProfileRow(
                            modifier = Modifier
                                .zIndex(if (profile.id == defaultId) 1f else 0f)
                                .animatePlacement(listIndex = index),
                            profile = profile,
                            isDefault = profile.id == defaultId,
                            onSelect = { container.profiles.selectDefault(profile.id) },
                            onEdit = { editing = profile; showEditor = true },
                            onDelete = { confirmDelete = profile },
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
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
            },
        )
    }
    if (showAlreadyEnabled) {
        GlassAlertDialog(
            onDismissRequest = { showAlreadyEnabled = false },
            title = "已启用",
            text = "通知栏问答已启用。下拉展开 NotiAsk 通知即可提问或截屏搜索。",
            confirmText = "确定",
            onConfirm = { showAlreadyEnabled = false },
        )
    }
    confirmDelete?.let { profile ->
        GlassAlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = "删除配置？",
            text = "将删除 ${profile.name.ifBlank { profile.provider.displayName }} 及其加密保存的 API Key。",
            confirmText = "删除",
            onConfirm = { container.profiles.delete(profile.id); confirmDelete = null },
            dismissText = "取消",
            onDismissButton = { confirmDelete = null },
        )
    }
}

@Composable
private fun TipLine(index: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        GlassStatusChip(text = index, tint = NotiAccent)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 10.dp, top = 2.dp),
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
    modifier: Modifier = Modifier,
) {
    val mark = ModelMark.forProvider(profile.provider)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .notiGlass(RoundedCornerShape(22.dp))
            .clickable(onClick = onEdit)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(mark.iconRes),
            contentDescription = profile.provider.displayName,
            tint = Color.Unspecified,
            modifier = Modifier.padding(end = 12.dp).size(40.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(profile.name.ifBlank { profile.provider.displayName }, style = MaterialTheme.typography.titleMedium)
            Text("${profile.provider.displayName} · ${profile.model}", style = MaterialTheme.typography.bodySmall)
        }
        Box(contentAlignment = Alignment.CenterEnd) {
            Text(
                "当前使用",
                color = NotiSuccess,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.graphicsLayer { alpha = if (isDefault) 1f else 0f },
            )
            GlassButton(
                onClick = onSelect,
                enabled = !isDefault,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.graphicsLayer { alpha = if (isDefault) 0f else 1f },
            ) { Text("使用该模型") }
        }
        GlassIconButton(onClick = onDelete, contentDescription = "删除配置") {
            Icon(Icons.Default.Delete, contentDescription = null)
        }
    }
}

private val placementSpec = tween<IntOffset>(durationMillis = 280, easing = FastOutSlowInEasing)

private fun <T> tipsToggleSpec() = tween<T>(durationMillis = 280, easing = FastOutSlowInEasing)

private fun Modifier.animatePlacement(
    listIndex: Int,
    animationSpec: FiniteAnimationSpec<IntOffset> = placementSpec,
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val translation = remember { Animatable(IntOffset.Zero, IntOffset.VectorConverter) }
    val lastLayoutPos = remember { arrayOfNulls<IntOffset>(1) }
    val lastListIndex = remember { mutableStateOf(listIndex) }
    val shouldAnimatePlacement = lastListIndex.value != listIndex
    lastListIndex.value = listIndex
    onPlaced { coordinates ->
        val newPos = coordinates.positionInParent().round()
        val previous = lastLayoutPos[0]
        lastLayoutPos[0] = newPos
        if (previous == null || !shouldAnimatePlacement) return@onPlaced
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
private fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = null,
    onDismissButton: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.White.copy(alpha = 0.78f),
        shape = RoundedCornerShape(28.dp),
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            GlassButton(onClick = onConfirm, prominent = true, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                Text(confirmText)
            }
        },
        dismissButton = if (dismissText != null && onDismissButton != null) {
            {
                GlassButton(onClick = onDismissButton, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(dismissText)
                }
            }
        } else {
            null
        },
    )
}

@Composable
private fun ProfileEditor(
    initial: AiProfile?,
    existingKey: String,
    onDismiss: () -> Unit,
    onSave: (AiProfile, String, Boolean) -> Unit,
) {
    var provider by remember(initial) { mutableStateOf(initial?.provider ?: ProviderKind.OPENAI) }
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var apiKey by remember(initial) { mutableStateOf(existingKey) }
    var baseUrl by remember(initial) { mutableStateOf(initial?.baseUrl ?: provider.defaultBaseUrl) }
    var model by remember(initial) { mutableStateOf(initial?.model ?: provider.defaultModel) }
    var expanded by remember { mutableStateOf(false) }
    var makeDefault by remember { mutableStateOf(initial == null) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White.copy(alpha = 0.38f),
        unfocusedContainerColor = Color.White.copy(alpha = 0.22f),
        focusedBorderColor = Color.White.copy(alpha = 0.82f),
        unfocusedBorderColor = Color.White.copy(alpha = 0.42f),
        focusedLabelColor = NotiInk,
        unfocusedLabelColor = NotiInkMuted,
        cursorColor = NotiAccent,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White.copy(alpha = 0.78f),
        shape = RoundedCornerShape(28.dp),
        title = { Text(if (initial == null) "添加 AI 配置" else "编辑 AI 配置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box {
                    GlassButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        val mark = ModelMark.forProvider(provider)
                        Icon(painterResource(mark.iconRes), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(22.dp))
                        Text("服务商：${provider.displayName}", modifier = Modifier.padding(start = 8.dp))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        ProviderKind.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painterResource(ModelMark.forProvider(option).iconRes),
                                            contentDescription = null,
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(22.dp),
                                        )
                                        Text(option.displayName, modifier = Modifier.padding(start = 8.dp))
                                    }
                                },
                                onClick = {
                                    provider = option
                                    baseUrl = option.defaultBaseUrl
                                    model = option.defaultModel
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text("配置名称（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors, shape = RoundedCornerShape(16.dp))
                OutlinedTextField(apiKey, { apiKey = it }, label = { Text("API Key") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors, shape = RoundedCornerShape(16.dp))
                OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors, shape = RoundedCornerShape(16.dp))
                OutlinedTextField(model, { model = it }, label = { Text("模型名称") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors, shape = RoundedCornerShape(16.dp))
                GlassButton(onClick = { makeDefault = !makeDefault }, modifier = Modifier.fillMaxWidth(), prominent = makeDefault) {
                    Text(if (makeDefault) "保存后使用该模型" else "保存但不切换当前模型")
                }
            }
        },
        confirmButton = {
            GlassButton(
                onClick = {
                    onSave(
                        AiProfile(
                            id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name,
                            provider = provider,
                            baseUrl = baseUrl,
                            model = model,
                        ),
                        apiKey,
                        makeDefault,
                    )
                },
                prominent = true,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) { Text("保存") }
        },
        dismissButton = {
            GlassButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) { Text("取消") }
        },
    )
}
