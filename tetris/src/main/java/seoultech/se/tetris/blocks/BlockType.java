package seoultech.se.tetris.blocks;

/**
 * 테트로미노 7종의 정의.
 *
 * <p>각 블럭의 회전 상태 4개를 미리 정의해 두고, 회전은 인덱스를 순환시키는 방식으로 처리한다.
 * 회전축을 좌표로 계산하지 않으므로 블럭별 예외 처리가 필요 없고,
 * 이후 표준 회전 방식(SRS)으로 교체할 때도 아래 모양 데이터만 갈아끼우면 된다.
 *
 * <p><b>이 클래스는 색을 모른다.</b> 색 결정은 화면 쪽 팔레트의 역할이다.
 * (계층 규칙 2 — 모델은 색을 모른다)
 */
public enum BlockType {

    I('I', new String[][] {
            {
                "....",
                "IIII",
                "....",
                "...."
            },
            {
                "..I.",
                "..I.",
                "..I.",
                "..I."
            },
            {
                "....",
                "....",
                "IIII",
                "...."
            },
            {
                ".I..",
                ".I..",
                ".I..",
                ".I.."
            }
    }),

    O('#', new String[][] {
            {
                ".##.",
                ".##.",
                "....",
                "...."
            },
            {
                ".##.",
                ".##.",
                "....",
                "...."
            },
            {
                ".##.",
                ".##.",
                "....",
                "...."
            },
            {
                ".##.",
                ".##.",
                "....",
                "...."
            }
    }),

    T('T', new String[][] {
            {
                ".T..",
                "TTT.",
                "....",
                "...."
            },
            {
                ".T..",
                ".TT.",
                ".T..",
                "...."
            },
            {
                "....",
                "TTT.",
                ".T..",
                "...."
            },
            {
                ".T..",
                "TT..",
                ".T..",
                "...."
            }
    }),

    S('S', new String[][] {
            {
                ".SS.",
                "SS..",
                "....",
                "...."
            },
            {
                ".S..",
                ".SS.",
                "..S.",
                "...."
            },
            {
                "....",
                ".SS.",
                "SS..",
                "...."
            },
            {
                "S...",
                "SS..",
                ".S..",
                "...."
            }
    }),

    Z('Z', new String[][] {
            {
                "ZZ..",
                ".ZZ.",
                "....",
                "...."
            },
            {
                "..Z.",
                ".ZZ.",
                ".Z..",
                "...."
            },
            {
                "....",
                "ZZ..",
                ".ZZ.",
                "...."
            },
            {
                ".Z..",
                "ZZ..",
                "Z...",
                "...."
            }
    }),

    J('J', new String[][] {
            {
                "J...",
                "JJJ.",
                "....",
                "...."
            },
            {
                ".JJ.",
                ".J..",
                ".J..",
                "...."
            },
            {
                "....",
                "JJJ.",
                "..J.",
                "...."
            },
            {
                ".J..",
                ".J..",
                "JJ..",
                "...."
            }
    }),

    L('L', new String[][] {
            {
                "..L.",
                "LLL.",
                "....",
                "...."
            },
            {
                ".L..",
                ".L..",
                ".LL.",
                "...."
            },
            {
                "....",
                "LLL.",
                "L...",
                "...."
            },
            {
                "LL..",
                ".L..",
                ".L..",
                "...."
            }
    });

    /** 모양 격자의 한 변 길이. 모든 블럭이 동일한 크기를 사용한다. */
    public static final int SIZE = 4;

    /** 회전 상태의 개수. */
    public static final int ROTATIONS = 4;

    /** 빈 칸을 나타내는 문자. */
    private static final char EMPTY = '.';

    /** 보드 배열에서 빈 칸을 나타내는 번호. */
    public static final int EMPTY_CODE = 0;

    /** 번호로 블럭 종류를 찾기 위한 표. 인덱스가 곧 번호다. */
    private static final BlockType[] BY_CODE = buildCodeTable();

    private final char symbol;
    private final boolean[][][] shapes;

    BlockType(char symbol, String[][] patterns) {
        this.symbol = symbol;
        this.shapes = parse(patterns);
    }

    /**
     * 이 블럭을 화면에 표시할 때 사용하는 문자.
     *
     * <p>색상과 별개로 블럭을 구분할 수 있게 하기 위한 것이다.
     * 색각이상자를 위한 요구사항을 색상만으로 충족하지 않도록,
     * 기본 상태에서도 문자로 구분이 가능해야 한다.
     */
    public char symbol() {
        return symbol;
    }

    /**
     * 지정한 회전 상태에서 (row, col) 칸이 채워져 있는지 여부.
     *
     * @param rotation 회전 상태. 범위를 벗어나면 4로 나눈 나머지로 순환한다.
     */
    public boolean isFilled(int rotation, int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            return false;
        }
        return shapes[normalize(rotation)][row][col];
    }

    /**
     * 지정한 회전 상태의 모양을 복사해서 반환한다.
     *
     * <p>내부 배열을 직접 넘기지 않으므로 호출한 쪽에서 수정해도 원본은 바뀌지 않는다.
     */
    public boolean[][] shape(int rotation) {
        boolean[][] source = shapes[normalize(rotation)];
        boolean[][] copy = new boolean[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            System.arraycopy(source[r], 0, copy[r], 0, SIZE);
        }
        return copy;
    }

    /** 회전 상태 번호를 0 이상 {@link #ROTATIONS} 미만으로 정규화한다. */
    public static int normalize(int rotation) {
        return Math.floorMod(rotation, ROTATIONS);
    }

    /**
     * 보드 배열에 저장할 번호.
     *
     * <p>{@link #EMPTY_CODE}와 겹치지 않도록 1부터 시작한다.
     *
     * <p><b>번호와 블럭 종류의 대응은 이 클래스에서만 정의한다.</b>
     * 화면 쪽에서 별도의 대응표를 만들면 양쪽이 어긋나도 컴파일 오류가 나지 않아
     * 조용히 잘못 그려지게 된다. 번호를 되돌릴 때는 {@link #fromCode(int)}를 사용한다.
     */
    public int code() {
        return ordinal() + 1;
    }

    /**
     * 보드 배열의 번호를 블럭 종류로 되돌린다.
     *
     * @param code {@link #code()}가 반환한 번호
     * @return 해당 블럭 종류. {@link #EMPTY_CODE}이면 {@code null}
     * @throws IllegalArgumentException 정의되지 않은 번호인 경우
     */
    public static BlockType fromCode(int code) {
        if (code == EMPTY_CODE) {
            return null;
        }
        if (code < 0 || code >= BY_CODE.length || BY_CODE[code] == null) {
            throw new IllegalArgumentException("정의되지 않은 블럭 번호입니다: " + code);
        }
        return BY_CODE[code];
    }

    private static BlockType[] buildCodeTable() {
        BlockType[] table = new BlockType[values().length + 1];
        for (BlockType type : values()) {
            table[type.code()] = type;
        }
        return table;
    }

    private static boolean[][][] parse(String[][] patterns) {
        if (patterns.length != ROTATIONS) {
            throw new IllegalArgumentException("회전 상태는 " + ROTATIONS + "개여야 합니다.");
        }

        boolean[][][] result = new boolean[ROTATIONS][SIZE][SIZE];
        for (int rotation = 0; rotation < ROTATIONS; rotation++) {
            String[] rows = patterns[rotation];
            if (rows.length != SIZE) {
                throw new IllegalArgumentException("모양은 " + SIZE + "줄이어야 합니다.");
            }
            for (int r = 0; r < SIZE; r++) {
                String row = rows[r];
                if (row.length() != SIZE) {
                    throw new IllegalArgumentException("모양의 각 줄은 " + SIZE + "칸이어야 합니다.");
                }
                for (int c = 0; c < SIZE; c++) {
                    result[rotation][r][c] = row.charAt(c) != EMPTY;
                }
            }
        }
        return result;
    }
}
