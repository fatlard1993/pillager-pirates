#!/usr/bin/env python3
"""Generate the crew's pirate clothes: overlay sheets drawn over a pillager's own texture.

Pandorical draws an overlay as a cutout layer on the entity's own model, so each sheet is laid out
exactly like vanilla's pillager.png (64x64, the illager model) and every transparent pixel leaves
the pillager showing through. Painted pixel by pixel onto that layout, the boxes as the illager
model declares them (IllagerModel.createBodyLayer, 26.3):

    head      8x10x8 at 0,0     (the face is the front, rows 8-17, columns 8-15)
    body      8x12x6 at 16,20
    jacket    8x20x6 at 0,38    (a robe inflated over the body; a pillager's only fills a belt
                                 and skirt, so the rest of it is free for a coat)
    arms      4x12x4 at 40,46   (both arms, the left mirrored)
    legs      4x12x4 at 0,22

The hat box at 32,0 is hidden on every illager but the illusioner, so a hat cannot stand off the
head here: the tricorne is painted onto the head itself, as the bandanas are.

Four sheets: the captain in a tricorne and a gold-trimmed coat, and three deckhands. Vanilla's
pillager is only read to render the previews; the sheets themselves are drawn from nothing.

Pure stdlib (the PNG reader and writer come from generate_icon.py). Deterministic: re-running
produces identical bytes.

Usage: python3 generate_crew_textures.py [path/to/minecraft.jar] [--preview DIR]
    --preview DIR   also write each sheet composited over the vanilla pillager, a front and back
                    view and the whole UV sheet, scaled up, plus all of them side by side.
"""

import os
import sys

PREVIEW = None
if "--preview" in sys.argv:
    i = sys.argv.index("--preview")
    PREVIEW = sys.argv[i + 1]
    del sys.argv[i:i + 2]

from generate_icon import CLEAR, HERE, crop, scale, stamp, vanilla, write_png  # noqa: E402

OUT = os.path.join(HERE, "src/main/resources/assets/pillager-pirates-justfatlard/textures/entity/crew")
SIZE = 64

FELT, FELT_LIGHT = (28, 26, 30, 255), (46, 42, 48, 255)
GOLD, GOLD_DARK = (214, 170, 52, 255), (150, 112, 30, 255)
BONE = (230, 228, 220, 255)
PATCH, STRAP = (16, 14, 14, 255), (40, 32, 30, 255)
RED, RED_DARK = (176, 32, 34, 255), (118, 20, 24, 255)
WHITE = (234, 230, 218, 255)
NAVY, NAVY_DARK = (32, 44, 92, 255), (20, 28, 62, 255)
BLUE, BLUE_DARK = (44, 84, 156, 255), (28, 54, 108, 255)
LEATHER, LEATHER_DARK = (104, 66, 36, 255), (70, 44, 24, 255)


class Box:
    """One cube of the model, by its texture offset and size, as Minecraft unwraps it: the top and
    bottom side by side above a strip of right, front, left, back. Right is the entity's own right,
    so on the front face the first column is the side the entity's right arm is on."""

    def __init__(self, u, v, w, h, d):
        self.u, self.v, self.w, self.h, self.d = u, v, w, h, d

    def top(self):
        return self.u + self.d, self.v, self.w, self.d

    def front(self):
        return self.u + self.d, self.v + self.d, self.w, self.h

    def back(self):
        return self.u + 2 * self.d + self.w, self.v + self.d, self.w, self.h

    def strip(self):
        """The four sides as one band, all the way round: x from the right face's back edge."""
        return self.u, self.v + self.d, 2 * (self.w + self.d), self.h


HEAD = Box(0, 0, 8, 10, 8)
BODY = Box(16, 20, 8, 12, 6)
JACKET = Box(0, 38, 8, 20, 6)
ARM = Box(40, 46, 4, 12, 4)
LEG = Box(0, 22, 4, 12, 4)


def blank():
    return [[CLEAR] * SIZE for _ in range(SIZE)]


def put(sheet, x, y, colour):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        sheet[y][x] = colour


def rect(sheet, x0, y0, x1, y1, colour):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(sheet, x, y, colour)


def face(sheet, region, colour):
    x, y, w, h = region
    rect(sheet, x, y, x + w - 1, y + h - 1, colour)


def band(sheet, box, row, colour, rows=1):
    """Rows of a box's sides, all the way round. row counts from the top of the box."""
    x, y, w, _ = box.strip()
    rect(sheet, x, y + row, x + w - 1, y + row + rows - 1, colour)


# The head's strip, for reading the drawings below: right side 0-7 (front edge at 7), front 8-15,
# left side 16-23 (front edge at 16), back 24-31; rows 8-17. Vanilla's eyes are on row 14, at
# 9-10 and 13-14, with the brows over them on rows 12-13.

def eyepatch(sheet):
    """Over the left eye, the strap climbing across the forehead and round the back of the head."""
    rect(sheet, 13, 13, 14, 14, PATCH)
    for x, y in ((12, 12), (11, 11), (10, 11), (9, 10), (8, 10),
                 (15, 13), (16, 13), (17, 12), (18, 12), (19, 11), (20, 11)):
        put(sheet, x, y, STRAP)
    for x in list(range(21, 32)) + list(range(0, 8)):
        put(sheet, x, 10, STRAP)


def earring(sheet):
    """A gold hoop in the right ear."""
    put(sheet, 4, 15, GOLD)
    put(sheet, 4, 16, GOLD_DARK)


def tricorne(sheet):
    """Black felt with a gold edge, its three points at the front and the two back corners, and a
    skull on the front. Flat on the head: see the module notes for why it cannot stand off it."""
    face(sheet, HEAD.top(), FELT)
    x, y, w, h = HEAD.top()
    rect(sheet, x + 2, y + 2, x + w - 3, y + h - 3, FELT_LIGHT)
    for i in range(w):
        put(sheet, x + i, y, GOLD)
        put(sheet, x + i, y + h - 1, GOLD)
    for j in range(h):
        put(sheet, x, y + j, GOLD)
        put(sheet, x + w - 1, y + j, GOLD)
    band(sheet, HEAD, 0, FELT, rows=2)
    band(sheet, HEAD, 2, GOLD)
    for px in (11, 12, 0, 31, 23, 24):  # the three points: front, and either side of each back corner
        put(sheet, px, 10, FELT)
        put(sheet, px, 11, GOLD)
    for px, py in ((11, 8), (12, 8), (10, 9), (13, 9)):
        put(sheet, px, py, BONE)
    put(sheet, 11, 9, FELT_LIGHT)
    put(sheet, 12, 9, FELT_LIGHT)


def bandana(sheet, cloth, dark, dots):
    """Tied round the head down to the brows, spotted, knotted at the back with the ends hanging."""
    face(sheet, HEAD.top(), cloth)
    band(sheet, HEAD, 0, cloth, rows=3)
    band(sheet, HEAD, 3, dark)
    x, y, w, h = HEAD.top()
    for j in range(h):
        for i in range(w):
            if (i + 2 * j) % 4 == 1 and j % 2 == 1:
                put(sheet, x + i, y + j, dots)
    for px in range(32):
        if px % 4 == 1:
            put(sheet, px, 8, dots)
        if px % 4 == 3:
            put(sheet, px, 10, dots)
    # The knot, middle of the back, and its two ends hanging from it.
    for px, py in ((27, 11), (28, 11), (27, 12), (28, 12), (27, 13), (27, 14)):
        put(sheet, px, py, cloth)
    put(sheet, 28, 11, dark)
    put(sheet, 27, 14, dark)


def headscarf(sheet, cloth, dark, edge):
    """Banded rather than spotted, a white edge, and knotted over the right ear instead."""
    face(sheet, HEAD.top(), cloth)
    band(sheet, HEAD, 0, cloth, rows=3)
    band(sheet, HEAD, 1, dark)
    band(sheet, HEAD, 3, edge)
    x, y, w, h = HEAD.top()
    for j in range(1, h, 3):
        for i in range(w):
            put(sheet, x + i, y + j, dark)
    for px, py in ((2, 12), (3, 12), (2, 13), (2, 14), (3, 13)):
        put(sheet, px, py, cloth)
    put(sheet, 3, 13, dark)


# The body's strip: right side 16-21, front 22-29, left side 30-35, back 36-43; rows 26-37. The
# belt is rows 34-37, and vanilla's grey collar hangs down the middle of the front.
NECK = {(24, 26), (25, 26), (26, 26), (27, 26), (24, 27), (25, 27), (26, 27), (27, 27), (25, 28), (26, 28)}


def striped_shirt(sheet, stripe, ground):
    """Hooped, body and sleeves, to the belt and the cuffs, leaving the collar open."""
    for row in range(8):
        band(sheet, BODY, row, ground if row % 2 == 0 else stripe)
    for px, py in NECK:
        put(sheet, px, py, CLEAR)
    for row in range(7):
        band(sheet, ARM, row, ground if row % 2 == 0 else stripe)
    face(sheet, ARM.top(), ground)


def coat(sheet):
    """A navy frock coat to the thigh, open down the front with gold edging and buttons, gold cuffs,
    and gold epaulettes on the shoulders. On the jacket box, which stands off the body."""
    face(sheet, JACKET.top(), NAVY)
    for row in range(17):
        band(sheet, JACKET, row, NAVY)
    band(sheet, JACKET, 16, GOLD)
    x, y, _, _ = JACKET.front()
    for row in range(17):
        put(sheet, x + 2, y + row, GOLD)
        put(sheet, x + 5, y + row, GOLD)
        put(sheet, x + 3, y + row, CLEAR)
        put(sheet, x + 4, y + row, CLEAR)
    for row in (3, 6, 9):
        put(sheet, x + 1, y + row, GOLD)
        put(sheet, x + 6, y + row, GOLD)
    bx, by, _, _ = JACKET.back()
    rect(sheet, bx + 3, by + 11, bx + 4, by + 15, NAVY_DARK)
    put(sheet, bx + 3, by + 10, GOLD)
    put(sheet, bx + 4, by + 10, GOLD)
    for row in range(6):
        band(sheet, ARM, row, NAVY)
    band(sheet, ARM, 6, GOLD, rows=2)
    face(sheet, ARM.top(), GOLD)
    tx, ty, tw, th = ARM.top()
    rect(sheet, tx + 1, ty + 1, tx + tw - 2, ty + th - 2, GOLD_DARK)


def sash(sheet):
    """A red sash wound over the belt and knotted at the left hip, and a leather bandolier from the
    right shoulder across to it, a gold buckle on the chest."""
    band(sheet, BODY, 8, RED)
    band(sheet, JACKET, 9, RED)
    band(sheet, JACKET, 10, RED_DARK)
    band(sheet, JACKET, 11, RED)
    x, y, _, _ = JACKET.front()
    rect(sheet, x + 6, y + 12, x + 7, y + 15, RED)
    for row in range(12, 16):
        put(sheet, x + 7, y + row, RED_DARK)
    put(sheet, x + 6, y + 15, RED_DARK)
    # Bandolier: down the front from the right shoulder to the left hip, back up behind.
    for i in range(7):
        put(sheet, 22 + i, 26 + i, LEATHER)
        put(sheet, 23 + i, 26 + i, LEATHER_DARK if i < 6 else LEATHER)
        put(sheet, 36 + i, 32 - i, LEATHER)
        put(sheet, 37 + i, 32 - i, LEATHER_DARK if i < 6 else LEATHER)
    for px in range(30, 36):
        put(sheet, px, 32, LEATHER)
    for px in range(16, 22):
        put(sheet, px, 26, LEATHER)
    put(sheet, 25, 29, GOLD)
    put(sheet, 26, 29, GOLD)


def captain():
    sheet = blank()
    eyepatch(sheet)
    earring(sheet)
    tricorne(sheet)
    coat(sheet)
    return sheet


def deckhand_stripes():
    sheet = blank()
    bandana(sheet, RED, RED_DARK, WHITE)
    striped_shirt(sheet, RED, WHITE)
    return sheet


def deckhand_patch():
    sheet = blank()
    eyepatch(sheet)
    bandana(sheet, FELT, PATCH, WHITE)
    earring(sheet)
    return sheet


def deckhand_sash():
    sheet = blank()
    headscarf(sheet, BLUE, BLUE_DARK, WHITE)
    earring(sheet)
    sash(sheet)
    return sheet


VARIANTS = {
    "captain": captain,
    "deckhand_stripes": deckhand_stripes,
    "deckhand_patch": deckhand_patch,
    "deckhand_sash": deckhand_sash,
}


# Previews: never shipped, only for looking at.

def composite(base, overlay):
    out = [list(row) for row in base]
    return stamp(out, overlay, 0, 0)


def mirror(pixels):
    return [list(reversed(row)) for row in pixels]


def doll(sheet, side):
    """A flat paper doll of one side, assembled from the faces: arms, body, legs, the jacket over
    them and the head on top. From the back, the entity's right is on the viewer's right."""
    pick = (lambda b: b.front()) if side == "front" else (lambda b: b.back())
    out = [[CLEAR] * 16 for _ in range(34)]

    def part(box, flip=False):
        art = crop(sheet, *pick(box))
        return mirror(art) if flip else art

    right_x, left_x = (0, 12) if side == "front" else (12, 0)
    stamp(out, part(LEG), 4 if side == "front" else 8, 22)
    stamp(out, part(LEG, True), 8 if side == "front" else 4, 22)
    stamp(out, part(BODY), 4, 10)
    stamp(out, part(JACKET), 4, 10)
    stamp(out, part(ARM), right_x, 10)
    stamp(out, part(ARM, True), left_x, 10)
    stamp(out, part(HEAD), 4, 0)
    return out


def backdrop(pixels, colour=(64, 96, 128, 255)):
    return [[px if px[3] else colour for px in row] for row in pixels]


def preview(name, sheet, base):
    dressed = composite(base, sheet)
    front, back = doll(dressed, "front"), doll(dressed, "back")
    pair = [[CLEAR] * 36 for _ in range(34)]
    stamp(pair, front, 0, 0)
    stamp(pair, back, 20, 0)
    write_png(os.path.join(PREVIEW, name + "_front_back.png"), scale(backdrop(pair), 12))
    write_png(os.path.join(PREVIEW, name + "_sheet.png"), scale(backdrop(dressed), 8))
    return pair


def main():
    sheets = {name: draw() for name, draw in VARIANTS.items()}
    for name, sheet in sheets.items():
        write_png(os.path.join(OUT, name + ".png"), sheet)
    if PREVIEW:
        base = vanilla("entity/illager/pillager.png")
        pairs = [preview("vanilla", blank(), base)]
        pairs += [preview(name, sheet, base) for name, sheet in sheets.items()]
        row = [[CLEAR] * (40 * len(pairs)) for _ in range(34)]
        for i, pair in enumerate(pairs):
            stamp(row, pair, 40 * i + 2, 0)
        write_png(os.path.join(PREVIEW, "all_crew.png"), scale(backdrop(row), 8))


if __name__ == "__main__":
    main()
