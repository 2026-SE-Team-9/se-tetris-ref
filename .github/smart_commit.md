---
description: (Fixed) 테트리스 프로젝트 전용 Git Flow 변경사항 분석, 커밋, 푸시 후 PR 상태를 확인하여 생성하거나 최신화합니다. 다중 주제 자동 분할 및 AI 리뷰 코멘트 분리 등록 포함.
---

> **참고:** 브랜치명과 PR 제목은 아래 6가지 유형만 사용합니다.
>
> | 유형 | 용도 | 브랜치명 | PR 제목 |
> | :--- | :--- | :--- | :--- |
> | `feat` | 새로운 기능 구현 | `feat/<작업명>` | `feat: <작업 내용>` |
> | `fix` | 버그 수정 | `fix/<작업명>` | `fix: <작업 내용>` |
> | `refactor` | 코드 구조 개선 (로직 불변) | `refactor/<작업명>` | `refactor: <작업 내용>` |
> | `test` | 단위/통합 테스트 추가 및 코드 커버리지 확보 | `test/<작업명>` | `test: <작업 내용>` |
> | `docs` | 문서 작성 및 수정 (README, 명세서 등) | `docs/<작업명>` | `docs: <작업 내용>` |
> | `chore` | 빌드/환경 설정, 라이브러리 추가, `.gitignore` 등 | `chore/<작업명>` | `chore: <작업 내용>` |
>
> 브랜치의 `<작업명>`은 영문 소문자 kebab-case로 작성하고, PR 제목은 반드시 해당 브랜치 유형과 동일한 접두사를 사용합니다.
> **언어:** 모든 결과 보고 및 PR 본문/댓글은 **한글**로 작성합니다.

// turbo-all

---

## 0. GitHub CLI 환경 설정 및 인증 확인 (필수!)

```bash
# GH CLI 경로 설정 (필요시)
if ! command -v gh &> /dev/null; then
    if [ -f "/opt/homebrew/bin/gh" ]; then
        export PATH="/opt/homebrew/bin:$PATH"
    elif [ -f "/usr/local/bin/gh" ]; then
        export PATH="/usr/local/bin:$PATH"
    fi
fi

# GH CLI 설치 확인
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

## 0-1. 브랜치 전략 준수 확인 (Git Flow 변형, 필수!)

**⚠️ 직접 push 금지 브랜치**: `main`, `dev`

```bash
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)

case "$CURRENT_BRANCH" in
  main|dev)
    echo "❌ $CURRENT_BRANCH 브랜치에 직접 push할 수 없습니다. 작업 유형에 맞는 브랜치를 생성하세요."
    exit 1
    ;;
  *)
    if [[ ! "$CURRENT_BRANCH" =~ ^(feat|fix|refactor|test|docs|chore)/[a-z0-9]+(-[a-z0-9]+)*$ ]]; then
      echo "❌ 허용되지 않는 브랜치명입니다. <type>/<kebab-case-작업명> 형식을 사용하세요."
      exit 1
    fi
    ;;
esac
```

| 현재 브랜치 | push 가능? | 조치 |
| :--- | :--- | :--- |
| `main` | ❌ 금지 | "main에 직접 push할 수 없습니다. 작업 유형에 맞는 브랜치를 생성하세요." 안내 후 **중단** |
| `dev` | ❌ 금지 | "dev에 직접 push할 수 없습니다. 작업 유형에 맞는 브랜치를 생성하세요." 안내 후 **중단** |
| `feat/*`, `fix/*`, `refactor/*`, `test/*`, `docs/*`, `chore/*` | ✅ 허용 (Target: `dev`) | 계속 진행 |
| 그 외 패턴 | ❌ 금지 | 변경사항에 맞는 허용 브랜치명으로 변경 또는 새 브랜치 생성 후 계속 진행 |

**main 또는 dev에 있는 경우 → 새 브랜치 생성 제안**:
```bash
# 새로운 기능 구현 시 (dev 기반)
git checkout -b feat/<기능명> dev

# 버그 수정 시 (dev 기반)
git checkout -b fix/<이슈설명> dev

# 테스트 추가 및 커버리지 확보 시 (dev 기반)
git checkout -b test/<작업명> dev
```

---

## 1. 현재 상태 및 변경사항 분석 (주제별 분리 필수!)

```bash
git status -s
```

변경된 파일들의 도메인/주제(Scope)를 분석하여 **단일 주제**인지 **다중 주제**인지 판단합니다:
- 🎮 **게임 로직 / 블럭:** `tetris/src/main/java/**` (보드 판정, 7종 테트로미노, 충돌, 자동 낙하, 회전, 스코어/점수 계산, 설정 영속성)
- 🖥️ **UI / 메뉴 / 렌더러:** `tetris/src/main/java/**` (메인 메뉴, 게임 화면, 색맹 모드 렌더링, 키 설정 화면, 다이얼로그)
- ⚙️ **빌드 / 설정 / CI:** `build.gradle`, `settings.gradle`, `gradle/**`, `.github/**`
- 📝 **문서 및 산출물:** `README.md`, `docs/**`
- 🧪 **단위/통합 테스트:** `tetris/src/test/**`

### 🔀 분기 판단:
1. **단일 주제인 경우:**
   - 현재 브랜치에서 변경사항을 원자적으로 커밋하고 푸시하여 **단일 PR**을 생성합니다. (아래 **2단계** 진행)
2. **다중 주제(예: 게임 로직 버그 수정 + UI 메뉴 개편 + 문서 작업 등 성격이 다른 변경이 공존)인 경우:**
   - ⚠️ **단순히 커밋만 나누는 것으로는 부족하며, 반드시 주제별로 별도의 독립 브랜치를 생성하여 각각 독립된 PR로 분리해야 합니다!**
   - 아래 **"1-A. 다중 주제 브랜치 및 다중 PR 분할 워크플로우"**에 따라 작업을 분리하여 수행합니다.

---

## 1-A. 다중 주제 브랜치 및 다중 PR 분할 워크플로우 (Multi-Topic Branch & PR Split)

성격이 다른 변경사항들이 섞여 있는 경우, 아래 절차에 따라 각 주제별로 브랜치를 파서 독립적인 PR을 순차적으로 생성합니다:

> **핵심 원리:** 기본 베이스 브랜치(`dev`) 기준으로 첫 번째 작업 브랜치를 파서 해당 파일만 커밋/PR을 올리고, 다시 베이스 브랜치 기준으로 두 번째 작업 브랜치를 파서 나머지 파일들을 커밋/PR로 올립니다.

### [실행 절차 예시] 로직 수정(Topic A)과 UI 개선(Topic B)이 섞여 있는 경우:

#### 1단계: 첫 번째 주제 (Topic A: 게임 로직) 브랜치 및 PR 생성
```bash
# 1. dev 기준으로 첫 번째 기능 브랜치 생성 (워킹 트리의 변경사항은 그대로 보존됨)
git checkout -b fix/tetris-collision-logic dev

# 2. Topic A에 해당하는 파일들만 선택적으로 스테이징
git add tetris/src/main/java/com/tetris/model/ tetris/src/test/

# 3. Topic A 원자적 커밋 및 푸시
git commit -m "fix(logic): 블럭 회전 시 보드 경계 충돌 판정 오류 수정"
git push -u origin fix/tetris-collision-logic

# 4. Topic A에 대한 독립 PR 생성 및 AI 리뷰 코멘트 등록
# (아래 4-A 단계의 Step 2 ~ Step 4와 동일하게 수행)
```

#### 2단계: 두 번째 주제 (Topic B: UI/메뉴) 브랜치 및 PR 생성
```bash
# 1. 다시 dev 기준으로 두 번째 기능 브랜치 생성 (남아있는 Topic B 변경사항 보존됨)
git checkout -b feat/menu-colorblind-ui dev

# 2. Topic B에 해당하는 파일들 스테이징
git add tetris/src/main/java/com/tetris/view/

# 3. Topic B 원자적 커밋 및 푸시
git commit -m "feat(ui): 색맹 모드 블럭 텍스처 패턴 및 시작 메뉴 UI 개선"
git push -u origin feat/menu-colorblind-ui

# 4. Topic B에 대한 독립 PR 생성 및 AI 리뷰 코멘트 등록
# (아래 4-A 단계의 Step 2 ~ Step 4와 동일하게 수행)
```

#### 3단계: 다중 PR 최종 보고
- 생성된 각 브랜치의 PR 링크(PR #1, PR #2)를 일목요연하게 보고합니다.

---

## 2. 조건부 커밋 및 푸시 (단일 주제인 경우)

**단일 주제 변경사항이 있는 경우에만 실행**:

> [!IMPORTANT]
> **원자적 커밋(Atomic Commits)**: 변경사항이 여러 기능이나 서로 다른 수정 사항을 포함하고 있다면, `git add -p` 등을 사용하여 **기능별로 커밋을 나누어** 진행하십시오. 한 번에 모든 변경사항을 하나의 커밋으로 묶지 마십시오.

```bash
# 기능별로 나누어 스테이징 및 커밋 (필요시 반복)
# git add <file_functional_group>
# git commit -m "<type>(<scope>): <설명>"

# Conventional Commit 메시지 생성 (diff 분석 기반)
git commit -m "<type>: <설명>"

# 원격에 푸시
git push origin $CURRENT_BRANCH
```

> [!IMPORTANT]
> **PR 브랜치에 push한 경우 AI 코드 리뷰 댓글 등록은 필수입니다.** 기존 리뷰 댓글이 있더라도 최신 HEAD 전체 diff를 다시 분석하여 새 리뷰 댓글을 남겨야 하며, 리뷰 결과가 `치명적 0건`이어도 생략할 수 없습니다. 코드 리뷰 내용은 PR 본문에 포함하지 않습니다.

---

## 3. Target Branch 결정 (Git Flow 변형)

| 현재 브랜치 패턴 | Target Branch | 설명 |
| :--- | :--- | :--- |
| `feat/*`, `fix/*`, `refactor/*`, `test/*`, `docs/*`, `chore/*` | **`dev`** | 기능 개발, 수정, 테스트, 문서 및 환경 작업 |

```bash
TARGET_BRANCH="dev"
```

---

## 4. PR 존재 여부 확인 (필수!)

```bash
PR_URL=$(gh pr view --json url,state --jq 'select(.state == "OPEN") | .url' 2>/dev/null || echo "")
```

| 결과 | 상태 |
| :--- | :--- |
| URL 있음 | PR이 이미 존재 → **4-B로** (업데이트) |
| 비어있음 | PR 없음 → **4-A로** (신규 생성) |

---

## 4-A. PR 신규 생성 (PR이 없는 경우)

**반드시 실행해야 하는 단계**:

### Step 1: 원격과의 차이 확인

```bash
git fetch origin $TARGET_BRANCH
COMMITS=$(git log origin/$TARGET_BRANCH..$CURRENT_BRANCH --oneline)
```

- 커밋이 없으면: "base 브랜치($TARGET_BRANCH) 대비 새로운 커밋이 없습니다." 보고 후 종료

### Step 2: PR 본문 및 AI 코드 리뷰 분리 작성

1. **PR 본문 작성:** 반드시 **`.github/pull_request_template.md`** 파일의 양식을 기반으로 `.pr_body_temp.md`를 작성합니다:
   - 📌 배경 및 목적 (Background)
   - 📋 주요 변경 사항 (Change Summary)
   - 📁 변경된 파일 (Changed Files)
   - 🧪 검증 절차 및 결과 (Testing Procedure: `.\gradlew.bat test`, Java 21 게임 동작, 기능/비기능 체크)
   - 📝 추가 참고사항 (Additional Notes)
   - 🔀 Merge 가이드 (Target: `<TARGET_BRANCH>`, Squash and Merge 권장)

2. **AI 코드 리뷰 작성:** 반드시 **`.github/code_review_template.md`** 파일의 테트리스 체크포인트(게임플레이, 20x10 보드, 7종 블럭, 색맹 모드, 점수판 영속성, Java 21 호환 등)와 3단계 우선순위(🔴치명적 / ⚠️경고 / 💡제안)를 확인합니다.
   - `git diff origin/$TARGET_BRANCH...$CURRENT_BRANCH`를 분석하여 `.pr_review_temp.md`에 작성합니다.
   - 리뷰에는 검토한 최신 HEAD SHA와 검증 결과를 포함합니다.
   - `.pr_review_temp.md`의 내용은 `.pr_body_temp.md`에 복사하거나 합치지 않습니다. PR 본문과 코드 리뷰 댓글은 반드시 분리합니다:
   ```markdown
   ## 🤖 AI Code Reviewer Report (.github/code_review_template.md 기반)

   > 본 리뷰는 코드 품질 참고용으로 자동 분석된 사전 검토 피드백이며, PR 머지를 차단하지 않습니다 (Non-blocking Report).

   ### 📊 종합 요약
   <diff 기반 주요 변경 및 도메인 영향 요약>

   ### 🔴 치명적 (Critical)
   *(없으면 '해당 없음')*
   - **파일:라인** - 이슈 설명 및 수정 권고안

   ### ⚠️ 경고 (Warning)
   *(없으면 '해당 없음')*
   - **파일:라인** - 이슈 설명

   ### 💡 제안 (Suggestion)
   - (개선 제안 1-3개, 없으면 생략)
   ```

### Step 3: Issue 자동 생성 및 연결 (현재 레포 기준)

PR과 연결할 이슈를 확인하거나, 없으면 현재 저장소에 새로 생성하여 연결합니다:

```bash
# 1. PR 제목 정의: 브랜치 접두사와 동일한 유형을 사용
# 예: feat/menu-colorblind-ui -> feat: 색맹 모드 메뉴 UI 구현
BRANCH_TYPE="${CURRENT_BRANCH%%/*}"
PR_TITLE="$BRANCH_TYPE: <종합된 변경 제목>"

# 2. 브랜치 이름에서 이슈 번호 감지 (예: feat/12-foo -> 12)
DETECTED_ISSUE_NUM=$(echo "$CURRENT_BRANCH" | grep -oE '/[0-9]+(-|$)' | tr -d '/-')

EXISTING_ISSUE_FOUND=false

if [ -n "$DETECTED_ISSUE_NUM" ]; then
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

### Step 4: PR 생성 및 AI 코드 리뷰 코멘트 분리 등록

```bash
# 1. PR 생성 (.github/pull_request_template.md 기반 본문)
PR_URL=$(gh pr create \
  --title "$PR_TITLE" \
  --body-file .pr_body_temp.md \
  --base $TARGET_BRANCH)

echo "✅ PR 생성 완료: $PR_URL"

# 2. PR 생성 직후 별도 댓글(Comment)로 AI 코드 리뷰 필수 등록
test -s .pr_review_temp.md || {
  echo "❌ Error: AI 코드 리뷰 파일이 없거나 비어 있습니다. PR 본문이 아닌 별도 댓글용 리뷰를 작성하세요."
  exit 1
}

REVIEW_COMMENT_URL=$(gh pr comment "$PR_URL" --body-file .pr_review_temp.md)
echo "🤖 AI 코드 리뷰 코멘트 등록 완료: $REVIEW_COMMENT_URL"
```

### Step 5: 임시 파일 정리

```bash
rm -f .pr_body_temp.md .pr_review_temp.md
```

---

## 4-B. 기존 PR 업데이트 (PR이 있는 경우)

### Step 1: 변경 내역 분석

```bash
git fetch origin $TARGET_BRANCH
git log origin/$TARGET_BRANCH..$CURRENT_BRANCH --oneline
```

### Step 2: PR 본문 및 AI 코드 리뷰 재작성

`.github/pull_request_template.md` 및 `.github/code_review_template.md`를 참조하여 최신 변경사항을 `.pr_body_temp.md` 및 `.pr_review_temp.md`에 재작성합니다.

> **필수:** PR 브랜치에 새 커밋을 push할 때마다 최신 HEAD 기준 전체 diff를 다시 검토하고 새 AI 코드 리뷰 댓글을 등록합니다. 이전 리뷰가 존재하거나 지적 사항이 0건이어도 생략하지 않습니다. 리뷰에는 검토한 HEAD SHA와 검증 결과를 포함하며, 리뷰 내용을 PR 본문에 넣지 않습니다.

### Step 3: PR 본문 업데이트 및 새 리뷰 코멘트 등록

```bash
# 1. PR 본문 갱신
BRANCH_TYPE="${CURRENT_BRANCH%%/*}"
PR_TITLE="$BRANCH_TYPE: <종합된 변경 제목>"

gh pr edit \
  --title "$PR_TITLE" \
  --body-file .pr_body_temp.md

# 2. 최신 HEAD 기준 AI 코드 리뷰 댓글 필수 등록
test -s .pr_review_temp.md || {
  echo "❌ Error: AI 코드 리뷰 파일이 없거나 비어 있습니다. PR 본문이 아닌 별도 댓글용 리뷰를 작성하세요."
  exit 1
}

REVIEW_COMMENT_URL=$(gh pr comment "$PR_URL" --body-file .pr_review_temp.md)
echo "🤖 최신 AI 코드 리뷰 코멘트 등록 완료: $REVIEW_COMMENT_URL"
```

### Step 4: 정리

```bash
rm -f .pr_body_temp.md .pr_review_temp.md
```

---

## 5. 최종 보고

반드시 아래 내용을 보고:

| 항목 | 값 |
| :--- | :--- |
| 브랜치 | `$CURRENT_BRANCH` |
| 커밋 | O / X (커밋 메시지) |
| 푸시 | O / X |
| PR 상태 | 신규 생성 / 업데이트 / 변경없음 |
| PR URL | `<URL>` |
| Issue | `#<Number>` (신규 생성 또는 기존 연결) |
| Target | `$TARGET_BRANCH` (`dev`) |
| AI 리뷰 | O / X (최신 HEAD SHA 및 댓글 URL) |

---

## ⚠️ 주의사항

1. **main 및 dev에 직접 push 절대 금지** - 반드시 허용 브랜치(`feat/*`, `fix/*`, `refactor/*`, `test/*`, `docs/*`, `chore/*`)에서 PR을 통해 `dev`에 병합합니다.
2. **PR 본문과 AI 리뷰 분리** - PR 본문은 `.github/pull_request_template.md` 기반으로 작성하고, AI 리뷰는 `.github/code_review_template.md` 기반으로 분석하여 별도 댓글(`gh pr comment`)로 분리 등록합니다.
3. **PR 존재 확인 필수** - `gh pr view`로 먼저 확인 후 신규 생성 또는 업데이트를 결정합니다.
4. **다중 주제 분할 필수** - 변경사항에 둘 이상의 성격(예: 게임 로직 + UI 개편)이 섞인 경우, 반드시 브랜치를 분할하여 개별 PR을 생성합니다.
5. **이슈 자동 연결**: 브랜치 이름에 번호(예: `feat/12-foo`)가 있으면 해당 이슈를 연결하고, 없으면 새로 생성합니다.
6. **PR 제목 유형 일치**: PR 제목은 반드시 현재 브랜치 접두사와 동일한 유형으로 시작해야 합니다. (예: `test/score-coverage` → `test: 점수 계산 테스트 및 커버리지 확보`)
7. **코드 리뷰는 별도 댓글로 필수 등록**: AI 코드 리뷰를 PR 본문에 포함하지 말고, `gh pr comment --body-file .pr_review_temp.md`로 등록합니다. push 후에는 최신 HEAD 전체 diff를 다시 검토하며, 지적 사항이 없어도 댓글 등록을 생략하지 않습니다.

---

## 흐름도

```
시작
  │
  ▼
0. GH CLI 환경 및 인증 확인
  │
  ▼
1. 변경사항 및 도메인(Scope) 분석 (git status -s)
  │
  ├── [다중 주제 감지] (예: 게임 로직 + UI/메뉴 변경 혼재)
  │     │
  │     ├── [Topic A 브랜치] (dev 기준 `git checkout -b fix/... dev`)
  │     │     │
  │     │     ▼
  │     │   Topic A 관련 파일 선택적 스테이징 (`git add ...`)
  │     │     │
  │     │     ▼
  │     │   Topic A 원자적 커밋 & 푸시
  │     │     │
  │     │     ▼
  │     │   Topic A 독립 PR 생성 (.github/pull_request_template.md)
  │     │     │
  │     │     ▼
  │     │   Topic A AI 리뷰 댓글 자동 등록 (.github/code_review_template.md)
  │     │
  │     └── [Topic B 브랜치] (dev 기준 `git checkout -b feat/... dev`)
  │           │
  │           ▼
  │         Topic B 관련 파일 스테이징
  │           │
  │           ▼
  │         Topic B 원자적 커밋 & 푸시
  │           │
  │           ▼
  │         Topic B 독립 PR 생성 & AI 리뷰 댓글 등록 (.github/code_review_template.md)
  │           │
  │           ▼
  │         다중 PR 최종 보고
  │
  └── [단일 주제]
        │
        ▼
      현재 브랜치 확인
        │
        ├── `main` 또는 `dev` 브랜치 ──▶ 작업 유형에 맞는 `<type>/<작업명>` 브랜치 생성
        │
        └── `feat/*` / `fix/*` / `refactor/*` / `test/*` / `docs/*` / `chore/*` 브랜치 유지
              │
              ▼
            원자적 커밋 & 푸시
              │
              ▼
            Target Branch 결정 (`dev`)
              │
              ▼
            PR 존재 여부 확인 (gh pr view)
              │
              ├── [기존 PR 존재] ──▶ PR 본문 갱신 & 새 리뷰 댓글 등록
              │
              └── [신규 PR 필요]
                    │
                    ▼
                  이슈 연결 (기존 이슈 또는 gh issue create)
                    │
                    ▼
                  PR 신규 생성 (.github/pull_request_template.md)
                    │
                    ▼
                  AI 코드 리뷰 댓글 자동 등록 (.github/code_review_template.md)
                    │
                    ▼
                  최종 보고
```
