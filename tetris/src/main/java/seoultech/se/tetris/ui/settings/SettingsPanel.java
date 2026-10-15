package seoultech.se.tetris.ui.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import seoultech.se.tetris.config.AppSettings;
import seoultech.se.tetris.config.AppSettings.ScreenSize;
import seoultech.se.tetris.config.KeyBindingEditor;
import seoultech.se.tetris.config.KeyConfigService.GameAction;
import seoultech.se.tetris.config.PropertiesAppSettingsService;
import seoultech.se.tetris.config.PropertiesKeyConfigService;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.score.ScoreRepository;

/** 시작 메뉴와 일시정지 화면에서 재사용할 수 있는 설정 화면. */
public final class SettingsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color BACKGROUND = Color.BLACK;
    private static final Color SURFACE = new Color(25, 25, 25);
    private static final Color OUTLINE = new Color(75, 75, 75);
    private static final Color MUTED = new Color(170, 170, 170);
    private static final Font BODY_FONT = new Font(Font.DIALOG, Font.PLAIN, 15);

    private final PropertiesAppSettingsService settings;
    private final KeyBindingEditor keyEditor;
    private final ScoreRepository scores;
    private final boolean allowScoreReset;
    private final Runnable onClose;
    private final JComboBox<ScreenSize> screenSize = new JComboBox<>(ScreenSize.values());
    private final JComboBox<Difficulty> difficulty = new JComboBox<>(Difficulty.values());
    private final JCheckBox colorBlind = new JCheckBox("색맹 모드");
    private final Map<GameAction, JButton[]> keyButtons = new EnumMap<>(GameAction.class);
    private final KeyEventDispatcher keyCapture = this::captureKey;
    private GameAction captureAction;
    private boolean captureMain;
    private int capturedCode = KeyEvent.VK_UNDEFINED;

    public SettingsPanel(PropertiesAppSettingsService settings,
            PropertiesKeyConfigService keys, ScoreRepository scores,
            boolean allowScoreReset, Runnable onKeysSaved, Runnable onClose) {
        super(new BorderLayout());
        this.settings = Objects.requireNonNull(settings, "settings");
        this.keyEditor = new KeyBindingEditor(Objects.requireNonNull(keys, "keys"),
                Objects.requireNonNull(onKeysSaved, "onKeysSaved"));
        this.scores = Objects.requireNonNull(scores, "scores");
        this.allowScoreReset = allowScoreReset;
        this.onClose = Objects.requireNonNull(onClose, "onClose");
        buildContent();
        loadCurrentValues();
    }

    private void buildContent() {
        setBackground(BACKGROUND);
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JLabel title = new JLabel("설정");
        title.setFont(new Font(Font.DIALOG, Font.BOLD, 26));
        title.setForeground(Color.CYAN);
        content.add(title);
        content.add(Box.createVerticalStrut(18));

        JPanel options = new JPanel(new GridLayout(3, 2, 12, 12));
        options.setBackground(BACKGROUND);
        options.add(label("화면 크기"));
        styleCombo(screenSize);
        options.add(screenSize);
        options.add(label("난이도"));
        styleCombo(difficulty);
        difficulty.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list,
                    Object value, int index, boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof Difficulty selectedDifficulty) {
                    setText(switch (selectedDifficulty) {
                        case EASY -> "쉬움";
                        case NORMAL -> "중간";
                        case HARD -> "어려움";
                    });
                }
                setOpaque(true);
                setBackground(index >= 0 && selected ? Color.CYAN : SURFACE);
                setForeground(index >= 0 && selected ? BACKGROUND : Color.WHITE);
                return this;
            }
        });
        options.add(difficulty);
        options.add(label("블록 표시"));
        colorBlind.setFont(BODY_FONT);
        colorBlind.setBackground(BACKGROUND);
        colorBlind.setForeground(Color.WHITE);
        colorBlind.setFocusPainted(false);
        options.add(colorBlind);
        content.add(options);
        content.add(Box.createVerticalStrut(8));
        JLabel sizeHint = label("화면 크기는 저장 후 메뉴와 다음 게임에 적용됩니다. 난이도는 다음 게임부터 적용됩니다.");
        sizeHint.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        sizeHint.setForeground(MUTED);
        content.add(sizeHint);
        content.add(Box.createVerticalStrut(24));
        JLabel section = label("조작 키");
        section.setForeground(Color.CYAN);
        section.setFont(new Font(Font.DIALOG, Font.BOLD, 18));
        content.add(section);
        content.add(Box.createVerticalStrut(4));
        JLabel hint = label("키 칸 선택 후 입력  ·  Esc로 해제");
        hint.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        hint.setForeground(MUTED);
        content.add(hint);
        content.add(Box.createVerticalStrut(12));
        JPanel bindings = new JPanel(new GridLayout(GameAction.values().length, 3, 10, 8));
        bindings.setBackground(BACKGROUND);
        bindings.add(label("동작"));
        bindings.add(label("기본 키"));
        bindings.add(label("보조 키"));
        for (GameAction action : GameAction.values()) {
            if (action == GameAction.PAUSE_MENU) {
                continue;
            }
            bindings.add(label(actionLabel(action)));
            JButton main = keyButton(action, true);
            JButton sub = keyButton(action, false);
            keyButtons.put(action, new JButton[] {main, sub});
            bindings.add(main);
            bindings.add(sub);
        }
        content.add(bindings);

        JPanel actions = new JPanel();
        actions.setBackground(BACKGROUND);
        actions.setBorder(BorderFactory.createEmptyBorder(12, 12, 16, 12));
        JButton reset = new JButton("모든 설정 기본값");
        styleButton(reset, false);
        reset.addActionListener(event -> restoreDefaults());
        actions.add(reset);
        if (allowScoreReset) {
            JButton clearScores = new JButton("스코어보드 기록 초기화");
            styleButton(clearScores, false);
            clearScores.addActionListener(event -> clearScores());
            actions.add(clearScores);
        }
        JButton save = new JButton("저장");
        styleButton(save, true);
        save.addActionListener(event -> saveSettings());
        actions.add(save);
        JButton cancel = new JButton("취소");
        styleButton(cancel, false);
        cancel.addActionListener(event -> close());
        actions.add(cancel);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BACKGROUND);
        add(scroll, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY_FONT);
        label.setForeground(Color.WHITE);
        return label;
    }

    private static void styleCombo(JComboBox<?> combo) {
        combo.setUI(new BasicComboBoxUI() {
            @Override
            public void paintCurrentValueBackground(Graphics graphics, Rectangle bounds, boolean focused) {
                graphics.setColor(SURFACE);
                graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            @Override
            protected JButton createArrowButton() {
                JButton arrow = new JButton("▼");
                arrow.setUI(new BasicButtonUI());
                arrow.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
                arrow.setBackground(SURFACE);
                arrow.setForeground(Color.CYAN);
                arrow.setBorder(BorderFactory.createLineBorder(OUTLINE));
                return arrow;
            }
        });
        combo.setFont(BODY_FONT);
        combo.setBackground(SURFACE);
        combo.setForeground(Color.WHITE);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list,
                    Object value, int index, boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                setOpaque(true);
                setBackground(index < 0 ? SURFACE : selected ? Color.CYAN : SURFACE);
                setForeground(index >= 0 && selected ? BACKGROUND : Color.WHITE);
                return this;
            }
        });
    }

    private static void styleButton(JButton button, boolean accent) {
        button.setUI(new BasicButtonUI());
        button.setFont(BODY_FONT);
        button.setBackground(accent ? Color.CYAN : SURFACE);
        button.setForeground(accent ? BACKGROUND : Color.WHITE);
        button.setFocusPainted(false);
        setButtonBorder(button, accent ? Color.CYAN : OUTLINE);
        button.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                setButtonBorder(button, Color.YELLOW);
            }

            @Override
            public void focusLost(FocusEvent event) {
                setButtonBorder(button, accent ? Color.CYAN : OUTLINE);
            }
        });
    }

    private static void setButtonBorder(JButton button, Color color) {
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)));
    }

    private JButton keyButton(GameAction action, boolean main) {
        JButton button = new JButton();
        styleButton(button, false);
        button.setToolTipText(actionLabel(action) + (main ? " 기본 키 변경" : " 보조 키 변경"));
        button.setPreferredSize(new java.awt.Dimension(110, button.getPreferredSize().height));
        button.addActionListener(event -> beginKeyCapture(action, main, button));
        return button;
    }

    private void beginKeyCapture(GameAction action, boolean main, JButton button) {
        if (captureAction == null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyCapture);
        }
        captureAction = action;
        captureMain = main;
        capturedCode = KeyEvent.VK_UNDEFINED;
        refreshKeys();
        button.setText("입력 대기…");
        button.setForeground(Color.YELLOW);
        button.requestFocusInWindow();
    }

    /** 키 캡처 동안 버튼의 기본 키 동작을 막고 편집기에만 전달한다. */
    private boolean captureKey(KeyEvent event) {
        if (captureAction == null) {
            return false;
        }
        if (event.getID() == KeyEvent.KEY_PRESSED && capturedCode == KeyEvent.VK_UNDEFINED) {
            capturedCode = event.getKeyCode();
            if (captureMain) {
                keyEditor.captureMainKey(captureAction, capturedCode);
            } else {
                keyEditor.captureSubKey(captureAction, capturedCode);
            }
            refreshKeys();
        } else if (event.getID() == KeyEvent.KEY_RELEASED
                && capturedCode == event.getKeyCode()) {
            endKeyCapture();
        }
        event.consume();
        return true;
    }

    private void endKeyCapture() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyCapture);
        captureAction = null;
        capturedCode = KeyEvent.VK_UNDEFINED;
    }

    private void cancelKeyCapture() {
        if (captureAction != null) {
            endKeyCapture();
            refreshKeys();
        }
    }

    private void close() {
        cancelKeyCapture();
        onClose.run();
    }

    private void loadCurrentValues() {
        AppSettings current = settings.current();
        screenSize.setSelectedItem(current.screenSize());
        difficulty.setSelectedItem(current.difficulty());
        colorBlind.setSelected(current.colorBlind());
        refreshKeys();
    }

    private void refreshKeys() {
        for (GameAction action : keyButtons.keySet()) {
            var binding = keyEditor.getDraftBinding(action);
            JButton[] buttons = keyButtons.get(action);
            buttons[0].setText(keyLabel(binding.mainKeyCode()));
            buttons[1].setText(keyLabel(binding.subKeyCode()));
            buttons[0].setForeground(Color.WHITE);
            buttons[1].setForeground(Color.WHITE);
        }
    }

    private void restoreDefaults() {
        cancelKeyCapture();
        screenSize.setSelectedItem(AppSettings.DEFAULT.screenSize());
        difficulty.setSelectedItem(AppSettings.DEFAULT.difficulty());
        colorBlind.setSelected(AppSettings.DEFAULT.colorBlind());
        keyEditor.restoreDefaults();
        refreshKeys();
    }

    private void saveSettings() {
        cancelKeyCapture();
        if (!keyEditor.canSave()) {
            JOptionPane.showMessageDialog(this, "모든 동작에 메인 키가 필요합니다.", "설정", JOptionPane.WARNING_MESSAGE);
            for (GameAction action : keyButtons.keySet()) {
                if (keyEditor.getDraftBinding(action).mainKeyCode().isEmpty()) {
                    keyButtons.get(action)[0].requestFocusInWindow();
                    break;
                }
            }
            return;
        }
        try {
            keyEditor.save();
            settings.save(new AppSettings((ScreenSize) screenSize.getSelectedItem(),
                    colorBlind.isSelected(), (Difficulty) difficulty.getSelectedItem()));
            close();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this,
                    e.getMessage() + "\n키 설정과 다른 설정은 별도 파일이므로 일부가 저장되었을 수 있습니다.",
                    "설정 저장 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearScores() {
        cancelKeyCapture();
        if (!allowScoreReset) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this, "모든 모드의 점수 기록을 삭제하시겠습니까?",
                "스코어보드 초기화", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            try {
                scores.clearScores();
                JOptionPane.showMessageDialog(this, "점수 기록을 초기화했습니다.");
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "초기화 실패", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static String keyLabel(OptionalInt code) {
        if (code.isEmpty()) {
            return "";
        }
        return switch (code.getAsInt()) {
            case KeyEvent.VK_ESCAPE -> "Esc";
            case KeyEvent.VK_LEFT -> "←";
            case KeyEvent.VK_RIGHT -> "→";
            case KeyEvent.VK_UP -> "↑";
            case KeyEvent.VK_DOWN -> "↓";
            case KeyEvent.VK_SPACE -> "Space";
            default -> KeyEvent.getKeyText(code.getAsInt());
        };
    }

    private static String actionLabel(GameAction action) {
        return switch (action) {
            case MOVE_LEFT -> "왼쪽 이동";
            case MOVE_RIGHT -> "오른쪽 이동";
            case SOFT_DROP -> "아래 이동";
            case HARD_DROP -> "즉시 낙하";
            case ROTATE -> "시계 방향 회전";
            case ROTATE_COUNTERCLOCKWISE -> "반시계 방향 회전";
            case PAUSE_MENU -> "일시정지 메뉴";
        };
    }
}
