import re, os
import sys

# Ensure translation_tools directory is in python path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from hi_data import translations_hi

en_file = 'app/src/main/res/values/strings.xml'
hi_file = 'app/src/main/res/values-hi/strings.xml'

def generate():
    with open(en_file, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    out = []
    missing = 0
    for line in lines:
        m = re.search(r'<string name="([^"]+)">([^<]+)</string>', line)
        if m:
            k = m.group(1)
            orig = m.group(2)
            if k in translations_hi:
                v = translations_hi[k]
            else:
                v = orig
                missing += 1
            indent = line[:line.find('<string')]
            out.append(f'{indent}<string name="{k}">{v}</string>\n')
        else:
            out.append(line)
    os.makedirs('app/src/main/res/values-hi', exist_ok=True)
    with open(hi_file, 'w', encoding='utf-8') as f:
        f.writelines(out)
    print(f"Generated {hi_file} with {len(translations_hi)} entries (missing: {missing})")

if __name__ == '__main__':
    generate()
