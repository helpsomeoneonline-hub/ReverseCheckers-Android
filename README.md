# Reverse Checkers Android

Offline Android APK for reverse / anti-checkers.

## Current modes
- Play vs Computer
- Easy, Medium, Hard, and God computer difficulty
- Local 2 Player on the same phone or tablet

## Rules in this build
- 8x8 board with 12 pieces per side
- Red moves first
- Captures are compulsory
- Men can capture forward or backward
- Multi-captures must continue with the same piece
- When several capture routes exist, only a route that captures the maximum possible number of pieces is legal
- A man that touches the king row during a capture stays a man until the capture sequence ends; it is crowned only if the complete turn finishes on the king row
- Kings are flying kings and may move any distance diagonally
- A player wins by losing all their pieces or by having no legal move

## APK
Every push to the main branch runs the **Build Android APK** GitHub Action. The debug APK is uploaded as an Actions artifact named **ReverseCheckers-debug**.


## AI
All four difficulty levels use the same anti-checkers search engine and sacrifice-aware evaluation. Difficulty changes search depth, search time, and intentional randomness. God mode removes intentional randomness, uses iterative deepening, alpha-beta pruning, a transposition table, deeper endgame search, forced-sacrifice evaluation, capture-pressure evaluation, and mobility/trap evaluation.


## Presentation
- 3D-styled board and checker pieces
- Selected-piece lift animation
- Last-move highlighting
- Jaw-style CHOMP capture animation
- King-promotion flash
- Built-in move, capture, king, win, and God-mode sound cues
- Optional vibration/haptic feedback with in-game Sound and Vibration switches
