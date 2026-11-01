package com.glad.app.ui.wallpapers

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.glad.app.data.remote.Http
import com.glad.app.data.remote.PicsumApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** key format: "p-<picsumId>" for online photos, "g-<index>" for built-in gradients. */
data class WallpaperItem(val key: String, val title: String) {
    val kind: String get() = key.substringBefore('-')
    val ref: String get() = key.substringAfter('-')

    companion object {
        fun online(id: String, author: String) = WallpaperItem("p-$id", "Photo by $author")
        fun gradient(index: Int) = WallpaperItem("g-$index", GradientCatalog.items[index].name)

        fun fromKey(key: String, title: String? = null): WallpaperItem? = when (key.substringBefore('-')) {
            "g" -> key.substringAfter('-').toIntOrNull()
                ?.takeIf { it in GradientCatalog.items.indices }
                ?.let { gradient(it) }
            "p" -> key.substringAfter('-').takeIf { it.isNotBlank() }
                ?.let { WallpaperItem(key, title ?: "Photo #$it") }
            else -> null
        }
    }
}

class GradientSpec(val name: String, val colors: IntArray, val angle: Float)

private fun hex(v: Long): Int = v.toInt()

/** Offline wallpapers generated on-device, so the section is useful with no connection. */
object GradientCatalog {
    val items: List<GradientSpec> = listOf(
        GradientSpec("Afterglow", intArrayOf(hex(0xFFFF9A5A), hex(0xFFFF4F81), hex(0xFF3A1C71)), 160f),
        GradientSpec("Lagoon Night", intArrayOf(hex(0xFF0F2027), hex(0xFF203A43), hex(0xFF19C3B1)), 200f),
        GradientSpec("Orchid Smoke", intArrayOf(hex(0xFF1A0B2E), hex(0xFF6A2C91), hex(0xFFE0A3FF)), 150f),
        GradientSpec("Citrus Pop", intArrayOf(hex(0xFFFFE259), hex(0xFFFFA751), hex(0xFFFF5E62)), 135f),
        GradientSpec("Deep Sea", intArrayOf(hex(0xFF020C1B), hex(0xFF0B3D6B), hex(0xFF4DA3FF)), 180f),
        GradientSpec("Rose Quartz", intArrayOf(hex(0xFF2B0B1E), hex(0xFFB33A6B), hex(0xFFFFB6C9)), 165f),
        GradientSpec("Mint Aurora", intArrayOf(hex(0xFF04201A), hex(0xFF1B7F63), hex(0xFFA6FFCB)), 210f),
        GradientSpec("Ember", intArrayOf(hex(0xFF1B0A06), hex(0xFF8A2B0F), hex(0xFFFFB84D)), 170f),
        GradientSpec("Midnight Bloom", intArrayOf(hex(0xFF0D0A14), hex(0xFF3B1F6B), hex(0xFFFF5FA2)), 145f),
        GradientSpec("Glacier", intArrayOf(hex(0xFF0B1F33), hex(0xFF5AA7C8), hex(0xFFEAF7FF)), 190f),
        GradientSpec("Dusk Road", intArrayOf(hex(0xFF2D1B4E), hex(0xFFD2517C), hex(0xFFFFC371)), 155f),
        GradientSpec("Moss", intArrayOf(hex(0xFF0A1A0F), hex(0xFF2E6B3A), hex(0xFFC6F26B)), 200f),
        GradientSpec("Nebula", intArrayOf(hex(0xFF050510), hex(0xFF2A1A6E), hex(0xFF8F7CFF), hex(0xFFFF6B7A)), 160f),
        GradientSpec("Sunrise", intArrayOf(hex(0xFF4A1A5C), hex(0xFFFF6B7A), hex(0xFFFFB84D)), 175f),
        GradientSpec("Graphite", intArrayOf(hex(0xFF0B0B0F), hex(0xFF23232D), hex(0xFF4C4C5E)), 180f),
        GradientSpec("Peach Fizz", intArrayOf(hex(0xFFFF9A9E), hex(0xFFFAD0C4), hex(0xFFFFF1E6)), 150f)
    )

    fun render(spec: GradientSpec, w: Int, h: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rad = Math.toRadians(spec.angle.toDouble())
        val dx = sin(rad).toFloat()
        val dy = -cos(rad).toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val half = (abs(w * dx) + abs(h * dy)) / 2f
        val base = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(cx - dx * half, cy - dy * half, cx + dx * half, cy + dy * half, spec.colors, null, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), base)
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.25f, h * 0.18f, max(w, h) * 0.75f,
                intArrayOf(0x44FFFFFF, 0x00FFFFFF), null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), glow)
        return bitmap
    }
}

enum class WallpaperTarget(val label: String) {
    HOME("Home screen"),
    LOCK("Lock screen"),
    BOTH("Home and lock screen")
}

class WallpaperActions(private val context: Context) {
    private val metrics = context.resources.displayMetrics
    private val width = metrics.widthPixels.coerceIn(720, 1440)
    private val height = metrics.heightPixels.coerceIn(1280, 3200)

    suspend fun bitmap(item: WallpaperItem): Bitmap = withContext(Dispatchers.IO) {
        if (item.kind == "g") {
            val spec = item.ref.toIntOrNull()?.let { GradientCatalog.items.getOrNull(it) }
                ?: throw IOException("Unknown gradient")
            GradientCatalog.render(spec, width, height)
        } else {
            val bytes = Http.bytes(PicsumApi.fullUrl(item.ref, width, height))
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw IOException("Could not decode the image")
        }
    }

    suspend fun apply(item: WallpaperItem, target: WallpaperTarget) {
        val bmp = bitmap(item)
        withContext(Dispatchers.IO) {
            val flags = when (target) {
                WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }
            WallpaperManager.getInstance(context).setBitmap(bmp, null, true, flags)
        }
    }

    suspend fun saveToGallery(item: WallpaperItem): Uri {
        val bmp = bitmap(item)
        return withContext(Dispatchers.IO) { saveBitmap(bmp, "Glad_${item.key}_${System.currentTimeMillis()}") }
    }

    private fun saveBitmap(bmp: Bitmap, name: String): Uri {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Glad")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Could not create the image file")
            resolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                ?: throw IOException("Could not write the image")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        }
        @Suppress("DEPRECATION")
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Glad")
        if (!dir.exists() && !dir.mkdirs()) throw IOException("Could not create the Pictures/Glad folder")
        val file = File(dir, "$name.jpg")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null)
        return Uri.fromFile(file)
    }
}
