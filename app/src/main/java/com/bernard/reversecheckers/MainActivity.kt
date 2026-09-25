package com.bernard.reversecheckers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class GameMode {
    COMPUTER,
    LOCAL_TWO_PLAYER
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFE63B3B),
                    secondary = Color(0xFFFFC857),
                    surface = Color(0xFF181A20),
                    background = Color(0xFF0A0B0E)
                )
            ) {
                ReverseCheckersApp()
            }
        }
    }
}

@Composable
private fun ReverseCheckersApp() {
    val context = LocalContext.current
    val feedback = remember { GameFeedback(context) }
    var gameMode by remember { mutableStateOf<GameMode?>(null) }
    var difficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    feedback.soundEnabled = soundEnabled
    feedback.vibrationEnabled = vibrationEnabled

    DisposableEffect(Unit) {
        onDispose { feedback.release() }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (gameMode == null) {
            MainMenu(
                difficulty = difficulty,
                onDifficultyChange = {
                    feedback.select()
                    difficulty = it
                },
                onPlayComputer = {
                    feedback.select()
                    gameMode = GameMode.COMPUTER
                },
                onLocalTwoPlayer = {
                    feedback.select()
                    gameMode = GameMode.LOCAL_TWO_PLAYER
                }
            )
        } else {
            GameScreen(
                mode = gameMode!!,
                difficulty = difficulty,
                feedback = feedback,
                soundEnabled = soundEnabled,
                vibrationEnabled = vibrationEnabled,
                onSoundEnabledChange = { soundEnabled = it },
                onVibrationEnabledChange = { vibrationEnabled = it },
                onExit = {
                    feedback.select()
                    gameMode = null
                }
            )
        }
    }
}

@Composable
private fun MainMenu(
    difficulty: Difficulty,
    onDifficultyChange: (Difficulty) -> Unit,
    onPlayComputer: () -> Unit,
    onLocalTwoPlayer: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF08090C),
                        Color(0xFF191116),
                        Color(0xFF08090C)
                    )
                )
            )
            .padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 540.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "REVERSE CHECKERS",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Lose your pieces. Outsmart the board.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = "Rules",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("• Captures are compulsory.")
                    Text("• If several routes exist, you must take a route that captures the most pieces.")
                    Text("• Multi-captures continue with the same piece.")
                    Text("• Touching the king row during a capture does not crown the piece.")
                    Text("• You become king only if the complete turn finishes on the king row.")
                    Text("• Flying kings move and capture across diagonals.")
                    Text("• Lose every piece — or have no legal move — to win.")
                }
            }

            Text(
                text = "Computer difficulty",
                fontWeight = FontWeight.SemiBold
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Difficulty.entries.chunked(2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowOptions.forEach { option ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = difficulty == option,
                                onClick = { onDifficultyChange(option) },
                                label = {
                                    Text(
                                        text = if (option == Difficulty.GOD) "⚡ GOD" else option.name,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                        fontWeight = if (option == Difficulty.GOD) FontWeight.Black else FontWeight.Medium
                                    )
                                }
                            )
                        }
                        if (rowOptions.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (difficulty == Difficulty.GOD) {
                Text(
                    text = "No intentional mistakes. Deep search. No mercy.",
                    color = Color(0xFFFFC857),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onPlayComputer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Play vs Computer")
            }

            OutlinedButton(
                onClick = onLocalTwoPlayer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Local 2 Player")
            }
        }
    }
}

@Composable
private fun GameScreen(
    mode: GameMode,
    difficulty: Difficulty,
    feedback: GameFeedback,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onExit: () -> Unit
) {
    var state by remember(mode, difficulty) { mutableStateOf(GameState.initial()) }
    var selected by remember { mutableStateOf<Pos?>(null) }
    var lastFrom by remember { mutableStateOf<Pos?>(null) }
    var lastTo by remember { mutableStateOf<Pos?>(null) }
    var chompPos by remember { mutableStateOf<Pos?>(null) }
    var chompEvent by remember { mutableIntStateOf(0) }
    var kingPos by remember { mutableStateOf<Pos?>(null) }
    var kingEvent by remember { mutableIntStateOf(0) }
    var previousWinner by remember { mutableStateOf<Player?>(null) }

    val winner = GameEngine.winner(state)
    val legalMoves = if (winner == null) GameEngine.legalMoves(state) else emptyList()
    val captureRequired = legalMoves.any { it.captured != null }
    val maxCapture = if (captureRequired) GameEngine.maximumCaptureCount(state, state.turn) else 0
    val computerTurn =
        mode == GameMode.COMPUTER &&
            state.turn == Player.BLACK &&
            winner == null

    fun applyVisualMove(move: Move) {
        val beforePiece = state.board[move.from.row][move.from.col]
        val next = GameEngine.applyMove(state, move)
        val afterPiece = next.board[move.to.row][move.to.col]

        lastFrom = move.from
        lastTo = move.to

        if (move.captured != null) {
            chompPos = move.captured
            chompEvent++
            feedback.capture()
        } else {
            feedback.move()
        }

        if (beforePiece?.king == false && afterPiece?.king == true) {
            kingPos = move.to
            kingEvent++
            feedback.king()
        }

        state = next
        selected = next.forcedPiece
    }

    LaunchedEffect(chompEvent) {
        if (chompEvent > 0) {
            delay(440)
            chompPos = null
        }
    }

    LaunchedEffect(kingEvent) {
        if (kingEvent > 0) {
            delay(750)
            kingPos = null
        }
    }

    LaunchedEffect(winner) {
        if (winner != null && previousWinner != winner) {
            feedback.win()
        }
        previousWinner = winner
    }

    LaunchedEffect(state, mode, difficulty) {
        if (computerTurn) {
            delay(if (difficulty == Difficulty.GOD) 220 else 320)
            val move = withContext(Dispatchers.Default) {
                ComputerPlayer.chooseMove(
                    state = state,
                    computer = Player.BLACK,
                    difficulty = difficulty
                )
            }

            if (move != null) {
                if (difficulty == Difficulty.GOD) feedback.godMove()
                applyVisualMove(move)
            }
        }
    }

    fun newGame() {
        state = GameState.initial()
        selected = null
        lastFrom = null
        lastTo = null
        chompPos = null
        kingPos = null
        previousWinner = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090A0D),
                        Color(0xFF14161B),
                        Color(0xFF090A0D)
                    )
                )
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 700.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onExit) {
                Text("‹ Menu")
            }

            Text(
                text = if (mode == GameMode.COMPUTER) {
                    if (difficulty == Difficulty.GOD) "VS ⚡ GOD" else "VS COMPUTER • ${difficulty.name}"
                } else {
                    "LOCAL 2 PLAYER"
                },
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = if (difficulty == Difficulty.GOD && mode == GameMode.COMPUTER) {
                    Color(0xFFFFC857)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            TextButton(onClick = {
                feedback.select()
                newGame()
            }) {
                Text("Restart")
            }
        }

        StatusPanel(
            state = state,
            winner = winner,
            captureRequired = captureRequired,
            maxCapture = maxCapture,
            computerTurn = computerTurn,
            difficulty = difficulty
        )

        Spacer(modifier = Modifier.size(10.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            val boardSize = minOf(maxWidth, maxHeight, 660.dp)

            GameBoard(
                modifier = Modifier.size(boardSize),
                state = state,
                selected = selected,
                legalMoves = legalMoves,
                inputEnabled = winner == null && !computerTurn,
                lastFrom = lastFrom,
                lastTo = lastTo,
                chompPos = chompPos,
                chompEvent = chompEvent,
                kingPos = kingPos,
                kingEvent = kingEvent,
                onSquareTapped = { pos ->
                    if (winner != null || computerTurn) return@GameBoard

                    val piece = state.board[pos.row][pos.col]
                    val currentSelected = selected

                    if (currentSelected != null) {
                        val move = legalMoves.firstOrNull {
                            it.from == currentSelected && it.to == pos
                        }

                        if (move != null) {
                            applyVisualMove(move)
                            return@GameBoard
                        }
                    }

                    if (
                        piece?.player == state.turn &&
                        legalMoves.any { it.from == pos }
                    ) {
                        selected = pos
                        feedback.select()
                    } else if (state.forcedPiece == null) {
                        selected = null
                    }
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 700.dp)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sound", fontSize = 12.sp)
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = onSoundEnabledChange
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vibration", fontSize = 12.sp)
                Switch(
                    checked = vibrationEnabled,
                    onCheckedChange = onVibrationEnabledChange
                )
            }
        }
    }
}

@Composable
private fun StatusPanel(
    state: GameState,
    winner: Player?,
    captureRequired: Boolean,
    maxCapture: Int,
    computerTurn: Boolean,
    difficulty: Difficulty
) {
    val redCount = GameEngine.pieceCount(state, Player.RED)
    val blackCount = GameEngine.pieceCount(state, Player.BLACK)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 700.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("RED  $redCount", fontWeight = FontWeight.Black)
                Text("BLACK  $blackCount", fontWeight = FontWeight.Black)
            }

            val message = when {
                winner != null ->
                    "${winner.displayName()} WINS"

                computerTurn && difficulty == Difficulty.GOD && state.forcedPiece != null ->
                    "GOD is continuing the forced capture…"

                computerTurn && difficulty == Difficulty.GOD ->
                    "GOD is calculating…"

                computerTurn && state.forcedPiece != null ->
                    "Computer is continuing its capture…"

                computerTurn ->
                    "Computer is thinking…"

                state.forcedPiece != null ->
                    "${state.turn.displayName()} must continue jumping."

                captureRequired ->
                    "${state.turn.displayName()} — MAXIMUM CAPTURE: $maxCapture"

                else ->
                    "${state.turn.displayName()} to move."
            }

            Text(
                text = message,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Black,
                color = when {
                    winner != null -> Color(0xFFFFC857)
                    difficulty == Difficulty.GOD && computerTurn -> Color(0xFFFFC857)
                    captureRequired || state.forcedPiece != null -> Color(0xFFFFC857)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun GameBoard(
    modifier: Modifier,
    state: GameState,
    selected: Pos?,
    legalMoves: List<Move>,
    inputEnabled: Boolean,
    lastFrom: Pos?,
    lastTo: Pos?,
    chompPos: Pos?,
    chompEvent: Int,
    kingPos: Pos?,
    kingEvent: Int,
    onSquareTapped: (Pos) -> Unit
) {
    val selectedTargets = remember(selected, legalMoves) {
        if (selected == null) {
            emptySet()
        } else {
            legalMoves.filter { it.from == selected }.map { it.to }.toSet()
        }
    }

    val movablePieces = remember(legalMoves) {
        legalMoves.map { it.from }.toSet()
    }

    Column(
        modifier = modifier
            .shadow(18.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF2A2D33),
                        Color(0xFF777B82),
                        Color(0xFF1A1C21)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(5.dp)
    ) {
        for (row in 0..7) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0..7) {
                    val pos = Pos(row, col)
                    val piece = state.board[row][col]
                    val isDark = (row + col) % 2 == 1
                    val isSelected = selected == pos
                    val isTarget = pos in selectedTargets
                    val isMovable = pos in movablePieces
                    val wasLast = pos == lastFrom || pos == lastTo

                    val squareBrush = if (isDark) {
                        Brush.linearGradient(
                            listOf(Color(0xFF332A2A), Color(0xFF15171B))
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(Color(0xFFD7D0C5), Color(0xFF9C958C))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(squareBrush)
                            .then(
                                if (wasLast) {
                                    Modifier.border(2.dp, Color(0x66FFC857))
                                } else {
                                    Modifier
                                }
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(3.dp, Color(0xFFFFC857))
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(enabled = inputEnabled && isDark) {
                                onSquareTapped(pos)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isTarget) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(0.30f)
                                    .clip(CircleShape)
                                    .background(Color(0xCCFFC857))
                                    .border(2.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                            )
                        }

                        if (piece != null) {
                            PieceView(
                                piece = piece,
                                canMove = isMovable && inputEnabled,
                                selected = isSelected
                            )
                        }

                        if (chompPos == pos) {
                            ChompEffect(event = chompEvent)
                        }

                        if (kingPos == pos) {
                            KingFlash(event = kingEvent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PieceView(
    piece: Piece,
    canMove: Boolean,
    selected: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.10f else 1f,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "pieceScale"
    )

    val lift by animateFloatAsState(
        targetValue = if (selected) -7f else 0f,
        animationSpec = tween(140),
        label = "pieceLift"
    )

    val colors = if (piece.player == Player.RED) {
        listOf(
            Color(0xFFFF8A80),
            Color(0xFFD62F3A),
            Color(0xFF6E1118)
        )
    } else {
        listOf(
            Color(0xFF8B9098),
            Color(0xFF25282E),
            Color(0xFF050607)
        )
    }

    val edge = when {
        selected -> Color(0xFFFFD875)
        canMove -> Color(0xFFFFC857)
        piece.player == Player.RED -> Color(0xFFFFA8A2)
        else -> Color(0xFF9FA4AD)
    }

    Box(
        modifier = Modifier
            .fillMaxSize(0.76f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = lift
            }
            .shadow(if (selected) 10.dp else 6.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = colors
                )
            )
            .border(if (selected) 3.dp else 2.dp, edge, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.68f)
                .clip(CircleShape)
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.18f),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(9.dp)
                .fillMaxSize(0.16f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.24f))
        )

        if (piece.king) {
            Text(
                text = "♛",
                color = Color(0xFFFFD166),
                fontSize = 27.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun ChompEffect(event: Int) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(event) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(360, easing = FastOutSlowInEasing)
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val p = progress.value
        val w = size.width
        val h = size.height
        val fade = (1f - p).coerceIn(0f, 1f)

        drawCircle(
            color = Color(0x99E53935).copy(alpha = 0.55f * fade),
            radius = w * (0.26f + 0.18f * p)
        )

        val topY = h * (0.18f + 0.20f * p)
        val bottomY = h * (0.82f - 0.20f * p)
        val jawHeight = h * 0.17f

        drawRoundRect(
            color = Color(0xFFE03B3B).copy(alpha = fade),
            topLeft = Offset(w * 0.08f, topY - jawHeight),
            size = Size(w * 0.84f, jawHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
        )
        drawRoundRect(
            color = Color(0xFF9D1820).copy(alpha = fade),
            topLeft = Offset(w * 0.08f, bottomY),
            size = Size(w * 0.84f, jawHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
        )

        for (i in 0..3) {
            val x0 = w * (0.20f + i * 0.16f)

            val topTooth = Path().apply {
                moveTo(x0, topY)
                lineTo(x0 + w * 0.07f, topY)
                lineTo(x0 + w * 0.035f, topY + h * 0.10f)
                close()
            }
            drawPath(topTooth, Color.White.copy(alpha = fade))

            val bottomTooth = Path().apply {
                moveTo(x0, bottomY)
                lineTo(x0 + w * 0.07f, bottomY)
                lineTo(x0 + w * 0.035f, bottomY - h * 0.10f)
                close()
            }
            drawPath(bottomTooth, Color.White.copy(alpha = fade))
        }
    }
}

@Composable
private fun KingFlash(event: Int) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(event) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(620))
    }

    val p = progress.value
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = 0.75f + p * 0.75f
                scaleY = 0.75f + p * 0.75f
                alpha = (1f - p).coerceIn(0f, 1f)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "♛",
            color = Color(0xFFFFD166),
            fontSize = 40.sp,
            fontWeight = FontWeight.Black
        )
    }
}

private fun Player.displayName(): String =
    if (this == Player.RED) "Red" else "Black"
