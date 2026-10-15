package seoultech.se.tetris.ui.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import seoultech.se.tetris.config.AppSettings.ScreenSize;
import seoultech.se.tetris.config.PropertiesAppSettingsService;
import seoultech.se.tetris.config.PropertiesKeyConfigService;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.score.CsvScoreRepository;

class SettingsPanelTest {
    @TempDir
    Path tempDir;

    /** 선택 칸 배경을 어둡게 그리고 크기·난이도 선택을 파일에 저장한다. */
    @Test
    void dropdownsStayDarkAndSaveSelectedSettings() throws Exception {
        Path settingsFile = tempDir.resolve("settings.properties");
        SwingUtilities.invokeAndWait(() -> {
            var panel = new SettingsPanel(new PropertiesAppSettingsService(settingsFile),
                    new PropertiesKeyConfigService(tempDir.resolve("keys.properties")),
                    new CsvScoreRepository(tempDir.resolve("scores.csv")), true, () -> {}, () -> {});
            @SuppressWarnings("unchecked")
            JComboBox<ScreenSize> size = (JComboBox<ScreenSize>) find(panel, JComboBox.class, 0);
            @SuppressWarnings("unchecked")
            JComboBox<Difficulty> difficulty = (JComboBox<Difficulty>) find(panel, JComboBox.class, 1);
            assertDark(size);
            assertDark(difficulty);
            size.setSelectedItem(ScreenSize.LARGE);
            difficulty.setSelectedItem(Difficulty.HARD);
            JButton save = (JButton) findButton(panel, "저장");
            save.doClick();
        });
        var restored = new PropertiesAppSettingsService(settingsFile);
        restored.load();
        assertEquals(ScreenSize.LARGE, restored.current().screenSize());
        assertEquals(Difficulty.HARD, restored.current().difficulty());
    }

    private static void assertDark(JComboBox<?> combo) {
        combo.setSize(240, 40);
        combo.doLayout();
        BufferedImage image = new BufferedImage(240, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        combo.paint(graphics);
        graphics.dispose();
        int color = image.getRGB(120, 20) & 0xFFFFFF;
        assertTrue(color < 0x404040, "선택된 항목의 배경은 어두워야 합니다");
    }

    private static Component find(Container root, Class<?> type, int index) {
        java.util.List<Component> matches = new java.util.ArrayList<>();
        collect(root, type, matches);
        return matches.get(index);
    }

    private static Component findButton(Container root, String text) {
        java.util.List<Component> matches = new java.util.ArrayList<>();
        collect(root, JButton.class, matches);
        return matches.stream().filter(button -> text.equals(((JButton) button).getText()))
                .findFirst().orElseThrow();
    }

    private static void collect(Container root, Class<?> type, java.util.List<Component> matches) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                matches.add(child);
            }
            if (child instanceof Container nested) {
                collect(nested, type, matches);
            }
        }
    }
}
