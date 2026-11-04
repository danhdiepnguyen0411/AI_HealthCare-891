import re, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from bn_data import translations_bn

en_file = 'app/src/main/res/values/strings.xml'
bn_file = 'app/src/main/res/values-bn/strings.xml'

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
            if k in translations_bn:
                v = translations_bn[k]
            else:
                v = orig
                missing += 1
            indent = line[:line.find('<string')]
            out.append(f'{indent}<string name="{k}">{v}</string>\n')
        else:
            out.append(line)
    os.makedirs('app/src/main/res/values-bn', exist_ok=True)
    with open(bn_file, 'w', encoding='utf-8') as f:
        f.writelines(out)
    print(f"Generated {bn_file} with {len(translations_bn)} entries (missing: {missing})")

if __name__ == '__main__':
    generate()

