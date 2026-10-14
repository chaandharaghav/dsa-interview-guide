import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import run_java as rj  # noqa: E402
from mdblocks import Block  # noqa: E402


class CandidatesTest(unittest.TestCase):
    def test_public_class_uses_its_name_first(self):
        names = [name for name, _ in rj.candidates("public class Foo {}")]
        self.assertEqual(names[0], "Foo.java")

    def test_imports_stay_on_top_when_wrapped(self):
        body = "import java.math.BigInteger;\nBigInteger x = BigInteger.ONE;"
        wrapped = [src for _, src in rj.candidates(body)[1:]]
        self.assertEqual(len(wrapped), 2)
        for source in wrapped:
            self.assertLess(source.index("import java.math.BigInteger;"), source.index("class Snippet"))


class ReleaseTest(unittest.TestCase):
    def test_release(self):
        self.assertEqual(rj.release_for(Path("kb/dsa/java-toolkit.md")), "17")
        self.assertEqual(rj.release_for(Path("kb/dsa/05-hashing-frequency/README.md")), "17")
        self.assertEqual(rj.release_for(Path("kb/java/10-virtual-threads.md")), "21")


@unittest.skipUnless(rj.JDK.joinpath("bin", "javac").exists(), "JDK 21 not installed")
class CompileTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.support = rj.build_support(rj.JDK / "bin" / "javac")

    def compile(self, body, path="kb/java/x.md", markers=None):
        block = Block(Path(path), 1, "java", body, markers or {})
        return rj.compile_block(block, rj.JDK / "bin" / "javac", self.support)

    def test_member_snippet_compiles(self):
        self.assertIsNone(self.compile("int twice(int x) { return 2 * x; }"))

    def test_statement_snippet_compiles(self):
        self.assertIsNone(self.compile("List<Integer> xs = new ArrayList<>();\nxs.add(1);"))

    def test_tree_node_is_available(self):
        self.assertIsNone(self.compile("int depth(TreeNode n) { return n == null ? 0 : 1; }"))

    def test_switch_pattern_needs_release_21(self):
        code = 'String f(Object o) { return switch (o) { case String s -> s; default -> ""; }; }'
        self.assertIsNone(self.compile(code))
        self.assertIsNotNone(self.compile(code, path="kb/dsa/cheatsheet.md"))

    def test_broken_code_reports_error(self):
        self.assertIn("error", self.compile("int x = ;"))


@unittest.skipUnless(rj.JDK.joinpath("bin", "java").exists(), "JDK 21 not installed")
class RunProgramTest(unittest.TestCase):
    def write_case(self, expected):
        folder = Path(tempfile.mkdtemp())
        (folder / "Main.java").write_text(
            'public class Main { public static void main(String[] a) '
            '{ System.out.println("hi"); } }')
        (folder / "README.md").write_text(
            f"## Expected Output\n\n<!-- expected-output -->\n```text\n{expected}\n```\n")
        return folder / "Main.java"

    def test_matching_output(self):
        self.assertIsNone(rj.run_program(self.write_case("hi"), rj.JDK / "bin" / "java"))

    def test_different_output(self):
        self.assertIn("differs", rj.run_program(self.write_case("bye"), rj.JDK / "bin" / "java"))


class StaticImportTest(unittest.TestCase):
    def test_static_import_is_hoisted(self):
        body = "import static java.util.Map.entry;\nvar e = entry(1, 2);"
        for _, source in rj.candidates(body)[1:]:
            self.assertLess(source.index("import static"), source.index("class Snippet"))


@unittest.skipUnless(rj.JDK.joinpath("bin", "javac").exists(), "JDK 21 not installed")
class CaseStudyTest(unittest.TestCase):
    MAIN = (
        "public class Main {\n"
        "    public static void main(String[] a) { System.out.println(new Spot().id()); }\n"
        "}\n"
        "\n"
        "class Vehicle {}\n"
        "\n"
        "class Spot {\n"
        "    Vehicle parked;\n"
        "\n"
        "    int id() { return 7; }\n"
        "}\n")

    def make_case(self, readme_code, expected="7"):
        folder = Path(tempfile.mkdtemp()) / "kb" / "lld" / "case-studies" / "01-x"
        folder.mkdir(parents=True)
        (folder / "Main.java").write_text(self.MAIN)
        fence = "`" * 3
        (folder / "README.md").write_text(
            f"## Key Code\n\n{fence}java\n{readme_code}\n{fence}\n\n"
            f"## Expected Output\n\n<!-- expected-output -->\n{fence}text\n{expected}\n{fence}\n")
        return folder

    def test_key_code_uses_main_classes(self):
        folder = self.make_case("class Spot {\n    Vehicle parked;")
        self.assertEqual(rj.main([str(folder / "README.md")]), 0)

    def test_key_code_must_match_main(self):
        folder = self.make_case("class Spot {\n    Vehicle other;")
        self.assertEqual(rj.main([str(folder / "README.md")]), 1)

    def test_file_argument_runs_main_program(self):
        folder = self.make_case("class Vehicle {}", expected="8")
        self.assertEqual(rj.main([str(folder / "README.md")]), 1)


@unittest.skipUnless(rj.JDK.joinpath("bin", "javac").exists(), "JDK 21 not installed")
class ErrorReportTest(unittest.TestCase):
    def test_statement_error_is_reported_from_wrapped_form(self):
        support = rj.build_support(rj.JDK / "bin" / "javac")
        block = Block(Path("kb/java/x.md"), 1, "java", "int x = undefinedName;", {})
        error = rj.compile_block(block, rj.JDK / "bin" / "javac", support)
        self.assertIn("undefinedName", error)


@unittest.skipUnless(rj.JDK.joinpath("bin", "java").exists(), "JDK 21 not installed")
class RunnableBlockTest(unittest.TestCase):
    FENCE = "`" * 3

    def page(self, code, expected=None):
        folder = Path(tempfile.mkdtemp()) / "kb" / "lld" / "fundamentals"
        folder.mkdir(parents=True)
        text = f"{self.FENCE}java\n{code}\n{self.FENCE}\n"
        if expected is not None:
            text += f"\n<!-- expected-output -->\n{self.FENCE}text\n{expected}\n{self.FENCE}\n"
        path = folder / "01-x.md"
        path.write_text(text)
        return path

    MAIN_FIRST = ("class Demo {\n    public static void main(String[] a) "
                  "{ System.out.println(new Helper().v()); }\n}\n"
                  "class Helper { int v() { return 3; } }")
    MAIN_LAST = ("class Helper { int v() { return 3; } }\n"
                 "class Demo {\n    public static void main(String[] a) "
                 "{ System.out.println(new Helper().v()); }\n}")

    def test_main_first_with_matching_output_passes(self):
        self.assertEqual(rj.main([str(self.page(self.MAIN_FIRST, "3"))]), 0)

    def test_wrong_expected_output_fails(self):
        self.assertEqual(rj.main([str(self.page(self.MAIN_FIRST, "4"))]), 1)

    def test_main_class_not_first_fails(self):
        self.assertEqual(rj.main([str(self.page(self.MAIN_LAST))]), 1)

    def test_runtime_exception_fails(self):
        code = ("class Demo { public static void main(String[] a) "
                "{ throw new IllegalStateException(); } }")
        self.assertEqual(rj.main([str(self.page(code))]), 1)


if __name__ == "__main__":
    unittest.main()
