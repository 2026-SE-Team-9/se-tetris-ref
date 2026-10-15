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
import seoultech.se.tetris.config.AppSettings;
import seoultech.se.tetris.engine.Board;

/** 보드(20x10), 다음 블럭, 점수·레벨·줄 수를 그리는 인게임 화면. {@link GameState}를 읽기만 한다. */
public class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int MARGIN = 24;
    private static final int SIDE_W = 150;
    private static final Color GRID = new Color(40, 40, 40);

    private final transient GameState state;
    private final int cell;
    private final int boardW;
    private final int boardH;
    private final int sideX;
    private final boolean colorBlind;

    public GamePanel(GameState state) {
        this(state, AppSettings.DEFAULT);
    }

    /** 저장된 화면 크기와 색맹 모드를 게임 화면에 반영한다. */
    public GamePanel(GameState state, AppSettings settings) {
        this.state = state;
        this.cell = settings.screenSize().cellPixels();
        this.boardW = Board.WIDTH * cell;
        this.boardH = Board.VISIBLE_HEIGHT * cell;
        this.sideX = MARGIN + boardW + MARGIN;
        this.colorBlind = settings.colorBlind();
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(sideX + SIDE_W + MARGIN, boardH + MARGIN * 2));
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
            g2.drawLine(MARGIN, MARGIN + r * cell, MARGIN + boardW, MARGIN + r * cell);
        }
        for (int c = 0; c <= Board.WIDTH; c++) {
            g2.drawLine(MARGIN + c * cell, MARGIN, MARGIN + c * cell, MARGIN + boardH);
        }

        int[][] grid = state.getBoard();
        int offset = Board.HEIGHT - Board.VISIBLE_HEIGHT;
        for (int r = offset; r < Board.HEIGHT; r++) {
            for (int c = 0; c < Board.WIDTH; c++) {
                if (grid[r][c] != BlockType.EMPTY_CODE) {
                    drawCell(g2, MARGIN + c * cell, MARGIN + (r - offset) * cell, BlockType.fromCode(grid[r][c]));
                }
            }
        }

        Block current = state.getCurrentBlock();
        if (current != null && !state.isGameOver()) {
            for (int r = 0; r < BlockType.SIZE; r++) {
                for (int c = 0; c < BlockType.SIZE; c++) {
                    int row = current.boardRow(r) - offset;
                    if (current.isFilled(r, c) && row >= 0) {
                        drawCell(g2, MARGIN + current.boardCol(c) * cell, MARGIN + row * cell, current.type());
                    }
                }
            }
        }
        g2.setColor(Color.WHITE);
        g2.drawRect(MARGIN - 1, MARGIN - 1, boardW + 1, boardH + 1);
    }

    private void drawSidePanel(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        g2.drawString("NEXT", sideX, MARGIN + 16);
        Block next = state.getNextBlock();
        if (next != null) {
            int mini = 22;
            for (int r = 0; r < BlockType.SIZE; r++) {
                for (int c = 0; c < BlockType.SIZE; c++) {
                    if (next.isFilled(r, c)) {
                        drawCell(g2, sideX + c * mini, MARGIN + 30 + r * mini, next.type(), mini);
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
        g2.drawString(label, sideX, y);
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
        g2.setColor(Color.WHITE);
        g2.drawString(value, sideX, y + 26);
        return y + 66;
    }

    private void drawCell(Graphics2D g2, int x, int y, BlockType type) {
        drawCell(g2, x, y, type, cell);
    }

    private void drawCell(Graphics2D g2, int x, int y, BlockType type, int size) {
        Color fill = BlockPalette.colorOf(type, colorBlind);
        g2.setColor(fill);
        g2.fillRect(x + 1, y + 1, size - 2, size - 2);
        int brightness = (fill.getRed() * 299 + fill.getGreen() * 587 + fill.getBlue() * 114) / 1000;
        g2.setColor(brightness < 128 ? Color.WHITE : Color.BLACK);
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, size * 5 / 8));
        FontMetrics fm = g2.getFontMetrics();
        String s = String.valueOf(type.symbol());
        g2.drawString(s, x + (size - fm.stringWidth(s)) / 2, y + (size + fm.getAscent() - fm.getDescent()) / 2);
    }

    private void drawOverlay(Graphics2D g2, String text) {
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(MARGIN, MARGIN, boardW, boardH);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 26));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, MARGIN + (boardW - fm.stringWidth(text)) / 2, MARGIN + boardH / 2);
    }
}
