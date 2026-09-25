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

    fun edgePieceCount(state: GameState, player: Player): Int {
        var count = 0
        for (row in 0..7) {
            for (col in 0..7) {
                if (state.board[row][col]?.player == player && (col == 0 || col == 7)) {
                    count++
                }
            }
        }
        return count
    }

    fun winner(state: GameState): Player? {
        if (pieceCount(state, Player.RED) == 0) return Player.RED
        if (pieceCount(state, Player.BLACK) == 0) return Player.BLACK

        if (legalMoves(state).isEmpty()) {
            return state.turn
        }

        return null
    }

    /**
     * Captures are compulsory. If more than one capture route exists, only
     * first steps belonging to a route that captures the maximum possible
     * number of enemy pieces are legal.
     */
    fun legalMoves(
        state: GameState,
        player: Player = state.turn,
        respectForcedPiece: Boolean = player == state.turn
    ): List<Move> {
        val forced = if (respectForcedPiece) state.forcedPiece else null

        if (forced != null) {
            val piece = state.board.getOrNull(forced.row)?.getOrNull(forced.col)
            if (piece?.player == player) {
                val captures = capturesFrom(state.board, forced, piece)
                return maximumCaptureFirstSteps(state.board, captures, piece)
            }
        }

        val captureCandidates = mutableListOf<Pair<Move, Piece>>()
        forEachPiece(state.board, player) { pos, piece ->
            capturesFrom(state.board, pos, piece).forEach { move ->
                captureCandidates += move to piece
            }
        }

        if (captureCandidates.isNotEmpty()) {
            val scored = captureCandidates.map { (move, piece) ->
                move to captureLengthForMove(state.board, move, piece)
            }
            val maximum = scored.maxOf { it.second }
            return scored.filter { it.second == maximum }.map { it.first }
        }

        val normalMoves = mutableListOf<Move>()
        forEachPiece(state.board, player) { pos, piece ->
            normalMoves += normalMovesFrom(state.board, pos, piece)
        }

        return normalMoves
    }

    fun maximumCaptureCount(state: GameState, player: Player): Int {
        var best = 0
        forEachPiece(state.board, player) { pos, piece ->
            for (move in capturesFrom(state.board, pos, piece)) {
                best = max(best, captureLengthForMove(state.board, move, piece))
            }
        }
        return best
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

        movingPiece = promoteIfNeeded(movingPiece, move.to)
        mutable[move.to.row][move.to.col] = movingPiece
        val newBoard = mutable.map { it.toList() }

        if (move.captured != null) {
            // Promotion takes effect immediately. If the new king can keep
            // capturing, the same turn must continue with that king.
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

    private fun maximumCaptureFirstSteps(
        board: List<List<Piece?>>,
        captures: List<Move>,
        piece: Piece
    ): List<Move> {
        if (captures.isEmpty()) return emptyList()
        val scored = captures.map { move ->
            move to captureLengthForMove(board, move, piece)
        }
        val maximum = scored.maxOf { it.second }
        return scored.filter { it.second == maximum }.map { it.first }
    }

    private fun captureLengthForMove(
        board: List<List<Piece?>>,
        move: Move,
        piece: Piece
    ): Int {
        if (move.captured == null) return 0

        val mutable = board.map { it.toMutableList() }.toMutableList()
        mutable[move.from.row][move.from.col] = null
        mutable[move.captured.row][move.captured.col] = null

        val movedPiece = promoteIfNeeded(piece, move.to)
        mutable[move.to.row][move.to.col] = movedPiece
        val nextBoard = mutable.map { it.toList() }

        val continuations = capturesFrom(nextBoard, move.to, movedPiece)
        if (continuations.isEmpty()) return 1

        val bestContinuation = continuations.maxOf {
            captureLengthForMove(nextBoard, it, movedPiece)
        }
        return 1 + bestContinuation
    }

    private fun promoteIfNeeded(piece: Piece, to: Pos): Piece {
        if (piece.king) return piece
        val promote =
            (piece.player == Player.RED && to.row == 0) ||
                (piece.player == Player.BLACK && to.row == 7)
        return if (promote) piece.copy(king = true) else piece
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
    EASY, MEDIUM, HARD, GOD
}

object ComputerPlayer {
    private data class AiProfile(
        val maxDepth: Int,
        val timeBudgetMs: Long,
        val randomness: Int
    )

    private enum class TTFlag {
        EXACT, LOWER, UPPER
    }

    private data class TTEntry(
        val depth: Int,
        val value: Int,
        val flag: TTFlag
    )

    private class SearchTimeout : RuntimeException()

    private val transposition = HashMap<Long, TTEntry>()
    private var deadlineNanos: Long = Long.MAX_VALUE
    private var searchedNodes: Int = 0

    fun chooseMove(
        state: GameState,
        computer: Player,
        difficulty: Difficulty
    ): Move? {
        val moves = GameEngine.legalMoves(state)
        if (moves.isEmpty()) return null
        if (moves.size == 1) return moves.first()

        val profile = when (difficulty) {
            Difficulty.EASY -> AiProfile(maxDepth = 3, timeBudgetMs = 350, randomness = 120)
            Difficulty.MEDIUM -> AiProfile(maxDepth = 5, timeBudgetMs = 850, randomness = 45)
            Difficulty.HARD -> AiProfile(maxDepth = 8, timeBudgetMs = 1700, randomness = 8)
            Difficulty.GOD -> AiProfile(maxDepth = 13, timeBudgetMs = 3000, randomness = 0)
        }

        val totalPieces =
            GameEngine.pieceCount(state, Player.RED) +
                GameEngine.pieceCount(state, Player.BLACK)

        val effectiveMaxDepth =
            if (difficulty == Difficulty.GOD && totalPieces <= 10) 18
            else profile.maxDepth

        transposition.clear()
        searchedNodes = 0
        deadlineNanos = System.nanoTime() + profile.timeBudgetMs * 1_000_000L

        var bestMove = moves.first()
        var completedScores: List<Pair<Move, Int>> = moves.map { it to 0 }

        // Every level uses the same anti-checkers engine. Difficulty changes
        // search depth, search time and how much imperfection is intentionally
        // added to the final choice.
        for (depth in 1..effectiveMaxDepth) {
            try {
                val scores = searchRoot(state, computer, depth)
                if (scores.isNotEmpty()) {
                    completedScores = scores
                    bestMove = scores.maxBy { it.second }.first
                }
            } catch (_: SearchTimeout) {
                break
            }
        }

        if (profile.randomness == 0) {
            return bestMove
        }

        val jittered = completedScores.map { (move, score) ->
            move to (score + Random.nextInt(-profile.randomness, profile.randomness + 1))
        }
        return jittered.maxBy { it.second }.first
    }

    private fun searchRoot(
        state: GameState,
        computer: Player,
        depth: Int
    ): List<Pair<Move, Int>> {
        checkDeadline()

        val moves = orderedMoves(state, GameEngine.legalMoves(state), computer)
        val scores = mutableListOf<Pair<Move, Int>>()

        var alpha = Int.MIN_VALUE + 1
        val beta = Int.MAX_VALUE

        for (move in moves) {
            checkDeadline()
            val next = GameEngine.applyMove(state, move)
            val nextDepth =
                if (next.turn == state.turn) depth
                else depth - 1

            val score = minimax(
                state = next,
                depth = nextDepth,
                alphaStart = alpha,
                betaStart = beta,
                computer = computer
            )

            scores += move to score
            alpha = max(alpha, score)
        }

        return scores
    }

    private fun minimax(
        state: GameState,
        depth: Int,
        alphaStart: Int,
        betaStart: Int,
        computer: Player
    ): Int {
        searchedNodes++
        if ((searchedNodes and 255) == 0) {
            checkDeadline()
        }

        val winner = GameEngine.winner(state)
        if (winner != null) {
            return if (winner == computer) {
                1_000_000 + depth * 100
            } else {
                -1_000_000 - depth * 100
            }
        }

        // Never stop the search in the middle of a forced multi-capture.
        if (depth <= 0 && state.forcedPiece == null) {
            return evaluate(state, computer)
        }

        var alpha = alphaStart
        var beta = betaStart
        val originalAlpha = alpha
        val originalBeta = beta

        val key = stateHash(state, computer)
        val cached = transposition[key]
        if (cached != null && cached.depth >= depth) {
            when (cached.flag) {
                TTFlag.EXACT -> return cached.value
                TTFlag.LOWER -> alpha = max(alpha, cached.value)
                TTFlag.UPPER -> beta = min(beta, cached.value)
            }
            if (alpha >= beta) return cached.value
        }

        val moves = GameEngine.legalMoves(state)
        if (moves.isEmpty()) {
            return evaluate(state, computer)
        }

        val ordered = orderedMoves(state, moves, computer)
        val maximizing = state.turn == computer
        var value = if (maximizing) Int.MIN_VALUE else Int.MAX_VALUE

        for (move in ordered) {
            val next = GameEngine.applyMove(state, move)
            val nextDepth =
                if (next.turn == state.turn) depth
                else depth - 1

            val child = minimax(
                state = next,
                depth = nextDepth,
                alphaStart = alpha,
                betaStart = beta,
                computer = computer
            )

            if (maximizing) {
                value = max(value, child)
                alpha = max(alpha, value)
            } else {
                value = min(value, child)
                beta = min(beta, value)
            }

            if (beta <= alpha) break
        }

        val flag = when {
            value <= originalAlpha -> TTFlag.UPPER
            value >= originalBeta -> TTFlag.LOWER
            else -> TTFlag.EXACT
        }
        transposition[key] = TTEntry(depth, value, flag)

        return value
    }

    private fun orderedMoves(
        state: GameState,
        moves: List<Move>,
        computer: Player
    ): List<Move> {
        val maximizing = state.turn == computer
        return moves.sortedBy { move ->
            val next = GameEngine.applyMove(state, move)
            val score = fastOrderingScore(next, computer)
            if (maximizing) -score else score
        }
    }

    private fun fastOrderingScore(state: GameState, computer: Player): Int {
        val opponent = computer.opponent()
        val ownPieces = GameEngine.pieceCount(state, computer)
        val opponentPieces = GameEngine.pieceCount(state, opponent)
        val ownForced = GameEngine.maximumCaptureCount(state, computer)
        val opponentForced = GameEngine.maximumCaptureCount(state, opponent)

        return (opponentPieces - ownPieces) * 100 +
            (opponentForced - ownForced) * 45
    }

    /**
     * Anti-checkers evaluation. All difficulty levels use these same ideas:
     * lose your own pieces, create forced sacrifices, avoid being forced to
     * remove the opponent's pieces, and work toward positions with little or
     * no mobility.
     */
    private fun evaluate(state: GameState, computer: Player): Int {
        val opponent = computer.opponent()

        val ownPieces = GameEngine.pieceCount(state, computer)
        val opponentPieces = GameEngine.pieceCount(state, opponent)
        val ownKings = GameEngine.kingCount(state, computer)
        val opponentKings = GameEngine.kingCount(state, opponent)

        val ownMoves = GameEngine.legalMoves(
            state.copy(turn = computer, forcedPiece = null),
            computer,
            respectForcedPiece = false
        ).size

        val opponentMoves = GameEngine.legalMoves(
            state.copy(turn = opponent, forcedPiece = null),
            opponent,
            respectForcedPiece = false
        ).size

        val ownForcedCapture = GameEngine.maximumCaptureCount(state, computer)
        val opponentForcedCapture = GameEngine.maximumCaptureCount(state, opponent)

        val ownEdges = GameEngine.edgePieceCount(state, computer)
        val opponentEdges = GameEngine.edgePieceCount(state, opponent)

        var score =
            (opponentPieces - ownPieces) * 170 +
                (opponentKings - ownKings) * 14 +
                (opponentMoves - ownMoves) * 5 +
                (opponentEdges - ownEdges) * 4

        // A capture available to the opponent means our pieces are being
        // offered as sacrifices. A capture available to us means we may be
        // forced to help the opponent get rid of theirs.
        score += opponentForcedCapture * 80
        score -= ownForcedCapture * 70

        if (state.turn == opponent && opponentForcedCapture > 0) {
            score += opponentForcedCapture * 55
        }
        if (state.turn == computer && ownForcedCapture > 0) {
            score -= ownForcedCapture * 55
        }

        // In losing checkers, low mobility can be a weapon because having no
        // legal move wins. Reward positions that are closer to that goal.
        if (ownMoves <= 2) score += (3 - ownMoves) * 18
        if (opponentMoves <= 2) score -= (3 - opponentMoves) * 18

        return score
    }

    private fun stateHash(state: GameState, computer: Player): Long {
        var hash = 0xcbf29ce484222325UL.toLong()
        val prime = 0x100000001b3UL.toLong()

        for (row in 0..7) {
            for (col in 0..7) {
                val piece = state.board[row][col]
                val code = when {
                    piece == null -> 0L
                    piece.player == Player.RED && !piece.king -> 1L
                    piece.player == Player.RED && piece.king -> 2L
                    piece.player == Player.BLACK && !piece.king -> 3L
                    else -> 4L
                }
                hash = (hash xor (code + (row * 8 + col) * 7L)) * prime
            }
        }

        hash = (hash xor if (state.turn == Player.RED) 11L else 13L) * prime
        hash = (hash xor if (computer == Player.RED) 17L else 19L) * prime
        state.forcedPiece?.let {
            hash = (hash xor (23L + it.row * 8L + it.col)) * prime
        }

        return hash
    }

    private fun checkDeadline() {
        if (System.nanoTime() >= deadlineNanos) {
            throw SearchTimeout()
        }
    }
}
