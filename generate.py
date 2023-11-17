import os
import sys
import re

UNICODE_DIR = "emoji/EmojiDumpApp/app/src/main/assets/emoji"
DATA_FILE = "emoji-data.txt"
ZWJ_SEQUENCES = "emoji-zwj-sequences.txt"
SEQUECNES = "emoji-sequences.txt"

DATA_FILE_REG = re.compile('^([0-9A-F\.]*) *; *(.*)# *(.*) *\[(\d*)\].*$')
SEQ_FILE_REG = re.compile('^([0-9A-F \.]*) *; *(.*); .*#(.*) *\[(\d*)\].*$')
TOTAL_REG = re.compile('^# Total elements: (\d*).*$')

def parse_sequence(x):
  if '..' in x:
    startHex, endHex = x.split('..')
    start = int(startHex, 16)
    end = int(endHex, 16)
    return [[y] for y in range(start, end + 1)]
  if ' ' in x:
    return [[int(y, 16) for y in x.split(' ')]]
  else:
    return [[int(x, 16)]]

def parse(fpath, regex):
  with open(fpath) as f:
    lines = f.read().splitlines()

  result = {}
  group_count = {}

  current_groupe = None
  for line in lines:
    m = regex.match(line)
    if m:
      cps = m.group(1).strip()
      group = m.group(2).strip()
      version = m.group(3).strip()
      count = int(m.group(4))
      arr = parse_sequence(cps)
      if len(arr) != count:
        print('The code point count doesn\'t match the count: len(arr) = %d, count = %d' % (len(arr), count))
        sys.exit(1)
      current_group = group
      if group not in result:
        result[group] = {}
      if version not in result[group]:
        result[group][version] = []
      result[group][version] += arr
      continue

    m = TOTAL_REG.match(line)
    if m:
      count = int(m.group(1))
      if current_group not in group_count:
        group_count[current_group] = 0
      group_count[current_group] = group_count[current_group] + count
    else:
      if not line.startswith('#') and len(line) != 0:
        print(line)
        print('Ignoring empty or comment line. maybe bug?')
        sys.exit(1)

  # check the count
  for g_key in result.keys():
    total = sum([len(x) for x in result[g_key].values()])
    if total != group_count[g_key]:
      print('The count of group %s does not match: expected %d, actual %d' % (g_key, group_count[g_key], total))

  return result

def read_emoji_txt():
  return parse(os.path.join(UNICODE_DIR, DATA_FILE), DATA_FILE_REG)

def read_emoji_sequences():
  return parse(os.path.join(UNICODE_DIR, SEQUECNES), SEQ_FILE_REG)

def read_emoji_zwj_sequences():
  return parse(os.path.join(UNICODE_DIR, ZWJ_SEQUENCES), SEQ_FILE_REG)

def parse_unicodes():
  r = {**read_emoji_txt(), **read_emoji_sequences(), **read_emoji_zwj_sequences()}
  for key in r.keys():
    r[key]
  return r

def main():
  show_keys = [
      'Basic_Emoji',
      'Emoji_Keycap_Sequence',
      'RGI_Emoji_Flag_Sequence',
      'RGI_Emoji_Modifier_Sequence',
      'RGI_Emoji_Tag_Sequence',
      'RGI_Emoji_ZWJ_Sequence'
      ]
  db = parse_unicodes()

  out = []
  out.append('<html><body>')
  for key in show_keys:
    out.append('<h1>%s</h1>' % key)
    for version in sorted(db[key], key=lambda x: float(x[1:])):
      out.append('  <h2>%s</h2>' % version)
      out.append('<div>')
      for seq in sorted(db[key][version]):
        out.append(''.join(['&#x%04X;' % x for x in seq]))
      out.append('</div>')
  out.append('</body></html>')
  print('\n'.join(out))

if __name__ == '__main__':
  main()
