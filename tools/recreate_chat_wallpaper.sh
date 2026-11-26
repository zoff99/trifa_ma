#!/usr/bin/env bash
# Recreate the light/night chat background tiles from the deterministic
# SVG generator. Requires only bash, python3 (stdlib), and rsvg-convert.
#
# Debian/Ubuntu: sudo apt-get install python3 librsvg2-bin
# Fedora:        sudo dnf install python3 librsvg2-tools
# Arch:          sudo pacman -S python librsvg
#
# Usage:
#   tools/recreate_chat_wallpaper.sh          # regenerate if pixels differ
#   tools/recreate_chat_wallpaper.sh --check  # exit 1 if regeneration changes files

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CHECK=0
if [[ "${1:-}" == "--check" ]]; then
  CHECK=1
elif [[ -n "${1:-}" ]]; then
  echo "usage: $0 [--check]" >&2
  exit 2
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "error: python3 is required" >&2
  exit 1
fi

if ! command -v rsvg-convert >/dev/null 2>&1; then
  echo "error: rsvg-convert is required (librsvg)" >&2
  echo " Debian/Ubuntu: sudo apt-get install librsvg2-bin" >&2
  echo " Fedora:        sudo dnf install librsvg2-tools" >&2
  echo " Arch:          sudo pacman -S librsvg" >&2
  exit 1
fi

exec python3 - "$ROOT" "$CHECK" <<'PY'
import math
import os
import random
import struct
import subprocess
import sys
import tempfile
import zlib
from pathlib import Path

root = Path(sys.argv[1])
check_only = sys.argv[2] == "1"

S = 360
SCALE = 3
OUT_SIZE = S * SCALE
rng = random.Random(7)

SYMS = {
    'bubble': '<path d="M-9,-6 H9 Q12,-6 12,-3 V4 Q12,7 9,7 H-1 L-6,11 V7 H-9 Q-12,7 -12,4 V-3 Q-12,-6 -9,-6 Z"/>',
    'heart': '<path d="M0,7 C-8,1 -10,-4 -6,-7 C-3,-9 0,-6 0,-4 C0,-6 3,-9 6,-7 C10,-4 8,1 0,7 Z"/>',
    'star': '<path d="M0,-9 L2.6,-3.2 L9,-3 L4,1.4 L5.8,8 L0,4.4 L-5.8,8 L-4,1.4 L-9,-3 L-2.6,-3.2 Z"/>',
    'note': '<circle cx="-4" cy="7" r="3"/><path d="M-1,7 V-8 L7,-6 V-3"/>',
    'camera': '<rect x="-10" y="-6" width="20" height="14" rx="2"/><path d="M-4,-6 L-2,-9 H4 L6,-6"/><circle cx="1" cy="1" r="4"/>',
    'phone': '<rect x="-6" y="-10" width="12" height="20" rx="2"/><path d="M-2,7 H2"/>',
    'plane': '<path d="M-10,0 L10,-7 L3,8 L0,2 Z"/><path d="M0,2 L10,-7"/>',
    'smile': '<circle cx="0" cy="0" r="9"/><circle cx="-3" cy="-3" r="0.8" fill="currentColor" stroke="none"/><circle cx="3" cy="-3" r="0.8" fill="currentColor" stroke="none"/><path d="M-4,2 Q0,6 4,2"/>',
    'bolt': '<path d="M2,-9 L-6,2 H-1 L-3,9 L6,-2 H1 Z"/>',
    'flower': '<circle cx="0" cy="0" r="2"/><circle cx="0" cy="-6" r="3"/><circle cx="5.7" cy="-1.8" r="3"/><circle cx="3.5" cy="5" r="3"/><circle cx="-3.5" cy="5" r="3"/><circle cx="-5.7" cy="-1.8" r="3"/>',
    'envelope': '<rect x="-10" y="-7" width="20" height="14" rx="2"/><path d="M-10,-6 L0,2 L10,-6"/>',
    'cloud': '<path d="M-7,5 H7 Q11,5 11,1 Q11,-3 7,-3 Q6,-8 1,-8 Q-4,-8 -5,-3 Q-10,-3 -10,1 Q-10,5 -7,5 Z"/>',
    'sun': '<circle cx="0" cy="0" r="4"/><path d="M0,-9 V-7 M0,7 V9 M-9,0 H-7 M7,0 H9 M-6,-6 L-4.5,-4.5 M4.5,4.5 L6,6 M-6,6 L-4.5,4.5 M4.5,-4.5 L6,-6"/>',
    'moon': '<path d="M4,-8 Q-6,-6 -6,2 Q-6,9 3,9 Q-2,6 -2,0 Q-2,-5 4,-8 Z"/>',
    'leaf': '<path d="M-8,8 Q-8,-6 8,-8 Q6,8 -8,8 Z"/><path d="M-6,6 Q0,0 6,-6"/>',
    'gift': '<rect x="-9" y="-4" width="18" height="12" rx="1"/><path d="M-9,0 H9 M0,-4 V8 M0,-4 Q-6,-10 -7,-4 M0,-4 Q6,-10 7,-4"/>',
    'coffee': '<path d="M-7,-4 H7 V4 Q7,8 3,8 H-3 Q-7,8 -7,4 Z"/><path d="M7,-2 Q11,-2 11,1 Q11,4 7,4 M-3,-8 Q-2,-6 -3,-4 M2,-8 Q3,-6 2,-4"/>',
    'lock': '<rect x="-7" y="-2" width="14" height="11" rx="2"/><path d="M-4,-2 V-5 Q-4,-9 0,-9 Q4,-9 4,-5 V-2"/>',
    'check': '<path d="M-7,0 L-2,5 L8,-6"/>',
}
NAMES = list(SYMS)


def uses(color, opacity):
    out = []
    cell = S / 7
    for gy in range(7):
        for gx in range(7):
            x = (gx + 0.5) * cell + rng.uniform(-15, 15)
            y = (gy + 0.5) * cell + rng.uniform(-15, 15)
            ang = rng.uniform(-35, 35)
            sc = rng.uniform(1.20, 1.65)
            name = NAMES[(gx * 3 + gy * 5 + rng.randrange(3)) % len(NAMES)]
            for ox in (-S, 0, S):
                for oy in (-S, 0, S):
                    out.append(
                        f'<use href="#{name}" transform="translate({x + ox:.2f},{y + oy:.2f}) '
                        f'rotate({ang:.2f}) scale({sc:.2f})"/>'
                    )
    for _ in range(26):
        x = rng.uniform(0, S)
        y = rng.uniform(0, S)
        r = rng.uniform(0.8, 1.6)
        for ox in (-S, 0, S):
            for oy in (-S, 0, S):
                out.append(
                    f'<circle cx="{x + ox:.2f}" cy="{y + oy:.2f}" r="{r:.2f}" '
                    f'fill="{color}" fill-opacity="{opacity}" stroke="none"/>'
                )
    return '\n'.join(out)


def svg(color, opacity):
    defs = '\n'.join(f'<g id="{k}">{v}</g>' for k, v in SYMS.items())
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{S}" height="{S}" viewBox="0 0 {S} {S}">
<defs>{defs}</defs>
<g fill="none" fill-opacity="{opacity}" stroke="{color}" stroke-width="1.25" stroke-linecap="round" stroke-linejoin="round" stroke-opacity="{opacity}" color="{color}">
{uses(color, opacity)}
</g>
</svg>'''


def paeth(a, b, c):
    p = a + b - c
    pa = abs(p - a)
    pb = abs(p - b)
    pc = abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c


def png_rgba(path):
    """Return (width, height, rgba_bytes) for 8-bit non-interlaced RGB/RGBA PNG."""
    data = path.read_bytes()
    if data[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError(f'not a PNG: {path}')
    pos = 8
    idat = bytearray()
    w = h = bd = ct = interlace = None
    while pos + 12 <= len(data):
        length = struct.unpack('>I', data[pos:pos + 4])[0]
        typ = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if typ == b'IHDR':
            w, h, bd, ct, _comp, _filt, interlace = struct.unpack('>IIBBBBB', chunk)
        elif typ == b'IDAT':
            idat += chunk
        elif typ == b'IEND':
            break
    if bd != 8 or interlace != 0 or ct not in (2, 6):
        raise ValueError(f'unsupported PNG format in {path}: depth={bd} type={ct} interlace={interlace}')
    ch = 4 if ct == 6 else 3
    raw = zlib.decompress(bytes(idat))
    stride = w * ch
    out = bytearray(w * h * 4)
    prev = bytearray(stride)
    p = 0
    for y in range(h):
        f = raw[p]
        p += 1
        line = bytearray(raw[p:p + stride])
        p += stride
        if f == 1:
            for i in range(ch, stride):
                line[i] = (line[i] + line[i - ch]) & 255
        elif f == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 255
        elif f == 3:
            for i in range(stride):
                a = line[i - ch] if i >= ch else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
        elif f == 4:
            for i in range(stride):
                a = line[i - ch] if i >= ch else 0
                c = prev[i - ch] if i >= ch else 0
                line[i] = (line[i] + paeth(a, prev[i], c)) & 255
        elif f != 0:
            raise ValueError(f'unsupported PNG filter {f} in {path}')
        base = y * w * 4
        if ch == 4:
            out[base:base + stride] = line
        else:
            for x in range(w):
                out[base + x * 4:base + x * 4 + 3] = line[x * 3:x * 3 + 3]
                out[base + x * 4 + 3] = 255
        prev = line
    return w, h, bytes(out)


def pixels_equal(a, b):
    try:
        from PIL import Image  # optional fast path
        with Image.open(a) as ia, Image.open(b) as ib:
            return ia.size == ib.size and ia.convert('RGBA').tobytes() == ib.convert('RGBA').tobytes()
    except Exception:
        return png_rgba(a) == png_rgba(b)


def render(color, opacity, target):
    target.parent.mkdir(parents=True, exist_ok=True)
    tmp = None
    try:
        with tempfile.NamedTemporaryFile(dir=target.parent, suffix='.svg', delete=False) as f:
            f.write(svg(color, opacity).encode('utf-8'))
            svg_path = Path(f.name)
        with tempfile.NamedTemporaryFile(dir=target.parent, suffix='.png', delete=False) as f:
            tmp = Path(f.name)
        subprocess.run(
            ['rsvg-convert', '-w', str(OUT_SIZE), '-h', str(OUT_SIZE), str(svg_path), '-o', str(tmp)],
            check=True,
        )
        if target.exists() and pixels_equal(target, tmp):
            print(f'unchanged: {target.relative_to(root)}')
            return False
        if check_only:
            print(f'would change: {target.relative_to(root)}')
            return True
        os.replace(tmp, target)
        tmp = None
        print(f'wrote: {target.relative_to(root)}')
        return False
    finally:
        if tmp is not None:
            try:
                tmp.unlink()
            except FileNotFoundError:
                pass
        try:
            svg_path.unlink()
        except (FileNotFoundError, UnboundLocalError):
            pass


changed = False
changed |= render('#5E6B63', 0.17, root / 'android-refimpl-app/app/src/main/res/drawable-xxhdpi/chat_pattern_tile.png')
changed |= render('#E9EDEF', 0.14, root / 'android-refimpl-app/app/src/main/res/drawable-night-xxhdpi/chat_pattern_tile.png')
sys.exit(1 if changed else 0)
PY
