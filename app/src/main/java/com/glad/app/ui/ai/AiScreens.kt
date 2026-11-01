package com.glad.app.ui.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.glad.app.AppContainer
import com.glad.app.data.prefs.AiConfig
import com.glad.app.data.remote.ChatMessage
import com.glad.app.data.remote.safeApi
import com.glad.app.data.remote.userMessage
import com.glad.app.ui.Ic
import com.glad.app.ui.components.GladCard
import com.glad.app.ui.components.GladScreen
import com.glad.app.ui.components.StateMessage
import com.glad.app.ui.components.gladViewModel
import com.glad.app.ui.nav.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector

enum class AiTool(
    val id: String,
    val title: String,
    val blurb: String,
    val icon: ImageVector,
    val online: Boolean,
    val system: String,
    val inputHint: String
) {
    CHAT("chat", "Ask Glad", "Chat with a general assistant", Ic.Chat, true,
        "You are Glad, a friendly, concise assistant inside an Android app. Keep answers short and practical.", "Ask anything"),
    REWRITE("rewrite", "Rewrite", "Fix grammar and polish tone", Ic.Edit, true,
        "Rewrite the user's text so it is clear, correct and natural. Keep the meaning. Reply with only the rewritten text.", "Paste text to improve"),
    SUMMARIZE("summarize", "Summarize", "Short summary and key points", Ic.Document, true,
        "Summarize the user's text in 2-3 sentences, then list up to 4 key points as short bullets.", "Paste text to summarize"),
    TRANSLATE("translate", "Translate", "Any language to English, or choose", Ic.Translate, true,
        "Translate the user's text. If the message starts with 'To <language>:' use that target language, otherwise translate to English. Reply with only the translation.", "Text to translate"),
    IDEAS("ideas", "Idea spark", "Names, captions and plans", Ic.Bulb, true,
        "Give 6 creative, distinct ideas for the user's request as a short numbered list. No preamble.", "What do you need ideas for?"),
    COUNTER("counter", "Text counter", "Words, characters, reading time", Ic.Text, false, "", "Type or paste text");

    companion object {
        fun byId(id: String) = entries.firstOrNull { it.id == id }
    }
}

class AiViewModel(private val c: AppContainer) : ViewModel() {
    val config: StateFlow<AiConfig> = c.settings.aiConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiConfig())

    val messages = mutableStateListOf<ChatMessage>()
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun send(tool: AiTool, text: String, extra: String? = null) {
        val clean = text.trim()
        if (clean.isEmpty() || busy) return
        error = null
        val content = if (extra != null) "$extra: $clean" else clean
        messages += ChatMessage("user", clean)
        busy = true
        viewModelScope.launch {
            val history = if (tool == AiTool.CHAT) messages.takeLast(12) else listOf(ChatMessage("user", content))
            val payload = listOf(ChatMessage("system", tool.system)) + history
            safeApi { c.ai.chat(config.value, payload) }
                .onSuccess { messages += ChatMessage("assistant", it) }
                .onFailure { error = it.userMessage() }
            busy = false
        }
    }

    fun clear() {
        messages.clear()
        error = null
    }

    fun saveConfig(value: AiConfig) {
        viewModelScope.launch { c.settings.setAiConfig(value) }
    }
}

@Composable
fun AiHubScreen(onOpen: (String) -> Unit, onBack: () -> Unit) {
    val vm = gladViewModel { AiViewModel(it) }
    val config by vm.config.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    GladScreen(
        "AI Hub",
        subtitle = if (config.useCustom) "Using your own endpoint" else "Free public assistant, no account needed",
        onBack = onBack,
        actions = { IconButton(onClick = { showSettings = true }) { Icon(Ic.Settings, "AI settings") } }
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AiTool.entries.forEach { tool ->
                GladCard(Modifier.fillMaxWidth(), onClick = { onOpen(Routes.aiTool(tool.id)) }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) { Icon(tool.icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tool.title, style = MaterialTheme.typography.titleMedium)
                            Text(tool.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            if (tool.online) "Online" else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (tool.online) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Text(
                "AI answers can be wrong. Don't share private information. Your text is sent to the selected provider only when you tap send.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    if (showSettings) AiSettingsDialog(config, onDismiss = { showSettings = false }) {
        vm.saveConfig(it)
        showSettings = false
    }
}

@Composable
private fun AiSettingsDialog(config: AiConfig, onDismiss: () -> Unit, onSave: (AiConfig) -> Unit) {
    var custom by remember { mutableStateOf(config.useCustom) }
    var endpoint by remember { mutableStateOf(config.endpoint) }
    var key by remember { mutableStateOf(config.apiKey) }
    var model by remember { mutableStateOf(config.model) }
    val valid = !custom || endpoint.trim().startsWith("https://")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI provider") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Use my own provider", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Any OpenAI-compatible chat endpoint. Off = free public service.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(custom, { custom = it })
                }
                if (custom) {
                    OutlinedTextField(
                        endpoint, { endpoint = it }, label = { Text("Endpoint URL") },
                        placeholder = { Text("https://…/v1/chat/completions") },
                        isError = !valid, singleLine = true
                    )
                    OutlinedTextField(
                        key, { key = it }, label = { Text("API key (optional)") },
                        singleLine = true, visualTransformation = PasswordVisualTransformation()
                    )
                    OutlinedTextField(model, { model = it }, label = { Text("Model name") }, singleLine = true)
                    Text(
                        "Your key is stored only on this device and sent only to the endpoint above.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(AiConfig(custom, endpoint, key, model)) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private val languages = listOf("English", "Spanish", "French", "German", "Hindi", "Portuguese", "Japanese", "Arabic")

@Composable
fun AiToolScreen(toolId: String, onBack: () -> Unit) {
    val tool = AiTool.byId(toolId)
    if (tool == null) {
        GladScreen("AI Hub", onBack = onBack) { StateMessage(Ic.Warning, "Unknown tool", "This tool doesn't exist.") }
        return
    }
    if (tool == AiTool.COUNTER) {
        CounterScreen(onBack)
        return
    }

    val vm = gladViewModel(key = "ai_$toolId") { AiViewModel(it) }
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var language by rememberSaveable { mutableStateOf("English") }
    val listState = rememberLazyListState()

    LaunchedEffect(vm.messages.size, vm.busy) {
        if (vm.messages.isNotEmpty()) listState.animateScrollToItem(vm.messages.lastIndex)
    }

    GladScreen(
        tool.title,
        subtitle = tool.blurb,
        onBack = onBack,
        actions = {
            if (vm.messages.isNotEmpty()) IconButton(onClick = vm::clear) { Icon(Ic.Delete, "Clear conversation") }
        }
    ) {
        Column(Modifier.fillMaxSize().imePadding()) {
            if (tool == AiTool.TRANSLATE) {
                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(languages) { lang ->
                        FilterChip(selected = language == lang, onClick = { language = lang }, label = { Text(lang) })
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (vm.messages.isEmpty() && vm.error == null) {
                    StateMessage(tool.icon, tool.title, tool.blurb + ". Type below and tap send.")
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(vm.messages.toList()) { m -> Bubble(m, context) }
                        vm.error?.let { err ->
                            item {
                                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(tool.inputHint) },
                    maxLines = 6,
                    shape = RoundedCornerShape(22.dp)
                )
                Button(
                    enabled = input.isNotBlank() && !vm.busy,
                    onClick = {
                        val extra = if (tool == AiTool.TRANSLATE) "To $language" else null
                        vm.send(tool, input, extra)
                        input = ""
                    }
                ) { Icon(Ic.Send, "Send") }
            }
        }
    }
}

@Composable
private fun Bubble(m: ChatMessage, context: Context) {
    val mine = m.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .background(if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                .padding(14.dp)
        ) {
            SelectionContainer { Text(m.content, style = MaterialTheme.typography.bodyMedium) }
            if (!mine) {
                TextButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Glad", m.content))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Ic.Copy, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy")
                }
            }
        }
    }
}

@Composable
private fun CounterScreen(onBack: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val words = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    val sentences = text.split(Regex("[.!?]+")).count { it.isNotBlank() }
    val minutes = words / 200.0
    GladScreen("Text counter", subtitle = "Works fully offline", onBack = onBack) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                text, { text = it },
                modifier = Modifier.fillMaxWidth().height(220.dp),
                placeholder = { Text("Type or paste text") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountTile("Words", words.toString(), Modifier.weight(1f))
                CountTile("Characters", text.length.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountTile("Sentences", sentences.toString(), Modifier.weight(1f))
                CountTile(
                    "Reading time",
                    if (words == 0) "0 min" else if (minutes < 1) "<1 min" else "%.0f min".format(minutes),
                    Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { text = text.uppercase() }) { Text("UPPER") }
                TextButton(onClick = { text = text.lowercase() }) { Text("lower") }
                TextButton(onClick = { text = text.replace(Regex("\\s+"), " ").trim() }) { Text("Tidy spaces") }
                TextButton(onClick = { text = "" }) { Text("Clear") }
            }
        }
    }
}

@Composable
private fun CountTile(label: String, value: String, modifier: Modifier) {
    GladCard(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
