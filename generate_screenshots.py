#!/usr/bin/env python3
"""
Generates high-DPI Android screenshots for Octopus Energy Agile Watcher (Android)
into the ./screenshots/ directory using Pillow (PIL.ImageDraw).
All icons, charts, cards, and arrows are vector-drawn from scratch.
"""

import os
from PIL import Image, ImageDraw, ImageFont

OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "screenshots")
os.makedirs(OUT_DIR, exist_ok=True)

FONT_PATH = "/System/Library/Fonts/HelveticaNeue.ttc"
W, H = 1200, 2600  # 2x supersampled (saved at 600x1300)


def font_reg(size):
    return ImageFont.truetype(FONT_PATH, size, index=0)


def font_med(size):
    return ImageFont.truetype(FONT_PATH, size, index=10)


def font_bold(size):
    return ImageFont.truetype(FONT_PATH, size, index=1)


# Palette from Color.kt
PINK_PRIMARY = (200, 35, 113)
PINK_TOP = (210, 40, 120)
PINK_BOT = (184, 27, 100)
PINK_PILL = (247, 120, 194)
PINK_BAR_LIGHT = (227, 165, 199)
PINK_ONBOARD_BTN = (255, 92, 151)

NAVY_BG = (17, 20, 38)
NAVY_CARD = (31, 35, 62)
NAVY_INPUT = (19, 22, 41)
TEXT_SEC_DARK = (163, 172, 194)
TEXT_MUTED_DARK = (110, 119, 145)

LIGHT_BG = (247, 246, 250)
WHITE = (255, 255, 255)
SEG_TRACK = (235, 233, 240)
BOT_PILL = (230, 228, 236)
TEXT_PRI = (17, 20, 38)
TEXT_SEC = (90, 96, 117)
TEXT_MUTED = (138, 144, 166)
GRID_LINE = (226, 224, 232)
GREEN_LOW = (13, 148, 104)


def draw_gradient_card(img, box, radius, top_rgb, bot_rgb):
    x0, y0, x1, y1 = box
    cw, ch = x1 - x0, y1 - y0
    grad = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(grad)
    for y in range(ch):
        t = y / max(ch - 1, 1)
        r = int(top_rgb[0] * (1 - t) + bot_rgb[0] * t)
        g = int(top_rgb[1] * (1 - t) + bot_rgb[1] * t)
        b = int(top_rgb[2] * (1 - t) + bot_rgb[2] * t)
        gdraw.line([(0, y), (cw, y)], fill=(r, g, b, 255))

    mask = Image.new("L", (cw, ch), 0)
    mdraw = ImageDraw.Draw(mask)
    mdraw.rounded_rectangle([(0, 0), (cw - 1, ch - 1)], radius=radius, fill=255)
    img.paste(grad, (x0, y0), mask)


def draw_right_arrow(draw, cx, cy, size, color, width=5):
    draw.line([(cx - size, cy), (cx + size, cy)], fill=color, width=width)
    draw.line([(cx + size - 12, cy - 12), (cx + size, cy)], fill=color, width=width)
    draw.line([(cx + size - 12, cy + 12), (cx + size, cy)], fill=color, width=width)


def draw_ne_arrow(draw, cx, cy, size, color, width=4):
    draw.line([(cx - size, cy + size), (cx + size, cy - size)], fill=color, width=width)
    draw.line([(cx - 2, cy - size), (cx + size, cy - size)], fill=color, width=width)
    draw.line([(cx + size, cy - size), (cx + size, cy + 2)], fill=color, width=width)


def draw_down_arrow(draw, cx, cy, size, color, width=4):
    draw.line([(cx, cy - size), (cx, cy + size)], fill=color, width=width)
    draw.line([(cx - 10, cy + size - 10), (cx, cy + size)], fill=color, width=width)
    draw.line([(cx + 10, cy + size - 10), (cx, cy + size)], fill=color, width=width)


def draw_down_chevron(draw, cx, cy, size, color, width=4):
    draw.line([(cx - size, cy - size // 2), (cx, cy + size // 2)], fill=color, width=width)
    draw.line([(cx, cy + size // 2), (cx + size, cy - size // 2)], fill=color, width=width)


def draw_bolt(draw, cx, cy, scale, color):
    pts = [
        (cx + int(4 * scale), cy - int(16 * scale)),
        (cx - int(12 * scale), cy + int(2 * scale)),
        (cx - int(1 * scale), cy + int(2 * scale)),
        (cx - int(5 * scale), cy + int(16 * scale)),
        (cx + int(12 * scale), cy - int(2 * scale)),
        (cx + int(1 * scale), cy - int(2 * scale)),
    ]
    draw.polygon(pts, fill=color)


def draw_bell_icon(draw, cx, cy, color):
    draw.arc([(cx - 14, cy - 16), (cx + 14, cy + 12)], 180, 360, fill=color, width=4)
    draw.line([(cx - 14, cy - 2), (cx - 16, cy + 10)], fill=color, width=4)
    draw.line([(cx + 14, cy - 2), (cx + 16, cy + 10)], fill=color, width=4)
    draw.line([(cx - 19, cy + 10), (cx + 19, cy + 10)], fill=color, width=4)
    draw.ellipse([(cx - 4, cy + 12), (cx + 4, cy + 18)], fill=color)


def draw_sliders_icon(draw, cx, cy, color):
    for i, dy in enumerate([-10, 0, 10]):
        draw.line([(cx - 16, cy + dy), (cx + 16, cy + dy)], fill=color, width=4)
        kx = cx - 6 if i % 2 == 0 else cx + 6
        draw.ellipse([(kx - 5, cy + dy - 5), (kx + 5, cy + dy + 5)], fill=color)


def draw_share_icon(draw, sx, sy, color):
    draw.rounded_rectangle([(sx - 16, sy - 4), (sx + 16, sy + 20)], radius=4, outline=color, width=4)
    draw.line([(sx, sy + 8), (sx, sy - 20)], fill=color, width=4)
    draw.line([(sx - 9, sy - 11), (sx, sy - 20)], fill=color, width=4)
    draw.line([(sx + 9, sy - 11), (sx, sy - 20)], fill=color, width=4)


def draw_android_status_bar(draw, is_dark=False):
    fg = WHITE if is_dark else TEXT_PRI
    draw.text((76, 42), "11:35", font=font_bold(34), fill=fg)
    draw.ellipse([(W // 2 - 24, 38), (W // 2 + 24, 86)], fill=(12, 14, 24))
    bx = W - 230
    for i in range(4):
        bh = 12 + i * 6
        draw.rounded_rectangle(
            [(bx + i * 12, 74 - bh), (bx + i * 12 + 7, 74)],
            radius=2,
            fill=fg,
        )
    wx = bx + 76
    draw.pieslice([(wx - 22, 44), (wx + 22, 88)], 225, 315, fill=fg)
    bat_x = wx + 42
    draw.rounded_rectangle([(bat_x, 46), (bat_x + 62, 74)], radius=8, outline=fg, width=3)
    draw.rounded_rectangle([(bat_x + 5, 51), (bat_x + 52, 69)], radius=4, fill=(46, 204, 113))
    draw.rectangle([(bat_x + 63, 54), (bat_x + 67, 66)], fill=fg)


def draw_android_bottom_nav(img, draw, active_idx=0):
    bar_box = (54, H - 230, W - 54, H - 80)
    draw.rounded_rectangle(
        bar_box,
        radius=74,
        fill=WHITE,
        outline=GRID_LINE,
        width=3,
    )
    tabs = ["Dashboard", "Electricity", "Gas", "Settings"]
    tab_w = (bar_box[2] - bar_box[0] - 32) // 4
    for idx, label in enumerate(tabs):
        tx0 = bar_box[0] + 16 + idx * tab_w
        tx1 = tx0 + tab_w
        cx = (tx0 + tx1) // 2
        if idx == active_idx:
            draw.rounded_rectangle(
                [(tx0 + 6, bar_box[1] + 12), (tx1 - 6, bar_box[3] - 12)],
                radius=62,
                fill=BOT_PILL,
            )
        col = PINK_PRIMARY if idx == active_idx else TEXT_PRI
        iy = bar_box[1] + 48
        if idx == 0:  # Gauge
            draw.ellipse([(cx - 24, iy - 22), (cx + 24, iy + 26)], outline=col, width=5)
            draw.line([(cx, iy + 4), (cx + 12, iy - 12)], fill=col, width=5)
        elif idx == 1:  # Bolt
            draw_bolt(draw, cx, iy, 1.5, col)
        elif idx == 2:  # Flame
            draw.ellipse([(cx - 18, iy - 12), (cx + 18, iy + 24)], fill=col)
            draw.polygon([(cx, iy - 26), (cx - 18, iy + 2), (cx + 18, iy + 2)], fill=col)
            draw.ellipse([(cx - 7, iy + 2), (cx + 7, iy + 20)], fill=BOT_PILL if idx == active_idx else WHITE)
        else:  # Gear
            draw.ellipse([(cx - 20, iy - 18), (cx + 20, iy + 22)], outline=col, width=7)
            draw.ellipse([(cx - 8, iy - 6), (cx + 8, iy + 10)], fill=col)

        f = font_bold(24) if idx == active_idx else font_med(24)
        tw = draw.textlength(label, font=f)
        draw.text((cx - tw / 2, bar_box[1] + 88), label, font=f, fill=col)

    draw.rounded_rectangle([(W // 2 - 140, H - 36), (W // 2 + 140, H - 24)], radius=6, fill=(180, 182, 195))


def draw_dashed_vline(draw, x, y0, y1, color, dash=10, gap=10):
    y = y0
    while y < y1:
        draw.line([(x, y), (x, min(y + dash, y1))], fill=color, width=2)
        y += dash + gap


def save_downsampled(img, filename):
    out = img.resize((W // 2, H // 2), Image.Resampling.LANCZOS)
    path = os.path.join(OUT_DIR, filename)
    out.save(path, "PNG", optimize=True)
    print("Saved:", path)


# ==============================================================================
# 1. ONBOARDING SCREEN (01-onboarding.png)
# ==============================================================================
def gen_onboarding():
    img = Image.new("RGB", (W, H), NAVY_BG)
    draw = ImageDraw.Draw(img)
    draw_android_status_bar(draw, is_dark=True)

    draw.text((64, 210), "Welcome to Agile Watcher", font=font_bold(64), fill=WHITE)

    # Primary API Key Card
    c0 = (56, 330, W - 56, 1140)
    draw.rounded_rectangle(c0, radius=54, fill=NAVY_CARD)

    # Key icon + Title
    kx, ky = 118, 422
    draw.ellipse([(kx - 16, ky - 22), (kx + 16, ky + 10)], outline=WHITE, width=6)
    draw.line([(kx, ky + 10), (kx, ky + 38)], fill=WHITE, width=6)
    draw.line([(kx, ky + 26), (kx + 12, ky + 26)], fill=WHITE, width=6)
    draw.line([(kx, ky + 36), (kx + 14, ky + 36)], fill=WHITE, width=6)

    draw.text((160, 398), "Enter your Octopus API key", font=font_bold(46), fill=WHITE)

    sub_lines = [
        "Copy your key from Developer settings in your",
        "Octopus account. We'll use it to find your home and",
        "meter.",
    ]
    for i, line in enumerate(sub_lines):
        draw.text((104, 490 + i * 48), line, font=font_reg(36), fill=TEXT_SEC_DARK)

    # Input Box
    inp = (104, 675, W - 104, 795)
    draw.rounded_rectangle(inp, radius=30, fill=NAVY_INPUT)
    draw.line([(142, 708), (142, 762)], fill=PINK_ONBOARD_BTN, width=4)
    draw.text((148, 712), "sk_live_...", font=font_reg(38), fill=TEXT_SEC_DARK)
    ex, ey = W - 168, 735
    draw.arc([(ex - 28, ey - 18), (ex + 28, ey + 18)], 0, 360, fill=TEXT_SEC_DARK, width=4)
    draw.ellipse([(ex - 9, ey - 9), (ex + 9, ey + 9)], outline=TEXT_SEC_DARK, width=4)

    # Pink "Find my details ->" button
    btn = (104, 845, W - 104, 965)
    draw.rounded_rectangle(btn, radius=34, fill=PINK_ONBOARD_BTN)
    btxt = "Find my details"
    tw = draw.textlength(btxt, font=font_bold(38))
    tx = (W - tw - 48) / 2
    draw.text((tx, 883), btxt, font=font_bold(38), fill=NAVY_BG)
    draw_right_arrow(draw, int(tx + tw + 30), 905, 16, NAVY_BG, width=5)

    wtxt = "Where do I find my key?"
    draw.text((104, 1020), wtxt, font=font_bold(34), fill=WHITE)
    w_len = draw.textlength(wtxt, font=font_bold(34))
    draw_ne_arrow(draw, int(104 + w_len + 24), 1038, 10, WHITE, width=4)

    # Secondary Public Region Card
    c1 = (56, 1210, W - 56, 1690)
    draw.rounded_rectangle(c1, radius=48, fill=NAVY_CARD)
    draw_bolt(draw, 118, 1286, 1.3, PINK_PILL)
    draw.text((146, 1265), "Or watch public Agile prices right away", font=font_bold(36), fill=WHITE)
    draw.text((104, 1328), "Public half-hourly Agile rates don't require an API key.", font=font_reg(30), fill=TEXT_SEC_DARK)
    draw.text((104, 1370), "Pick your UK electricity region below:", font=font_reg(30), fill=TEXT_SEC_DARK)

    reg_box = (104, 1435, W - 104, 1535)
    draw.rounded_rectangle(reg_box, radius=26, fill=NAVY_INPUT, outline=PINK_PILL, width=2)
    draw.text((138, 1466), "Region L — South Western England", font=font_med(32), fill=WHITE)
    draw_down_chevron(draw, W - 156, 1485, 12, PINK_PILL, width=4)

    cont_box = (104, 1560, W - 104, 1645)
    draw.rounded_rectangle(cont_box, radius=28, outline=PINK_PILL, width=3)
    ctxt = "Continue with Region L (South Western)"
    ctw = draw.textlength(ctxt, font=font_bold(32))
    ctx = (W - ctw - 42) / 2
    draw.text((ctx, 1584), ctxt, font=font_bold(32), fill=PINK_PILL)
    draw_right_arrow(draw, int(ctx + ctw + 28), 1602, 13, PINK_PILL, width=4)

    # Footer disclaimer
    f1 = "Agile Watcher is an independent app. It connects directly to Octopus"
    f2 = "Energy without an Agile Watcher server."
    draw.text(((W - draw.textlength(f1, font=font_reg(29))) / 2, 1770), f1, font=font_reg(29), fill=TEXT_SEC_DARK)
    draw.text(((W - draw.textlength(f2, font=font_reg(29))) / 2, 1812), f2, font=font_reg(29), fill=TEXT_SEC_DARK)

    draw.rounded_rectangle([(W // 2 - 140, H - 36), (W // 2 + 140, H - 24)], radius=6, fill=TEXT_MUTED_DARK)
    save_downsampled(img, "01-onboarding.png")


# ==============================================================================
# 2. DASHBOARD SCREEN (02-dashboard.png)
# ==============================================================================
def gen_dashboard():
    img = Image.new("RGB", (W, H), LIGHT_BG)
    draw = ImageDraw.Draw(img)
    draw_android_status_bar(draw, is_dark=False)

    title = "Agile Prices"
    draw.text(((W - draw.textlength(title, font=font_bold(42))) / 2, 168), title, font=font_bold(42), fill=TEXT_PRI)

    bt_box = (W - 336, 142, W - 54, 232)
    draw.rounded_rectangle(bt_box, radius=45, fill=WHITE, outline=GRID_LINE, width=3)
    btxt = "Best Times"
    draw.text(
        ((bt_box[0] + bt_box[2] - draw.textlength(btxt, font=font_med(36))) / 2, 166),
        btxt,
        font=font_med(36),
        fill=TEXT_PRI,
    )

    # Hero Pink Card
    hero = (54, 286, W - 54, 880)
    draw_gradient_card(img, hero, radius=58, top_rgb=PINK_TOP, bot_rgb=PINK_BOT)
    draw = ImageDraw.Draw(img)

    draw.text((108, 344), "CURRENT PRICE", font=font_bold(29), fill=(255, 235, 245))
    draw.text((108, 405), "9.53p", font=font_bold(110), fill=WHITE)
    p_w = draw.textlength("9.53p", font=font_bold(110))
    draw.text((108 + p_w + 14, 462), "/kWh", font=font_med(52), fill=WHITE)

    draw.text((108, 565), "Next Half Hour: 8.39p/kWh", font=font_bold(37), fill=WHITE)
    draw.text((108, 628), "Average for Day: 3.13p/kWh", font=font_bold(37), fill=WHITE)
    draw.text((108, 694), "Prices checked: Oct 25, 2026 at 11:36 PM", font=font_reg(29), fill=(255, 225, 240))

    # Earlier Prices Pill with vector down arrow
    ep_box = (108, 755, 446, 836)
    draw.rounded_rectangle(ep_box, radius=40, fill=PINK_PILL)
    draw_down_arrow(draw, 152, 795, 14, WHITE, width=4)
    draw.text((180, 776), "Earlier Prices", font=font_bold(33), fill=WHITE)

    # Share circle button
    sh_box = (W - 186, 752, W - 102, 836)
    draw.ellipse(sh_box, fill=PINK_PILL)
    sx, sy = (sh_box[0] + sh_box[2]) // 2, (sh_box[1] + sh_box[3]) // 2
    draw_share_icon(draw, sx, sy, WHITE)

    # Sun, Oct 25
    draw.text((66, 940), "Sun, Oct 25", font=font_bold(42), fill=PINK_PRIMARY)

    r0 = (54, 1012, W - 54, 1122)
    draw.rounded_rectangle(r0, radius=32, fill=WHITE)
    draw.text((98, 1046), "11:30 PM – 12:00 AM", font=font_med(36), fill=PINK_PRIMARY)
    rt0 = "9.53p/kWh"
    draw.text((W - 98 - draw.textlength(rt0, font=font_bold(37)), 1046), rt0, font=font_bold(37), fill=PINK_PRIMARY)

    # Mon, Oct 26
    draw.text((66, 1178), "Mon, Oct 26", font=font_bold(42), fill=PINK_PRIMARY)

    slots = [
        ("12:00 AM – 12:30 AM", "8.39p/kWh", TEXT_PRI),
        ("12:30 AM – 1:00 AM", "13.08p/kWh", TEXT_PRI),
        ("1:00 AM – 1:30 AM", "10.82p/kWh", TEXT_PRI),
        ("1:30 AM – 2:00 AM", "8.84p/kWh", TEXT_PRI),
        ("2:00 AM – 2:30 AM", "11.16p/kWh", TEXT_PRI),
        ("2:30 AM – 3:00 AM", "9.26p/kWh", TEXT_PRI),
        ("3:00 AM – 3:30 AM", "8.00p/kWh", GREEN_LOW),
    ]
    for i, (t_lbl, r_lbl, r_col) in enumerate(slots):
        ry0 = 1250 + i * 146
        ry1 = ry0 + 114
        draw.rounded_rectangle([(54, ry0), (W - 54, ry1)], radius=32, fill=WHITE)
        draw.text((98, ry0 + 36), t_lbl, font=font_reg(36), fill=TEXT_PRI)
        draw.text((W - 98 - draw.textlength(r_lbl, font=font_bold(37)), ry0 + 36), r_lbl, font=font_bold(37), fill=r_col)

    draw_android_bottom_nav(img, draw, active_idx=0)
    save_downsampled(img, "02-dashboard.png")


# ==============================================================================
# 3. BEST TIMES MODAL SCREEN (03-best-times.png)
# ==============================================================================
def gen_best_times():
    img = Image.new("RGB", (W, H), LIGHT_BG)
    draw = ImageDraw.Draw(img)
    draw_android_status_bar(draw, is_dark=False)

    draw.rounded_rectangle([(W // 2 - 75, 132), (W // 2 + 75, 144)], radius=6, fill=GRID_LINE)

    # Refresh circle left, Best Times center, Done pill right
    draw.ellipse([(54, 168), (158, 272)], fill=WHITE, outline=GRID_LINE, width=3)
    draw.arc([(82, 196), (130, 244)], 35, 315, fill=PINK_PRIMARY, width=5)
    draw.polygon([(122, 192), (135, 196), (125, 208)], fill=PINK_PRIMARY)

    title = "Best Times"
    draw.text(((W - draw.textlength(title, font=font_bold(42))) / 2, 198), title, font=font_bold(42), fill=TEXT_PRI)

    done_box = (W - 236, 172, W - 54, 268)
    draw.rounded_rectangle(done_box, radius=48, fill=WHITE, outline=GRID_LINE, width=3)
    dtxt = "Done"
    draw.text(((done_box[0] + done_box[2] - draw.textlength(dtxt, font=font_med(36))) / 2, 198), dtxt, font=font_med(36), fill=PINK_PRIMARY)

    draw.text((54, 336), "Find a cheaper time", font=font_bold(56), fill=TEXT_PRI)
    draw.text((54, 418), "Choose how long you need. We'll find the lowest average", font=font_reg(34), fill=TEXT_SEC)
    draw.text((54, 466), "Agile rate in consecutive half-hour slots.", font=font_reg(34), fill=TEXT_SEC)

    # Segmented control: 1 hour | 2 hours | 4 hours
    seg = (54, 540, W - 54, 622)
    draw.rounded_rectangle(seg, radius=40, fill=SEG_TRACK)
    sw = (seg[2] - seg[0] - 12) // 3
    draw.rounded_rectangle([(seg[0] + 6, seg[1] + 6), (seg[0] + 6 + sw, seg[3] - 6)], radius=34, fill=WHITE)
    for idx, lbl in enumerate(["1 hour", "2 hours", "4 hours"]):
        cx = seg[0] + 6 + idx * sw + sw // 2
        f = font_bold(31) if idx == 0 else font_med(31)
        draw.text((cx - draw.textlength(lbl, font=f) / 2, seg[1] + 22), lbl, font=f, fill=TEXT_PRI)

    # TODAY Pink Card
    today_box = (54, 668, W - 54, 882)
    draw_gradient_card(img, today_box, radius=48, top_rgb=PINK_TOP, bot_rgb=PINK_BOT)
    draw = ImageDraw.Draw(img)
    draw.text((108, 718), "TODAY", font=font_bold(28), fill=(255, 230, 242))
    draw.text((108, 775), "No complete 1-hour window remains today.", font=font_reg(38), fill=WHITE)

    # TOMORROW White Card
    tom_box = (54, 926, W - 54, 1368)
    draw.rounded_rectangle(tom_box, radius=48, fill=WHITE)
    draw.text((108, 976), "TOMORROW", font=font_bold(28), fill=TEXT_SEC)
    draw.text((108, 1036), "3:30 AM – 4:30 AM", font=font_bold(50), fill=TEXT_PRI)
    draw.text((108, 1126), "7.95", font=font_bold(74), fill=TEXT_PRI)
    w795 = draw.textlength("7.95", font=font_bold(74))
    draw.text((108 + w795 + 14, 1160), "p/kWh average", font=font_med(36), fill=TEXT_PRI)

    rem_btn = (108, 1236, 448, 1322)
    draw.rounded_rectangle(rem_btn, radius=43, fill=PINK_PRIMARY)
    draw_bell_icon(draw, 166, 1278, WHITE)
    draw.text((202, 1258), "Remind Me", font=font_bold(33), fill=WHITE)

    # Half-hour prices Chart Card
    ch_card = (54, 1412, W - 54, 2410)
    draw.rounded_rectangle(ch_card, radius=48, fill=WHITE)
    draw.text((104, 1468), "Half-hour prices", font=font_bold(40), fill=TEXT_PRI)

    # Today | Tomorrow toggle pill
    tt_box = (496, 1452, W - 98, 1528)
    draw.rounded_rectangle(tt_box, radius=38, fill=SEG_TRACK)
    tt_w = (tt_box[2] - tt_box[0] - 10) // 2
    draw.rounded_rectangle([(tt_box[0] + 5, tt_box[1] + 5), (tt_box[0] + 5 + tt_w, tt_box[3] - 5)], radius=33, fill=WHITE)
    draw.text((tt_box[0] + 5 + tt_w / 2 - draw.textlength("Today", font=font_bold(30)) / 2, tt_box[1] + 19), "Today", font=font_bold(30), fill=TEXT_PRI)
    draw.text((tt_box[0] + 5 + tt_w + tt_w / 2 - draw.textlength("Tomorrow", font=font_med(30)) / 2, tt_box[1] + 19), "Tomorrow", font=font_med(30), fill=TEXT_PRI)

    gx0, gy0, gx1, gy1 = 104, 1580, W - 160, 2060
    gw, gh = gx1 - gx0, gy1 - gy0

    def y_for_rate(val):
        return int(gy0 + ((40.0 - val) / 60.0) * gh)

    zero_y = y_for_rate(0.0)
    for lvl in [40, 20, 0, -20]:
        ly = y_for_rate(lvl)
        draw.line([(gx0, ly), (gx1, ly)], fill=GRID_LINE, width=3 if lvl == 0 else 2)
        draw.text((gx1 + 14, ly - 16), str(lvl), font=font_reg(27), fill=TEXT_MUTED)

    for g in range(7):
        vx = gx0 + int(gw * g / 6)
        draw_dashed_vline(draw, vx, gy0, gy1, GRID_LINE)

    rates_48 = [
        -1.2, -3.5, -8.5, -6.8, -4.2, -3.1, -2.0, -3.4, -2.8, -2.5, -4.1, -3.2,
        -2.9, -2.1, -1.8, -3.0, -3.2, -3.5, -3.8, -4.2, -4.5, -5.2, -3.8, -4.1,
        -5.0, -3.2, -1.5, -2.0, 1.5, 2.4, 8.2, 28.5, 31.2, 33.8, 31.5, 30.8,
        25.4, 15.2, 11.4, 9.8, 7.2, 9.5, 3.8, 2.9, 0.8, 14.5, 9.6, 8.1
    ]
    bw = gw / 48.0
    for idx, r in enumerate(rates_48):
        bx0 = gx0 + idx * bw + 1
        bx1 = bx0 + bw - 2
        vy = y_for_rate(r)
        top_y, bot_y = min(zero_y, vy), max(zero_y, vy)
        col = PINK_PRIMARY if idx in (2, 3) else PINK_BAR_LIGHT
        draw.rectangle([(bx0, top_y), (bx1, max(bot_y, top_y + 3))], fill=col)

    x_labels = ["12 AM", "3 AM", "7 AM", "11 AM", "3 PM", "7 PM"]
    for i, xl in enumerate(x_labels):
        lx = gx0 + int(gw * i / 6) + 8
        draw.text((lx, gy1 + 16), xl, font=font_reg(26), fill=TEXT_MUTED)

    draw.text((104, 2165), "Pink marks the cheapest complete window when one", font=font_reg(32), fill=TEXT_SEC)
    draw.text((104, 2212), "is available.", font=font_reg(32), fill=TEXT_SEC)

    draw.rounded_rectangle([(W // 2 - 140, H - 36), (W // 2 + 140, H - 24)], radius=6, fill=(180, 182, 195))
    save_downsampled(img, "03-best-times.png")


# ==============================================================================
# 4. ELECTRICITY CONSUMPTION 30-DAY SCREEN (04-electricity.png)
# ==============================================================================
def gen_electricity():
    img = Image.new("RGB", (W, H), LIGHT_BG)
    draw = ImageDraw.Draw(img)
    draw_android_status_bar(draw, is_dark=False)

    title = "Electric Consumption"
    draw.text(((W - draw.textlength(title, font=font_bold(42))) / 2, 168), title, font=font_bold(42), fill=TEXT_PRI)

    hero = (54, 275, W - 54, 868)
    draw_gradient_card(img, hero, radius=58, top_rgb=PINK_TOP, bot_rgb=PINK_BOT)
    draw = ImageDraw.Draw(img)

    draw.text((108, 326), "Last 30 Days", font=font_bold(40), fill=WHITE)
    draw.text((108, 396), "£236.19", font=font_bold(96), fill=WHITE)
    w_cost = draw.textlength("£236.19", font=font_bold(96))
    draw.text((108 + w_cost + 22, 438), "769.06 kWh", font=font_med(50), fill=WHITE)

    draw.text((108, 536), "Last 7 Days: £51.95 · 170.80 kWh", font=font_bold(36), fill=WHITE)
    draw.text((108, 598), "Avg. reported day: £7.87 · 25.64 kWh", font=font_bold(36), fill=WHITE)
    draw.text((108, 666), "Day pricing includes standing charge of £0.67.", font=font_reg(29), fill=(255, 230, 242))
    draw.text((108, 722), "Last checked: Oct 25, 2026 at 11:31PM · Latest", font=font_reg(29), fill=(255, 230, 242))
    draw.text((108, 764), "reading: Oct 25, 2026 at 1:30 AM", font=font_reg(29), fill=(255, 230, 242))

    sh_box = (W - 186, 742, W - 102, 826)
    draw.ellipse(sh_box, fill=PINK_PILL)
    sx, sy = (sh_box[0] + sh_box[2]) // 2, (sh_box[1] + sh_box[3]) // 2
    draw_share_icon(draw, sx, sy, WHITE)

    ch_card = (54, 916, W - 54, 1690)
    draw.rounded_rectangle(ch_card, radius=48, fill=WHITE)
    draw.text((104, 968), "Usage over 30 days", font=font_bold(42), fill=TEXT_PRI)
    draw.text((W - 168, 976), "kWh", font=font_med(32), fill=TEXT_SEC)

    gx0, gy0, gx1, gy1 = 104, 1068, W - 160, 1520
    gw, gh = gx1 - gx0, gy1 - gy0

    for i, lvl in enumerate([60, 40, 20, 0]):
        ly = gy0 + int(gh * (i + 0.35) / 3.35) if lvl != 0 else gy1
        draw.line([(gx0, ly), (gx1, ly)], fill=GRID_LINE, width=2)
        draw.text((gx1 + 14, ly - 16), str(lvl), font=font_reg(27), fill=TEXT_MUTED)

    for g in range(5):
        vx = gx0 + int(gw * g / 4)
        draw_dashed_vline(draw, vx, gy0, gy1, GRID_LINE)

    usage_30 = [
        18, 59, 18, 17, 15, 15, 33, 23, 18, 17,
        36, 31, 21, 30, 23, 44, 22, 21, 21, 18,
        17, 23, 68, 21, 20, 19, 51, 21, 36, 4
    ]
    bw = gw / 30.0
    for idx, u in enumerate(usage_30):
        bx0 = gx0 + idx * bw + 3
        bx1 = bx0 + bw - 6
        bh = int((u / 72.0) * gh)
        draw.rounded_rectangle([(bx0, gy1 - bh), (bx1, gy1)], radius=6, fill=PINK_PRIMARY)

    for i, dl in enumerate(["Sep 26", "Oct 3", "Oct 10", "Oct 17", "Oct 24"]):
        lx = gx0 + int(gw * i / 4)
        draw.text((lx, gy1 + 16), dl, font=font_reg(27), fill=TEXT_MUTED)

    draw.text((54, 1756), "Daily usage", font=font_bold(42), fill=TEXT_PRI)

    d_cards = [
        ("Sun, Oct 25", "Partial readings · 3 received", "£0.57", "3.76 kWh"),
        ("Sat, Oct 24", "Avg. 7.90p/kWh · 48 readings", "£2.87", "36.29 kWh"),
        ("Fri, Oct 23", "Avg. 8.42p/kWh · 48 readings", "£2.44", "20.94 kWh"),
    ]
    for i, (d_title, d_sub, d_cost, d_kwh) in enumerate(d_cards):
        cy0 = 1830 + i * 194
        cy1 = cy0 + 168
        draw.rounded_rectangle([(54, cy0), (W - 54, cy1)], radius=36, fill=WHITE)
        draw.text((98, cy0 + 34), d_title, font=font_bold(38), fill=TEXT_PRI)
        draw.text((98, cy0 + 92), d_sub, font=font_reg(31), fill=TEXT_SEC)

        draw.text((W - 142 - draw.textlength(d_cost, font=font_bold(38)), cy0 + 34), d_cost, font=font_bold(38), fill=TEXT_PRI)
        draw.text((W - 142 - draw.textlength(d_kwh, font=font_reg(31)), cy0 + 92), d_kwh, font=font_reg(31), fill=TEXT_SEC)
        draw.text((W - 112, cy0 + 60), "›", font=font_bold(44), fill=TEXT_MUTED)

    draw_android_bottom_nav(img, draw, active_idx=1)
    save_downsampled(img, "04-electricity.png")


# ==============================================================================
# 5. ELECTRICITY DAY DETAIL SCREEN (05-electricity-day.png)
# ==============================================================================
def gen_electricity_day():
    img = Image.new("RGB", (W, H), LIGHT_BG)
    draw = ImageDraw.Draw(img)
    draw_android_status_bar(draw, is_dark=False)

    draw.ellipse([(54, 140), (168, 254)], fill=WHITE, outline=GRID_LINE, width=3)
    draw.text((96, 162), "‹", font=font_bold(62), fill=TEXT_PRI)

    title = "Sat, Oct 24"
    draw.text(((W - draw.textlength(title, font=font_bold(42))) / 2, 174), title, font=font_bold(42), fill=TEXT_PRI)

    hero = (54, 286, W - 54, 650)
    draw_gradient_card(img, hero, radius=58, top_rgb=PINK_TOP, bot_rgb=PINK_BOT)
    draw = ImageDraw.Draw(img)

    draw.text((108, 340), "Saturday, October 24", font=font_bold(40), fill=WHITE)
    draw.text((108, 412), "36.29", font=font_bold(100), fill=WHITE)
    w_val = draw.textlength("36.29", font=font_bold(100))
    draw.text((108 + w_val + 20, 456), "kWh", font=font_med(52), fill=WHITE)
    draw.text((108, 548), "Day cost: £2.87", font=font_bold(36), fill=WHITE)

    ch_card = (54, 698, W - 54, 1480)
    draw.rounded_rectangle(ch_card, radius=48, fill=WHITE)
    draw.text((104, 752), "Usage through the day", font=font_bold(42), fill=TEXT_PRI)
    draw.text((W - 168, 760), "kWh", font=font_med(32), fill=TEXT_SEC)

    gx0, gy0, gx1, gy1 = 104, 850, W - 150, 1310
    gw, gh = gx1 - gx0, gy1 - gy0

    for i, lvl in enumerate([4, 3, 2, 1, 0]):
        ly = gy0 + int(gh * i / 4)
        draw.line([(gx0, ly), (gx1, ly)], fill=GRID_LINE, width=2)
        draw.text((gx1 + 16, ly - 16), str(lvl), font=font_reg(27), fill=TEXT_MUTED)

    for g in range(7):
        vx = gx0 + int(gw * g / 6)
        draw_dashed_vline(draw, vx, gy0, gy1, GRID_LINE)

    slots_48 = [
        0.55, 0.25, 0.27, 0.28, 0.24, 0.38, 0.24, 0.22, 0.19, 0.26, 0.25, 0.21,
        0.20, 0.21, 0.24, 0.27, 0.58, 0.52, 0.60, 0.42, 0.48, 0.52, 0.51, 0.49,
        0.58, 0.45, 0.39, 0.42, 0.44, 0.59, 0.54, 0.62, 0.78, 0.67, 0.34, 0.28,
        0.35, 0.32, 0.28, 0.50, 3.92, 4.12, 3.94, 3.90, 2.28, 0.34, 0.46, 1.14
    ]
    bw = gw / 48.0
    for idx, val in enumerate(slots_48):
        bx0 = gx0 + idx * bw + 1
        bx1 = bx0 + bw - 2
        bh = int((val / 4.2) * gh)
        draw.rounded_rectangle([(bx0, gy1 - bh), (bx1, gy1)], radius=4, fill=PINK_PRIMARY)

    for i, xl in enumerate(["12 AM", "4 AM", "8 AM", "12 PM", "4 PM", "8 PM"]):
        lx = gx0 + int(gw * i / 6) + 10
        draw.text((lx, gy1 + 16), xl, font=font_reg(26), fill=TEXT_MUTED)

    draw.text((54, 1546), "Half-hour readings", font=font_bold(42), fill=TEXT_PRI)

    readings = [
        ("12:00 AM – 12:30 AM", "6.48p/kWh", "£0.04", "0.55 kWh"),
        ("12:30 AM – 1:00 AM", "7.57p/kWh", "£0.02", "0.25 kWh"),
        ("1:00 AM – 1:30 AM", "2.33p/kWh", "£0.01", "0.27 kWh"),
        ("1:30 AM – 2:00 AM", "4.58p/kWh", "£0.01", "0.28 kWh"),
        ("2:00 AM – 2:30 AM", "5.12p/kWh", "£0.01", "0.24 kWh"),
    ]
    for i, (t_str, r_str, c_str, u_str) in enumerate(readings):
        cy0 = 1620 + i * 190
        cy1 = cy0 + 164
        draw.rounded_rectangle([(54, cy0), (W - 54, cy1)], radius=36, fill=WHITE)
        draw.text((98, cy0 + 32), t_str, font=font_bold(36), fill=TEXT_PRI)
        draw.text((98, cy0 + 90), r_str, font=font_reg(31), fill=TEXT_SEC)
        draw.text((W - 98 - draw.textlength(c_str, font=font_bold(36)), cy0 + 32), c_str, font=font_bold(36), fill=TEXT_PRI)
        draw.text((W - 98 - draw.textlength(u_str, font=font_reg(31)), cy0 + 90), u_str, font=font_reg(31), fill=TEXT_SEC)

    draw.rounded_rectangle([(W // 2 - 140, H - 36), (W // 2 + 140, H - 24)], radius=6, fill=(180, 182, 195))
    save_downsampled(img, "05-electricity-day.png")


# ==============================================================================
# 6. GAS CONSUMPTION 30-DAY SCREEN (06-gas.png)
# ==============================================================================
def gen_gas():
    img = Image.new("RGB", (W, H), LIGHT_BG)
    draw = ImageDraw.Draw(img)
    draw_android_status_bar(draw, is_dark=False)

    title = "Gas Consumption"
    draw.text(((W - draw.textlength(title, font=font_bold(42))) / 2, 168), title, font=font_bold(42), fill=TEXT_PRI)

    hero = (54, 275, W - 54, 1020)
    draw_gradient_card(img, hero, radius=58, top_rgb=PINK_TOP, bot_rgb=PINK_BOT)
    draw = ImageDraw.Draw(img)

    draw.text((108, 326), "Last 30 Days", font=font_bold(40), fill=WHITE)
    draw.text((108, 396), "£59.41", font=font_bold(96), fill=WHITE)
    w_cost = draw.textlength("£59.41", font=font_bold(96))
    draw.text((108 + w_cost + 22, 438), "56.37 m³", font=font_med(50), fill=WHITE)

    draw.text((108, 536), "Last 7 Days: £20.19 · 20.18 m³", font=font_bold(36), fill=WHITE)
    draw.text((108, 598), "Avg. reported day: £1.98 · 1.88 m³", font=font_bold(36), fill=WHITE)
    draw.text((108, 666), "Day pricing includes standing charge of £0.29.", font=font_reg(29), fill=(255, 230, 242))
    draw.text((108, 720), "Costs are estimates using your saved gas rate. Your bill", font=font_reg(29), fill=(255, 230, 242))
    draw.text((108, 760), "may differ.", font=font_reg(29), fill=(255, 230, 242))
    draw.text((108, 816), "Last checked: Oct 25, 2026 at 11:31PM · Latest reading: Oct", font=font_reg(29), fill=(255, 230, 242))
    draw.text((108, 856), "25, 2026 at 1:00 AM", font=font_reg(29), fill=(255, 230, 242))

    # Setup Tariff Pill with vector sliders icon
    st_box = (108, 906, 446, 986)
    draw.rounded_rectangle(st_box, radius=40, fill=PINK_PILL)
    draw_sliders_icon(draw, 160, 946, WHITE)
    draw.text((194, 926), "Setup Tariff", font=font_bold(33), fill=WHITE)

    sh_box = (W - 186, 902, W - 102, 986)
    draw.ellipse(sh_box, fill=PINK_PILL)
    sx, sy = (sh_box[0] + sh_box[2]) // 2, (sh_box[1] + sh_box[3]) // 2
    draw_share_icon(draw, sx, sy, WHITE)

    ch_card = (54, 1068, W - 54, 1840)
    draw.rounded_rectangle(ch_card, radius=48, fill=WHITE)
    draw.text((104, 1120), "Usage over 30 days", font=font_bold(42), fill=TEXT_PRI)
    draw.text((W - 156, 1128), "m³", font=font_med(32), fill=TEXT_SEC)

    gx0, gy0, gx1, gy1 = 104, 1216, W - 150, 1668
    gw, gh = gx1 - gx0, gy1 - gy0

    for i, lvl in enumerate([6, 4, 2, 0]):
        ly = gy0 + int(gh * i / 3)
        draw.line([(gx0, ly), (gx1, ly)], fill=GRID_LINE, width=2)
        draw.text((gx1 + 16, ly - 16), str(lvl), font=font_reg(27), fill=TEXT_MUTED)

    for g in range(5):
        vx = gx0 + int(gw * g / 4)
        draw_dashed_vline(draw, vx, gy0, gy1, GRID_LINE)

    gas_30 = [
        1.25, 1.92, 1.32, 1.40, 0.90, 0.88, 1.46, 1.84, 1.40, 1.46,
        1.64, 1.24, 1.32, 1.96, 1.48, 1.58, 1.22, 1.38, 2.32, 1.32,
        1.58, 1.36, 3.60, 3.00, 1.56, 1.62, 4.02, 4.38, 5.46, 0.02
    ]
    bw = gw / 30.0
    for idx, u in enumerate(gas_30):
        bx0 = gx0 + idx * bw + 3
        bx1 = bx0 + bw - 6
        bh = max(int((u / 6.0) * gh), 4)
        draw.rounded_rectangle([(bx0, gy1 - bh), (bx1, gy1)], radius=6, fill=PINK_PRIMARY)

    for i, dl in enumerate(["Sep 26", "Oct 3", "Oct 10", "Oct 17", "Oct 24"]):
        lx = gx0 + int(gw * i / 4)
        draw.text((lx, gy1 + 16), dl, font=font_reg(27), fill=TEXT_MUTED)

    draw.text((54, 1900), "Daily usage", font=font_bold(42), fill=TEXT_PRI)

    g_cards = [
        ("Sun, Oct 25", "Partial readings · 2 received", "£0.29", "0.00 m³"),
        ("Sat, Oct 24", "48 readings · 6.24p/kWh", "£4.11", "5.46 m³"),
    ]
    for i, (d_title, d_sub, d_cost, d_m3) in enumerate(g_cards):
        cy0 = 1972 + i * 194
        cy1 = cy0 + 168
        draw.rounded_rectangle([(54, cy0), (W - 54, cy1)], radius=36, fill=WHITE)
        draw.text((98, cy0 + 34), d_title, font=font_bold(38), fill=TEXT_PRI)
        draw.text((98, cy0 + 92), d_sub, font=font_reg(31), fill=TEXT_SEC)
        draw.text((W - 142 - draw.textlength(d_cost, font=font_bold(38)), cy0 + 34), d_cost, font=font_bold(38), fill=TEXT_PRI)
        draw.text((W - 142 - draw.textlength(d_m3, font=font_reg(31)), cy0 + 92), d_m3, font=font_reg(31), fill=TEXT_SEC)
        draw.text((W - 112, cy0 + 60), "›", font=font_bold(44), fill=TEXT_MUTED)

    draw_android_bottom_nav(img, draw, active_idx=2)
    save_downsampled(img, "06-gas.png")


if __name__ == "__main__":
    gen_onboarding()
    gen_dashboard()
    gen_best_times()
    gen_electricity()
    gen_electricity_day()
    gen_gas()
