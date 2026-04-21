#!/usr/bin/env bash
# E2E 시나리오 러너 — 직접 adb input/event 주입 + phase 단위 screenrecord.
# 용도: 일기 작성 → 단어 카드 추가 → 복습 시나리오를 1회 호출로 실행.
#
# 전제:
#   - ANDROID_SERIAL 또는 기본값 emulator-5554 가 연결되어 있음
#   - jq 설치
#   - android CLI (`android layout`) 사용 가능
#   - `.claude/cache/ui-coords.json` 에 기기 프로필·좌표 맵 존재
#
# 결과물:
#   test-artifacts/scenarios/<yyyymmdd-HHMMSS>/
#     01_write_diary.mp4
#     02_add_word_card.mp4
#     03_review.mp4
#     summary.md
#     run.log

set -uo pipefail

SERIAL="${ANDROID_SERIAL:-emulator-5554}"
ADB=(adb -s "$SERIAL")
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
UI_COORDS="$REPO_ROOT/.claude/cache/ui-coords.json"

TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUT_DIR="$REPO_ROOT/test-artifacts/scenarios/$TIMESTAMP"
mkdir -p "$OUT_DIR"
SUMMARY="$OUT_DIR/summary.md"
LOGFILE="$OUT_DIR/run.log"

RECORD_PID=""
RECORD_NAME=""

log() { printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*" | tee -a "$LOGFILE" >&2; }

die() { log "FATAL: $*"; exit 1; }

# --- preflight ---
preflight() {
  command -v jq >/dev/null || die "jq 필요. brew install jq"
  command -v android >/dev/null || die "android CLI 필요"
  [[ -f "$UI_COORDS" ]] || die "ui-coords.json 없음: $UI_COORDS"

  "${ADB[@]}" get-state >/dev/null 2>&1 || die "기기 $SERIAL 연결 안 됨"

  local res cached_res
  res="$(${ADB[@]} shell wm size | awk '{print $NF}' | tr -d '\r')"
  cached_res="$(jq -r .device.resolution "$UI_COORDS")"
  if [[ "$res" != "$cached_res" ]]; then
    log "WARN: 해상도 $res 이 캐시 $cached_res 와 다름 — 좌표 드리프트 가능"
  fi

  # IME 서브타입이 한국어면 en-US 로 전환
  local lang
  lang="$(${ADB[@]} shell dumpsys input_method 2>/dev/null \
    | grep -m1 '^    languageTag =' \
    | awk -F'= ' '{print $2}' | tr -d '\r' || true)"
  if [[ "$lang" == ko* ]]; then
    log "IME subtype=$lang → en 으로 전환"
    "${ADB[@]}" shell input keyevent 204
    sleep 0.4
  else
    log "IME subtype=$lang (전환 불필요)"
  fi
}

# --- coord lookup ---
# 사용: tap_by home fab_write | tap_by home bottom_nav.flashcard
tap_by() {
  local screen="$1" element="$2"
  local xy
  xy="$(jq -r --arg s "$screen" --arg e "$element" \
    '.screens[$s].elements[$e] | @tsv' "$UI_COORDS" 2>/dev/null)"
  if [[ -z "$xy" || "$xy" == "null" ]]; then
    log "ERR: 좌표 미발견 $screen.$element"
    return 1
  fi
  local x="${xy%%$'\t'*}" y="${xy##*$'\t'}"
  "${ADB[@]}" shell input tap "$x" "$y"
}

tap_xy() { "${ADB[@]}" shell input tap "$1" "$2"; }

type_text() { "${ADB[@]}" shell input text "$1"; }

backspace_n() {
  local n="${1:-40}"
  local seq
  seq="$(printf '67 %.0s' $(seq 1 "$n"))"
  # shellcheck disable=SC2086
  "${ADB[@]}" shell input keyevent $seq
}

key() { "${ADB[@]}" shell input keyevent "$1"; }

# --- layout 검증 ---
# assert_text "substring" [timeout_sec]
assert_text() {
  local needle="$1" timeout="${2:-5}"
  local deadline=$(( $(date +%s) + timeout ))
  local dump="/tmp/layout_assert.json"
  while (( $(date +%s) < deadline )); do
    android layout --device "$SERIAL" -o "$dump" >/dev/null 2>&1 || true
    if [[ -f "$dump" ]] && grep -qF -- "$needle" "$dump"; then
      return 0
    fi
    sleep 0.4
  done
  log "ASSERT FAIL: '$needle' ${timeout}s 내 미노출"
  return 1
}

# layout 덤프 후 주어진 text 의 center 좌표를 "x y" 로 출력
find_center_of() {
  local needle="$1"
  local dump="/tmp/layout_find.json"
  android layout --device "$SERIAL" -o "$dump" >/dev/null 2>&1 || return 1
  jq -r --arg t "$needle" \
    '.[] | select(.text==$t) | .center' "$dump" \
    | head -n1 | tr -d '[]' | tr ',' ' '
}

# --- screenrecord ---
start_record() {
  RECORD_NAME="$1"
  log "record start: $RECORD_NAME"
  # `adb shell screenrecord ...` 을 호스트 백그라운드로 띄우고 디바이스 PID 는 pidof 로 잡는다.
  # --size 720x1280 으로 지정: AVC 인코더가 일부 기기의 네이티브 해상도(예: 1280x2856)에서
  # 매크로블록 제약으로 실패하므로 안전한 크기로 고정. 비트레이트도 낮춰 용량 절감.
  "${ADB[@]}" shell "screenrecord --size 720x1280 --bit-rate 2000000 /sdcard/${RECORD_NAME}.mp4" &
  RECORD_PID=$!
  sleep 0.6
}

stop_record() {
  [[ -z "$RECORD_PID" ]] && return 0
  local name="$RECORD_NAME"
  # 디바이스 측 screenrecord 에 SIGINT → MP4 헤더 정상 finalize
  "${ADB[@]}" shell "pid=\$(pidof screenrecord); [ -n \"\$pid\" ] && kill -INT \$pid" \
    >/dev/null 2>&1 || true
  wait "$RECORD_PID" 2>/dev/null || true
  sleep 1
  if "${ADB[@]}" pull "/sdcard/${name}.mp4" "$OUT_DIR/${name}.mp4" >/dev/null 2>&1; then
    log "record saved: $OUT_DIR/${name}.mp4"
  else
    log "WARN: ${name}.mp4 pull 실패"
  fi
  "${ADB[@]}" shell rm "/sdcard/${name}.mp4" >/dev/null 2>&1 || true
  RECORD_PID=""
  RECORD_NAME=""
}

# --- phases ---
phase_write_diary() {
  start_record 01_write_diary
  tap_by home fab_write;                            sleep 1
  tap_by write_diary_new lang_en;                   sleep 0.3
  tap_by write_diary_new title_field;               sleep 0.5
  backspace_n 40
  type_text "E2E%stest%sdiary"
  tap_by write_diary_new content_field;             sleep 0.5
  backspace_n 80
  type_text "Today%sI%slearned%sa%snew%sword%scalled%sserendipity."
  tap_by write_diary_new save_button;               sleep 2
  assert_text "E2E test diary" 5
}

phase_add_word_card() {
  start_record 02_add_word_card
  local card_xy
  card_xy="$(find_center_of 'E2E test diary')" \
    || { log "엔트리 카드 좌표 미발견"; return 1; }
  # shellcheck disable=SC2086
  tap_xy $card_xy;                                  sleep 1
  # bookmark_add 좌표는 원문 언어에 따라 다름 → EN 원문 좌표 사용
  tap_by diary_detail bookmark_add_en_source;       sleep 1
  type_text "serendipity";                          sleep 0.4
  tap_by word_card_sheet save;                      sleep 2
  # 시트 닫힘 → 상세의 원문 content 확인
  assert_text "Today I learned a new word called serendipity." 5
}

phase_review() {
  start_record 03_review
  key KEYCODE_BACK;                                 sleep 1
  tap_by home bottom_nav.flashcard;                 sleep 1
  assert_text "serendipity" 5
  local card_xy
  card_xy="$(find_center_of serendipity)" \
    || { log "카드 좌표 미발견"; return 1; }
  # shellcheck disable=SC2086
  tap_xy $card_xy;                                  sleep 1
  local rate_xy
  rate_xy="$(find_center_of 완벽)" \
    || { log "복습 평가 버튼 미노출"; return 1; }
  # shellcheck disable=SC2086
  tap_xy $rate_xy;                                  sleep 1
  assert_text "복습 1회" 5
}

# --- main ---
cleanup() { stop_record 2>/dev/null || true; }
trap cleanup EXIT

main() {
  {
    echo "# Scenario run $TIMESTAMP"
    echo ""
    echo "device: $SERIAL"
    echo ""
    echo "| phase | elapsed | result |"
    echo "|---|---|---|"
  } > "$SUMMARY"

  preflight

  local phases=(phase_write_diary phase_add_word_card phase_review)
  local failed=0
  for phase in "${phases[@]}"; do
    log "=== $phase ==="
    local start elapsed result
    start=$(date +%s)
    if "$phase"; then
      result="PASS"
    else
      result="FAIL"
      failed=1
    fi
    stop_record
    elapsed=$(( $(date +%s) - start ))
    echo "| $phase | ${elapsed}s | $result |" >> "$SUMMARY"
    log "phase $phase: $result (${elapsed}s)"
    # 실패해도 다음 phase 로 진행 — 중간 산출물 확보
  done

  echo "" >> "$SUMMARY"
  echo "산출물: \`$OUT_DIR\`" >> "$SUMMARY"
  log "done → $OUT_DIR"
  return $failed
}

main "$@"
