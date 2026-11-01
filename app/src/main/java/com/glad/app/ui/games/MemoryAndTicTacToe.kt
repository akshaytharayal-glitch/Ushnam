package com.glad.app.ui.games

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glad.app.container
import com.glad.app.ui.components.GladScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- Memory

private val symbols = listOf("🌙", "⭐", "🔥", "🌊", "🍀", "🎧", "🦋", "🍒")

private class MemoryCard(val id: Int, val symbol: String) {
    var faceUp by mutableStateOf(false)
    var matched by mutableStateOf(false)
}

private fun newDeck(): List<MemoryCard> =
    (symbols + symbols).shuffled().mapIndexed { i, s -> MemoryCard(i, s) }

@Composable
fun GameMemoryScreen(onBack: () -> Unit) {
    val settings = LocalContext.current.container.settings
    val scope = rememberCoroutineScope()
    var deck by remember { mutableStateOf(newDeck()) }
    var moves by remember { mutableIntStateOf(0) }
    var first by remember { mutableStateOf<MemoryCard?>(null) }
    var locked by remember { mutableStateOf(false) }
    var best by remember { mutableIntStateOf(0) }
    val done = deck.all { it.matched }

    LaunchedEffect(Unit) { settings.gameStats.collect { best = it.bestMemoryMoves } }

    fun restart() {
        deck = newDeck()
        moves = 0
        first = null
        locked = false
    }

    fun flip(card: MemoryCard) {
        if (locked || card.faceUp || card.matched) return
        card.faceUp = true
        val other = first
        if (other == null) {
            first = card
            return
        }
        moves += 1
        first = null
        if (other.symbol == card.symbol) {
            other.matched = true
            card.matched = true
            if (deck.all { it.matched }) scope.launch { settings.reportMemory(moves) }
        } else {
            locked = true
            scope.launch {
                delay(750)
                other.faceUp = false
                card.faceUp = false
                locked = false
            }
        }
    }

    GladScreen("Memory Match", subtitle = "Moves: $moves" + if (best > 0) " · Best: $best" else "", onBack = onBack) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (r in 0 until 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (c in 0 until 4) {
                        val card = deck[r * 4 + c]
                        val bg by animateColorAsState(
                            when {
                                card.matched -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                                card.faceUp -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            label = "card"
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(0.85f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(bg)
                                .clickable { flip(card) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (card.faceUp || card.matched) card.symbol else "?", fontSize = 30.sp)
                        }
                    }
                }
            }
            if (done) {
                Text(
                    "Cleared in $moves moves!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Button(onClick = ::restart, modifier = Modifier.fillMaxWidth()) { Text("New game") }
        }
    }
}

// ------------------------------------------------------------ Tic-Tac-Toe

private val lines = listOf(
    intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
    intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
    intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
)

private fun winner(b: CharArray): Char? {
    for (l in lines) {
        val a = b[l[0]]
        if (a != ' ' && a == b[l[1]] && a == b[l[2]]) return a
    }
    return null
}

/** Minimax from the computer's (O) point of view. */
private fun minimax(b: CharArray, oTurn: Boolean, depth: Int): Int {
    when (winner(b)) {
        'O' -> return 10 - depth
        'X' -> return depth - 10
    }
    if (b.none { it == ' ' }) return 0
    var best = if (oTurn) Int.MIN_VALUE else Int.MAX_VALUE
    for (i in b.indices) {
        if (b[i] != ' ') continue
        b[i] = if (oTurn) 'O' else 'X'
        val score = minimax(b, !oTurn, depth + 1)
        b[i] = ' '
        best = if (oTurn) maxOf(best, score) else minOf(best, score)
    }
    return best
}

private fun computerMove(b: CharArray, perfect: Boolean): Int {
    val free = b.indices.filter { b[it] == ' ' }
    if (!perfect) return free.random()
    var bestScore = Int.MIN_VALUE
    var bestMove = free.first()
    for (i in free) {
        b[i] = 'O'
        val score = minimax(b, false, 1)
        b[i] = ' '
        if (score > bestScore) {
            bestScore = score
            bestMove = i
        }
    }
    return bestMove
}

@Composable
fun GameTicTacToeScreen(onBack: () -> Unit) {
    val settings = LocalContext.current.container.settings
    val scope = rememberCoroutineScope()
    val board = remember { mutableStateListOf(*Array(9) { ' ' }) }
    var hard by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    fun finishIfOver(): Boolean {
        val arr = board.toCharArray()
        val w = winner(arr)
        val outcome = when {
            w == 'X' -> 1
            w == 'O' -> -1
            arr.none { it == ' ' } -> 0
            else -> return false
        }
        result = when (outcome) {
            1 -> "You win!"
            -1 -> "The computer wins."
            else -> "It's a draw."
        }
        scope.launch { settings.reportTicTacToe(outcome) }
        return true
    }

    fun restart() {
        for (i in 0 until 9) board[i] = ' '
        result = null
    }

    fun play(i: Int) {
        if (result != null || board[i] != ' ') return
        board[i] = 'X'
        if (finishIfOver()) return
        val arr = board.toCharArray()
        val move = computerMove(arr, hard)
        board[move] = 'O'
        finishIfOver()
    }

    GladScreen("Tic-Tac-Toe", subtitle = "You are X", onBack = onBack) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.FilterChip(selected = !hard, onClick = { hard = false; restart() }, label = { Text("Easy") })
                androidx.compose.material3.FilterChip(selected = hard, onClick = { hard = true; restart() }, label = { Text("Unbeatable") })
            }
            for (r in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (c in 0 until 3) {
                        val i = r * 3 + c
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .clickable { play(i) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                board[i].toString().trim(),
                                fontSize = 54.sp,
                                color = if (board[i] == 'X') MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
            result?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Button(onClick = ::restart, modifier = Modifier.fillMaxWidth()) { Text("New round") }
        }
    }
}
