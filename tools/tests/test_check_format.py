import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import check_format as cf  # noqa: E402

TOPIC_SECTIONS = [
    "Why It Matters", "Core Concepts", "How It Works", "Trade-offs and When to Use",
    "Java Example", "Interview Angle", "Common Mistakes", "Commonly Confused With",
    "Quick Reference Card", "Further Reading",
]


def topic_page(priority="P2", questions=4, skip=None, extra=""):
    parts = [f"---\npriority: {priority}\n---\n# T\n"]
    for number, name in enumerate(TOPIC_SECTIONS, 1):
        if name != skip:
            parts.append(f"## {number}. {name}\n\ntext\n")
    parts.append("".join(f'??? question "Q{i}"\n    a\n' for i in range(questions)))
    parts.append(extra)
    return "\n".join(parts)


class TemplateForTest(unittest.TestCase):
    def test_paths(self):
        self.assertEqual(cf.template_for(Path("kb/java/01-language-core.md")), "topic")
        self.assertEqual(cf.template_for(Path("kb/hld/fundamentals/03-caching.md")), "topic")
        self.assertEqual(
            cf.template_for(Path("kb/lld/case-studies/01-parking-lot/README.md")), "lld-case")
        self.assertEqual(
            cf.template_for(Path("kb/hld/case-studies/01-url-shortener.md")), "hld-case")
        self.assertEqual(
            cf.template_for(Path("kb/dsa/21-segment-tree-and-fenwick-tree/README.md")),
            "dsa-pattern")
        self.assertIsNone(cf.template_for(Path("kb/dsa/01-sliding-window/README.md")))
        self.assertIsNone(cf.template_for(Path("kb/java/README.md")))
        self.assertIsNone(cf.template_for(Path("kb/java/interview-question-bank.md")))

    def test_legacy(self):
        self.assertTrue(cf.is_legacy(Path("kb/dsa/cheatsheet.md")))
        self.assertTrue(cf.is_legacy(Path("kb/dsa/20-intervals/README.md")))
        self.assertFalse(cf.is_legacy(Path("kb/dsa/foundations/01-complexity-analysis.md")))
        self.assertFalse(cf.is_legacy(Path("kb/dsa/21-segment-tree-and-fenwick-tree/README.md")))


class CheckFileTest(unittest.TestCase):
    path = Path("kb/java/01-language-core.md")

    def test_complete_page_passes(self):
        self.assertEqual(cf.check_file(self.path, topic_page()), [])

    def test_missing_section_fails(self):
        errors = cf.check_file(self.path, topic_page(skip="Java Example"))
        self.assertEqual(len(errors), 1)
        self.assertIn("Java Example", errors[0])

    def test_omit_marker_allows_missing_section(self):
        text = topic_page(skip="Java Example", extra="<!-- omit: Java Example -- no code -->\n")
        self.assertEqual(cf.check_file(self.path, text), [])

    def test_omit_marker_needs_reason(self):
        text = topic_page(skip="Java Example", extra="<!-- omit: Java Example -->\n")
        self.assertTrue(any("reason" in e for e in cf.check_file(self.path, text)))

    def test_too_few_questions_for_priority(self):
        errors = cf.check_file(self.path, topic_page(priority="P1", questions=7))
        self.assertTrue(any("8" in e for e in errors))

    def test_missing_priority(self):
        text = topic_page().replace("priority: P2", "owner: me")
        self.assertTrue(any("priority" in e for e in cf.check_file(self.path, text)))

    def test_long_code_line_fails(self):
        text = topic_page(extra="```java\n" + "x" * 101 + "\n```\n")
        self.assertTrue(any("101" in e for e in cf.check_file(self.path, text)))

    def test_legacy_file_skips_width(self):
        text = "```\n" + "x" * 150 + "\n```\n"
        self.assertEqual(cf.check_file(Path("kb/dsa/cheatsheet.md"), text), [])

    def test_heading_prefix_match(self):
        names = [
            "Pattern Name", "Core Intuition", "Recognition Signals",
            "Brute Force Thought Process", 'Optimization Insight (The "Aha" Moment)',
            "Generalized Templates", "Variations of the Pattern", "Classic Interview Problems",
            "How to Practice This Pattern Effectively",
            "Common Mistakes Even Experienced Developers Make",
            "Commonly Confused With", "Quick Reference Card",
        ]
        text = "---\npriority: P2\n---\n" + "\n".join(f"## {n}\n" for n in names)
        path = Path("kb/dsa/22-string-algorithms/README.md")
        self.assertEqual(cf.check_file(path, text), [])


if __name__ == "__main__":
    unittest.main()
