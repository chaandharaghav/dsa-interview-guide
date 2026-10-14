# Review Brief for KB Pages

Opus gives this brief to each reviewer agent. The reviewer checks one or more drafted pages and corrects them in place.

## 1. Your Role

You are the quality gate for a software engineering knowledge base (KB). A Sonnet drafter wrote the pages that you review. The reader is a Java developer who prepares for product-company interviews and relies on the KB as the only resource. A wrong fact or a wrong model answer harms the reader in an interview. Your job is to find and correct every error.

Repo root: `/Users/raghav-17110/Documents/personal/dummyJava/dsa-interview-guide`.

Read these first:

1. `docs/superpowers/drafting-brief.md`: the rules the drafter had to follow (writing rules, templates, markers, verification commands).
2. The assignment for the page in the phase plan that the prompt names. It lists the items each page must cover.
3. The drafter's review note, if the prompt includes it. Its "claims I could not verify" list is where you start.

## 2. Checklist (spec section 8.5)

Check each item for each page. Do not skim. Read the whole page.

1. **Template:** all template sections are present and in order, or each omission has a reason in an `omit` marker.
2. **Model answers:** read every `??? question` block. Each answer must be correct, complete, and at the level a senior interviewer accepts. Correct wrong answers. Complete thin answers.
3. **Facts:** check each number, each API name and behavior, each version statement, each quote, and each "always" or "never" claim. Use the web (official docs, JLS, javadoc, JEPs, RFCs, original papers) for anything you are not certain of. Correct or remove what is wrong. Remove a claim that you cannot verify, or label it as an assumption.
4. **Code:** `python3 tools/run_java.py <page>` passes. Also read the code. It must show what the text says it shows. It must follow good Java practice, because the reader copies it.
5. **Writing rules:** brief section 2. Correct violations: passive voice where the actor matters, vague qualifiers, idioms, undefined terms, synonym drift.
6. **Overlap:** brief section 8. The page does not repeat a topic whose home is another page. It links there.
7. **Terms:** they match `kb/glossary.md` where the glossary has them.
8. **Checks:** `python3 tools/check_format.py <page>` and `python3 tools/run_java.py <page>` pass after your changes.
9. **Depth:** the page meets the question minimum for its priority (P1: 8, P2: 4, P3: 2) and has the depth that the reader needs to answer follow-up questions.
10. **Coverage:** the page covers every item that its assignment lists. Add any missing item.

Also check:

- Mermaid diagrams have valid syntax for their diagram type (`classDiagram`, `sequenceDiagram`, `stateDiagram-v2`, `flowchart`).
- Links resolve. Pages that do not exist yet appear as backticked `kb/...` paths, not as links.
- Further Reading entries are real sources, and each URL is plausible and current. Open the URL if you are not certain.

## 3. How to Change the Page

- Edit the page directly. Keep the drafter's structure unless it is wrong.
- Make each correction complete. Do not leave a note such as "verify this".
- Do not cut correct content to make the page shorter.
- Do not commit. Do not run `git` commands that change the repo.

## 4. Report

End with a report that lists:

1. each page reviewed, and the result of `check_format.py` and `run_java.py` after your changes;
2. each factual correction: what the page said, what it says now, and the source;
3. each model answer that you rewrote or completed, and why;
4. other changes (writing, coverage, links, diagrams), summarized;
5. any claim that you kept but could not verify, with the reason you kept it.
