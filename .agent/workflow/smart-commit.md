---
description: (Fixed) 변경사항 분석, 커밋, 푸시 후 PR 상태를 확인하여 생성하거나 최신화합니다. GH CLI 자동 경로 설정 포함.
---

> **참고:** 커밋 컨벤션은 Conventional Commits(`feat`, `fix`, `docs`, `refactor`, `chore` 등)를 따릅니다.
> **언어:** 모든 결과 보고 및 PR 본문은 **한글**로 작성합니다.

// turbo-all

---

## 0. 환경 설정 및 브랜치 전략 준수 확인 (필수!)

**GH CLI 설정**:

```bash
# GH CLI 경로 설정
if ! command -v gh &> /dev/null; then
    if [ -f "/opt/homebrew/bin/gh" ]; then
        export PATH="/opt/homebrew/bin:$PATH"
    elif [ -f "/usr/local/bin/gh" ]; then
        export PATH="/usr/local/bin:$PATH"
    fi
fi

# GH CLI 확인
if ! command -v gh &> /dev/null; then
    echo "❌ Error: 'gh' command not found. Please install GitHub CLI."
    exit 1
fi

# Auth Status 확인
if ! gh auth status &> /dev/null; then
    echo "❌ Error: GitHub CLI is not authenticated. Please run 'gh auth login'."
    exit 1
fi
```

---

## 0. 브랜치 전략 준수 확인 (필수!)

**⚠️ 직접 push 금지 브랜치**: `main`, `dev`

```bash
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
```

| 현재 브랜치                      | push 가능? | 조치                                                                            |
| -------------------------------- | ---------- | ------------------------------------------------------------------------------- |
| `main`                           | ❌ 금지    | "main에 직접 push할 수 없습니다. feature 브랜치를 생성하세요." 안내 후 **중단** |
| `dev`                            | ❌ 금지    | "dev에 직접 push할 수 없습니다. feature 브랜치를 생성하세요." 안내 후 **중단**  |
| `feature/*`, `fix/*`, `hotfix/*` | ✅ 허용    | 계속 진행                                                                       |

**main/dev에 있는 경우 → 새 브랜치 생성 제안**:

```bash
# 권장 명령어 안내
git checkout -b feature/<기능명>
# 또는
git checkout -b fix/<이슈설명>
```

---

## 1. 현재 상태 및 변경사항 확인

```bash
git status
git add .
git diff --staged --stat
```

- 스테이징된 변경사항이 있는지 확인
- 없으면 "커밋할 내용이 없습니다" 안내 후 **3단계로 건너뛰기**

---

## 2. 조건부 커밋 및 푸시

**변경사항이 있는 경우에만 실행**:

> [!IMPORTANT]
> **원자적 커밋(Atomic Commits)**: 변경사항이 여러 기능이나 서로 다른 수정 사항을 포함하고 있다면, `git add -p` 등을 사용하여 **기능별로 커밋을 나누어** 진행하십시오. 한 번에 모든 변경사항을 하나의 커밋으로 묶지 마십시오.

```bash
# 기능별로 나누어 스테이징 및 커밋 (필요시 반복)
# git add <file_functional_group>
# git commit -m "<type>(<scope>): <설명>"

# Conventional Commit 메시지 생성 (diff 분석 기반)
# Hook 실행을 위해 --no-verify 제거 (Lint/Type Check 수행)
git commit -m "<type>: <설명>"

# 원격에 푸시
# Hook 실행을 위해 --no-verify 제거 (Type Check 수행)
git push origin $CURRENT_BRANCH
```

---

## 3. Target Branch 결정

| 현재 브랜치 패턴           | Target Branch |
| -------------------------- | ------------- |
| `hotfix/*`                 | `main`        |
| `feature/*`, `fix/*`, 기타 | `dev`         |

```bash
if [[ "$CURRENT_BRANCH" == hotfix/* ]]; then
  TARGET_BRANCH="main"
else
  TARGET_BRANCH="dev"
fi
```

---

## 4. PR 존재 여부 확인 (필수!)

```bash
PR_URL=$(gh pr view --json url,state --jq 'select(.state == "OPEN") | .url' 2>/dev/null || echo "")
```

| 결과     | 상태                                  |
| -------- | ------------------------------------- |
| URL 있음 | PR이 이미 존재 → **4-B로** (업데이트) |
| 비어있음 | PR 없음 → **4-A로** (생성)            |

---

## 4-A. PR 신규 생성 (PR이 없는 경우)

**반드시 실행해야 하는 단계**:

### Step 1: 원격과의 차이 확인

```bash
git fetch origin $TARGET_BRANCH
COMMITS=$(git log origin/$TARGET_BRANCH..$CURRENT_BRANCH --oneline)
```

- 커밋이 없으면: "base 브랜치 대비 새로운 커밋이 없습니다." 보고 후 종료

### Step 2: PR 본문 작성 및 AI 코드 리뷰 (필수!)

1. **리뷰 가이드라인 참조:** 반드시 `.agent/workflow/code-review.md` 파일의 체크포인트(메뉴/게임플레이, 블럭/접근성, 점수/스코어보드, 설정 영속성, 테스트/배포)와 3단계 우선순위(🔴치명적 / ⚠️경고 / 💡제안)를 읽고 확인합니다.
2. **Diff 자체 리뷰 수행:** `git diff origin/$TARGET_BRANCH...$CURRENT_BRANCH`의 실제 변경 내용을 대조하여 AI 코드 리뷰를 작성합니다.
3. **PR 본문 작성:** `.pr_body_temp.md` 파일에 아래 서식에 맞추어 내용을 채워 넣습니다.
> **⚠️ 주의:** 코드 리뷰는 PR 검토자의 이해를 돕기 위한 보고서이며, **경고나 제안이 있더라도 PR 생성을 중단하거나 차단하지 않습니다.**

```markdown
## 📋 변경 사항

<커밋 기반 변경 내용 요약 - 한글로 작성>

## 📁 변경된 파일

<파일 목록>

## 🤖 AI 코드 리뷰 (.agent/workflow/code-review.md 기반)

> **안내:** 본 리뷰는 코드 이해 및 품질 참고용으로 자동 작성되었으며, PR 머지를 차단하지 않습니다.

### 요약
<diff 기반 주요 변경 및 영향 요약>

### 🔴 치명적 (N건)
*(없으면 생략 또는 '해당 없음')*

### ⚠️ 경고 (N건)
*(없으면 생략)*

### 💡 제안
- (개선 제안 1-3개, 없으면 생략)

## ✅ 체크리스트

- [ ] Gradle 빌드 및 테스트 실행 (`.\gradlew.bat test`)
- [ ] Java 21 이상 및 대상 OS에서 게임 실행 확인
- [ ] 메뉴, 게임플레이, 설정, 스코어보드, 종료 흐름 확인
- [ ] 저장/복원, 색맹 모드, 키 설정 및 초기화 동작 확인
- [ ] 테스트 커버리지와 비기능 요구사항 검증 결과 확인
- [ ] 아이콘을 포함한 배포용 실행 파일/패키지 실행 확인

## 🔀 Merge 가이드

- Target: `<TARGET_BRANCH>`
- Squash and Merge 권장
```

### Step 3: Issue 자동 생성 및 연결 (현재 레포 기준)

PR과 연결할 이슈를 확인하거나, 없으면 현재 저장소에 새로 생성하여 연결합니다.

```bash
# 1. PR 제목 정의
PR_TITLE="<종합된 변경 제목>"

# 2. 브랜치 이름에서 이슈 번호 감지 (예: feature/12-foo -> 12)
DETECTED_ISSUE_NUM=$(echo "$CURRENT_BRANCH" | grep -oE '/[0-9]+(-|$)' | tr -d '/-')

EXISTING_ISSUE_FOUND=false

if [ -n "$DETECTED_ISSUE_NUM" ]; then
  # 현재 저장소에 해당 이슈가 존재하는지 확인
  if gh issue view "$DETECTED_ISSUE_NUM" > /dev/null 2>&1; then
    echo "✅ 기존 이슈 #$DETECTED_ISSUE_NUM 확인됨. 해당 이슈에 연결합니다."
    ISSUE_NUM="$DETECTED_ISSUE_NUM"
    EXISTING_ISSUE_FOUND=true
  fi
fi

# 3. 기존 이슈가 없으면 현재 저장소에 새 이슈 자동 생성
if [ "$EXISTING_ISSUE_FOUND" = false ]; then
  echo "🆕 현재 저장소에 새 이슈를 생성합니다..."
  ISSUE_URL=$(gh issue create \
    --title "$PR_TITLE" \
    --body-file .pr_body_temp.md \
    --assignee "@me")

  ISSUE_NUM=${ISSUE_URL##*/}
  echo "✅ 이슈 #$ISSUE_NUM 생성 완료."
fi

# 4. PR 본문 하단에 자동 Close 키워드 추가
echo -e "\n\nCloses #$ISSUE_NUM" >> .pr_body_temp.md
```

### Step 4: PR 생성

```bash
gh pr create \
  --title "$PR_TITLE" \
  --body-file .pr_body_temp.md \
  --base $TARGET_BRANCH
```

### Step 5: 정리

```bash
rm .pr_body_temp.md
```

---

## 4-B. 기존 PR 업데이트 (PR이 있는 경우)

### Step 1: 변경 내역 분석

```bash
git fetch origin $TARGET_BRANCH
git log origin/$TARGET_BRANCH..$CURRENT_BRANCH --oneline
```

### Step 2: PR 본문 재작성

`.pr_body_temp.md` 파일에 최신 변경사항 및 `.agent/workflow/code-review.md` 기반 AI 코드 리뷰를 갱신하여 반영 (Non-blocking)

### Step 3: PR 업데이트

```bash
gh pr edit \
  --title "<종합된 변경 제목>" \
  --body-file .pr_body_temp.md
```

### Step 4: 정리

```bash
rm .pr_body_temp.md
```

---

## 5. 최종 보고

반드시 아래 내용을 보고:

| 항목    | 값                                     |
| ------- | -------------------------------------- |
| 브랜치  | `$CURRENT_BRANCH`                      |
| 커밋    | O / X (커밋 메시지)                    |
| 푸시    | O / X                                  |
| PR 상태 | 신규 생성 / 업데이트 / 변경없음        |
| PR URL  | `<URL>`                                |
| Issue   | `#<Number>` (신규 생성 또는 기존 연결) |
| Target  | `$TARGET_BRANCH`                       |

---

## ⚠️ 주의사항

1. **main/dev에 직접 push 금지** - feature 브랜치 사용 필수
2. **PR 본문 없이 생성 금지** - 항상 `.pr_body_temp.md` 작성 후 생성
3. **PR 존재 확인 필수** - gh pr view로 확인 후 생성/업데이트 결정
4. **변경사항 없어도 PR 상태 확인** - 기존 PR이 있으면 업데이트 가능
5. **이슈 자동 연결**: 브랜치 이름에 번호(예: `feature/12-foo`)가 있으면 해당 이슈를 연결하고, 없으면 새로 생성합니다.
6. **기능별 커밋 분리**: 하나의 커밋에 너무 많은 변경사항을 담지 말고 기능 단위로 나누어 커밋하십시오.

---

## 흐름도

```
시작
  │
  ▼
브랜치 확인 ──main/dev──▶ ❌ 중단
  │
  ▼ (feature/fix/hotfix)
  │
변경사항 있음? ──No──▶ 3단계로 건너뛰기
  │
  ▼ Yes
커밋 & 푸시
  │
  ▼
Target 결정
  │
  ▼
PR 존재? ──Yes──▶ 4-B: PR 업데이트
  │
  ▼ No
이슈 번호 감지? ──Yes (존재함)──▶ 기존 이슈 연결 ──┐
  │                                           │
  No (또는 없음)                              ▼
  └─────────────────────────────▶ 신규 이슈 생성 ──▶ PR 신규 생성
  │
  ▼
최종 보고
```
