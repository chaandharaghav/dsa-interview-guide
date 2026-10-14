import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import run_sql as rs  # noqa: E402

SETUP = "```sql\nCREATE TABLE t(id INTEGER, name TEXT);\nINSERT INTO t VALUES (1,'a'),(2,NULL);\n```\n"


class RunSqlTest(unittest.TestCase):
    def test_matching_result(self):
        text = SETUP + (
            "```sql\nSELECT id, name FROM t ORDER BY id;\n```\n"
            "<!-- sql-result -->\n```text\nid | name\n1 | a\n2 | NULL\n```\n")
        self.assertEqual(rs.check_text(text, Path("x.md")), [])

    def test_wrong_result(self):
        text = SETUP + (
            "```sql\nSELECT id FROM t ORDER BY id;\n```\n"
            "<!-- sql-result -->\n```text\nid\n9\n```\n")
        self.assertEqual(len(rs.check_text(text, Path("x.md"))), 1)

    def test_syntax_error_without_result_block(self):
        text = SETUP + "```sql\nSELEC id FROM t;\n```\n"
        errors = rs.check_text(text, Path("x.md"))
        self.assertEqual(len(errors), 1)
        self.assertIn("syntax error", errors[0])

    def test_skip_marker(self):
        text = "<!-- sql-skip: postgres -->\n```sql\nSELECT now()::date;\n```\n"
        self.assertEqual(rs.check_text(text, Path("x.md")), [])


    def test_two_statements_on_one_line(self):
        text = "```sql\nCREATE TABLE a(id INT); CREATE TABLE b(id INT);\n```\n"
        self.assertEqual(rs.check_text(text, Path("x.md")), [])

    def test_right_join_is_supported(self):
        text = SETUP + (
            "```sql\nSELECT b.id FROM t AS a RIGHT JOIN t AS b ON a.id = b.id ORDER BY b.id;\n```\n"
            "<!-- sql-result -->\n```text\nid\n1\n2\n```\n")
        self.assertEqual(rs.check_text(text, Path("x.md")), [])

    def test_tilde_fence_page_is_checked(self):
        text = "~~~sql\nSELEC 1;\n~~~\n"
        self.assertEqual(len(rs.check_text(text, Path("x.md"))), 1)


if __name__ == "__main__":
    unittest.main()
