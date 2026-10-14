import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from mdblocks import front_matter, h2_headings, parse_blocks  # noqa: E402

FENCE = "`" * 3
DOC = f"""---
priority: P1
---
# Title

## 1. Why It Matters

<!-- fragment -->

{FENCE}java
int x = 1;
{FENCE}

??? question "Q1"

    {FENCE}java
    class A {{}}
    {FENCE}

{FENCE}`markdown
{FENCE}java
not a block
{FENCE}
{FENCE}`

## 5. Optimization Insight (The "Aha" Moment)
"""


class ParseBlocksTest(unittest.TestCase):
    def test_finds_top_level_block_with_marker(self):
        blocks = parse_blocks(DOC)
        self.assertEqual(blocks[0].lang, "java")
        self.assertEqual(blocks[0].body, "int x = 1;")
        self.assertIn("fragment", blocks[0].markers)

    def test_strips_indent_inside_collapsible(self):
        blocks = parse_blocks(DOC)
        self.assertEqual(blocks[1].body, "class A {}")
        self.assertEqual(blocks[1].markers, {})

    def test_longer_fence_contains_inner_fence(self):
        blocks = parse_blocks(DOC)
        self.assertEqual(len(blocks), 3)
        self.assertEqual(blocks[2].lang, "markdown")
        self.assertIn("not a block", blocks[2].body)

    def test_line_numbers_are_one_based(self):
        self.assertEqual(parse_blocks(DOC)[0].line, 10)


class FrontMatterTest(unittest.TestCase):
    def test_reads_keys(self):
        self.assertEqual(front_matter(DOC), {"priority": "P1"})

    def test_no_front_matter(self):
        self.assertEqual(front_matter("# Title\n"), {})


class HeadingsTest(unittest.TestCase):
    def test_strips_number_prefix_and_ignores_code(self):
        self.assertEqual(
            h2_headings(DOC),
            ["Why It Matters", 'Optimization Insight (The "Aha" Moment)'],
        )


if __name__ == "__main__":
    unittest.main()
