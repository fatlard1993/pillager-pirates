#!/usr/bin/env python3
"""Generate the crew's pirate clothes: overlay sheets drawn over an illager's own texture.

Pandorical draws an overlay as a cutout layer on the entity's own model, so each sheet is laid out
exactly like vanilla's pillager.png and vindicator.png (64x64, the illager model) and every
transparent pixel leaves the illager showing through. Painted pixel by pixel onto that layout, the
boxes as the illager model declares them (IllagerModel.createBodyLayer, 26.3):

    head      8x10x8 at 0,0     (the face is the front, rows 8-17, columns 8-15)
    body      8x12x6 at 16,20
    jacket    8x20x6 at 0,38    (a robe inflated over the body; a pillager's only fills a belt
                                 and skirt, so the rest of it is free for a coat)
    arms      4x12x4 at 40,46   (both arms, the left mirrored)
    legs      4x12x4 at 0,22

and the crossed arms, the one part shown instead of the two arms whenever the arm pose is CROSSED,
which on a vindicator is whenever it is not after somebody:

    upper arms  4x8x4 at 44,22  (both, the left mirrored)
    forearms    8x4x4 at 40,38  (one bar across the chest, the hands in its middle)

A pillager never crosses its arms, so its sheets leave those alone. A vindicator's sheets paint both
forms alike, so its clothes do not change when it raises its axe. A vindicator also wears its own
coat on the jacket box, over the body (only the top 13 rows of it, open at the neck): whatever it
wears instead goes on the jacket too, since the body underneath only shows at that neck.

The hat box at 32,0 is hidden on every illager but the illusioner, so a hat cannot stand off the
head here: the tricorne is painted onto the head itself, as the bandanas are.

Six sheets: for the pillagers, the captain in a tricorne and a gold-trimmed coat and three
deckhands; for the vindicators, the bosun and the quartermaster. Vanilla's textures are only read
to render the previews; the sheets themselves are drawn from nothing.

Pure stdlib (the PNG reader and writer come from generate_icon.py). Deterministic: re-running
produces identical bytes.

Usage: python3 generate_crew_textures.py [path/to/minecraft.jar] [--preview DIR]
    --preview DIR   also write each sheet composited over the vanilla illager it is for, a front
                    and back view (a vindicator's with its arms crossed too) and the whole UV
                    sheet, scaled up, plus all of them side by side.
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
SKIN, SKIN_SHADE = (142, 147, 147, 255), (126, 131, 131, 255)  # vanilla's illager grey
HAND = (149, 155, 155, 255)
INK = (40, 62, 112, 255)
SEA, SEA_DARK = (40, 128, 112, 255), (24, 84, 74, 255)
GREEN, GREEN_DARK = (52, 112, 58, 255), (32, 74, 38, 255)
LINEN, LINEN_DARK = (226, 222, 208, 255), (186, 180, 164, 255)


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
UPPER_ARM = Box(44, 22, 4, 8, 4)
FOREARMS = Box(40, 38, 8, 4, 4)


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


# The vindicators. Its coat is on the jacket box, rows 44-56; the front face is columns 6-13.

def sleeves(sheet, cloth, cuff, shade=None):
    """Both arm forms dressed alike: the separate arms (a sleeve to the wrist, the cuff on the last
    rows above the hand) and the crossed arms (the upper arms whole, and the bar of forearms everywhere
    but the hands). Vanilla's hands are the bar's middle five columns - one further left on the back -
    and the separate arm's last rows, with a few of row 59; those are left showing.
    shade, if given, darkens the back of each arm."""
    shade = shade or cloth
    # Separate arms: right 40-43, front 44-47, left 48-51, back 52-55; rows 50-61.
    for row in range(8):
        band(sheet, ARM, row, cloth)
    rect(sheet, 52, 50, 55, 57, shade)
    band(sheet, ARM, 8, cuff)
    for px in range(40, 56):
        if px - 40 not in (1, 2, 9, 10):
            put(sheet, px, 59, cuff)
    put(sheet, 45, 60, HAND)  # vanilla's dark sleeve end notches into the front of the hand
    put(sheet, 46, 60, HAND)
    face(sheet, ARM.top(), cloth)
    # Crossed: the upper arms, shoulder to elbow and both ends.
    for row in range(8):
        band(sheet, UPPER_ARM, row, cloth)
    rect(sheet, 56, 26, 59, 33, shade)
    face(sheet, UPPER_ARM.top(), cloth)
    x, y, w, h = UPPER_ARM.top()
    rect(sheet, x + w, y, x + 2 * w - 1, y + h - 1, cloth)  # the elbows
    # The forearms: both ends, and round the hands.
    rect(sheet, 40, 42, 43, 45, cloth)
    rect(sheet, 52, 42, 55, 45, cloth)
    for fx, fy in ((44, 42), (44, 38), (52, 38)):  # front, top, bottom: hands in columns 2-6
        rect(sheet, fx, fy, fx, fy + 3, cloth)
        rect(sheet, fx + 1, fy, fx + 1, fy + 3, cuff)
        rect(sheet, fx + 7, fy, fx + 7, fy + 3, cuff)
    rect(sheet, 56, 42, 56, 45, cuff)  # back: hands in columns 1-5
    rect(sheet, 62, 42, 62, 45, cuff)
    rect(sheet, 63, 42, 63, 45, shade)


def anchor(sheet, x, y):
    """A tattooed anchor, three wide and five tall, its top-left corner at x, y."""
    for dx, dy in ((1, 0), (0, 1), (1, 1), (2, 1), (1, 2), (0, 3), (2, 3), (1, 4)):
        put(sheet, x + dx, y + dy, INK)


def tails(sheet, cloth, dark):
    """A headscarf knotted at the back of the head, its two ends hanging long behind."""
    for px, py in ((27, 11), (28, 11), (27, 12), (28, 12), (27, 13), (28, 13), (27, 14),
                   (28, 15), (27, 16)):
        put(sheet, px, py, cloth)
    for px, py in ((28, 12), (28, 14), (27, 15), (28, 16), (27, 17)):
        put(sheet, px, py, dark)


def bosun_scarf(sheet):
    """Sea green, plain, pulled down to the brows, knotted at the back with long tails."""
    face(sheet, HEAD.top(), SEA)
    band(sheet, HEAD, 0, SEA, rows=3)
    band(sheet, HEAD, 3, SEA_DARK)
    x, y, w, h = HEAD.top()
    for j in range(0, h, 3):
        put(sheet, x + 1 + j % 5, y + j, SEA_DARK)
    tails(sheet, SEA, SEA_DARK)


def breton(sheet):
    """A sleeveless shirt in navy and white hoops, over the coat on the jacket box, open at the neck,
    with a broad belt and a gold buckle at the hem."""
    face(sheet, JACKET.top(), WHITE)
    for row in range(11):
        band(sheet, JACKET, row, WHITE if row % 2 == 0 else NAVY)
    x, y, _, _ = JACKET.front()
    put(sheet, x + 3, y, CLEAR)  # the neck, where vanilla's coat is open too
    put(sheet, x + 4, y, CLEAR)
    band(sheet, JACKET, 11, LEATHER, rows=2)
    band(sheet, JACKET, 12, LEATHER_DARK)
    rect(sheet, x + 3, y + 11, x + 4, y + 12, GOLD)
    put(sheet, x + 3, y + 12, GOLD_DARK)


def whistle(sheet):
    """A bosun's call on a gold chain round the neck, the whistle on the chest."""
    x, y, _, _ = JACKET.front()
    for dx, dy in ((2, 0), (3, 1), (5, 0), (4, 1), (3, 2), (4, 2)):
        put(sheet, x + dx, y + dy, GOLD)
    put(sheet, x + 5, y + 2, GOLD_DARK)
    put(sheet, x + 3, y + 3, GOLD_DARK)


def tattoos(sheet):
    """An anchor on the outside of each upper arm, crossed or not: the outside is the box's right
    face, which the mirrored left arm turns outward as well."""
    anchor(sheet, 40, 51)   # separate arm, right face 40-43
    anchor(sheet, 44, 27)   # crossed upper arm, right face 44-47


def quartermaster_scarf(sheet):
    """Black, banded gold, knotted over the left ear, where the deckhands' knot over the right."""
    face(sheet, HEAD.top(), FELT)
    band(sheet, HEAD, 0, FELT, rows=3)
    band(sheet, HEAD, 1, FELT_LIGHT)
    band(sheet, HEAD, 3, GOLD)
    x, y, w, h = HEAD.top()
    for j in range(1, h, 3):
        for i in range(w):
            put(sheet, x + i, y + j, FELT_LIGHT)
    # The left side runs 16-23 with its front edge at 16: the knot sits behind the ear.
    for px, py in ((20, 12), (21, 12), (20, 13), (21, 13), (21, 14), (21, 15), (20, 16)):
        put(sheet, px, py, FELT)
    put(sheet, 21, 13, FELT_LIGHT)
    put(sheet, 20, 16, GOLD)


def waistcoat(sheet):
    """A leather waistcoat over a linen shirt, buttoned gold below an open neck, laced up the back;
    a green sash wound over it and knotted at the right hip."""
    face(sheet, JACKET.top(), LEATHER)
    for row in range(13):
        band(sheet, JACKET, row, LEATHER)
    band(sheet, JACKET, 12, LEATHER_DARK)
    x, y, _, _ = JACKET.front()
    # The shirt, open at the neck and showing down to the sash.
    rect(sheet, x + 2, y, x + 5, y + 2, LINEN)
    rect(sheet, x + 3, y + 3, x + 4, y + 8, LINEN)
    put(sheet, x + 3, y, CLEAR)
    put(sheet, x + 4, y, CLEAR)
    for row in range(3, 9):
        put(sheet, x + 2, y + row, LEATHER_DARK)
        put(sheet, x + 5, y + row, LEATHER_DARK)
    for row in (4, 6, 8):
        put(sheet, x + 2, y + row, GOLD)
    bx, by, _, _ = JACKET.back()
    for row in range(1, 9):
        put(sheet, bx + 3 + row % 2, by + row, LEATHER_DARK)
    # The sash, and its knot hanging at the right hip: the front face's first columns.
    band(sheet, JACKET, 9, GREEN)
    band(sheet, JACKET, 10, GREEN_DARK)
    band(sheet, JACKET, 11, GREEN)
    rect(sheet, x, y + 12, x + 1, y + 15, GREEN)
    for row in range(12, 16):
        put(sheet, x, y + row, GREEN_DARK)
    put(sheet, x + 1, y + 15, GREEN_DARK)


def baldric(sheet):
    """A black strap from the left shoulder across to the right hip, the other way to the deckhands'
    bandolier, with a gold buckle on the chest; up the back from the left hip to the right shoulder."""
    x, y, _, _ = JACKET.front()
    bx, by, _, _ = JACKET.back()
    for i in range(8):
        put(sheet, x + 7 - i, y + i, STRAP)
        put(sheet, bx + i, by + 7 - i, STRAP)
    put(sheet, x + 4, y + 3, GOLD)
    put(sheet, x + 5, y + 2, GOLD)


def earrings(sheet):
    """Gold hoops in both ears."""
    earring(sheet)
    put(sheet, 19, 15, GOLD)
    put(sheet, 19, 16, GOLD_DARK)


def bosun():
    sheet = blank()
    bosun_scarf(sheet)
    breton(sheet)
    whistle(sheet)
    sleeves(sheet, SKIN, LEATHER_DARK, SKIN_SHADE)
    tattoos(sheet)
    return sheet


def quartermaster():
    sheet = blank()
    quartermaster_scarf(sheet)
    earrings(sheet)
    waistcoat(sheet)
    baldric(sheet)
    sleeves(sheet, LINEN, LINEN_DARK)
    return sheet


VARIANTS = {
    "captain": captain,
    "deckhand_stripes": deckhand_stripes,
    "deckhand_patch": deckhand_patch,
    "deckhand_sash": deckhand_sash,
    "bosun": bosun,
    "quartermaster": quartermaster,
}

# Which vanilla illager each sheet is drawn over: only for the previews.
VINDICATORS = ("bosun", "quartermaster")


# Previews: never shipped, only for looking at.

def composite(base, overlay):
    out = [list(row) for row in base]
    return stamp(out, overlay, 0, 0)


def mirror(pixels):
    return [list(reversed(row)) for row in pixels]


def doll(sheet, side, crossed=False):
    """A flat paper doll of one side, assembled from the faces: arms, body, legs, the jacket over
    them and the head on top. From the back, the entity's right is on the viewer's right. Crossed,
    the upper arms hang at the sides and the forearms lie across the chest, roughly where the
    model tilts them up to."""
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
    if crossed:
        stamp(out, part(UPPER_ARM), right_x, 11)
        stamp(out, part(UPPER_ARM, True), left_x, 11)
        if side == "front":
            stamp(out, part(FOREARMS), 4, 15)
    else:
        stamp(out, part(ARM), right_x, 10)
        stamp(out, part(ARM, True), left_x, 10)
    stamp(out, part(HEAD), 4, 0)
    return out


def backdrop(pixels, colour=(64, 96, 128, 255)):
    return [[px if px[3] else colour for px in row] for row in pixels]


def preview(name, sheet, base, crossed=False):
    """Front and back; a vindicator's again with its arms crossed, front and back."""
    dressed = composite(base, sheet)
    views = [doll(dressed, "front"), doll(dressed, "back")]
    if crossed:
        views += [doll(dressed, "front", True), doll(dressed, "back", True)]
    pair = [[CLEAR] * (20 * len(views) - 4) for _ in range(34)]
    for i, view in enumerate(views):
        stamp(pair, view, 20 * i, 0)
    write_png(os.path.join(PREVIEW, name + "_front_back.png"), scale(backdrop(pair), 12))
    write_png(os.path.join(PREVIEW, name + "_sheet.png"), scale(backdrop(dressed), 8))
    return pair


def main():
    sheets = {name: draw() for name, draw in VARIANTS.items()}
    for name, sheet in sheets.items():
        write_png(os.path.join(OUT, name + ".png"), sheet)
    if PREVIEW:
        pillager = vanilla("entity/illager/pillager.png")
        vindicator = vanilla("entity/illager/vindicator.png")
        lineup("all_crew.png", [preview("vanilla", blank(), pillager)]
               + [preview(name, sheet, pillager) for name, sheet in sheets.items() if name not in VINDICATORS])
        lineup("all_vindicators.png", [preview("vanilla_vindicator", blank(), vindicator, True)]
               + [preview(name, sheet, vindicator, True) for name, sheet in sheets.items() if name in VINDICATORS])


def lineup(name, pairs):
    """The previews side by side, one after another."""
    widths = [len(pair[0]) + 4 for pair in pairs]
    row = [[CLEAR] * sum(widths) for _ in range(34)]
    left = 0
    for pair, width in zip(pairs, widths):
        stamp(row, pair, left + 2, 0)
        left += width
    write_png(os.path.join(PREVIEW, name), scale(backdrop(row), 8))


if __name__ == "__main__":
    main()
