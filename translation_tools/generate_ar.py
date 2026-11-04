import re, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from ar_data import translations_ar

en_file = 'app/src/main/res/values/strings.xml'
ar_file = 'app/src/main/res/values-ar/strings.xml'

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
            if k in translations_ar:
                v = translations_ar[k]
            else:
                v = orig
                missing += 1
            indent = line[:line.find('<string')]
            out.append(f'{indent}<string name="{k}">{v}</string>\n')
        else:
            out.append(line)
    os.makedirs('app/src/main/res/values-ar', exist_ok=True)
    with open(ar_file, 'w', encoding='utf-8') as f:
        f.writelines(out)
    print(f"Generated {ar_file} with {len(translations_ar)} entries (missing: {missing})")

if __name__ == '__main__':
    generate()

