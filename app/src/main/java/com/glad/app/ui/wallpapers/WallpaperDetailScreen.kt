package com.glad.app.ui.wallpapers

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glad.app.container
import com.glad.app.data.local.FavoriteWallpaper
import com.glad.app.data.remote.PicsumApi
import com.glad.app.data.remote.safeApi
import com.glad.app.data.remote.userMessage
import com.glad.app.ui.Ic
import com.glad.app.ui.components.RemoteImage
import com.glad.app.ui.components.StateMessage
import kotlinx.coroutines.launch

@Composable
fun WallpaperDetailScreen(key: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val item = remember(key) { WallpaperItem.fromKey(key) }
    if (item == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
            StateMessage(Ic.Warning, "Wallpaper not found", "This wallpaper is no longer available.", actionLabel = "Go back", onAction = onBack)
        }
        return
    }

    val dao = remember { context.container.db.wallpaperDao() }
    val favoriteFlow = remember(key) { dao.observeIsFavorite(key) }
    val isFavorite by favoriteFlow.collectAsStateWithLifecycle(initialValue = 0)
    val actions = remember { WallpaperActions(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var showTargets by remember { mutableStateOf(false) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    fun runTask(block: suspend () -> String) {
        if (busy) return
        busy = true
        scope.launch {
            val message = safeApi { block() }.fold({ it }, { "Failed: ${it.userMessage()}" })
            busy = false
            toast(message)
        }
    }

    fun save() = runTask {
        actions.saveToGallery(item)
        "Saved to Pictures/Glad"
    }

    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) save() else toast("Storage permission is needed to save images on this Android version.")
    }

    fun requestSave() {
        val needsPermission = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else save()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (item.kind == "g") {
            val spec = GradientCatalog.items[item.ref.toIntOrNull()?.coerceIn(GradientCatalog.items.indices) ?: 0]
            val image = remember(key) { GradientCatalog.render(spec, 540, 960).asImageBitmap() }
            Image(image, item.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            RemoteImage(PicsumApi.previewUrl(item.ref), Modifier.fillMaxSize(), item.title)
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(280.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
        )

        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CircleButton(Ic.Back, "Back", onClick = onBack)
            CircleButton(
                icon = if (isFavorite > 0) Ic.Favorite else Ic.FavoriteBorder,
                description = if (isFavorite > 0) "Remove from saved" else "Save for later",
                tint = if (isFavorite > 0) Color(0xFFFF6B7A) else Color.White
            ) {
                scope.launch {
                    if (isFavorite > 0) dao.remove(key)
                    else dao.add(FavoriteWallpaper(key, item.title, System.currentTimeMillis()))
                }
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(item.title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { showTargets = true }, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Icon(Ic.Wallpaper, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Set as wallpaper")
                }
                OutlinedButton(onClick = { requestSave() }, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Icon(Ic.Download, null, Modifier.size(18.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Save", color = Color.White)
                }
            }
        }
    }

    if (showTargets) {
        AlertDialog(
            onDismissRequest = { showTargets = false },
            title = { Text("Set wallpaper on") },
            text = {
                Column {
                    WallpaperTarget.entries.forEach { target ->
                        Text(
                            target.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    showTargets = false
                                    runTask {
                                        actions.apply(item, target)
                                        "Wallpaper set on ${target.label.lowercase()}"
                                    }
                                }
                                .padding(vertical = 14.dp, horizontal = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showTargets = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) { Icon(icon, description, tint = tint) }
    }
}
