"""对账 openapi path 与代码里的真实映射（专抓幽灵端点）。

原版缺陷：只匹配带引号子路径的 @XxxMapping("...")，把无参的
@PostMapping（映射到类级路径本身）当成不存在，误报 7 条幽灵端点。
这里改成「类级前缀 × 方法级子路径（含空子路径）」的笛卡尔组合。
"""
import io
import os
import re
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

BASE = 'src/main/java/sicau/policialPartyManager/api/controller'
VERBS = ('Get', 'Post', 'Put', 'Delete', 'Patch')


def mapping_paths(txt):
    """返回该控制器里所有 (verb, 完整路径) —— 含无参映射。"""
    m = re.search(r'@RequestMapping\("([^"]+)"\)', txt)
    base = m.group(1) if m else ''
    out = []
    for verb in VERBS:
        # 匹配 @GetMapping 与 @GetMapping(...) 两种形态
        for mm in re.finditer(r'@' + verb + r'Mapping\b(\s*\(([^)]*)\))?', txt):
            args = mm.group(2) or ''
            sm = re.search(r'"([^"]*)"', args)
            sub = sm.group(1) if sm else ''
            full = base + sub if sub else base
            out.append((verb.lower(), full))
    return out


t = io.open('openapi.yaml', encoding='utf-8').read()
paths = sorted(set(re.findall(r'(?m)^  (/[^:]+):', t)))

code = []
for f in os.listdir(BASE):
    if f.endswith('.java'):
        code.extend(mapping_paths(io.open(os.path.join(BASE, f), encoding='utf-8',
                                          errors='ignore').read()))

codeset = set(p for _, p in code)
print('openapi paths:', len(paths))
print('code mappings:', len(code), '(unique paths:', len(codeset), ')')

missing = [p for p in paths if p not in codeset]
print()
print('=== openapi 有、代码无（幽灵端点）:', len(missing))
for p in missing:
    print('   ', p)

extra = sorted(c for c in codeset if c not in paths)
print()
print('=== 代码有、openapi 无:', len(extra))
for p in extra:
    print('   ', p)

sys.exit(1 if (missing or extra) else 0)
