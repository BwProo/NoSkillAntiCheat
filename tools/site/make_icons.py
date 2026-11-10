#!/usr/bin/env python3
"""Creates the NoSkillAntiCheat icon (pixel shield) and the social preview image in docs/.
Needs rsvg-convert for the PNG files.   python3 tools/site/make_icons.py"""
import subprocess
from pathlib import Path

DOCS = Path(__file__).resolve().parents[2] / "docs"
VIOLET, WHITE, INK, PAPER = "#6d28d9", "#ffffff", "#16131f", "#f4f3f8"

# 14x14 pixel shield; '#' = shield, '.' = background; the check mark is cut out of the shield.
SHIELD = [
    "..##########..",
    ".############.",
    "##############",
    "##############",
    "##############",
    "##############",
    "##############",
    ".############.",
    ".############.",
    "..##########..",
    "...########...",
    "....######....",
    ".....####.....",
    "......##......",
]
CHECK = {(6, 3), (7, 3), (7, 4), (8, 4), (8, 5), (9, 5), (8, 6), (7, 6), (7, 7), (6, 7), (6, 8), (5, 8), (5, 9), (4, 9), (4, 10), (3, 10)}


def icon_svg(size=64, pad=7):
    cell = (size - 2 * pad) / 14
    rects = []
    for y, row in enumerate(SHIELD):
        for x, ch in enumerate(row):
            if ch == "#" and (y, x) not in CHECK:
                rects.append(f'<rect x="{pad + x * cell:.2f}" y="{pad + y * cell:.2f}" width="{cell + 0.02:.2f}" height="{cell + 0.02:.2f}"/>')
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {size} {size}" shape-rendering="crispEdges">'
            f'<rect width="{size}" height="{size}" rx="{size * 0.22:.1f}" fill="{VIOLET}"/>'
            f'<g fill="{WHITE}">{"".join(rects)}</g></svg>')


def og_svg():
    icon = icon_svg(220, 30).replace('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 220 220" shape-rendering="crispEdges">', "").replace("</svg>", "")
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 630">
<rect width="1200" height="630" fill="{PAPER}"/>
<g transform="translate(90 205)" shape-rendering="crispEdges">{icon}</g>
<text x="360" y="300" font-family="DejaVu Sans, Arial, sans-serif" font-weight="800" font-size="76" fill="{INK}">NoSkillAntiCheat</text>
<text x="362" y="360" font-family="DejaVu Sans, Arial, sans-serif" font-size="32" fill="#5d5870">Free, lightweight Minecraft anticheat</text>
<text x="362" y="410" font-family="DejaVu Sans Mono, monospace" font-size="24" fill="{VIOLET}">Paper · Folia · Purpur · Spigot · Velocity</text>
</svg>'''


def main():
    (DOCS / "favicon.svg").write_text(icon_svg(), encoding="utf-8")
    (DOCS / "og-image.svg").write_text(og_svg(), encoding="utf-8")
    for name, size in [("favicon-48.png", 48), ("icon-192.png", 192), ("icon-512.png", 512), ("apple-touch-icon.png", 180)]:
        subprocess.run(["rsvg-convert", "-w", str(size), "-h", str(size), "-o", str(DOCS / name), str(DOCS / "favicon.svg")], check=True)
    subprocess.run(["rsvg-convert", "-w", "1200", "-h", "630", "-o", str(DOCS / "og-image.png"), str(DOCS / "og-image.svg")], check=True)
    (DOCS / "og-image.svg").unlink()
    print("icons written to docs/")


if __name__ == "__main__":
    main()
