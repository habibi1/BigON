"""Convert the Sinema technical-solution artifact HTML into repository Markdown.

Purpose-built rather than generic: the document encodes meaning in classes
(status pills, section numbers, callout panels) that a generic converter would
flatten into indistinguishable paragraphs.
"""
import html
import re
import sys
from html.parser import HTMLParser

VOID = {"br", "hr", "img", "meta", "link", "input"}


class Node:
    def __init__(self, tag, attrs=None):
        self.tag = tag
        self.attrs = dict(attrs or {})
        self.kids = []

    def cls(self):
        return self.attrs.get("class", "").split()

    def text(self):
        out = []
        for k in self.kids:
            out.append(k if isinstance(k, str) else k.text())
        return "".join(out)


class Tree(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.root = Node("root")
        self.stack = [self.root]

    def handle_starttag(self, tag, attrs):
        n = Node(tag, attrs)
        self.stack[-1].kids.append(n)
        if tag not in VOID:
            self.stack.append(n)

    def handle_startendtag(self, tag, attrs):
        self.stack[-1].kids.append(Node(tag, attrs))

    def handle_endtag(self, tag):
        for i in range(len(self.stack) - 1, 0, -1):
            if self.stack[i].tag == tag:
                del self.stack[i:]
                return

    def handle_data(self, data):
        self.stack[-1].kids.append(data)


def collapse(s):
    return re.sub(r"\s+", " ", s)


def esc(s):
    # Escape only what would otherwise become markdown syntax mid-sentence.
    return re.sub(r"(?<![\w`])([*_])(?=\w)", r"\\\1", s)


PILL = {"ok": "**Done**", "warn": "**Partial**", "gap": "**Gap**"}


def inline(node):
    """Render inline content, preserving code/emphasis/links."""
    out = []
    for k in node.kids:
        if isinstance(k, str):
            out.append(esc(collapse(k)))
            continue
        t, c = k.tag, k.cls()
        if t == "code":
            out.append("`" + collapse(k.text()).strip() + "`")
        elif t in ("strong", "b"):
            out.append("**" + inline(k).strip() + "**")
        elif t in ("em", "i"):
            out.append("*" + inline(k).strip() + "*")
        elif t == "a":
            out.append(f"[{inline(k).strip()}]({k.attrs.get('href','')})")
        elif t == "br":
            out.append("  \n")
        elif t == "span" and "pill" in c:
            label = collapse(k.text()).strip()
            kind = next((x for x in c if x in PILL), None)
            # Keep the author's own wording; the class only picks the weight.
            out.append(f"**{label}**" if kind else label)
        elif t == "span" and "mono" in c:
            out.append("`" + collapse(k.text()).strip() + "`")
        else:
            out.append(inline(k))
    return "".join(out)


def code_block(pre):
    """<pre> keeps literal whitespace; syntax <span>s are decoration only."""
    parts = []

    def walk(n):
        for k in n.kids:
            if isinstance(k, str):
                parts.append(k)
            else:
                walk(k)

    walk(pre)
    body = html.unescape("".join(parts))
    lines = [ln.rstrip() for ln in body.split("\n")]
    while lines and not lines[0]:
        lines.pop(0)
    while lines and not lines[-1]:
        lines.pop()
    lang = "mermaid" if "mermaid" in pre.cls() else "kotlin"
    return "```" + lang + "\n" + "\n".join(lines) + "\n```"


def table(node):
    rows = []
    for tr in iter_tag(node, "tr"):
        cells = [inline(td).strip() or "—"
                 for td in tr.kids
                 if not isinstance(td, str) and td.tag in ("td", "th")]
        rows.append([c.replace("|", "\\|") for c in cells])
    if not rows:
        return ""
    head, body = rows[0], rows[1:]
    width = max(len(r) for r in rows)
    head += [""] * (width - len(head))
    out = ["| " + " | ".join(head) + " |",
           "|" + "|".join([" --- "] * width) + "|"]
    for r in body:
        r += [""] * (width - len(r))
        out.append("| " + " | ".join(r) + " |")
    return "\n".join(out)


def split_pills(node):
    """Separate a heading's text from its status pills.

    Pills render as bold; nesting them inside a bold heading would produce
    `**a **b****`, which no Markdown parser resolves the way it reads.
    """
    pills = []

    def strip(n):
        out = []
        for k in n.kids:
            if isinstance(k, str):
                out.append(esc(collapse(k)))
            elif k.tag == "span" and "pill" in k.cls():
                pills.append(collapse(k.text()).strip())
            else:
                out.append(strip(k))
        return "".join(out)

    title = re.sub(r"\s+", " ", strip(node)).strip(" ·—-")
    return title, pills


def iter_tag(node, tag):
    for k in node.kids:
        if isinstance(k, str):
            continue
        if k.tag == tag:
            yield k
        else:
            yield from iter_tag(k, tag)


def blocks(node, out, depth=0):
    for k in node.kids:
        if isinstance(k, str):
            continue
        t, c = k.tag, k.cls()

        if t == "h1":
            out.append("# " + collapse(k.text()).strip().replace(" — ", " — "))
        elif t == "h2":
            pass  # emitted by the .shead handler with its section number
        elif t == "div" and "shead" in c:
            no = next((collapse(x.text()).strip() for x in k.kids
                       if not isinstance(x, str) and "no" in x.cls()), "")
            h2 = next((x for x in k.kids if not isinstance(x, str) and x.tag == "h2"), None)
            title = collapse(h2.text()).strip() if h2 is not None else ""
            out.append(f"## {no} {title}".strip())
        elif t == "h3":
            title, pills = split_pills(k)
            suffix = "".join(f" — **{p}**" for p in pills)
            out.append(f"### {title}{suffix}")
        elif t == "div" and "eyebrow" in c:
            out.append(f"**{collapse(k.text()).strip()}**")
        elif t == "p":
            txt = inline(k).strip()
            if txt:
                out.append(txt)
        elif t == "pre":
            out.append(code_block(k))
        elif t == "table":
            out.append(table(k))
        elif t == "ol" and "spine" in c:
            n = 0
            for li in k.kids:
                if isinstance(li, str) or li.tag != "li":
                    continue
                n += 1
                step = next((collapse(x.text()).strip() for x in li.kids
                             if not isinstance(x, str) and "step" in x.cls()), str(n))
                h3 = next(iter_tag(li, "h3"), None)
                title, pills = split_pills(h3) if h3 is not None else ("", [])
                suffix = "".join(f" — **{p}**" for p in pills)
                out.append(f"{int(step)}. **{title}**{suffix}")
                sub = []
                blocks(li, sub, depth + 1)
                for s in sub[1:]:  # sub[0] is the heading, already emitted
                    out.append("\n".join("   " + ln if ln else ""
                                         for ln in s.split("\n")))
        elif t in ("ul", "ol"):
            items = []
            for li in k.kids:
                if isinstance(li, str) or li.tag != "li":
                    continue
                items.append("- " + inline(li).strip())
            if items:
                out.append("\n".join(items))
        elif t == "div" and ("panel" in c or "callout" in c):
            sub = []
            blocks(k, sub, depth + 1)
            quoted = []
            for s in sub:
                quoted.append("\n".join("> " + ln if ln else ">"
                                        for ln in s.split("\n")))
            out.append("\n>\n".join(quoted))
        elif t == "footer":
            sub = [collapse(x.text()).strip() for x in k.kids
                   if not isinstance(x, str) and x.tag == "span"]
            out.append("---\n\n" + " · ".join(s for s in sub if s))
        else:
            blocks(k, out, depth)


BANNER = """\
<!--
  GENERATED FILE — do not edit directly. Edit {src}, then run:

      python3 {tool} \\
          {src} {dst}

  {src} is the source of truth and is also published as a
  styled page. This Markdown mirror exists so the document is reviewable in
  pull requests and diffable alongside the code it describes.
-->
"""


def main(src, dst, preamble=None):
    raw = open(src, encoding="utf-8").read()
    tree = Tree()
    tree.feed(raw)
    out = []
    blocks(tree.root, out)
    body = "\n\n".join(b for b in out if b.strip())
    body = re.sub(r"\n{4,}", "\n\n\n", body)
    # The banner is emitted here rather than added by hand, so it survives
    # every regeneration instead of being silently dropped by the next one.
    banner = BANNER.format(src=src, dst=dst,
                           tool="docs/tools/artifact_to_markdown.py")
    head = ""
    if preamble:
        head = open(preamble, encoding="utf-8").read().rstrip() + "\n\n---\n\n"
        banner = banner.replace(
            "-->",
            f"  The landing section above the first rule comes from {preamble};\n"
            "  everything below it is converted from the HTML.\n-->",
        )
    open(dst, "w", encoding="utf-8").write(
        banner + "\n" + head + body.rstrip() + "\n")
    print(f"wrote {dst} ({len((head + body).splitlines())} lines)")


if __name__ == "__main__":
    args = sys.argv[1:]
    pre = None
    if "--preamble" in args:
        i = args.index("--preamble")
        pre = args[i + 1]
        del args[i:i + 2]
    main(args[0], args[1], pre)
