#!/usr/bin/env bash
#
# 预置示例采集计划数据，保证地图 MCP 不可用时演示可复现。
# 将 scripts/sample-data/plans.json 拷贝到运行期数据目录 tmp/vision/plans.json。
# 之后即可对该 planId 直接导出 PDF：sourceId=plan-sample-20260618
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/scripts/sample-data/plans.json"
DEST_DIR="$ROOT/tmp/vision"
DEST="$DEST_DIR/plans.json"

mkdir -p "$DEST_DIR/reports"

if [ -f "$DEST" ]; then
  cp "$DEST" "$DEST.bak.$(date +%s)"
  echo "已备份现有 plans.json"
fi

cp "$SRC" "$DEST"
echo "示例采集计划已写入：$DEST"
echo "演示用 planId：plan-sample-20260618"
