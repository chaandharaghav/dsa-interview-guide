#!/usr/bin/env python3
"""Print the text of a PDF file or URL, so that reviewers can quote papers.

Usage: .venv/bin/python tools/pdf2text.py <file.pdf | https://...pdf> [first_page [last_page]]
Needs `pypdf` in .venv (`.venv/bin/pip install pypdf`). A URL is downloaded with urllib.
"""
import io
import sys
import urllib.request

from pypdf import PdfReader


def main(argv):
    source = argv[0]
    if source.startswith("http"):
        request = urllib.request.Request(source, headers={"User-Agent": "Mozilla/5.0"})
        data = io.BytesIO(urllib.request.urlopen(request, timeout=60).read())
    else:
        data = open(source, "rb")
    reader = PdfReader(data)
    first = int(argv[1]) if len(argv) > 1 else 1
    last = int(argv[2]) if len(argv) > 2 else len(reader.pages)
    for number in range(first, last + 1):
        print(f"--- page {number} ---")
        print(reader.pages[number - 1].extract_text() or "")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
