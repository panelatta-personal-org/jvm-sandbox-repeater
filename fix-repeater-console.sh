#!/usr/bin/env bash
set -euo pipefail

# 可传入仓库路径，默认当前目录
ROOT="${1:-$(pwd)}"
cd "$ROOT"

VELOCITY_DIR="repeater-console/repeater-console-start/src/main/resources/velocity"
REPLAY="repeater-console/repeater-console-start/src/main/java/com/alibaba/repeater/console/start/controller/page/ReplayController.java"
REGRESS="repeater-console/repeater-console-start/src/main/java/com/alibaba/repeater/console/start/controller/test/RegressPageController.java"

echo "==> 4.3.1 修复 velocity 模板的 #parse(\"/blocks -> #parse(\"blocks"
if [ -d "$VELOCITY_DIR" ]; then
  # 逐文件替换；-i.bak 兼容 GNU/BSD sed，并生成 .bak 备份
  find "$VELOCITY_DIR" -type f -print0 | while IFS= read -r -d '' f; do
    sed -i.bak 's/#parse(\"\/blocks/#parse(\"blocks/g' "$f"
    rm -f "$f.bak"
  done
else
  echo "WARN: 未找到目录 $VELOCITY_DIR" >&2
fi

echo "==> 4.3.2 修复 ReplayController 的 return 前导斜杠"
if [ -f "$REPLAY" ]; then
  sed -i.bak 's|return "/replay/detail";|return "replay/detail";|g' "$REPLAY"
  rm -f "$REPLAY.bak"
else
  echo "WARN: 未找到文件 $REPLAY" >&2
fi

echo "==> 4.3.3 修复 RegressPageController 的 return 前导斜杠"
if [ -f "$REGRESS" ]; then
  sed -i.bak 's|return "/regress/index";|return "regress/index";|g' "$REGRESS"
  rm -f "$REGRESS.bak"
else
  echo "WARN: 未找到文件 $REGRESS" >&2
fi

echo "==> 验证结果："
if [ -d "$VELOCITY_DIR" ]; then
  if grep -R --line-number '#parse("/blocks' "$VELOCITY_DIR" >/dev/null 2>&1; then
    echo "仍有 '#parse(\"/blocks' 残留："
    grep -R --line-number '#parse("/blocks' "$VELOCITY_DIR" || true
  else
    echo "✔ velocity 模板已全部修复"
  fi
fi

if [ -f "$REPLAY" ]; then
  if grep -n 'return "/replay/detail";' "$REPLAY" >/dev/null 2>&1; then
    echo "⚠ ReplayController 仍包含旧返回值"
  else
    echo "✔ ReplayController 已修复"
  fi
fi

if [ -f "$REGRESS" ]; then
  if grep -n 'return "/regress/index";' "$REGRESS" >/dev/null 2>&1; then
    echo "⚠ RegressPageController 仍包含旧返回值"
  else
    echo "✔ RegressPageController 已修复"
  fi
fi

echo "完成。建议执行：git diff 复核改动。"
