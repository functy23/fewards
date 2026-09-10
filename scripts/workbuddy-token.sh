#!/usr/bin/env bash
# 在 macOS 上查找 WorkBuddy / CodeBuddy 桌面端 accessToken，并复制到剪贴板。
set -euo pipefail

CANDIDATES=(
  "$HOME/Library/Application Support/CodeBuddyExtension/Data/Public/auth/workbuddy-desktop.info"
  "$HOME/Library/Application Support/WorkBuddy/auth/workbuddy-desktop.info"
  "$HOME/Library/Application Support/CodeBuddy/Data/Public/auth/workbuddy-desktop.info"
)

found=""
for f in "${CANDIDATES[@]}"; do
  if [[ -f "$f" ]]; then
    found="$f"
    break
  fi
done

if [[ -z "$found" ]]; then
  found="$(find "$HOME/Library/Application Support" -name 'workbuddy-desktop.info' 2>/dev/null | head -n 1 || true)"
fi

if [[ -z "$found" || ! -f "$found" ]]; then
  echo "未找到 WorkBuddy 登录文件。请先在桌面端登录 WorkBuddy / CodeBuddy。" >&2
  exit 1
fi

token="$(python3 - "$found" <<'PY'
import json, sys
path = sys.argv[1]
with open(path, "r", encoding="utf-8") as f:
    data = json.load(f)
token = None
if isinstance(data, dict):
    auth = data.get("auth") if isinstance(data.get("auth"), dict) else data
    token = auth.get("accessToken") or auth.get("access_token") or data.get("accessToken")
if not token:
    sys.stderr.write("文件里没有 accessToken。\n")
    sys.exit(2)
print(token, end="")
PY
)"

printf "%s" "$token" | pbcopy
echo "已复制 WorkBuddy Access Token 到剪贴板。"
echo "来源: $found"
echo "长度: ${#token}"
