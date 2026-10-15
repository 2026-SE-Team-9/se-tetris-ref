package seoultech.se.tetris.ui.menu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JPanel;
import seoultech.se.tetris.config.AppSettings.ScreenSize;

/** 게임 이름(아스키 아트)과 메뉴 리스트를 그리는 시작 메뉴 화면. */
public class StartMenuPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String[] TITLE = {
            " _____ _____ _____ ____  ___ ____  ",
            "|_   _| ____|_   _|  _ \\|_ _/ ___| ",
            "  | | |  _|   | | | |_) || |\\___ \\ ",
            "  | | | |___  | | |  _ < | | ___) |",
            "  |_| |_____| |_| |_| \\_\\___|____/ "
    };
    private static final String KEY_HINT = "사용 가능한 키:  ↑ ↓ 이동   Enter 선택";
    private static final int BASE_SIZE = 480;

    private final transient StartMenuModel model;
    private double scale = 1.0;

    public StartMenuPanel(StartMenuModel model) {
        this.model = model;
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(BASE_SIZE, BASE_SIZE));
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e.getKeyCode());
            }
        });
    }

    /** 설정된 크기에 맞춰 시작 메뉴의 창과 글자·간격을 함께 조절한다. */
    public void setScreenSize(ScreenSize size) {
        scale = (double) size.cellPixels() / ScreenSize.MEDIUM.cellPixels();
        int pixels = (int) Math.round(BASE_SIZE * scale);
        setPreferredSize(new Dimension(pixels, pixels));
        revalidate();
        repaint();
    }

    private void handleKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP -> model.moveUp();
            case KeyEvent.VK_DOWN -> model.moveDown();
            case KeyEvent.VK_ENTER -> model.select();
            default -> model.rejectInput();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.scale(scale, scale);

        int y = 70;
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        g2.setColor(Color.CYAN);
        int lineHeight = g2.getFontMetrics().getHeight() + 2;
        for (String line : TITLE) {
            drawCentered(g2, line, y);
            y += lineHeight;
        }

        y += 50;
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
        List<MenuItem> items = model.getItems();
        for (int i = 0; i < items.size(); i++) {
            boolean selected = i == model.getSelectedIndex();
            g2.setColor(selected ? Color.YELLOW : Color.GRAY);
            drawCentered(g2, (selected ? "▶ " : "   ") + items.get(i).label(), y);
            y += 40;
        }

        if (model.isKeyHintVisible()) {
            g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
            g2.setColor(Color.WHITE);
            drawCentered(g2, KEY_HINT, BASE_SIZE - 30);
        }
        g2.dispose();
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (BASE_SIZE - fm.stringWidth(text)) / 2, y);
    }
}
