package seoultech.se.tetris.ui.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
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

    /** Esc 확인에서 예는 저장 후 복귀하고, 아니오는 저장 없이 복귀하며, 취소는 편집을 유지한다. */
    @Test
    void escapeChoiceControlsSavingAndNavigation() throws Exception {
        Path savedFile = tempDir.resolve("saved.properties");
        Path discardedFile = tempDir.resolve("discarded.properties");
        Path pendingFile = tempDir.resolve("pending.properties");
        AtomicInteger closes = new AtomicInteger();

        SwingUtilities.invokeAndWait(() -> {
            SettingsPanel saved = settingsPanel(savedFile, closes::incrementAndGet);
            @SuppressWarnings("unchecked")
            JComboBox<ScreenSize> size = (JComboBox<ScreenSize>) find(saved, JComboBox.class, 0);
            size.setSelectedItem(ScreenSize.LARGE);
            saved.handleEscapeChoice(JOptionPane.YES_OPTION);

            SettingsPanel discarded = settingsPanel(discardedFile, closes::incrementAndGet);
            discarded.handleEscapeChoice(JOptionPane.NO_OPTION);

            SettingsPanel pending = settingsPanel(pendingFile, closes::incrementAndGet);
            @SuppressWarnings("unchecked")
            JComboBox<ScreenSize> pendingSize = (JComboBox<ScreenSize>) find(pending, JComboBox.class, 0);
            pendingSize.setSelectedItem(ScreenSize.LARGE);
            pending.handleEscapeChoice(JOptionPane.CANCEL_OPTION);
            assertEquals(ScreenSize.LARGE, pendingSize.getSelectedItem());
            assertEquals(2, closes.get());
        });

        PropertiesAppSettingsService restored = new PropertiesAppSettingsService(savedFile);
        restored.load();
        assertEquals(ScreenSize.LARGE, restored.current().screenSize());
        assertTrue(Files.exists(savedFile));
        assertTrue(Files.notExists(discardedFile));
        assertTrue(Files.notExists(pendingFile));
    }

    private SettingsPanel settingsPanel(Path file, Runnable onClose) {
        return new SettingsPanel(new PropertiesAppSettingsService(file),
                new PropertiesKeyConfigService(tempDir.resolve(file.getFileName() + ".keys")),
                new CsvScoreRepository(tempDir.resolve("scores.csv")), true, () -> {}, onClose);
    }

    /** 방향키로 설정 항목과 기본·보조 키, 하단 버튼을 이동해도 설정값은 바뀌지 않는다. */
    @Test
    void arrowNavigationReachesOptionsKeyColumnsAndActions() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SettingsPanel panel = settingsPanel(tempDir.resolve("navigation.properties"), () -> {});
            JComboBox<?> size = (JComboBox<?>) find(panel, JComboBox.class, 0);
            Component difficulty = find(panel, JComboBox.class, 1);
            Component colorBlind = find(panel, JCheckBox.class, 0);
            Object originalSize = size.getSelectedItem();
            assertSame(difficulty, panel.navigationTarget(size, KeyEvent.VK_DOWN));
            assertSame(size, panel.navigationTarget(difficulty, KeyEvent.VK_UP));
            assertSame(colorBlind, panel.navigationTarget(difficulty, KeyEvent.VK_DOWN));

            Component mainKey = panel.navigationTarget(colorBlind, KeyEvent.VK_DOWN);
            Component subKey = panel.navigationTarget(mainKey, KeyEvent.VK_RIGHT);
            assertTrue(((JButton) mainKey).getToolTipText().endsWith("기본 키 변경"));
            assertTrue(((JButton) subKey).getToolTipText().endsWith("보조 키 변경"));
            assertSame(mainKey, panel.navigationTarget(subKey, KeyEvent.VK_LEFT));

            Component current = mainKey;
            for (int row = 0; row < 6; row++) {
                current = panel.navigationTarget(current, KeyEvent.VK_DOWN);
            }
            assertSame(findButton(panel, "모든 설정 기본값"), current);
            current = panel.navigationTarget(current, KeyEvent.VK_RIGHT);
            assertSame(findButton(panel, "스코어보드 기록 초기화"), current);
            current = panel.navigationTarget(current, KeyEvent.VK_RIGHT);
            assertSame(findButton(panel, "저장"), current);
            assertSame(findButton(panel, "취소"), panel.navigationTarget(current, KeyEvent.VK_RIGHT));
            assertEquals(originalSize, size.getSelectedItem());
        });
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
