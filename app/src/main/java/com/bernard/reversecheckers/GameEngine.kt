package com.bernard.reversecheckers

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class Player {
    RED, BLACK;

    fun opponent(): Player = if (this == RED) BLACK else RED
}

data class Piece(
    val player: Player,
    val king: Boolean = false
)

data class Pos(
    val row: Int,
    val col: Int
)

data class Move(
    val from: Pos,
    val to: Pos,
    val captured: Pos? = null
)

data class GameState(
    val board: List<List<Piece?>>,
    val turn: Player = Player.RED,
    val forcedPiece: Pos? = null,
    val moveNumber: Int = 1
) {
    companion object {
        fun initial(): GameState {
            val board = MutableList(8) { MutableList<Piece?>(8) { null } }

            for (row in 0..2) {
                for (col in 0..7) {
                    if ((row + col) % 2 == 1) {
                        board[row][col] = Piece(Player.BLACK)
                    }
                }
            }

            for (row in 5..7) {
                for (col in 0..7) {
                    if ((row + col) % 2 == 1) {
                        board[row][col] = Piece(Player.RED)
                    }
                }
            }

            return GameState(board = board.map { it.toList() })
        }
    }
}

object GameEngine {
    private val diagonalDirections = listOf(
        -1 to -1,
        -1 to 1,
        1 to -1,
        1 to 1
    )

    fun pieceCount(state: GameState, player: Player): Int =
        state.board.sumOf { row -> row.count { it?.player == player } }

    fun kingCount(state: GameState, player: Player): Int =
        state.board.sumOf { row -> row.count { it?.player == player && it.king } }

    fun winner(state: GameState): Player? {
        if (pieceCount(state, Player.RED) == 0) return Player.RED
        if (pieceCount(state, Player.BLACK) == 0) return Player.BLACK

        if (legalMoves(state).isEmpty()) {
            return state.turn
        }

        return null
    }

    fun legalMoves(
        state: GameState,
        player: Player = state.turn,
        respectForcedPiece: Boolean = player == state.turn
    ): List<Move> {
        val forced = if (respectForcedPiece) state.forcedPiece else null

        if (forced != null) {
            val piece = state.board.getOrNull(forced.row)?.getOrNull(forced.col)
            if (piece?.player == player) {
                return capturesFrom(state.board, forced, piece)
            }
        }

        val captures = mutableListOf<Move>()
        forEachPiece(state.board, player) { pos, piece ->
            captures += capturesFrom(state.board, pos, piece)
        }

        if (captures.isNotEmpty()) return captures

        val normalMoves = mutableListOf<Move>()
        forEachPiece(state.board, player) { pos, piece ->
            normalMoves += normalMovesFrom(state.board, pos, piece)
        }

        return normalMoves
    }

    fun applyMove(state: GameState, requestedMove: Move): GameState {
        val move = legalMoves(state).firstOrNull {
            it.from == requestedMove.from &&
                it.to == requestedMove.to &&
                it.captured == requestedMove.captured
        } ?: return state

        val mutable = state.board.map { it.toMutableList() }.toMutableList()
        var movingPiece = mutable[move.from.row][move.from.col] ?: return state

        mutable[move.from.row][move.from.col] = null
        move.captured?.let { mutable[it.row][it.col] = null }

        if (!movingPiece.king) {
            val promote =
                (movingPiece.player == Player.RED && move.to.row == 0) ||
                    (movingPiece.player == Player.BLACK && move.to.row == 7)

            if (promote) {
                movingPiece = movingPiece.copy(king = true)
            }
        }

        mutable[move.to.row][move.to.col] = movingPiece
        val newBoard = mutable.map { it.toList() }

        if (move.captured != null) {
            val moreCaptures = capturesFrom(newBoard, move.to, movingPiece)
            if (moreCaptures.isNotEmpty()) {
                return state.copy(
                    board = newBoard,
                    forcedPiece = move.to
                )
            }
        }

        return GameState(
            board = newBoard,
            turn = state.turn.opponent(),
            forcedPiece = null,
            moveNumber = state.moveNumber + 1
        )
    }

    private fun normalMovesFrom(
        board: List<List<Piece?>>,
        pos: Pos,
        piece: Piece
    ): List<Move> {
        if (piece.king) {
            return flyingKingMoves(board, pos)
        }

        val forward = if (piece.player == Player.RED) -1 else 1
        return listOf(-1, 1).mapNotNull { dc ->
            val to = Pos(pos.row + forward, pos.col + dc)
            if (inside(to) && board[to.row][to.col] == null) {
                Move(pos, to)
            } else {
                null
            }
        }
    }

    private fun capturesFrom(
        board: List<List<Piece?>>,
        pos: Pos,
        piece: Piece
    ): List<Move> {
        return if (piece.king) {
            flyingKingCaptures(board, pos, piece.player)
        } else {
            manCaptures(board, pos, piece.player)
        }
    }

    private fun manCaptures(
        board: List<List<Piece?>>,
        pos: Pos,
        player: Player
    ): List<Move> {
        val moves = mutableListOf<Move>()

        for ((dr, dc) in diagonalDirections) {
            val jumped = Pos(pos.row + dr, pos.col + dc)
            val landing = Pos(pos.row + dr * 2, pos.col + dc * 2)

            if (!inside(jumped) || !inside(landing)) continue

            val jumpedPiece = board[jumped.row][jumped.col]
            if (
                jumpedPiece != null &&
                jumpedPiece.player != player &&
                board[landing.row][landing.col] == null
            ) {
                moves += Move(pos, landing, jumped)
            }
        }

        return moves
    }

    private fun flyingKingMoves(
        board: List<List<Piece?>>,
        pos: Pos
    ): List<Move> {
        val moves = mutableListOf<Move>()

        for ((dr, dc) in diagonalDirections) {
            var row = pos.row + dr
            var col = pos.col + dc

            while (inside(Pos(row, col)) && board[row][col] == null) {
                moves += Move(pos, Pos(row, col))
                row += dr
                col += dc
            }
        }

        return moves
    }

    private fun flyingKingCaptures(
        board: List<List<Piece?>>,
        pos: Pos,
        player: Player
    ): List<Move> {
        val moves = mutableListOf<Move>()

        for ((dr, dc) in diagonalDirections) {
            var row = pos.row + dr
            var col = pos.col + dc
            var enemy: Pos? = null

            while (inside(Pos(row, col))) {
                val square = board[row][col]

                if (enemy == null) {
                    when {
                        square == null -> Unit
                        square.player == player -> break
                        else -> enemy = Pos(row, col)
                    }
                } else {
                    if (square != null) break
                    moves += Move(pos, Pos(row, col), enemy)
                }

                row += dr
                col += dc
            }
        }

        return moves
    }

    private fun forEachPiece(
        board: List<List<Piece?>>,
        player: Player,
        action: (Pos, Piece) -> Unit
    ) {
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = board[row][col]
                if (piece?.player == player) {
                    action(Pos(row, col), piece)
                }
            }
        }
    }

    private fun inside(pos: Pos): Boolean =
        pos.row in 0..7 && pos.col in 0..7
}

enum class Difficulty {
    EASY, MEDIUM, HARD
}

object ComputerPlayer {
    fun chooseMove(
        state: GameState,
        computer: Player,
        difficulty: Difficulty
    ): Move? {
        val moves = GameEngine.legalMoves(state)
        if (moves.isEmpty()) return null

        if (difficulty == Difficulty.EASY) {
            return moves.random()
        }

        val depth = when (difficulty) {
            Difficulty.EASY -> 1
            Difficulty.MEDIUM -> 3
            Difficulty.HARD -> 5
        }

        var bestScore = Int.MIN_VALUE
        val bestMoves = mutableListOf<Move>()

        for (move in moves) {
            val next = GameEngine.applyMove(state, move)
            val score = minimax(
                state = next,
                depth = depth - 1,
                alphaStart = Int.MIN_VALUE + 1,
                betaStart = Int.MAX_VALUE,
                computer = computer
            )

            if (score > bestScore) {
                bestScore = score
                bestMoves.clear()
                bestMoves += move
            } else if (score == bestScore) {
                bestMoves += move
            }
        }

        return bestMoves.randomOrNull(Random.Default)
    }

    private fun minimax(
        state: GameState,
        depth: Int,
        alphaStart: Int,
        betaStart: Int,
        computer: Player
    ): Int {
        val winner = GameEngine.winner(state)
        if (winner != null) {
            return if (winner == computer) 100_000 + depth else -100_000 - depth
        }

        if (depth <= 0) {
            return evaluate(state, computer)
        }

        val moves = GameEngine.legalMoves(state)
        if (moves.isEmpty()) {
            return evaluate(state, computer)
        }

        var alpha = alphaStart
        var beta = betaStart

        return if (state.turn == computer) {
            var value = Int.MIN_VALUE

            for (move in moves) {
                value = max(
                    value,
                    minimax(
                        GameEngine.applyMove(state, move),
                        depth - 1,
                        alpha,
                        beta,
                        computer
                    )
                )
                alpha = max(alpha, value)
                if (beta <= alpha) break
            }

            value
        } else {
            var value = Int.MAX_VALUE

            for (move in moves) {
                value = min(
                    value,
                    minimax(
                        GameEngine.applyMove(state, move),
                        depth - 1,
                        alpha,
                        beta,
                        computer
                    )
                )
                beta = min(beta, value)
                if (beta <= alpha) break
            }

            value
        }
    }

    private fun evaluate(state: GameState, computer: Player): Int {
        val opponent = computer.opponent()

        val ownPieces = GameEngine.pieceCount(state, computer)
        val opponentPieces = GameEngine.pieceCount(state, opponent)
        val ownKings = GameEngine.kingCount(state, computer)
        val opponentKings = GameEngine.kingCount(state, opponent)

        val ownMobility = GameEngine.legalMoves(
            state,
            computer,
            respectForcedPiece = false
        ).size

        val opponentMobility = GameEngine.legalMoves(
            state,
            opponent,
            respectForcedPiece = false
        ).size

        // Reverse-checkers goal: fewer of your own pieces and fewer legal moves
        // are generally desirable because zero pieces OR zero legal moves wins.
        return (opponentPieces - ownPieces) * 100 +
            (opponentKings - ownKings) * 12 +
            (opponentMobility - ownMobility) * 3
    }
}
