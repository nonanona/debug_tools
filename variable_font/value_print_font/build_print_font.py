import sys
import math
import argparse
from fontTools import ttLib
from fontTools.ttLib import TTFont
from fontTools.ttLib.tables import otTables
from fontTools.ttLib import newTable
from fontTools.varLib.featureVars import addFeatureVariations
from fontTools.varLib.models import normalizeValue
from fontTools.pens.ttGlyphPen import TTGlyphPen

glyf_mod = ttLib.getTableModule('glyf')
fvar_mod = ttLib.getTableModule('fvar')

SEGMENT_WIDTH_RATIO = 0.10  # 11%
SEGMENT_PADDING_RATIO = 0.05  # 2%

W = SEGMENT_WIDTH_RATIO  # single segment width
P = SEGMENT_PADDING_RATIO # segment padding
L = (1.0 - W) / 2 # segment length (calculated)
G_W = L + W  + 2 * P  # glyph width

VERTICAL_SEGMENT = [
    (W / 2, P), (W, W / 2 + P), (W, L - W / 2 - P),
    (W / 2, L - P), (0, L - W / 2 - P), (0, W / 2 + P)
]

HORIZONTAL_SEGMENT = [
    (W / 2 + P, 0), (L - W / 2 - P, 0), (L - P, W / 2),
    (L - W / 2 - P, W), (W / 2 + P, W), (P, W / 2)
]

def offset(segments, x_off, y_off):
  return [(x + x_off, y + y_off) for x, y in segments]

SEGMENTS = [
    offset(HORIZONTAL_SEGMENT, W / 2, (1.0 - W)),
    offset(VERTICAL_SEGMENT,       0, L + W / 2),
    offset(VERTICAL_SEGMENT,       L, L + W / 2),
    offset(HORIZONTAL_SEGMENT, W / 2, (1.0 - W) / 2),
    offset(VERTICAL_SEGMENT,       0, W / 2),
    offset(VERTICAL_SEGMENT,       L, W / 2),
    offset(HORIZONTAL_SEGMENT, W / 2,     0),
]

# -0-
# 1 2
# -3-
# 4 5
# -6-
NUM_TO_SEGMENT = [
    [0, 1, 2, 4, 5, 6], # 0
    [2, 5], # 1
    [0, 2, 3, 4, 6], # 2
    [0, 2, 3, 5, 6], # 3
    [1, 2, 3, 5], # 4
    [0, 1, 3, 5, 6], #5
    [0, 1, 3, 4, 5, 6], # 6
    [0, 2, 5], # 7
    [0, 1, 2, 3, 4, 5, 6], # 8
    [0, 1, 2, 3, 5, 6], # 9
]

def scale(segments, factor):
  return [(int(x * factor), int(y * factor)) for x, y in segments]

def setup7SegGlyphs(ttf):
  head = ttf['head']
  glyf = ttf['glyf']
  hmtx = ttf['hmtx']
  glyph_order = list(ttf.getGlyphOrder())

  glyph_height = head.unitsPerEm * 0.9  # set glyph height to 0.9em
  for i in range(0, 10):
    contours = [scale(SEGMENTS[x], glyph_height) for x in NUM_TO_SEGMENT[i]]

    glyph_id = '%d.7seg' % i

    pen = TTGlyphPen()

    for contour in contours:
      pen.moveTo(contour[0])
      for coord in contour[1:]:
        pen.lineTo(coord)
      pen.closePath()
    glyph = pen.glyph()

    glyf[glyph_id] = glyph

    lsb = glyph_height
    for contour in contours:
      for coord in contour:
        lsb = min(coord[0], lsb)
    hmtx[glyph_id] = (int(G_W * glyph_height), int(glyph_height * P + lsb))
    glyph_order.append(glyph_id)

    # debug
    # cmap = ttf['cmap']
    # for t in cmap.tables:
    #   t.cmap[0x0030 + i] = glyph_id

SUPPORTED_AXES = {
    'wght': (0, 400, 1000),
    'wdth': (0, 100, 200),
    'opsz': (0, 14, 200),
}

def normalize(value, axis):
  return normalizeValue(value, (axis.minValue, axis.defaultValue, axis.maxValue))

def buildComponent(num, hmtx):
  digits = list('%04d' % num)

  if digits[0] == '0':
    largest_digit_index = 1 # reserve 3 digits
  else:
    largest_digit_index = 0

  glyph_advance = 0
  components = []

  for i in range(largest_digit_index, 4):
    glyph_id = '%s.7seg' % digits[i]

    c = glyf_mod.GlyphComponent()
    c.glyphName = glyph_id
    c.x = glyph_advance
    c.y = 0
    c.flags = 0x4
    components.append(c)

    (advance, _) = hmtx[glyph_id]
    glyph_advance += advance

  larget_glyph_id = '%s.7seg' % digits[largest_digit_index]
  (_, overall_lsb) = hmtx[larget_glyph_id]
  return (components, glyph_advance, overall_lsb)

def generate_font(template, pua, axis_tag, out_ttx, out_ttf):
  ttf = TTFont()
  ttf.importXML(template)

  setup7SegGlyphs(ttf)

  glyf = ttf['glyf']
  hmtx = ttf['hmtx']
  glyph_order = list(ttf.getGlyphOrder())

  # First, Generate component glyphs 000 to 1000
  for i in range(0, 1001):
    glyph_id = '%03d.glyph' % i
    glyph = glyf_mod.Glyph()
    glyph.numberOfContours = -1
    (components, advance, lsb) = buildComponent(i, hmtx)
    glyph.components = components
    glyf[glyph_id] = glyph
    hmtx[glyph_id] = (advance, lsb)

    glyph_order.append(glyph_id)
  ttf.setGlyphOrder(glyph_order)

  # Add fvar for the given axis
  ttf['fvar'] = fvar = newTable('fvar')
  axis = fvar_mod.Axis()
  (minValue, defaultValue, maxValue) = SUPPORTED_AXES[axis_tag]
  axis.axisTag = axis_tag
  axis.minValue = minValue
  axis.defaultValue = defaultValue
  axis.maxValue = maxValue
  fvar.axes.append(axis)

  # Add feature vairations in GSUB
  condSubst = []
  for axis in ttf['fvar'].axes:
    for v in range(int(math.floor(axis.minValue)), int(math.ceil(axis.maxValue)) + 1):
      minNormalized = normalize(v - 0.5, axis)
      maxNormalized = normalize(v + 0.5, axis)
      condSubst.append(
          ([{axis.axisTag: (minNormalized, maxNormalized)}],
           {'PUA.Trigger': "%03d.glyph" % v})
          )
  addFeatureVariations(ttf, condSubst)

  # Add Trigger PUA for it
  # PUA.Trigger is already in the template, therefore only add it into cmap
  cmap = ttf['cmap']
  for t in cmap.tables:
    t.cmap[pua] = 'PUA.Trigger'

  # TODO: Rename name table which only indicates weight prints but thie can generate width as well.

  # Generate XML and TTF file
  ttf.save(out_ttf)
  ttf = TTFont(out_ttf)
  ttf.saveXML(out_ttx)

if __name__ == '__main__':
  parser = argparse.ArgumentParser()
  parser.add_argument('-i', '--input')
  parser.add_argument('pua')
  parser.add_argument('axis')
  parser.add_argument('ttx')
  parser.add_argument('ttf')
  args = parser.parse_args()
  generate_font(args.input, int(args.pua, 16), args.axis, args.ttx, args.ttf)

