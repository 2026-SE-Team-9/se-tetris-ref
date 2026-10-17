package seoultech.se.tetris.ui.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.config.AppSettings.ScreenSize;
import seoultech.se.tetris.engine.TetrisEngine;

class GamePanelTest {

    private static final Dimension BASE_SIZE = new Dimension(10 * 28 + 24 * 3 + 150, 20 * 28 + 24 * 2);

    @Test
    void 보통_크기는_기존_화면_크기와_같다() {
        GamePanel panel = new GamePanel(new TetrisEngine(), ScreenSize.MEDIUM.scale());

        assertEquals(BASE_SIZE, panel.getPreferredSize());
        assertEquals(BASE_SIZE, new GamePanel(new TetrisEngine()).getPreferredSize());
    }

    @Test
    void 화면_크기_설정은_작게_보통_크게_순서로_커진다() {
        Dimension small = new GamePanel(new TetrisEngine(), ScreenSize.SMALL.scale()).getPreferredSize();
        Dimension medium = new GamePanel(new TetrisEngine(), ScreenSize.MEDIUM.scale()).getPreferredSize();
        Dimension large = new GamePanel(new TetrisEngine(), ScreenSize.LARGE.scale()).getPreferredSize();

        assertTrue(small.width < medium.width && medium.width < large.width);
        assertTrue(small.height < medium.height && medium.height < large.height);
    }

    @Test
    void 배율을_바꾸면_선호_크기가_바뀐다() {
        GamePanel panel = new GamePanel(new TetrisEngine());

        panel.setScale(ScreenSize.LARGE.scale());

        assertEquals(ScreenSize.LARGE.scale(), panel.getScale());
        assertTrue(panel.getPreferredSize().width > BASE_SIZE.width);
    }

    @Test
    void 배율은_0보다_커야_한다() {
        GamePanel panel = new GamePanel(new TetrisEngine());

        assertThrows(IllegalArgumentException.class, () -> panel.setScale(0));
        assertThrows(IllegalArgumentException.class, () -> panel.setScale(-1));
        assertThrows(IllegalArgumentException.class, () -> panel.setScale(Double.NaN));
    }

    @Test
    void 모든_크기에서_화면을_그릴_수_있다() {
        for (ScreenSize size : ScreenSize.values()) {
            GamePanel panel = new GamePanel(new TetrisEngine(), size.scale());
            Dimension preferred = panel.getPreferredSize();
            panel.setSize(preferred);
            BufferedImage image = new BufferedImage(preferred.width, preferred.height, BufferedImage.TYPE_INT_RGB);

            panel.paint(image.createGraphics());
        }
    }

    @Test
    void 화면_크기_배율은_모두_양수이고_보통은_1이다() {
        for (ScreenSize size : ScreenSize.values()) {
            assertTrue(size.scale() > 0);
        }
        assertEquals(1.0, ScreenSize.MEDIUM.scale());
    }
}
