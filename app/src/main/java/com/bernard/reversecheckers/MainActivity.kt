package com.bernard.reversecheckers

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
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

private enum class ClockOption(val label: String, val millis: Long) {
    OFF("Off", 0L),
    THREE("3 min", 3L * 60_000L),
    FIVE("5 min", 5L * 60_000L),
    TEN("10 min", 10L * 60_000L),
    FIFTEEN("15 min", 15L * 60_000L)
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
    var clockOption by remember { mutableStateOf(ClockOption.OFF) }
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
                clockOption = clockOption,
                onDifficultyChange = {
                    feedback.select()
                    difficulty = it
                },
                onClockOptionChange = {
                    feedback.select()
                    clockOption = it
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
                clockOption = clockOption,
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
    clockOption: ClockOption,
    onDifficultyChange: (Difficulty) -> Unit,
    onClockOptionChange: (ClockOption) -> Unit,
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
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    .shadow(10.dp, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Rules", fontWeight = FontWeight.Bold)
                    Text("• Captures are compulsory.")
                    Text("• If several routes exist, you must take a route that captures the most pieces.")
                    Text("• Multi-captures continue with the same piece.")
                    Text("• A man is crowned only if the complete turn finishes on the king row.")
                    Text("• Flying kings move and capture across diagonals.")
                    Text("• Lose every piece — or have no legal move — to win.")
                }
            }

            Text("Computer difficulty", fontWeight = FontWeight.SemiBold)

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

            Text("Game clock", fontWeight = FontWeight.SemiBold)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ClockOption.entries.chunked(3).forEach { options ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        options.forEach { option ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = clockOption == option,
                                onClick = { onClockOptionChange(option) },
                                label = {
                                    Text(
                                        option.label,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            )
                        }
                        repeat(3 - options.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Text(
                text = if (clockOption == ClockOption.OFF) {
                    "Clock off — play without a time limit."
                } else {
                    "Each player gets ${clockOption.label}. The clock switches after the full turn ends."
                },
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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

            Text(
                text = "Rotate your phone or tablet at any time — portrait and landscape have separate layouts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GameScreen(
    mode: GameMode,
    difficulty: Difficulty,
    clockOption: ClockOption,
    feedback: GameFeedback,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onExit: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var state by remember(mode, difficulty, clockOption) { mutableStateOf(GameState.initial()) }
    var selected by remember { mutableStateOf<Pos?>(null) }
    var lastFrom by remember { mutableStateOf<Pos?>(null) }
    var lastTo by remember { mutableStateOf<Pos?>(null) }
    var chompPos by remember { mutableStateOf<Pos?>(null) }
    var chompEvent by remember { mutableIntStateOf(0) }
    var kingPos by remember { mutableStateOf<Pos?>(null) }
    var kingEvent by remember { mutableIntStateOf(0) }
    var pressPlayer by remember { mutableStateOf<Player?>(null) }
    var pressEvent by remember { mutableIntStateOf(0) }
    var previousWinner by remember { mutableStateOf<Player?>(null) }
    var timedOutPlayer by remember(mode, difficulty, clockOption) { mutableStateOf<Player?>(null) }

    var redTime by remember(mode, difficulty, clockOption) {
        mutableLongStateOf(clockOption.millis)
    }
    var blackTime by remember(mode, difficulty, clockOption) {
        mutableLongStateOf(clockOption.millis)
    }

    val ruleWinner = GameEngine.winner(state)
    val winner = timedOutPlayer?.opponent() ?: ruleWinner
    val legalMoves = if (winner == null) GameEngine.legalMoves(state) else emptyList()
    val captureRequired = legalMoves.any { it.captured != null }
    val maxCapture = if (captureRequired) GameEngine.maximumCaptureCount(state, state.turn) else 0
    val computerTurn =
        mode == GameMode.COMPUTER &&
            state.turn == Player.BLACK &&
            winner == null

    LaunchedEffect(clockOption, state.turn, winner) {
        if (clockOption == ClockOption.OFF || winner != null) return@LaunchedEffect

        while (true) {
            delay(100)
            if (state.turn == Player.RED) {
                redTime = (redTime - 100L).coerceAtLeast(0L)
                if (redTime == 0L) {
                    timedOutPlayer = Player.RED
                    break
                }
            } else {
                blackTime = (blackTime - 100L).coerceAtLeast(0L)
                if (blackTime == 0L) {
                    timedOutPlayer = Player.BLACK
                    break
                }
            }
        }
    }

    fun applyVisualMove(move: Move) {
        val mover = state.turn
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

        if (next.turn != mover) {
            pressPlayer = mover
            pressEvent++
            if (clockOption != ClockOption.OFF) {
                feedback.clockPress()
            }
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

    LaunchedEffect(state, mode, difficulty, winner) {
        if (computerTurn) {
            delay(if (difficulty == Difficulty.GOD) 220 else 320)
            val move = withContext(Dispatchers.Default) {
                ComputerPlayer.chooseMove(
                    state = state,
                    computer = Player.BLACK,
                    difficulty = difficulty
                )
            }

            if (move != null && winner == null) {
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
        pressPlayer = null
        previousWinner = null
        timedOutPlayer = null
        redTime = clockOption.millis
        blackTime = clockOption.millis
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
            .padding(horizontal = if (landscape) 8.dp else 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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

        if (!landscape) {
            StatusPanel(
                state = state,
                winner = winner,
                timedOutPlayer = timedOutPlayer,
                captureRequired = captureRequired,
                maxCapture = maxCapture,
                computerTurn = computerTurn,
                difficulty = difficulty
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (landscape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerStation(
                    modifier = Modifier
                        .weight(0.22f)
                        .fillMaxHeight(),
                    player = Player.BLACK,
                    active = state.turn == Player.BLACK && winner == null,
                    remainingMillis = blackTime,
                    clockOption = clockOption,
                    pressPlayer = pressPlayer,
                    pressEvent = pressEvent,
                    landscape = true,
                    opponentLabel = if (mode == GameMode.COMPUTER) {
                        if (difficulty == Difficulty.GOD) "GOD" else "COMPUTER"
                    } else {
                        "BLACK"
                    }
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(0.56f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    val boardSize = minOf(maxWidth, maxHeight)
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
                            handleSquareTap(
                                pos = pos,
                                state = state,
                                selected = selected,
                                legalMoves = legalMoves,
                                winner = winner,
                                computerTurn = computerTurn,
                                onSelect = {
                                    selected = it
                                    feedback.select()
                                },
                                onClear = { selected = null },
                                onMove = { applyVisualMove(it) }
                            )
                        }
                    )
                }

                PlayerStation(
                    modifier = Modifier
                        .weight(0.22f)
                        .fillMaxHeight(),
                    player = Player.RED,
                    active = state.turn == Player.RED && winner == null,
                    remainingMillis = redTime,
                    clockOption = clockOption,
                    pressPlayer = pressPlayer,
                    pressEvent = pressEvent,
                    landscape = true,
                    opponentLabel = "YOU"
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            StatusPanel(
                modifier = Modifier.widthIn(max = 900.dp),
                state = state,
                winner = winner,
                timedOutPlayer = timedOutPlayer,
                captureRequired = captureRequired,
                maxCapture = maxCapture,
                computerTurn = computerTurn,
                difficulty = difficulty,
                compact = true
            )
        } else {
            PlayerStation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
                player = Player.BLACK,
                active = state.turn == Player.BLACK && winner == null,
                remainingMillis = blackTime,
                clockOption = clockOption,
                pressPlayer = pressPlayer,
                pressEvent = pressEvent,
                landscape = false,
                opponentLabel = if (mode == GameMode.COMPUTER) {
                    if (difficulty == Difficulty.GOD) "GOD" else "COMPUTER"
                } else {
                    "BLACK"
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
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
                        handleSquareTap(
                            pos = pos,
                            state = state,
                            selected = selected,
                            legalMoves = legalMoves,
                            winner = winner,
                            computerTurn = computerTurn,
                            onSelect = {
                                selected = it
                                feedback.select()
                            },
                            onClear = { selected = null },
                            onMove = { applyVisualMove(it) }
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            PlayerStation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
                player = Player.RED,
                active = state.turn == Player.RED && winner == null,
                remainingMillis = redTime,
                clockOption = clockOption,
                pressPlayer = pressPlayer,
                pressEvent = pressEvent,
                landscape = false,
                opponentLabel = "YOU"
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sound", fontSize = 11.sp)
            Switch(
                checked = soundEnabled,
                onCheckedChange = onSoundEnabledChange
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text("Vibration", fontSize = 11.sp)
            Switch(
                checked = vibrationEnabled,
                onCheckedChange = onVibrationEnabledChange
            )
        }
    }
}

private fun handleSquareTap(
    pos: Pos,
    state: GameState,
    selected: Pos?,
    legalMoves: List<Move>,
    winner: Player?,
    computerTurn: Boolean,
    onSelect: (Pos) -> Unit,
    onClear: () -> Unit,
    onMove: (Move) -> Unit
) {
    if (winner != null || computerTurn) return

    val piece = state.board[pos.row][pos.col]

    if (selected != null) {
        val move = legalMoves.firstOrNull {
            it.from == selected && it.to == pos
        }
        if (move != null) {
            onMove(move)
            return
        }
    }

    if (
        piece?.player == state.turn &&
        legalMoves.any { it.from == pos }
    ) {
        onSelect(pos)
    } else if (state.forcedPiece == null) {
        onClear()
    }
}

@Composable
private fun PlayerStation(
    modifier: Modifier = Modifier,
    player: Player,
    active: Boolean,
    remainingMillis: Long,
    clockOption: ClockOption,
    pressPlayer: Player?,
    pressEvent: Int,
    landscape: Boolean,
    opponentLabel: String
) {
    val press = remember { Animatable(0f) }

    LaunchedEffect(pressEvent) {
        if (pressEvent > 0 && pressPlayer == player) {
            press.snapTo(0f)
            press.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
            press.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
        }
    }

    Card(
        modifier = modifier
            .shadow(if (active) 9.dp else 4.dp, RoundedCornerShape(18.dp))
            .then(
                if (active) {
                    Modifier.border(2.dp, Color(0xFFFFC857), RoundedCornerShape(18.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(18.dp)
    ) {
        if (landscape) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                PlayerBust(
                    player = player,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Text(
                    opponentLabel,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = if (active) Color(0xFFFFC857) else MaterialTheme.colorScheme.onSurface
                )

                ClockReadout(
                    player = player,
                    active = active,
                    remainingMillis = remainingMillis,
                    clockOption = clockOption,
                    pressProgress = press.value,
                    compact = true
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PlayerBust(
                    player = player,
                    modifier = Modifier
                        .width(92.dp)
                        .fillMaxHeight()
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        opponentLabel,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = if (active) Color(0xFFFFC857) else MaterialTheme.colorScheme.onSurface
                    )
                    ClockReadout(
                        player = player,
                        active = active,
                        remainingMillis = remainingMillis,
                        clockOption = clockOption,
                        pressProgress = press.value,
                        compact = false
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerBust(
    player: Player,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val shirt = if (player == Player.RED) Color(0xFFD8333D) else Color(0xFF20242A)
        val shirtDark = if (player == Player.RED) Color(0xFF74141B) else Color(0xFF07080A)
        val skin = Color(0xFFB97855)
        val cx = size.width / 2f

        drawOval(
            brush = Brush.verticalGradient(listOf(shirt, shirtDark)),
            topLeft = Offset(size.width * 0.17f, size.height * 0.52f),
            size = Size(size.width * 0.66f, size.height * 0.42f)
        )

        drawCircle(
            color = skin,
            radius = minOf(size.width, size.height) * 0.16f,
            center = Offset(cx, size.height * 0.37f)
        )

        drawArc(
            color = Color(0xFF101114),
            startAngle = 190f,
            sweepAngle = 160f,
            useCenter = true,
            topLeft = Offset(cx - size.width * 0.19f, size.height * 0.17f),
            size = Size(size.width * 0.38f, size.height * 0.30f)
        )

        drawLine(
            color = Color.White.copy(alpha = 0.18f),
            start = Offset(size.width * 0.30f, size.height * 0.64f),
            end = Offset(size.width * 0.70f, size.height * 0.64f),
            strokeWidth = 3f
        )
    }
}

@Composable
private fun ClockReadout(
    player: Player,
    active: Boolean,
    remainingMillis: Long,
    clockOption: ClockOption,
    pressProgress: Float,
    compact: Boolean
) {
    val buttonColor = if (player == Player.RED) Color(0xFFE33B46) else Color(0xFF171A1F)
    val borderColor = if (player == Player.RED) Color(0xFFFF9A9F) else Color(0xFF9CA1AA)
    val buttonScale = 1f - 0.18f * pressProgress

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp)
    ) {
        Text(
            text = if (clockOption == ClockOption.OFF) "CLOCK OFF" else formatClock(remainingMillis),
            fontSize = if (compact) 17.sp else 19.sp,
            fontWeight = FontWeight.Black,
            color = when {
                clockOption == ClockOption.OFF -> MaterialTheme.colorScheme.onSurfaceVariant
                remainingMillis <= 15_000L -> Color(0xFFFF6B6B)
                active -> Color(0xFFFFC857)
                else -> MaterialTheme.colorScheme.onSurface
            }
        )

        Box(
            modifier = Modifier
                .size(if (compact) 46.dp else 50.dp)
                .graphicsLayer {
                    scaleX = buttonScale
                    scaleY = buttonScale
                    translationY = 4f * pressProgress
                }
                .shadow(7.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            borderColor,
                            buttonColor,
                            buttonColor.copy(alpha = 0.86f)
                        )
                    )
                )
                .border(2.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.60f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            )
        }

        if (pressProgress > 0.02f) {
            Text(
                text = "☝",
                fontSize = if (compact) 22.sp else 25.sp,
                modifier = Modifier.graphicsLayer {
                    translationY = -16f + 14f * pressProgress
                    alpha = (0.35f + 0.65f * pressProgress)
                }
            )
        }
    }
}

private fun formatClock(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) + 999L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Composable
private fun StatusPanel(
    modifier: Modifier = Modifier,
    state: GameState,
    winner: Player?,
    timedOutPlayer: Player?,
    captureRequired: Boolean,
    maxCapture: Int,
    computerTurn: Boolean,
    difficulty: Difficulty,
    compact: Boolean = false
) {
    val redCount = GameEngine.pieceCount(state, Player.RED)
    val blackCount = GameEngine.pieceCount(state, Player.BLACK)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        val message = when {
            timedOutPlayer != null ->
                "${timedOutPlayer.displayName()} ran out of time — ${timedOutPlayer.opponent().displayName()} wins!"

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

        if (compact) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RED $redCount", fontWeight = FontWeight.Black, fontSize = 12.sp)
                Text(
                    text = message,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = if (winner != null || captureRequired || state.forcedPiece != null) {
                        Color(0xFFFFC857)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text("BLACK $blackCount", fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("RED  $redCount", fontWeight = FontWeight.Black)
                    Text("BLACK  $blackCount", fontWeight = FontWeight.Black)
                }

                Text(
                    text = message,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    color = if (winner != null || captureRequired || state.forcedPiece != null) {
                        Color(0xFFFFC857)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
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
            .aspectRatio(1f)
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
            cornerRadius = CornerRadius(12f, 12f)
        )
        drawRoundRect(
            color = Color(0xFF9D1820).copy(alpha = fade),
            topLeft = Offset(w * 0.08f, bottomY),
            size = Size(w * 0.84f, jawHeight),
            cornerRadius = CornerRadius(12f, 12f)
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
