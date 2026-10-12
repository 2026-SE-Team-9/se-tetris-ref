package seoultech.se.tetris.ui.game;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.component.model.GameState;
import seoultech.se.tetris.engine.Board;

/** 보드(20x10), 다음 블럭, 점수·레벨·줄 수를 그리는 인게임 화면. {@link GameState}를 읽기만 한다. */
public class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int CELL = 28;
    private static final int MARGIN = 24;
    private static final int BOARD_W = Board.WIDTH * CELL;
    private static final int BOARD_H = Board.VISIBLE_HEIGHT * CELL;
    private static final int SIDE_X = MARGIN + BOARD_W + MARGIN;
    private static final int SIDE_W = 150;
    private static final Color GRID = new Color(40, 40, 40);

    private final transient GameState state;

    public GamePanel(GameState state) {
        this.state = state;
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(SIDE_X + SIDE_W + MARGIN, BOARD_H + MARGIN * 2));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBoard(g2);
        drawSidePanel(g2);
        if (state.isGameOver()) {
            drawOverlay(g2, "GAME OVER");
        } else if (state.isPaused()) {
            drawOverlay(g2, "일시정지");
        }
        g2.dispose();
    }

    private void drawBoard(Graphics2D g2) {
        g2.setColor(GRID);
        for (int r = 0; r <= Board.VISIBLE_HEIGHT; r++) {
            g2.drawLine(MARGIN, MARGIN + r * CELL, MARGIN + BOARD_W, MARGIN + r * CELL);
        }
        for (int c = 0; c <= Board.WIDTH; c++) {
            g2.drawLine(MARGIN + c * CELL, MARGIN, MARGIN + c * CELL, MARGIN + BOARD_H);
        }

        int[][] grid = state.getBoard();
        int offset = Board.HEIGHT - Board.VISIBLE_HEIGHT;
        for (int r = offset; r < Board.HEIGHT; r++) {
            for (int c = 0; c < Board.WIDTH; c++) {
                if (grid[r][c] != BlockType.EMPTY_CODE) {
                    drawCell(g2, MARGIN + c * CELL, MARGIN + (r - offset) * CELL, BlockType.fromCode(grid[r][c]));
                }
            }
        }

        Block current = state.getCurrentBlock();
        if (current != null && !state.isGameOver()) {
            for (int r = 0; r < BlockType.SIZE; r++) {
                for (int c = 0; c < BlockType.SIZE; c++) {
                    int row = current.boardRow(r) - offset;
                    if (current.isFilled(r, c) && row >= 0) {
                        drawCell(g2, MARGIN + current.boardCol(c) * CELL, MARGIN + row * CELL, current.type());
                    }
                }
            }
        }
        g2.setColor(Color.WHITE);
        g2.drawRect(MARGIN - 1, MARGIN - 1, BOARD_W + 1, BOARD_H + 1);
    }

    private void drawSidePanel(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        g2.drawString("NEXT", SIDE_X, MARGIN + 16);
        Block next = state.getNextBlock();
        if (next != null) {
            int mini = 22;
            for (int r = 0; r < BlockType.SIZE; r++) {
                for (int c = 0; c < BlockType.SIZE; c++) {
                    if (next.isFilled(r, c)) {
                        drawCell(g2, SIDE_X + c * mini, MARGIN + 30 + r * mini, next.type(), mini);
                    }
                }
            }
        }

        int y = MARGIN + 170;
        y = drawStat(g2, "SCORE", String.valueOf(state.getScore()), y);
        y = drawStat(g2, "LEVEL", String.valueOf(state.getLevel()), y);
        drawStat(g2, "LINES", String.valueOf(state.getLinesCleared()), y);
    }

    private int drawStat(Graphics2D g2, String label, String value, int y) {
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
        g2.setColor(Color.GRAY);
        g2.drawString(label, SIDE_X, y);
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
        g2.setColor(Color.WHITE);
        g2.drawString(value, SIDE_X, y + 26);
        return y + 66;
    }

    private void drawCell(Graphics2D g2, int x, int y, BlockType type) {
        drawCell(g2, x, y, type, CELL);
    }

    private void drawCell(Graphics2D g2, int x, int y, BlockType type, int size) {
        g2.setColor(BlockPalette.colorOf(type));
        g2.fillRect(x + 1, y + 1, size - 2, size - 2);
        g2.setColor(Color.BLACK);
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, size * 5 / 8));
        FontMetrics fm = g2.getFontMetrics();
        String s = String.valueOf(type.symbol());
        g2.drawString(s, x + (size - fm.stringWidth(s)) / 2, y + (size + fm.getAscent() - fm.getDescent()) / 2);
    }

    private void drawOverlay(Graphics2D g2, String text) {
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(MARGIN, MARGIN, BOARD_W, BOARD_H);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 26));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, MARGIN + (BOARD_W - fm.stringWidth(text)) / 2, MARGIN + BOARD_H / 2);
    }
}
