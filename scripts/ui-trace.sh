#!/usr/bin/env bash
# Fewards UI 轨迹抓取：录屏 -> 抽帧 -> 逐帧量化「+」按钮与其菜单锚点副本的位置。
# 用来诊断「点加号后按钮跳位 / 关菜单后闪现回去」这类纯视觉问题（logcat 里看不到）。
#
# 用法: scripts/ui-trace.sh [输出目录]     默认 /tmp/fewards-ui-trace
# 依赖: adb + ffmpeg + python3（无需 numpy/opencv）
set -euo pipefail

OUT="${1:-/tmp/fewards-ui-trace}"
PKG=com.functy.fewards
ACT="$PKG/.ui.MainActivity"
W=704; H=1536; DUR=8          # 录屏尺寸要能被 AVC 编码器接受，704x1536 在 1272x2772 上实测可用
                             # DUR 是 screenrecord 的自然结束时间；必须等它自己写完 moov，
                             # 提前 pull 会拿到 "moov atom not found" 的半个文件。

for c in adb ffmpeg python3; do command -v "$c" >/dev/null || { echo "缺少 $c"; exit 1; }; done
adb get-state >/dev/null 2>&1 || { echo "没有已连接的设备"; exit 1; }

mkdir -p "$OUT"; cd "$OUT"

echo "[1/5] 唤醒屏幕并启动 $PKG"
# 屏幕休眠时 screenrecord 会直接报 UNASSIGNED_LAYER_STACK 并写出 0 字节文件
adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
sleep 2
adb shell am start -n "$ACT" >/dev/null
sleep 4
DEV=$(adb shell wm size | sed 's/.*: //' | tr -d '\r')
DEVD=$(adb shell wm density | sed 's/.*: //' | tr -d '\r')
echo "    屏幕 $DEV @ ${DEVD}dpi"

echo "[2/5] 切到账号页，再从 UI 树定位「+」按钮"
dump_ui() { adb shell uiautomator dump /sdcard/ui_trace.xml >/dev/null; adb shell cat /sdcard/ui_trace.xml > ui.xml; }
find_node() { python3 - "$1" "$2" <<'PY'
import re, sys
needle, fallback = sys.argv[1], sys.argv[2]
xml = open("ui.xml", encoding="utf-8").read()
for m in re.finditer(r'<node[^>]*>', xml):
    s = m.group(0)
    if needle in s:
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', s)
        x0, y0, x1, y1 = map(int, b.groups())
        print((x0 + x1) // 2, (y0 + y1) // 2)
        break
else:
    print(fallback)
PY
}
dump_ui
# 底栏 tab 要按「text 恰好等于 账号」+ 屏幕下半部来找：content-desc「添加账号」里也有「账号」，
# 直接子串匹配会命中右上角的「+」，于是切页变成开菜单。
read -r TABX TABY <<<"$(python3 - <<'PY'
import re
xml = open("ui.xml", encoding="utf-8").read()
cands = []
for m in re.finditer(r'<node[^>]*>', xml):
    s = m.group(0)
    if 'text="账号"' not in s: continue
    b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', s)
    x0, y0, x1, y1 = map(int, b.groups())
    cands.append(((x0 + x1) // 2, (y0 + y1) // 2))
best = max(cands, key=lambda c: c[1]) if cands else (636, 2593)
print(*best)
PY
)"      # 冷启动固定回首页，必须先切页
adb shell input tap "$TABX" "$TABY"
sleep 1.5
dump_ui
read -r TAPX TAPY <<<"$(find_node '添加账号' '1154 389')"
echo "    账号 tab ($TABX,$TABY) -> + 按钮 ($TAPX,$TAPY)"

echo "[3/5] 录屏 + 交互（点开菜单，1.5s 后返回键关闭）"
adb shell pkill -f screenrecord 2>/dev/null || true   # 上一轮残留的 screenrecord 会让新录制写不出 moov
adb shell rm -f /sdcard/ui_trace.mp4
( adb shell screenrecord --size ${W}x${H} --bit-rate 24M --time-limit $DUR /sdcard/ui_trace.mp4 >/dev/null 2>&1 & )
sleep 2.5
adb shell input tap "$TAPX" "$TAPY"
sleep 2.0
adb shell input keyevent 4
sleep 5                        # 等到超过 DUR，让 screenrecord 自己收尾并写完 moov
adb pull /sdcard/ui_trace.mp4 . >/dev/null
ffprobe -v error -show_entries format=duration -of default=nw=1:nk=1 ui_trace.mp4 >/dev/null \
  || { echo "录屏文件不完整（多半是没等到 DUR 就 pull）"; exit 1; }

echo "[4/5] 抽帧 + 逐帧量化"
# -fps_mode passthrough 必须加：否则 rawvideo 会按固定帧率补帧，帧号与真实时间对不上
ffmpeg -hide_banner -v error -i ui_trace.mp4 -fps_mode passthrough -f rawvideo -pix_fmt gray -s ${W}x${H} -y frames.raw
# screenrecord 是变帧率：r_frame_rate 会报 60，必须用「总帧数 / 容器时长」算真实帧率
NFRAMES=$(ffprobe -v error -select_streams v:0 -count_frames -show_entries stream=nb_read_frames -of default=nw=1:nk=1 ui_trace.mp4)
DURATION=$(ffprobe -v error -show_entries format=duration -of default=nw=1:nk=1 ui_trace.mp4)
FPS=$(awk -v n="$NFRAMES" -v d="$DURATION" 'BEGIN{printf "%.4f", n/d}')
python3 - "$W" "$H" "$DEV" "$TAPX" "$TAPY" "$FPS" <<'PY' | tee timeline.txt
import sys
W, H = int(sys.argv[1]), int(sys.argv[2])
FPS = float(sys.argv[6])
devW, devH = (int(v) for v in sys.argv[3].split('x'))
tapx, tapy = int(sys.argv[4]), int(sys.argv[5])
data = open("frames.raw", "rb").read()
n = len(data) // (W * H)
sx, sy = W / devW, H / devH
cx, cy = tapx * sx, tapy * sy
rx, ry = 130 * sx, 130 * sy
x0, x1 = max(0, int(cx - rx)), min(W, int(cx + rx))
y0, y1 = max(0, int(cy - ry)), min(H, int(cy + ry))
print(f"# 帧数={n}  真实帧率={FPS:.2f}fps  窗口=视频({x0},{y0})-({x1},{y1})  设备坐标 x{1/sx:.3f}")
print("# frame  t(s)   tot   |  图标中心(设备px)   bbox(设备px)   尺寸")
prev = None
for i in range(n):
    f = data[i * W * H:(i + 1) * W * H]
    xs = []; ys = []; tot = 0
    if prev is not None:
        for y in range(0, H, 2):
            o = y * W
            for x in range(0, W, 2):
                d = f[o + x] - prev[o + x]
                tot += d if d >= 0 else -d
    for y in range(y0, y1):
        o = y * W
        for x in range(x0, x1):
            if f[o + x] > 120:
                xs.append(x); ys.append(y)
    prev = f
    t = i / FPS
    if xs:
        bx0, bx1 = min(xs) / sx, max(xs) / sx
        by0, by1 = min(ys) / sy, max(ys) / sy
        print(f"{i:6d} {t:5.2f} {tot:7.0f} | ({sum(xs)/len(xs)/sx:7.1f},{sum(ys)/len(ys)/sy:6.1f})  "
              f"[{bx0:6.1f},{by0:6.1f}]-[{bx1:6.1f},{by1:6.1f}]  {bx1-bx0:5.1f}x{by1-by0:5.1f}")
    elif tot > 400:
        print(f"{i:6d} {t:5.2f} {tot:7.0f} | (图标不可见)")
PY

echo "[5/5] 关键帧拼图（顶部区域，每 4 帧一张）"
ffmpeg -hide_banner -v error -f rawvideo -pix_fmt gray -s ${W}x${H} -i frames.raw \
  -vf "select='not(mod(n,4))',crop=704:360:0:60,scale=352:180,tile=4x4:padding=4:color=red" \
  -fps_mode passthrough -frames:v 1 -y keyframes.png || true
echo
echo "结果目录: $OUT"
echo "  timeline.txt  逐帧位置时间线"
echo "  keyframes.png 顶部区域关键帧拼图"
