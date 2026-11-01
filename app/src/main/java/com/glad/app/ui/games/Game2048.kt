package com.glad.app.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glad.app.container
import com.glad.app.ui.components.GladScreen
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

private const val SIZE = 4

/** Pure game logic so it is easy to reason about. */
class Board2048(val cells: IntArray = IntArray(SIZE * SIZE), val score: Int = 0) {
    fun withTile(): Board2048 {
        val empty = cells.indices.filter { cells[it] == 0 }
        if (empty.isEmpty()) return this
        val next = cells.copyOf()
        next[empty.random()] = if (Random.nextInt(10) == 0) 4 else 2
        return Board2048(next, score)
    }

    /** dir: 0 left, 1 right, 2 up, 3 down. Returns null when nothing moved. */
    fun move(dir: Int): Board2048? {
        val next = IntArray(cells.size)
        var gained = 0
        for (line in 0 until SIZE) {
            val idx = IntArray(SIZE) { i ->
                when (dir) {
                    0 -> line * SIZE + i
                    1 -> line * SIZE + (SIZE - 1 - i)
                    2 -> i * SIZE + line
                    else -> (SIZE - 1 - i) * SIZE + line
                }
            }
            val values = idx.map { cells[it] }.filter { it != 0 }
            val merged = mutableListOf<Int>()
            var i = 0
            while (i < values.size) {
                if (i + 1 < values.size && values[i] == values[i + 1]) {
                    val sum = values[i] * 2
                    merged += sum
                    gained += sum
                    i += 2
                } else {
                    merged += values[i]
                    i += 1
                }
            }
            for (k in 0 until SIZE) next[idx[k]] = merged.getOrElse(k) { 0 }
        }
        if (next.contentEquals(cells)) return null
        return Board2048(next, score + gained)
    }

    fun canMove(): Boolean = (0..3).any { move(it) != null }
    val max: Int get() = cells.maxOrNull() ?: 0
}

private fun tileColor(v: Int): Color = when (v) {
    0 -> Color(0x22FFFFFF)
    2 -> Color(0xFF3A3354)
    4 -> Color(0xFF4A3F6E)
    8 -> Color(0xFFFF9A5A)
    16 -> Color(0xFFFF7A4D)
    32 -> Color(0xFFFF5E62)
    64 -> Color(0xFFFF3F6C)
    128 -> Color(0xFFFFC145)
    256 -> Color(0xFFFFB020)
    512 -> Color(0xFF8F7CFF)
    1024 -> Color(0xFF6B5BFF)
    else -> Color(0xFF19C3B1)
}

@Composable
fun Game2048Screen(onBack: () -> Unit) {
    val settings = LocalContext.current.container.settings
    val scope = rememberCoroutineScope()
    var board by remember { mutableStateOf(Board2048().withTile().withTile()) }
    var won by remember { mutableStateOf(false) }
    var over by remember { mutableStateOf(false) }
    var best by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        settings.gameStats.collect { best = it.best2048 }
    }

    fun restart() {
        board = Board2048().withTile().withTile()
        won = false
        over = false
    }

    fun swipe(dir: Int) {
        if (over) return
        val moved = board.move(dir) ?: return
        board = moved.withTile()
        if (board.max >= 2048) won = true
        if (!board.canMove()) {
            over = true
        }
        if (over || board.score > best) {
            scope.launch { settings.report2048(board.score) }
        }
    }

    GladScreen("2048", subtitle = "Swipe to merge matching tiles", onBack = onBack) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ScoreBox("Score", board.score, Modifier.weight(1f))
                ScoreBox("Best", maxOf(best, board.score), Modifier.weight(1f))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(8.dp)
                    .pointerInput(Unit) {
                        var total = androidx.compose.ui.geometry.Offset.Zero
                        detectDragGestures(
                            onDragStart = { total = androidx.compose.ui.geometry.Offset.Zero },
                            onDrag = { change, amount ->
                                change.consume()
                                total += amount
                            },
                            onDragEnd = {
                                if (maxOf(abs(total.x), abs(total.y)) > 40f) {
                                    if (abs(total.x) > abs(total.y)) swipe(if (total.x > 0) 1 else 0)
                                    else swipe(if (total.y > 0) 3 else 2)
                                }
                            }
                        )
                    }
            ) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (r in 0 until SIZE) {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (c in 0 until SIZE) {
                                val v = board.cells[r * SIZE + c]
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(tileColor(v)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (v != 0) {
                                        Text(
                                            v.toString(),
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = if (v < 100) 28.sp else if (v < 1000) 24.sp else 19.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (over || won) {
                Text(
                    if (over) "No more moves. Final score ${board.score}." else "You made 2048! Keep going for a higher score.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Button(onClick = ::restart, modifier = Modifier.fillMaxWidth()) { Text("New game") }
        }
    }
}

@Composable
private fun ScoreBox(label: String, value: Int, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(14.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.toString(), style = MaterialTheme.typography.headlineSmall)
    }
}
