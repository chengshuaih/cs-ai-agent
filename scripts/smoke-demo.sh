#!/usr/bin/env bash
#
# 机器视觉智能问答与采集辅助系统 —— 冒烟/演示脚本
#
# 用途：在后端已启动（默认 http://localhost:8123，context-path=/api）的前提下，
# 一键跑通：知识问答 → 采集规划 → 采集计划 PDF 导出 → 报告中心列表 → 实验规划 → 实验计划 PDF。
# 任一步失败立即退出并打印响应，便于软著演示前自检。
#
# 使用：
#   1) 预置示例数据（可选，保证地图 MCP 不可用时演示可复现）：
#        bash scripts/seed-sample-data.sh
#   2) 启动后端（需先 export DASHSCOPE_API_KEY 等环境变量）：
#        export JAVA_HOME=$(/usr/libexec/java_home -v 17)
#        ./mvnw spring-boot:run
#   3) 另开终端运行本脚本：
#        bash scripts/smoke-demo.sh
#
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8123/api}"
PASS=0
FAIL=0

note()  { printf '\n\033[1;34m==> %s\033[0m\n' "$1"; }
ok()    { printf '\033[1;32m[PASS]\033[0m %s\n' "$1"; PASS=$((PASS+1)); }
bad()   { printf '\033[1;31m[FAIL]\033[0m %s\n' "$1"; FAIL=$((FAIL+1)); }

# 校验业务接口返回 code==0
assert_code0() {
  local body="$1" label="$2"
  if printf '%s' "$body" | grep -q '"code"[[:space:]]*:[[:space:]]*0'; then
    ok "$label"
  else
    bad "$label -> $body"
  fi
}

# 1. 知识问答（同步）
note "1/6 知识问答：什么是目标检测的 mAP"
QA=$(curl -s "$BASE/ai/vision/chat/sync?message=%E4%BB%80%E4%B9%88%E6%98%AFmAP&chatId=smoke1" || true)
if [ -n "$QA" ]; then ok "问答返回非空"; else bad "问答返回为空"; fi

# 2. 采集规划
note "2/6 采集规划：北京市海淀区中关村 目标检测"
PLAN_REQ='{"task":{"taskType":"目标检测","targetObjects":["车辆","行人","交通灯"],"sceneTypes":["十字路口"],"location":"北京市海淀区中关村","timeBudget":"半天","transportMode":"骑行"},"needPdf":false}'
PLAN=$(curl -s -X POST "$BASE/vision/collection/plan" -H 'Content-Type: application/json' -d "$PLAN_REQ" || true)
assert_code0 "$PLAN" "采集规划返回 code=0"
PLAN_ID=$(printf '%s' "$PLAN" | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)
printf '   planId=%s\n' "${PLAN_ID:-<none>}"

# 3. 采集计划 PDF 导出
note "3/6 采集计划 PDF 导出"
if [ -n "${PLAN_ID:-}" ]; then
  PDF_REQ=$(printf '{"type":"collection_plan","title":"采集计划演示","sourceId":"%s"}' "$PLAN_ID")
  PDF=$(curl -s -X POST "$BASE/vision/report/pdf" -H 'Content-Type: application/json' -d "$PDF_REQ" || true)
  assert_code0 "$PDF" "PDF 导出返回 code=0"
  printf '   %s\n' "$(printf '%s' "$PDF" | sed -n 's/.*"pdfPath"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/pdfPath=\1/p')"
else
  bad "无 planId，跳过 PDF 导出"
fi

# 4. 报告中心
note "4/6 报告中心列表"
REPORTS=$(curl -s "$BASE/vision/report/list" || true)
assert_code0 "$REPORTS" "报告列表返回 code=0"

# 5. 实验规划
note "5/6 实验规划：目标检测"
EXP=$(curl -s -X POST "$BASE/vision/experiment/plan" -H 'Content-Type: application/json' -d '{"taskType":"目标检测"}' || true)
assert_code0 "$EXP" "实验规划返回 code=0"
EXP_ID=$(printf '%s' "$EXP" | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)

# 6. 实验计划 PDF
note "6/6 实验计划 PDF 导出"
if [ -n "${EXP_ID:-}" ]; then
  EPDF_REQ=$(printf '{"type":"experiment_plan","title":"实验计划演示","sourceId":"%s"}' "$EXP_ID")
  EPDF=$(curl -s -X POST "$BASE/vision/report/pdf" -H 'Content-Type: application/json' -d "$EPDF_REQ" || true)
  assert_code0 "$EPDF" "实验计划 PDF 导出返回 code=0"
else
  bad "无 experimentId，跳过实验计划 PDF"
fi

printf '\n\033[1m冒烟结果：PASS=%d FAIL=%d\033[0m\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
