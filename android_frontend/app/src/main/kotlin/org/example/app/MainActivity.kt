package org.example.app

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/**
 * PUBLIC_INTERFACE
 * MainActivity: Entry point activity for Tic Tac Toe Classic.
 *
 * This activity renders the game board, manages gameplay state for two players on the same device,
 * and provides visual feedback for turns, wins, and ties. It also supports resetting the board
 * and tracking scores across rounds.
 *
 * UI layout:
 * - Top: Title, running score, and a current turn indicator
 * - Center: 3x3 grid of cells
 * - Bottom: Reset button to clear the board (scores persist)
 *
 * No external configuration or network services are required.
 */
class MainActivity : Activity() {

    // UI references
    private lateinit var tvScore: TextView
    private lateinit var tvTurn: TextView
    private lateinit var gridLayout: GridLayout
    private lateinit var resetButton: Button

    // Game state
    private val board = Array(3) { Array(3) { ' ' } }
    private var currentPlayer = 'X'
    private var moves = 0
    private var scoreX = 0
    private var scoreO = 0
    private var gameOver = false
    private lateinit var cellButtons: List<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind views
        tvScore = findViewById(R.id.tvScore)
        tvTurn = findViewById(R.id.tvTurn)
        gridLayout = findViewById(R.id.grid)
        resetButton = findViewById(R.id.btnReset)

        // Prepare grid button references by id
        val ids = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2,
            R.id.btn3, R.id.btn4, R.id.btn5,
            R.id.btn6, R.id.btn7, R.id.btn8
        )
        cellButtons = ids.map { findViewById<Button>(it) }

        // Initialize UI
        updateScore()
        updateTurnIndicator()

        // Set click listeners for each cell
        cellButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                onCellClicked(index, button)
            }
            styleCell(button)
        }

        // Reset button clears the board but keeps score
        resetButton.setOnClickListener {
            newRound()
        }
    }

    private fun styleCell(button: Button) {
        // We primarily use icons; keep text invisible to avoid layout shifts
        button.setTextColor(ContextCompat.getColor(this, android.R.color.transparent))
        button.textSize = 0f
        button.typeface = Typeface.DEFAULT_BOLD
        // Ensure content description is set programmatically for accessibility
        ViewCompat.replaceAccessibilityAction(
            button,
            AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
            getString(R.string.turn_indicator_static),
        ) { _, _ -> false }
    }

    private fun onCellClicked(index: Int, button: Button) {
        if (gameOver) return

        val row = index / 3
        val col = index % 3

        if (board[row][col] != ' ') {
            return // already taken
        }

        board[row][col] = currentPlayer
        // Set appropriate icon based on player
        val iconRes = if (currentPlayer == 'X') R.drawable.ic_knight else R.drawable.ic_queen
        button.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        button.setPadding(0, 0, 0, 0)
        button.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, iconRes)
        // For better centering, also set background overlay drawable
        button.setForeground(null)
        // Accessibility: announce placed piece
        val cd = if (currentPlayer == 'X')
            getString(R.string.player_x) + " placed a knight"
        else
            getString(R.string.player_o) + " placed a queen"
        button.contentDescription = cd
        moves++

        when (checkGameState()) {
            GameResult.X_WINS -> {
                scoreX++
                onGameOver(getString(R.string.x_wins), winner = 'X')
            }
            GameResult.O_WINS -> {
                scoreO++
                onGameOver(getString(R.string.o_wins), winner = 'O')
            }
            GameResult.TIE -> {
                onGameOver(getString(R.string.tie_game), winner = null)
            }
            GameResult.CONTINUE -> {
                togglePlayer()
                updateTurnIndicator()
            }
        }
    }

    private fun togglePlayer() {
        currentPlayer = if (currentPlayer == 'X') 'O' else 'X'
    }

    private fun updateTurnIndicator() {
        val playerLabel = if (currentPlayer == 'X') getString(R.string.player_x) else getString(R.string.player_o)
        tvTurn.text = "$playerLabel's turn"
        val color = if (currentPlayer == 'X') R.color.op_primary else R.color.op_secondary
        tvTurn.setTextColor(ContextCompat.getColor(this, color))
    }

    private fun updateScore() {
        tvScore.text = getString(R.string.score_format, scoreX, scoreO)
    }

    private fun onGameOver(message: String, winner: Char?) {
        gameOver = true
        updateScore()
        tvTurn.text = message
        val color = when (winner) {
            'X' -> R.color.op_primary
            'O' -> R.color.op_secondary
            else -> R.color.op_text_secondary
        }
        tvTurn.setTextColor(ContextCompat.getColor(this, color))

        // Highlight winning trio
        val winningLine = findWinningLine()
        if (winningLine != null) {
            highlightCells(winningLine, color)
        }
    }

    private fun newRound() {
        // Reset board state, keep scores
        for (r in 0..2) {
            for (c in 0..2) {
                board[r][c] = ' '
            }
        }
        moves = 0
        gameOver = false
        currentPlayer = 'X'
        cellButtons.forEach {
            it.text = ""
            it.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            it.isEnabled = true
            it.alpha = 1.0f
            it.setTextColor(ContextCompat.getColor(this, android.R.color.transparent))
            it.background = ContextCompat.getDrawable(this, R.drawable.cell_background)
            it.contentDescription = getString(R.string.grid_cell)
        }
        updateTurnIndicator()
        updateScore()
    }

    private fun highlightCells(indices: IntArray, colorRes: Int) {
        val colorInt = ContextCompat.getColor(this, colorRes)
        indices.forEach { idx ->
            val btn = cellButtons[idx]
            btn.background = ContextCompat.getDrawable(this, R.drawable.cell_background)
            btn.background?.setTint(colorInt and 0x33FFFFFF or (0x33 shl 24)) // subtle tint overlay
        }
        // Dim non-winning cells for subtle emphasis
        cellButtons.forEachIndexed { idx, b ->
            if (!indices.contains(idx)) b.alpha = 0.6f
        }
    }

    private enum class GameResult { X_WINS, O_WINS, TIE, CONTINUE }

    private fun checkGameState(): GameResult {
        val lines = arrayOf(
            intArrayOf(0, 1, 2),
            intArrayOf(3, 4, 5),
            intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6),
            intArrayOf(1, 4, 7),
            intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8),
            intArrayOf(2, 4, 6)
        )

        for (line in lines) {
            val (a, b, c) = line
            val ra = a / 3; val ca = a % 3
            val rb = b / 3; val cb = b % 3
            val rc = c / 3; val cc = c % 3

            val va = board[ra][ca]
            val vb = board[rb][cb]
            val vc = board[rc][cc]

            if (va != ' ' && va == vb && vb == vc) {
                return if (va == 'X') GameResult.X_WINS else GameResult.O_WINS
            }
        }

        return if (moves == 9) GameResult.TIE else GameResult.CONTINUE
    }

    private fun findWinningLine(): IntArray? {
        val lines = arrayOf(
            intArrayOf(0, 1, 2),
            intArrayOf(3, 4, 5),
            intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6),
            intArrayOf(1, 4, 7),
            intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8),
            intArrayOf(2, 4, 6)
        )
        for (line in lines) {
            val (a, b, c) = line
            val va = board[a / 3][a % 3]
            val vb = board[b / 3][b % 3]
            val vc = board[c / 3][c % 3]
            if (va != ' ' && va == vb && vb == vc) {
                return line
            }
        }
        return null
    }
}
