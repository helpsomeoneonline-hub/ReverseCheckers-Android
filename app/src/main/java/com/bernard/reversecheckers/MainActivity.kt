package com.bernard.reversecheckers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
                    primary = Color(0xFFE53935),
                    secondary = Color(0xFFFFC857),
                    surface = Color(0xFF17191D),
                    background = Color(0xFF0E1013)
                )
            ) {
                ReverseCheckersApp()
            }
        }
    }
}

@Composable
private fun ReverseCheckersApp() {
    var gameMode by remember { mutableStateOf<GameMode?>(null) }
    var difficulty by remember { mutableStateOf(Difficulty.MEDIUM) }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (gameMode == null) {
            MainMenu(
                difficulty = difficulty,
                onDifficultyChange = { difficulty = it },
                onPlayComputer = { gameMode = GameMode.COMPUTER },
                onLocalTwoPlayer = { gameMode = GameMode.LOCAL_TWO_PLAYER }
            )
        } else {
            GameScreen(
                mode = gameMode!!,
                difficulty = difficulty,
                onExit = { gameMode = null }
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
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "REVERSE CHECKERS",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Lose all your pieces to win.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "How it works",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("• Captures are compulsory.")
                    Text("• If several capture routes exist, you must take a route that captures the most pieces.")
                    Text("• If another jump is available, you must continue.")
                    Text("• A piece that becomes king during a capture continues immediately as a king.")
                    Text("• Kings can move and capture across diagonals.")
                    Text("• Get rid of every piece — or have no legal move — to win.")
                }
            }

            Text(
                text = "Computer difficulty",
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Difficulty.entries.forEach { option ->
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = difficulty == option,
                        onClick = { onDifficultyChange(option) },
                        label = {
                            Text(
                                text = option.name.lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                }
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
    onExit: () -> Unit
) {
    var state by remember(mode, difficulty) { mutableStateOf(GameState.initial()) }
    var selected by remember { mutableStateOf<Pos?>(null) }

    val winner = GameEngine.winner(state)
    val legalMoves = if (winner == null) GameEngine.legalMoves(state) else emptyList()
    val captureRequired = legalMoves.any { it.captured != null }
    val computerTurn =
        mode == GameMode.COMPUTER &&
            state.turn == Player.BLACK &&
            winner == null

    LaunchedEffect(state, mode, difficulty) {
        if (computerTurn) {
            delay(320)
            val move = withContext(Dispatchers.Default) {
                ComputerPlayer.chooseMove(
                    state = state,
                    computer = Player.BLACK,
                    difficulty = difficulty
                )
            }

            if (move != null) {
                state = GameEngine.applyMove(state, move)
                selected = state.forcedPiece
            }
        }
    }

    fun newGame() {
        state = GameState.initial()
        selected = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onExit) {
                Text("‹ Menu")
            }

            Text(
                text = if (mode == GameMode.COMPUTER) {
                    "VS COMPUTER • ${difficulty.name}"
                } else {
                    "LOCAL 2 PLAYER"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            TextButton(onClick = { newGame() }) {
                Text("Restart")
            }
        }

        StatusPanel(
            state = state,
            winner = winner,
            captureRequired = captureRequired,
            computerTurn = computerTurn
        )

        Spacer(modifier = Modifier.size(10.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            val boardSize = minOf(maxWidth, maxHeight, 640.dp)

            GameBoard(
                modifier = Modifier.size(boardSize),
                state = state,
                selected = selected,
                legalMoves = legalMoves,
                inputEnabled = winner == null && !computerTurn,
                onSquareTapped = { pos ->
                    if (winner != null || computerTurn) return@GameBoard

                    val piece = state.board[pos.row][pos.col]
                    val currentSelected = selected

                    if (currentSelected != null) {
                        val move = legalMoves.firstOrNull {
                            it.from == currentSelected && it.to == pos
                        }

                        if (move != null) {
                            state = GameEngine.applyMove(state, move)
                            selected = state.forcedPiece
                            return@GameBoard
                        }
                    }

                    if (
                        piece?.player == state.turn &&
                        legalMoves.any { it.from == pos }
                    ) {
                        selected = pos
                    } else if (state.forcedPiece == null) {
                        selected = null
                    }
                }
            )
        }

        Text(
            text = "Reverse rules: maximum captures are compulsory. Lose every piece — or run out of legal moves — to win.",
            modifier = Modifier
                .widthIn(max = 680.dp)
                .padding(vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusPanel(
    state: GameState,
    winner: Player?,
    captureRequired: Boolean,
    computerTurn: Boolean
) {
    val redCount = GameEngine.pieceCount(state, Player.RED)
    val blackCount = GameEngine.pieceCount(state, Player.BLACK)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Red: $redCount", fontWeight = FontWeight.Bold)
                Text("Black: $blackCount", fontWeight = FontWeight.Bold)
            }

            val message = when {
                winner != null ->
                    "${winner.displayName()} wins! They got rid of their pieces or ran out of moves."

                computerTurn && state.forcedPiece != null ->
                    "Computer is continuing its capture…"

                computerTurn ->
                    "Computer is thinking…"

                state.forcedPiece != null ->
                    "${state.turn.displayName()} must continue jumping."

                captureRequired ->
                    "${state.turn.displayName()} to move — maximum capture required."

                else ->
                    "${state.turn.displayName()} to move."
            }

            Text(
                text = message,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                color = if (captureRequired || state.forcedPiece != null) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurface
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
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 2.dp,
                color = Color(0xFF767A82),
                shape = RoundedCornerShape(10.dp)
            )
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

                    val squareColor = if (isDark) {
                        Color(0xFF4E342E)
                    } else {
                        Color(0xFFD7CCC8)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(squareColor)
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
                                    .fillMaxSize(0.28f)
                                    .clip(CircleShape)
                                    .background(Color(0xAAFFC857))
                            )
                        }

                        if (piece != null) {
                            PieceView(
                                piece = piece,
                                canMove = isMovable && inputEnabled
                            )
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
    canMove: Boolean
) {
    val pieceColor = if (piece.player == Player.RED) {
        Color(0xFFD73535)
    } else {
        Color(0xFF15171A)
    }

    val edgeColor = when {
        canMove -> Color(0xFFFFC857)
        piece.player == Player.RED -> Color(0xFFFF8A80)
        else -> Color(0xFF9EA3AA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize(0.72f)
            .clip(CircleShape)
            .background(pieceColor)
            .border(3.dp, edgeColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (piece.king) {
            Text(
                text = "♛",
                color = Color(0xFFFFD166),
                fontSize = 25.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

private fun Player.displayName(): String =
    if (this == Player.RED) "Red" else "Black"
