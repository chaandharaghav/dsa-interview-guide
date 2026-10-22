---
priority: P1
---
# Modern Java (9 to 21)

## Why It Matters

Interviewers at product companies expect a candidate to write Java 17 or Java 21 code, not Java 8 code. They ask what `var` does, how a `record` differs from a class with Lombok, why a `switch` expression needs no `default`, and what a sealed interface is for. On the job, these features remove boilerplate (records), make a model of a domain checkable by the compiler (sealed types with pattern matching), and change how you write the code that reads data (text blocks, `HttpClient`, sequenced collections). This page covers each feature from Java 9 to Java 21 with its JEP, the rule that interviews test, and a pitfall. It also lists what Java 22 to 27 finalized or changed, checked on openjdk.org on 2026-10-22. The page targets Java 21, the version that the KB programs run on.

## Core Concepts

**JEP** (JDK Enhancement Proposal): the document that describes a change to the JDK. Each feature below has a JEP number. Read the JEP for the motivation and for the rules that the language specification states in detail.

**Release cadence**: since Java 10 (March 2018), a new feature release comes every six months, in March and September. Some releases are **long-term support** (LTS) releases. LTS is a vendor term: Oracle and other vendors support these releases for several years. The Oracle Java SE Support Roadmap lists Java SE 8, 11, 17, 21, and 25 as LTS releases, and says that Oracle intends to make an LTS release every two years, so the next planned LTS release is Java 29 (September 2027). The pages of this KB target Java 21.

**Preview feature**: a new language, JVM, or Java SE API feature that is fully specified and fully implemented, but not permanent (JEP 12). Code that uses a preview feature compiles only with `--enable-preview` (and `--release` set to the current version), and the class file records this. A preview feature may change or disappear in a later release. Example: string templates were a preview feature in Java 21 (JEP 430) and a second preview in Java 22 (JEP 459). The third preview, JEP 465, has the status "Closed / Withdrawn" on openjdk.org, and JDK 23 removed the feature (JDK-8329948), so no current JDK has string templates. **Incubator module**: a non-final API in a module whose name starts with `jdk.incubator` (JEP 11), for example the Vector API, which is still an incubator module in JDK 27 (JEP 537).

**Algebraic data type**: a type that you build from two operations. A **product type** groups fixed fields (a record). A **sum type** is one of a fixed set of alternatives (a sealed interface with its permitted subtypes).

### What each release added, Java 9 to 21

| Feature | JEP | Java version (final) | One-line rule |
|---|---|---|---|
| Module system | 261 | 9 | A `module-info.java` declares what a module requires and exports. Packages that are not exported are not accessible |
| Collection factory methods (`List.of`, `Map.of`) | 269 | 9 | The collections are immutable and reject `null` |
| `var` for local variables | 286 | 10 | The compiler infers the type from the initializer. The type is still static |
| `var` in lambda parameters | 323 | 11 | Allows annotations on lambda parameters |
| `HttpClient` | 321 | 11 | A standard HTTP/1.1 and HTTP/2 client with synchronous and asynchronous calls. It was an incubator module in Java 9 (JEP 110) |
| Single-file source launch | 330 | 11 | `java Hello.java` compiles in memory and runs |
| Switch expressions | 361 | 14 | `switch` returns a value. Arrow cases do not fall through |
| Helpful `NullPointerException` messages | 358 | 14 (off by default), on by default since 15 | The message names the null expression |
| Text blocks | 378 | 15 | A multi-line string literal that starts with `"""` |
| Records | 395 | 16 | A transparent, shallowly immutable data carrier |
| Pattern matching for `instanceof` | 394 | 16 | `if (o instanceof String s)` tests and binds in one step |
| Strong encapsulation of JDK internals | 396 (16), 403 (17) | 16, 17 | Reflection into `java.*` internals fails unless you pass `--add-opens` |
| Sealed classes and interfaces | 409 | 17 | A type lists the subtypes that can extend it |
| UTF-8 by default | 400 | 18 | The default charset is UTF-8 on all platforms |
| Simple Web Server | 408 | 18 | `jwebserver` serves static files |
| Sequenced collections | 431 | 21 | Collections with a defined first and last element get `getFirst`, `getLast`, `reversed()` |
| Record patterns | 440 | 21 | A pattern can take a record apart: `case Point(int x, int y)` |
| Pattern matching for `switch` | 441 | 21 | `case` labels can be type patterns with guards, and `null` |
| Generational ZGC | 439 | 21 | ZGC keeps young and old objects apart. See [memory and GC](06-memory-and-gc.md) |
| Virtual threads | 444 | 21 | See [virtual threads](10-virtual-threads.md) |

Sources: the JEP pages on openjdk.org and the JDK release pages (`openjdk.org/projects/jdk/NN`). The API additions that have no JEP are in the section "Smaller changes" below.

Java 21 also contained preview features: string templates (JEP 430), unnamed patterns and variables (JEP 443), unnamed classes and instance `main` methods (JEP 445), scoped values (JEP 446), structured concurrency (JEP 453), and the foreign function and memory API (JEP 442, third preview). Section "What Java 22 to 27 changed" gives the status of each.

## How It Works

### Modules in brief (JEP 261)

Before Java 9, a program had one flat **classpath**, and any public class was reachable by any other. The module system (Project Jigsaw) adds the **module**: a named set of packages with a descriptor. The descriptor is the file `module-info.java` at the root of the source tree.

<!-- fragment -->
```java
module com.example.orders {
    requires java.sql;                          // needs this module at compile and run time
    requires transitive com.example.model;     // readers of this module also read it
    exports com.example.orders.api;             // other modules can use this package
    exports com.example.orders.spi to com.example.plugins; // qualified export
    opens com.example.orders.entity;            // allows deep reflection at run time (JPA, Jackson)
    uses com.example.orders.spi.PaymentProvider;           // a service that this module loads
    provides com.example.orders.spi.PaymentProvider
        with com.example.orders.internal.CardPayment;      // a service implementation
}
```

The facts that interviews test:

- A package that the module does not `export` is not accessible from other modules, even if its classes are `public`. This is **strong encapsulation**. Since Java 16 (JEP 396) and Java 17 (JEP 403), the JDK applies it to its own internal packages: reflection on `java.lang` internals throws `InaccessibleObjectException` unless you pass `--add-opens`.
- `exports` allows compile-time and run-time access to public types. `opens` allows run-time reflective access to all members, including private ones. A frameworks-heavy application (Spring, Hibernate, Jackson) needs `opens` for the packages that the framework reflects on.
- A JAR on the **classpath** belongs to the **unnamed module**, which reads every module and exports every package. Most applications still run on the classpath. A JAR on the **module path** without a descriptor becomes an **automatic module**.
- The JDK itself is modular. `java --list-modules` lists the modules, `jdeps` lists the dependencies of a JAR, and `jlink` builds a smaller runtime image with only the modules that the program needs.

The module system is an interview topic mostly for these facts. A backend application seldom writes a `module-info.java`.

### `var` (JEP 286)

`var` declares a local variable whose type the compiler infers from the initializer. The variable has a static type, exactly as if you had written it. `var` is a **reserved type name**, not a keyword: `var` is still a legal name for a variable or a method.

The rules (JLS 14.4):

- It is allowed for local variables with an initializer, the variable of a basic `for` and an enhanced `for` statement, and the resources of `try-with-resources`. Java 11 (JEP 323) also allows it for lambda parameters.
- It is **not** allowed for fields, method parameters, return types, or catch parameters.
- The initializer must give the compiler enough information. `var x;`, `var n = null;`, `var a = {1, 2};`, and `var f = () -> 1;` fail.
- If the initializer has a non-denotable type (an anonymous class type, or an intersection type), `var` keeps it. For an anonymous class, `var o = new Object() { int count; };` lets you read `o.count`, which no named type can do.

<!-- fragment -->
```java
var list = new ArrayList<>();          // ArrayList<Object>: no type arguments to infer from
var names = new ArrayList<String>();   // ArrayList<String>
var id = 1L;                           // long, not int
var total = 0;                         // int: the assignment total = 0.5 does not compile
```

Recommendation: use `var` when the type is clear from the right-hand side (`new` expression, a factory method with an obvious name, a cast). Write the type when the right-hand side hides it (`var result = service.process(input);`). The official style guidance is the "Local Variable Type Inference: Style Guidelines" document, linked below.

### Switch expressions (JEP 361)

A `switch` expression yields a value. A `switch` statement does not. The differences from the old `switch`:

- An **arrow case** (`case A -> expr;`) runs only its own branch. It has no fall-through, so it needs no `break`.
- A case can list several constants: `case MON, FRI, SUN -> 6`.
- A branch that needs several statements uses a block, and the block ends with `yield value;`.
- A switch expression must be **exhaustive**: it needs a branch for every possible value. For an enum, a case for each constant is enough. For other types, `default` is required, unless the selector is a sealed type that the cases cover, or a pattern covers the whole type (`case Object o`).
- What happens at run time if no label applies. This is possible only if a class changed after the switch was compiled, for example an enum that got a new constant. JLS 21 sections 15.28.2 and 14.11.3 set the rules:

    | Switch form | Exhaustive at compile time? | No label applies at run time (selector not `null`) |
    |---|---|---|
    | Switch expression, compiled for Java 21 | Required | Throws `MatchException` |
    | Switch expression, compiled for Java 14 to 20 | Required | Throws `IncompatibleClassChangeError` (JLS 17, 15.28.2) |
    | **Enhanced switch statement** (Java 21): the selector type is not `char`, `byte`, `short`, `int`, their boxes, `String`, or an enum, or a label uses a pattern or `null` | Required | Throws `MatchException` |
    | Other switch statement, for example over an enum with no pattern | Not required | Runs no branch and completes normally |

    JEP 441 lists the change from `IncompatibleClassChangeError` to `MatchException` for enum switch expressions as a minor incompatible change. The `javac` release that compiled the switch decides the error: in a test on JDK 21, the same switch compiled with `--release 17` threw `IncompatibleClassChangeError`, and compiled with `--release 21` threw `MatchException`. In every form, a `null` selector without `case null` throws `NullPointerException`.
- The old colon form (`case A:`) still exists, still falls through, and can also appear in a switch expression with `yield`. Mixing the two forms in one `switch` is a compile error (JLS 14.11.1).

Pattern matching for `instanceof` (JEP 394) and the switch expressions appear together in the program below, with `var` and a text block.

```java
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SyntaxTour {
    enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }

    static int fullNameLength(Day d) {
        // Switch expression: arrows do not fall through, and the compiler checks that
        // every enum constant has a case (no default needed).
        return switch (d) {
            case MON, FRI, SUN -> 6; // MONDAY, FRIDAY, SUNDAY
            case TUE -> 7;
            case THU, SAT -> 8;
            case WED -> {
                int n = "WEDNESDAY".length(); // a block must end with yield
                yield n;
            }
        };
    }

    static String describe(Object o) {
        // Pattern matching for instanceof: s is in scope where the test is true.
        if (o instanceof String s && !s.isEmpty()) {
            return "non-empty string of length " + s.length();
        }
        if (!(o instanceof Integer i)) {
            return "something else";
        }
        return "integer " + (i + 1); // i is in scope here: the code above returns if the test fails
    }

    public static void main(String[] args) {
        // var: the compiler infers ArrayList<String> from the initializer.
        var names = new ArrayList<String>(List.of("ada", "linus"));
        var counts = Map.of("a", 1);
        for (var name : names) System.out.println(name.toUpperCase());
        System.out.println(counts.get("a") + 1);

        // A text block: the closing delimiter sets the indentation that is removed.
        String json = """
                {
                  "name": "ada",
                  "tags": ["x", "y"]
                }
                """;
        System.out.print(json);
        String oneLine = """
                a long line \
                continues here""";
        System.out.println(oneLine);
        String padded = """
                trailing\s\s
                """;
        System.out.println("[" + padded.stripTrailing() + "] vs ["
                + padded.replace("\n", "") + "]");

        System.out.println(fullNameLength(Day.WED) + " " + fullNameLength(Day.TUE));
        System.out.println(describe("hello"));
        System.out.println(describe(41));
        System.out.println(describe(2.5));
    }
}
```

<!-- expected-output -->
```text
ADA
LINUS
2
{
  "name": "ada",
  "tags": ["x", "y"]
}
a long line continues here
[trailing] vs [trailing  ]
9 7
non-empty string of length 5
integer 42
something else
```

In `describe`, the **binding variable** `s` is in scope only where the compiler can prove that the test succeeded: inside the `if` body, and after the `&&`. In the second `if`, `!(o instanceof Integer i)` returns early, so `i` is in scope for the rest of the method. This is called **flow scoping**.

### Text blocks (JEP 378)

A **text block** is a string literal that starts with `"""` and a line break, and ends with `"""`. JLS 3.10.6 sets the rules:

- **Incidental white space** is removed. The compiler finds the smallest indentation among the non-blank lines **and the line with the closing delimiter**, and removes that many leading spaces from every line. Moving the closing `"""` to the left keeps more indentation in the string.
- **Trailing spaces** on each line are removed. The escape `\s` produces a space that stays, and `\s\s` at the end of a line keeps two spaces.
- The escape `\<line break>` joins the line with the next line, with no newline in the string.
- Line breaks inside are normalized to `\n`. A `"` needs no escape, and `"""` inside the block needs `\"""`.
- The result is an ordinary `String`. A text block has the same type as any literal, and the string pool treats it as a constant (see [language core](01-language-core.md)).

The program above shows an indented JSON block, a joined line, and the difference between `stripTrailing()` and the `\s` escape.

### Records (JEP 395)

A **record** declares a class whose purpose is to carry data. `record Range(int low, int high) { }` makes the compiler generate:

- a `private final` field for each **component**,
- a **canonical constructor** that assigns all fields,
- an **accessor** method for each component, named like the component (`low()`, not `getLow()`),
- `equals`, `hashCode`, and `toString` based on all components. `equals` compares the components, and the `Object` contract holds. The javadoc of `java.lang.Record` states that `equals` and `hashCode` derive from all components, and it does not give the exact `hashCode` formula, so do not depend on a number. `toString` has the form `Range[low=2, high=5]`, but the javadoc leaves the exact format unspecified as well (see [language core](01-language-core.md) for the `equals` and `hashCode` contract).

The rules (JLS 8.10):

- A record is implicitly `final`. It cannot extend a class (it extends `java.lang.Record`), but it can implement interfaces, have static fields and methods, and have instance methods.
- It cannot declare instance fields beyond its components.
- A **compact canonical constructor** (`Range { ... }`, no parameter list) runs the validation code and then the compiler assigns the fields from the parameters. In the compact form, you may reassign the parameters (for a defensive copy) and you may not assign the fields.
- Any other constructor must start with `this(...)`.
- A record can be generic and can be nested. A nested record is implicitly `static`.

**Shallow immutability.** The fields are `final`, but a component that is a mutable object stays mutable. The `Team` record below shows the fix: copy the list in the compact constructor.

```java
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecordDemo {
    record Range(int low, int high) {
        // Compact canonical constructor: validates, then the compiler assigns the fields.
        Range {
            if (low > high) throw new IllegalArgumentException(low + " > " + high);
        }

        // Extra constructors must delegate to the canonical one.
        Range(int single) { this(single, single); }

        static Range of(int a, int b) { return new Range(Math.min(a, b), Math.max(a, b)); }

        int length() { return high - low; }
    }

    // A record is shallowly immutable: the list reference is final, the list is not.
    record Team(String name, List<String> members) {
        Team {
            members = List.copyOf(members); // defensive copy makes it deeply immutable
        }
    }

    public static void main(String[] args) {
        Range r = new Range(2, 5);
        System.out.println(r);                       // generated toString
        System.out.println(r.low() + " " + r.high()); // accessors have no "get" prefix
        System.out.println(r.equals(new Range(2, 5)));
        Set<Range> set = new HashSet<>(List.of(r));
        System.out.println(set.contains(new Range(2, 5)));
        System.out.println(new Range(7) + " length " + Range.of(9, 3).length());
        try {
            new Range(5, 2);
        } catch (IllegalArgumentException e) {
            System.out.println("rejected: " + e.getMessage());
        }

        var source = new java.util.ArrayList<>(List.of("ada"));
        Team team = new Team("core", source);
        source.add("linus");
        System.out.println(team);
    }
}
```

<!-- expected-output -->
```text
Range[low=2, high=5]
2 5
true
true
Range[low=7, high=7] length 6
rejected: 5 > 2
Team[name=core, members=[ada]]
```

Records suit DTOs, map keys, tuples, and value objects. They do not suit JPA entities (an entity needs a no-argument constructor and mutable state for proxies, see [JDBC, JPA, and Hibernate](16-jdbc-jpa-and-hibernate.md)) or classes that need a mutable state or an inheritance hierarchy. Compared with Lombok `@Value`: a record is part of the language, so every tool understands it, but you cannot add a field or customize the generated `toString` shape without writing the method yourself.

### Sealed classes and interfaces (JEP 409)

A **sealed** class or interface restricts which classes can extend or implement it. The `permits` clause lists them. JLS 8.1.6 and 9.1.4 set the rules:

- Each permitted subtype must **directly** extend the sealed type, and must be in the same module (or, in the unnamed module, the same package) as it.
- Each permitted subtype must be `final`, `sealed`, or `non-sealed`. `final` ends the hierarchy. `sealed` continues it with a new permits list. `non-sealed` opens that branch again to any subclass.
- A record is implicitly `final`, so a record fits as a permitted subtype. An enum is also allowed.
- If all subtypes are in the same source file as the sealed type, the `permits` clause can be omitted, and the compiler infers the list.

The purpose is not security. The purpose is a closed set of cases that the compiler knows, so that a `switch` over the type can be checked for completeness.

### Pattern matching for `switch` and record patterns (JEP 441, JEP 440)

A pattern `switch` lets `case` labels test the type of the selector, bind variables, and add a condition:

- A **type pattern** (`case Num n`) matches if the value is an instance of `Num`, and binds `n`.
- A **guard** (`case Num n when n.value() < 0`) adds a boolean condition.
- A **record pattern** (`case Add(Expr l, Expr r)`) matches an `Add` and binds its components. Record patterns can nest: `case Mul(Expr l, Num(int v))`. A nested pattern can use `var`: `case Add(var l, var r)`.
- `case null` handles `null`. If a pattern switch has no `case null`, a `null` selector throws `NullPointerException`. `case null, default ->` handles both.
- **Dominance**: a label that an earlier label already covers is a compile error (JLS 14.11.1). A general pattern before a specific one, for example `case Expr e` before `case Num n`, does not compile. A guarded label does not dominate the labels after it.
- **Exhaustiveness**: a switch that uses patterns must cover every possible value. If the selector is a sealed type, a case for each permitted subtype is enough, and the compiler verifies it. If you add a subtype to the `permits` list later, every such switch fails to compile, which is the point of the design. If a separately compiled class changes after compilation, the switch throws `MatchException` at run time.
- A record pattern never matches `null`, at the top level or nested. A nested **type pattern** that is unconditional for the component type (it matches every value of that type), or a nested `var` pattern, also matches a `null` component (JLS 14.30.1 defines these "null-matching" type patterns, and JLS 14.30.2 gives the matching rules). Example: `Pair(Object a, Object b)` matches `new Pair(null, null)`, and `Add(Expr l, Expr r)` matches `new Add(null, x)`. A nested record pattern does not: `Mul(Expr l, Num(int v))` does not match `new Mul(x, null)`.

### Algebraic data types: records, sealed types, and switch patterns together

A sealed interface (the sum), records as the cases (the products), and a pattern `switch` (the operations) give a closed, checkable model. The program below models arithmetic expressions. `eval` and `show` have no `default` branch: the compiler checks them against the permits list. `simplify` uses nested record patterns and guards.

```java
public class ExprDemo {
    // An algebraic data type: Expr is a sum of four cases, each case is a product of fields.
    sealed interface Expr permits Num, Add, Mul, Neg { }
    record Num(int value) implements Expr { }
    record Add(Expr left, Expr right) implements Expr { }
    record Mul(Expr left, Expr right) implements Expr { }
    record Neg(Expr operand) implements Expr { }

    // No default branch: the compiler checks that the switch covers every permitted type.
    // If you add a fifth record to the permits list, this method stops compiling.
    static int eval(Expr e) {
        return switch (e) {
            case Num n -> n.value();
            case Add(Expr l, Expr r) -> eval(l) + eval(r);
            case Mul(Expr l, Expr r) -> eval(l) * eval(r);
            case Neg(Expr inner) -> -eval(inner);
        };
    }

    static String show(Expr e) {
        return switch (e) {
            case Num(int v) when v < 0 -> "(" + v + ")"; // guard with "when"
            case Num(int v) -> String.valueOf(v);
            case Add(Expr l, Expr r) -> "(" + show(l) + " + " + show(r) + ")";
            case Mul(Expr l, Expr r) -> show(l) + " * " + show(r);
            case Neg(Expr inner) -> "-" + show(inner);
        };
    }

    // Nested record patterns: simplify x * 1, 1 * x, and x * 0.
    static Expr simplify(Expr e) {
        return switch (e) {
            case Mul(Expr l, Num(int v)) when v == 1 -> simplify(l);
            case Mul(Num(int v), Expr r) when v == 1 -> simplify(r);
            case Mul(Expr l, Num(int v)) when v == 0 -> new Num(0);
            case Mul(Expr l, Expr r) -> new Mul(simplify(l), simplify(r));
            case Add(Expr l, Expr r) -> new Add(simplify(l), simplify(r));
            case Neg(Neg(Expr inner)) -> simplify(inner);
            case Neg(Expr inner) -> new Neg(simplify(inner));
            case Num n -> n;
        };
    }

    public static void main(String[] args) {
        Expr e = new Add(new Mul(new Num(3), new Num(1)), new Neg(new Neg(new Num(-4))));
        System.out.println(show(e) + " = " + eval(e));
        Expr s = simplify(e);
        System.out.println(show(s) + " = " + eval(s));
        System.out.println(s);
    }
}
```

<!-- expected-output -->
```text
(3 * 1 + --(-4)) = -1
(3 + (-4)) = -1
Add[left=Num[value=3], right=Num[value=-4]]
```

How this compares with the **Visitor pattern** (see [behavioral patterns](../lld/fundamentals/08-behavioral-patterns.md)): both add operations to a fixed set of types without changing them. The sealed `switch` needs no `accept` method, no visitor interface, and no double dispatch, and the operation sits in one method. The cost is the **expression problem**: adding a new operation is easy (one new method), but adding a new case means a change to every switch. The compiler finds those switches, so the change is safe. Use sealed types and records for data that has a closed set of shapes (commands, events, results, AST nodes, API responses). Use an interface with polymorphic methods when the set of types is open and the set of operations is closed.

Data-carrying results are a common use: `sealed interface Result<T> permits Ok, Err`, with `record Ok<T>(T value)` and `record Err<T>(String message)`, so that a method returns a failure as a value and the caller must handle both cases. This is an alternative to the checked exceptions in [exceptions](03-exceptions.md).

### Sequenced collections (JEP 431)

Before Java 21, the first element of a collection had a different API in each type: `list.get(0)`, `deque.getFirst()`, `sortedSet.first()`, `linkedHashSet.iterator().next()`. The last element was worse for `LinkedHashSet`. JEP 431 adds three interfaces (the javadoc of `java.util` lists them):

| Interface | Extends | New methods | Implemented by (among others) |
|---|---|---|---|
| `SequencedCollection<E>` | `Collection<E>` | `addFirst`, `addLast`, `getFirst`, `getLast`, `removeFirst`, `removeLast`, `reversed()` | `List`, `Deque` |
| `SequencedSet<E>` | `SequencedCollection<E>`, `Set<E>` | `reversed()` (covariant return) | `LinkedHashSet`, `SortedSet`, `NavigableSet`, `TreeSet` |
| `SequencedMap<K,V>` | `Map<K,V>` | `firstEntry`, `lastEntry`, `pollFirstEntry`, `pollLastEntry`, `putFirst`, `putLast`, `reversed()`, `sequencedKeySet`, `sequencedValues`, `sequencedEntrySet` | `LinkedHashMap`, `SortedMap`, `NavigableMap`, `TreeMap` |

`HashSet` and `HashMap` stay outside the family, because they define no order.

The rules and pitfalls:

- `reversed()` returns a **view**, not a copy. A write through the view changes the original, and the reverse.
- Methods that a collection cannot support throw `UnsupportedOperationException`. `TreeSet.addFirst` throws, because the comparator decides the position. An immutable `List.of(...)` throws on every mutating method.
- `getFirst` and `getLast` throw `NoSuchElementException` on an empty collection. `firstEntry` returns `null` for an empty map.
- `LinkedHashSet.addFirst("c")` for an element that exists **moves** it to the front (the program below shows this).
- A class that implements both `List` and `Deque` (rare) had to resolve the conflicting `reversed()` return types when it moved to Java 21 (JEP 431 lists this source incompatibility).

```java
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.TreeSet;
import java.util.ArrayList;

public class SequencedDemo {
    static <T> T last(SequencedCollection<T> c) { return c.getLast(); }

    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
        list.addFirst(0);
        System.out.println(list + " first=" + list.getFirst() + " last=" + list.getLast());
        System.out.println("reversed view: " + list.reversed());
        list.reversed().add(99);               // the view writes through
        System.out.println("after write through: " + list);

        SequencedSet<String> set = new LinkedHashSet<>(List.of("b", "a", "c"));
        set.addFirst("c");                     // moves an existing element to the front
        System.out.println(set + " reversed=" + set.reversed());

        SequencedMap<String, Integer> map = new LinkedHashMap<>();
        map.put("x", 1);
        map.put("y", 2);
        map.putFirst("z", 0);
        System.out.println(map + " firstEntry=" + map.firstEntry());
        System.out.println("pollLast=" + map.pollLastEntry() + " left=" + map);

        System.out.println(last(new ArrayDeque<>(List.of("p", "q"))));
        System.out.println(new TreeSet<>(List.of(5, 1, 3)).reversed());
        try {
            new TreeSet<>(List.of(1)).addFirst(0);
        } catch (UnsupportedOperationException e) {
            System.out.println("TreeSet.addFirst: UnsupportedOperationException");
        }
        try {
            List.of(1).addFirst(0);
        } catch (UnsupportedOperationException e) {
            System.out.println("List.of(..).addFirst: UnsupportedOperationException");
        }
    }
}
```

<!-- expected-output -->
```text
[0, 1, 2, 3] first=0 last=3
reversed view: [3, 2, 1, 0]
after write through: [99, 0, 1, 2, 3]
[c, b, a] reversed=[a, b, c]
{z=0, x=1, y=2} firstEntry=z=0
pollLast=y=2 left={z=0, x=1}
q
[5, 3, 1]
TreeSet.addFirst: UnsupportedOperationException
List.of(..).addFirst: UnsupportedOperationException
```

The JDK internals behind these types are in [collections internals](04-collections-internals.md).

### `HttpClient` (JEP 321)

The package `java.net.http` replaced `HttpURLConnection` in Java 11. `HttpClient` is immutable, so one instance can serve many requests and many threads. It speaks HTTP/2 and falls back to HTTP/1.1 if the server does not support HTTP/2. `send` blocks. `sendAsync` returns a `CompletableFuture<HttpResponse<T>>` (see [CompletableFuture](09-completablefuture.md)). The `BodyHandler` decides how the client reads the response body: a `String`, a file, a stream of lines, or a discarded body. Java 26 added support for HTTP/3 (JEP 517). The application must opt in. The default version stays HTTP/2.

The program below builds a client and a request without a network call, so its output is the same on every machine.

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpDemo {
    public static void main(String[] args) {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://example.org/api/items"))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"id\":1}"))
                .build();
        System.out.println(request.method() + " " + request.uri());
        System.out.println(request.headers().firstValue("Accept").orElse("none"));
        System.out.println(client.version() + " " + client.connectTimeout().orElseThrow());

        // Sending needs a network, so this program only builds the request. The calls are:
        //   HttpResponse<String> r = client.send(request, HttpResponse.BodyHandlers.ofString());
        //   client.sendAsync(request, HttpResponse.BodyHandlers.ofString())  // CompletableFuture
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        System.out.println(handler != null);
    }
}
```

<!-- expected-output -->
```text
POST https://example.org/api/items
application/json
HTTP_2 PT2S
true
```

### Virtual threads (JEP 444)

Java 21 made virtual threads final: cheap threads that the JVM schedules on a small pool of platform threads. The page [virtual threads](10-virtual-threads.md) covers them.

### Smaller changes that interviews mention

| Area | Change | Version |
|---|---|---|
| `String` | `isBlank`, `strip`, `stripLeading`, `stripTrailing`, `lines`, `repeat` (11); `formatted`, `stripIndent`, `translateEscapes` (15) | 11, 15 |
| `Stream` | `takeWhile`, `dropWhile`, `ofNullable`, `iterate` with a predicate (9); `Stream.toList()` (16); `mapMulti` (16) | 9, 16 |
| `Collectors` | `teeing` (12), `filtering`, `flatMapping` (9) | 9, 12 |
| `Optional` | `ifPresentOrElse`, `or`, `stream` (9); `isEmpty` (11); `orElseThrow()` with no argument (10) | 9 to 11 |
| `Files` | `readString`, `writeString` (11) | 11 |
| Interfaces | `private` methods in interfaces (9, JEP 213) | 9 |
| `try-with-resources` | An existing effectively final variable can be the resource (9, JEP 213) | 9 |
| Diamond | `new Foo<>() { ... }` for anonymous classes (9, JEP 213) | 9 |
| JVM | G1 is the default collector (9, JEP 248). Compact strings (9, JEP 254) | 9 |
| JShell | An interactive REPL (9, JEP 222) | 9 |
| Diagnostics | Helpful `NullPointerException` messages (JEP 358). JDK 14 added them behind the flag `-XX:+ShowCodeDetailsInExceptionMessages`, off by default. JDK 15 turned the flag on by default (JDK-8233014). In JDK 21 the flag is on by default (`globals.hpp`), and `-XX:-ShowCodeDetailsInExceptionMessages` turns it off | 14, 15 |

The helpful message shows what the JVM names in the message:

```java
public class NpeDemo {
    record Person(String name) { }
    public static void main(String[] args) {
        Person p = new Person(null);
        try {
            System.out.println(p.name().length());
        } catch (NullPointerException e) {
            System.out.println(e.getMessage());
        }
    }
}
```

<!-- expected-output -->
```text
Cannot invoke "String.length()" because the return value of "NpeDemo$Person.name()" is null
```

The sources for the API rows are the JDK 21 javadoc of each class. The version column comes from the `@since` tags in the javadoc. The JVM computes the message only when code calls `getMessage`, and the message names the method and the expression, not the source line. A `NullPointerException` from a C2 "fast throw" has no message at all (see [exceptions](03-exceptions.md)).

## What Java 22 to 27 changed

Java 21 is the target of this KB. The table lists what later releases made final or changed in a way that matters for the Java 21 features above. Status checked on openjdk.org on 2026-10-22. The latest release is JDK 27 (general availability on 2026-09-15). JDK 25 is an LTS release.

| Release | Feature | JEP | Status and effect |
|---|---|---|---|
| 22 (2024-03-19) | Unnamed variables and patterns (`_`) | 456 | Final. Preview in 21 (JEP 443). Use `_` for a variable or a pattern component that the code never reads: `catch (Exception _)`, `case Point(int x, _)` |
| 22 | Foreign function and memory API | 454 | Final. Preview in 21 (JEP 442). Replaces most JNI use |
| 22 | Launch multi-file source-code programs | 458 | Final. `java Main.java` can compile the other source files that `Main.java` uses |
| 23 | Markdown documentation comments | 467 | Final. Javadoc comments can use Markdown |
| 23 | ZGC: generational mode by default | 474 | Final. The non-generational mode was deprecated, and JEP 490 removed it in 24 |
| 24 | Stream gatherers | 485 | Final. A `gather` operation for custom intermediate stream operations. Preview in 22 and 23 |
| 24 | Class-File API | 484 | Final |
| 24 | Synchronize virtual threads without pinning | 491 | Final. A virtual thread that blocks inside `synchronized` can unmount. See [virtual threads](10-virtual-threads.md) |
| 24 | Permanently disable the Security Manager | 486 | Final |
| 25 | Scoped values | 506 | Final. Preview in 21 (JEP 446), 22, 23, and 24 |
| 25 | Compact source files and instance `main` methods | 512 | Final. A small program can omit the class and the `public static` on `main` |
| 25 | Flexible constructor bodies | 513 | Final. Statements can appear before `super(...)` or `this(...)`. They cannot use the object under construction, except to assign its fields that have no initializer |
| 25 | Module import declarations | 511 | Final. `import module java.base;` imports all exported packages of a module |
| 25 | Compact object headers | 519 | Changed from experimental to a product feature. Off by default in 25 (`-XX:+UseCompactObjectHeaders` turns it on) |
| 25 | Generational Shenandoah | 521 | Changed from experimental to a product feature. Shenandoah still defaults to one generation |
| 26 (2026-03-17) | HTTP/3 for the HTTP Client API | 517 | Final. The application opts in with `HttpClient.Version.HTTP_3`. The default stays HTTP/2 |
| 26 | Prepare to make `final` mean final | 500 | Delivered. Deep reflection that changes a `final` field now prints a run-time warning, as a step toward blocking it by default. See [Reflection and Annotations](14-reflection-and-annotations.md) |
| 27 (2026-09-15) | Make G1 the default collector in all environments | 523 | Final. Small machines that used Serial now use G1 |
| 27 | Compact object headers by default | 534 | Final. The 64-bit header layout (96 bits before) is the default on 64-bit platforms. The old layout is still available |

Features that were still preview or incubator in JDK 27 (status on openjdk.org):

| Feature | JEP in JDK 27 | Status |
|---|---|---|
| Structured concurrency | 533 | Seventh preview (preview in 21 as JEP 453) |
| Primitive types in patterns, `instanceof`, and `switch` | 532 | Fifth preview |
| Lazy constants | 531 | Third preview |
| Vector API | 537 | Twelfth incubator |
| PEM encodings of cryptographic objects | 538 | Third preview |

What this means for Java 21 code: everything that this page lists as final in Java 21 is still final in Java 27. A project that moves from 21 to 25 gains `_`, the final FFM API, stream gatherers, scoped values, and virtual threads without pinning in `synchronized`. Code that used a Java 21 preview feature must be checked: string templates (JEP 430) had a second preview in 22 (JEP 459), the third preview (JEP 465) was withdrawn, and JDK 23 removed them. Structured concurrency is still a preview feature in JDK 27, and its API changed between previews.

## Trade-offs and When to Use

| Feature | Use when | Avoid when | Cost or risk |
|---|---|---|---|
| `var` | The right-hand side shows the type | The type is not obvious, or you want the variable to have an interface type (`List<String> x = new ArrayList<>()`) | Weaker readability in code review, because tools show types and reviews do not. `var` with a diamond infers `Object` |
| Switch expression | You compute a value from a case, or want the compiler to check the cases | Each branch has side effects and no value (a statement is as clear) | Mixing arrow and colon forms is a compile error |
| Text block | Multi-line SQL, JSON, HTML, or test data | A one-line string | The indentation rule: a mis-placed closing delimiter changes the string |
| Record | A DTO, a key, a value object, a tuple, a message | A JPA entity, a class with mutable state, or a class that needs inheritance | Shallow immutability. No extra instance fields |
| Sealed type | A closed set of shapes that you control | A public extension point, or a type that a library user must implement | Every `switch` over it breaks when you add a case, by design |
| Pattern `switch` | You dispatch on the shape of data | You dispatch on behavior that belongs to the type (use a polymorphic method) | A long `switch` can hide behavior that should be a method |
| Sequenced collections | You need the first or last element, or a reverse view | The code must run on Java 20 or older | `reversed()` is a view. Some implementations throw on `addFirst` |
| `HttpClient` | A plain HTTP call without a framework | The application already uses `RestClient` or `WebClient` from Spring (see [Spring Web and REST](20-spring-web-and-rest.md)) | You write the JSON conversion yourself |
| Modules | You ship a library or a `jlink` runtime image | A typical Spring application | `opens` and `--add-opens` for reflection, and a split-package problem when libraries share packages |

## Java Example

The programs in "How It Works" run on Java 21 as they are. This block combines the features in one small domain model: a sealed result type, records with validation, a pattern `switch`, and a sequenced collection of events.

```java
import java.util.ArrayList;
import java.util.List;

public class OrderDemo {
    sealed interface PaymentResult permits Paid, Declined, Pending { }
    record Paid(String receipt, long cents) implements PaymentResult { }
    record Declined(String reason) implements PaymentResult { }
    record Pending(int retryAfterSeconds) implements PaymentResult {
        Pending {
            if (retryAfterSeconds <= 0) throw new IllegalArgumentException("retryAfterSeconds");
        }
    }

    static String message(PaymentResult result) {
        return switch (result) {
            case Paid(String receipt, long cents) when cents >= 100_00 ->
                    "large payment " + receipt;
            case Paid(String receipt, long cents) -> "paid " + cents + " cents, " + receipt;
            case Declined(String reason) -> "declined: " + reason;
            case Pending(int seconds) -> "retry in " + seconds + "s";
        };
    }

    public static void main(String[] args) {
        List<PaymentResult> events = new ArrayList<>(List.of(
                new Paid("R-1", 2_500), new Declined("card expired"), new Pending(30)));
        events.addFirst(new Paid("R-0", 150_00));
        for (var event : events) System.out.println(message(event));
        System.out.println("last event: " + events.getLast());
        System.out.println("newest first:");
        events.reversed().forEach(e -> System.out.println("  " + e.getClass().getSimpleName()));
    }
}
```

<!-- expected-output -->
```text
large payment R-0
paid 2500 cents, R-1
declined: card expired
retry in 30s
last event: Pending[retryAfterSeconds=30]
newest first:
  Pending
  Declined
  Paid
  Paid
```

The compiler checks that `message` covers `Paid`, `Declined`, and `Pending`. If you add a fourth record to the `permits` list, `message` no longer compiles. The compact constructor of `Pending` rejects invalid data when the record is created, so the `switch` never sees an invalid value.

## Interview Angle

### Questions

??? question "What does `var` do, and where can you not use it?"

    `var` tells the compiler to infer the type of a local variable from its initializer (JEP 286, Java 10). The variable still has a static type, so `var x = 1;` is an `int` and cannot later hold a `String`. It works for local variables with an initializer, for loop variables, for `try-with-resources` resources, and since Java 11 for lambda parameters. It does not work for fields, method parameters, return types, or a declaration without an initializer or with a `null` or lambda initializer.

    The trade-off: `var` removes noise when the type is obvious (`var users = new ArrayList<User>()`) and hides information when it is not (`var r = service.process(x)`). Also, `var list = new ArrayList<>()` infers `ArrayList<Object>`, which is rarely what you want.

??? question "What is the difference between a switch statement and a switch expression?"

    A switch expression (JEP 361, Java 14) produces a value, and a statement does not. With arrow cases there is no fall-through, a case can list several constants, and a multi-statement branch uses a block with `yield`. A switch expression must be exhaustive: for an enum, every constant needs a case or there must be a `default`, and for a sealed type, every permitted subtype needs a case. The compiler enforces this. The old colon form falls through unless you write `break`. So the expression form removes two classic bugs: the missing `break` and the forgotten case.

    The edge case: an exhaustive enum switch can still meet an unknown constant at run time, if someone adds a constant to the enum and recompiles only the enum. In Java 21 a switch expression then throws `MatchException` (JLS 15.28.2). Java 14 to 20 threw `IncompatibleClassChangeError`. An old-style switch *statement* over an enum is not required to be exhaustive, so it runs no branch and continues. Only an enhanced switch statement (one that uses patterns or `case null`, or whose selector is not a primitive, box, `String`, or enum type) must be exhaustive and throws `MatchException`.

??? question "What is a record? What does the compiler generate, and what are the limits?"

    A record (JEP 395, Java 16) is a final class for immutable data. For `record Point(int x, int y)`, the compiler generates private final fields, a canonical constructor, accessors `x()` and `y()`, and `equals`, `hashCode`, and `toString` that use all components. A compact constructor can validate or normalize the arguments.

    Limits: a record cannot extend a class or declare extra instance fields, and it is implicitly final. It can implement interfaces and have static members and methods. The immutability is shallow: a `List` component can still change, so copy mutable components in the constructor with `List.copyOf`. Records suit DTOs, keys, and messages. They do not suit JPA entities.

??? question "How is a record different from a Lombok `@Value` class?"

    A record is a language feature, so the compiler, the IDE, serialization libraries, and pattern matching all know its structure. A record can be taken apart in a pattern (`case Point(int x, int y)`), and `@Value` cannot. A `@Value` class can have extra fields, can extend a class, and can generate builders and other code with more annotations. A record has accessor names without `get`, which some frameworks needed to learn. For new code on Java 16 or later, I use a record for a plain data carrier, and a normal class when I need inheritance or mutable state.

??? question "What are sealed classes for?"

    A sealed class or interface (JEP 409, Java 17) lists the subtypes that may extend it, with `permits`. Each permitted subtype must be `final`, `sealed`, or `non-sealed`, and must be in the same module or package. The purpose is a closed hierarchy: the compiler then knows all the cases, so a `switch` over the type needs no `default`, and the compiler reports an error if the switch misses a case. Together with records, a sealed interface models a sum type, such as a result that is `Ok` or `Err`, or an expression tree. The trade-off is that a library user cannot add a subtype, so a sealed type is wrong for an extension point.

??? question "Explain pattern matching for `switch`. What is dominance and what is exhaustiveness?"

    Pattern matching for `switch` (JEP 441, final in Java 21) lets a `case` test the type of the selector, bind a variable, add a `when` guard, and match `null` with `case null`. With record patterns (JEP 440), a `case` can also take a record apart, and the patterns nest.

    Dominance: the compiler reports an error if an earlier label covers a later one, for example `case Object o` before `case String s`. Order the labels from specific to general, and put guarded labels before unguarded labels of the same type. Exhaustiveness: a pattern `switch` must cover all values. For a sealed selector, a case per permitted subtype is enough. If I add a subtype later, each such `switch` fails to compile. Without `case null`, a `null` selector throws `NullPointerException`.

??? question "What does pattern matching for `instanceof` change?"

    Before Java 16, you wrote `if (o instanceof String) { String s = (String) o; ... }`. With JEP 394, `if (o instanceof String s)` tests the type and binds `s` in one step, with no cast. The scope of `s` is flow-sensitive: it is in scope where the compiler can prove the test is true, for example in the `if` body, after `&&`, or after `if (!(o instanceof String s)) return;`. The change removes a cast that could be wrong, and it makes `equals` methods shorter: `return o instanceof Point p && p.x == x && p.y == y;`.

??? question "How do text blocks handle indentation?"

    A text block (JEP 378, Java 15) starts with `"""` and a line break. The compiler removes the incidental indentation: it takes the smallest indentation among the non-blank lines and the closing-delimiter line, and removes it from every line. It strips trailing spaces on each line, so `\s` is the way to keep a trailing space. A backslash at the end of a line joins the next line. The line breaks become `\n`. So the position of the closing delimiter controls how much indentation stays in the string.

??? question "What are sequenced collections?"

    Java 21 (JEP 431) added `SequencedCollection`, `SequencedSet`, and `SequencedMap`, which describe collections with a defined encounter order. They give one API for the first and last element (`getFirst`, `getLast`, `addFirst`, `removeLast`) and a `reversed()` view. `List` and `Deque` are sequenced collections, and so are `LinkedHashSet`, `TreeSet`, `LinkedHashMap`, and `TreeMap`. `HashSet` and `HashMap` are not. `reversed()` is a view, so writes go through. Some operations throw `UnsupportedOperationException` (`TreeSet.addFirst`, and every mutator of `List.of`). `getFirst` on an empty list throws `NoSuchElementException`.

??? question "What is the module system, and does a typical backend need it?"

    The module system (JEP 261, Java 9) groups packages into modules with a `module-info.java` that says what the module requires and exports. A package that is not exported is inaccessible from other modules, even if its classes are public. This gives strong encapsulation and lets `jlink` build a small runtime. A typical Spring application runs on the classpath, in the unnamed module, and does not write a descriptor. You meet modules when a library needs `--add-opens` because it uses reflection on JDK internals, which Java 16 and 17 blocked by default (JEP 396, JEP 403), and when you build a custom runtime image.

??? question "How would you model a closed set of results (success, validation error, not found) in Java 21?"

    Use a sealed interface with record cases: `sealed interface Result permits Ok, Invalid, NotFound`, with `record Ok(Order order)`, `record Invalid(List<String> errors)`, and `record NotFound(String id)`. The caller uses a pattern `switch` with no `default`, and the compiler guarantees that it handles every case. If a later change adds a case, each `switch` fails to compile until a developer handles it. The alternative is exceptions: they cost less code in the caller but give no compiler check on the cases that a method can return. I use result types when failure is a normal outcome, and exceptions for unexpected failures.

??? question "Which Java versions are LTS, and what changed after Java 21?"

    The long-term-support releases are Java 8, 11, 17, 21, and 25 (Oracle Java SE Support Roadmap). Oracle plans an LTS release every two years, so the next one is Java 29 in September 2027. A new feature release comes every six months, in March and September. LTS is a support promise from vendors such as Oracle, not a different kind of release. After Java 21: `_` for unused variables (JEP 456, Java 22), the final foreign function and memory API (JEP 454, Java 22), stream gatherers (JEP 485, Java 24), virtual threads that no longer pin on `synchronized` (JEP 491, Java 24), and scoped values, compact source files, and flexible constructor bodies (Java 25). Structured concurrency was still a preview feature in Java 27.

### Follow-up Questions

??? question "Can a record have a mutable field?"

    A record has no instance fields beyond its components, and its component fields are `final`. A component can refer to a mutable object (a `List`, an array, a `Date`), and then the record is only shallowly immutable. Copy the object in the compact constructor, and return a copy or an unmodifiable view from the accessor if the caller must not change it.

??? question "Why does a switch over an enum not need `default`, but a switch over `String` does?"

    The question applies to a switch *expression*, which must be exhaustive. The compiler knows all constants of an enum, so it can check that the cases cover them. A `String` has unlimited values, so a switch expression over a `String` needs a `default`. A sealed type works like an enum: the compiler knows the permitted subtypes. A classic switch *statement* over a `String` or an enum needs no `default` at all, because the JLS does not require it to be exhaustive. If no case matches, it does nothing. In Java 21 an enum switch expression with no matching constant at run time throws `MatchException`.

??? question "What is `yield`?"

    `yield value;` ends a block branch of a switch expression and gives the value of the whole expression. It is a contextual keyword. It is needed because `return` would leave the method, and an arrow with a block has no other way to name the result.

??? question "What is the difference between `exports` and `opens` in a module?"

    `exports` makes the public types of a package accessible at compile time and run time. `opens` makes all members, including private ones, accessible to reflection at run time only. Frameworks such as Hibernate and Jackson need `opens` for the entity and DTO packages.

??? question "Does `var` make Java dynamically typed?"

    No. The compiler infers one static type at compile time, and the variable keeps it. Reassignment with an incompatible type is a compile error.

### Red Flags

- Saying "`var` is like `Object`" or "`var` makes Java dynamic".
- Using `var` where the type is not visible (`var data = load();`) and defending it as "shorter".
- Writing `default` in a switch over a sealed type, which turns off the exhaustiveness check.
- Saying that a record is "immutable" with no mention of shallow immutability.
- Using a record as a JPA entity.
- Saying that sealed classes are for security.
- Not knowing that `reversed()` returns a view.
- Listing Java features without knowing the version in which each became final, or treating a preview feature as final.
- Confusing `exports` and `opens`.

## Common Mistakes

| Mistake | Why it fails | Fix |
|---|---|---|
| `var list = new ArrayList<>();` | Infers `ArrayList<Object>` | Write `new ArrayList<String>()` or the declared type |
| `default ->` in a switch over a sealed type | A new subtype no longer causes a compile error | Remove `default`, and let the compiler check |
| `case Object o` before `case String s` | Dominance error: the later label is unreachable | Order the labels from specific to general |
| Missing `case null` | A `null` selector throws `NullPointerException` | Add `case null` or check before the switch |
| A record with a mutable component | The record is shallowly immutable. A caller can change the list | Copy in the compact constructor: `List.copyOf` |
| Using a record as a JPA entity | The persistence provider needs a no-argument constructor and mutable fields | Use a class for the entity and a record for the DTO |
| Moving the closing `"""` of a text block without checking the output | The incidental-indentation rule changes the string | Print the string, or place the delimiter on its own line at the indentation you want |
| Trailing spaces in a text block | The compiler strips them | Use `\s` for the last space |
| Mixing `case A ->` and `case B:` in one switch | Compile error | Use one form |
| Calling `reversed()` and expecting a copy | The view writes through to the original | Copy it: `new ArrayList<>(list.reversed())` |
| Using a Java 21 preview feature in production without `--enable-preview` in every environment | The class does not load on a JVM without the flag, and the API can change | Avoid preview features in production code, or pin the JDK and the flag |
| Accessing a private field of a JDK class with reflection | `setAccessible(true)` throws `InaccessibleObjectException` by default since Java 16 (JEP 396). Java 17 (JEP 403) removed the `--illegal-access` option that relaxed this | Use the public API, or pass `--add-opens` and document it |

## Commonly Confused With

| Looks like | Difference | Deciding question |
|---|---|---|
| `var` vs `Object` | `var` keeps the exact static type. `Object` loses it | Do you need the members of the real type? |
| Switch statement vs switch expression | A statement runs code. An expression gives a value and must be exhaustive | Do you use the result? |
| Record vs Lombok `@Value` vs a normal class | A record is a language feature with fixed shape. `@Value` is generated code. A class has no restrictions | Is it a plain data carrier? |
| `final` class vs `sealed` class | `final` allows no subclass. `sealed` allows the listed subclasses | Is the set of subtypes closed but larger than zero? |
| `non-sealed` vs an unsealed class | `non-sealed` re-opens one branch of a sealed hierarchy. A normal class is open without a sealed parent | Is there a sealed parent? |
| Type pattern vs record pattern | A type pattern tests the type and binds one variable. A record pattern also takes the components apart | Do you need the components? |
| `instanceof` pattern vs a cast | The pattern binds a typed variable if the test passes, with no cast | Do you test and then cast? |
| `List.of` vs `Collections.unmodifiableList` | `List.of` is immutable and rejects `null`. The wrapper is a read-only view of a list that can change | Can the source list still change? |
| `exports` vs `opens` | Compile-time and run-time access to public types vs run-time reflective access to all members | Is it a reflection use? |
| Preview vs incubator | A preview feature (JEP 12) is a complete language, JVM, or Java SE API feature that needs `--enable-preview`. An incubator module (JEP 11) is a non-final API in a `jdk.incubator` module | Does the feature need `--enable-preview`, or a `jdk.incubator` module? |
| `HttpClient` vs `HttpURLConnection` | `HttpClient` is immutable, supports HTTP/2, and has asynchronous calls | Do you need HTTP/2 or async? |

## Quick Reference Card

- Releases every six months. LTS: 8, 11, 17, 21, 25 (next planned: 29, September 2027). This KB targets 21. Latest in October 2026: JDK 27.
- `var` (10): local variables with an initializer only. Static type. `ArrayList<>` with `var` gives `ArrayList<Object>`.
- Switch expressions (14): arrows, no fall-through, `yield`, exhaustive. No matching label at run time: `MatchException` in 21 (`IncompatibleClassChangeError` in 14 to 20). An old enum switch statement does nothing.
- Text blocks (15): remove incidental indentation, strip trailing spaces, `\s` keeps a space, `\` joins lines.
- Records (16): final, components, canonical and compact constructors, generated `equals`, `hashCode`, `toString`. Shallow immutability. No extra instance fields.
- `instanceof` pattern (16): binds a variable with flow scoping.
- Sealed (17): `permits`, subtypes are `final`, `sealed`, or `non-sealed`, same module or package.
- Pattern `switch` and record patterns (21): type patterns, `when` guards, nested patterns, `case null`, dominance, exhaustiveness over sealed types.
- Sequenced collections (21): `getFirst`, `getLast`, `addFirst`, `reversed()` view. `HashSet` and `HashMap` are not sequenced.
- Modules (9): `exports` for compile-time and run-time access, `opens` for reflection. Most backends run on the classpath.
- `HttpClient` (11): immutable, HTTP/2, `send` and `sendAsync`.
- After 21: `_` (22), FFM (22), gatherers (24), no pinning in `synchronized` (24), scoped values, flexible constructors, and compact source files (25), G1 default everywhere and compact headers by default (27). Structured concurrency is still preview in 27.
- Algebraic data type: sealed interface plus records plus pattern `switch`. Adding an operation is easy. Adding a case needs a change to each switch, and the compiler finds them.

## Further Reading

- [JLS 21, chapter 14.11 (`switch`) and 14.30 (patterns)](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html): the rules for exhaustiveness, dominance, pattern scope, and the run-time behavior of a switch statement (14.11.3).
- [JLS 21, section 15.28.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.28.2): the run-time evaluation of a switch expression and `MatchException`.
- [JLS 21, section 8.10 (record classes)](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10): the generated members and the restrictions.
- [JLS 21, section 3.10.6 (text blocks)](https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html#jls-3.10.6): the indentation and escape rules.
- [JEP 261: Module System](https://openjdk.org/jeps/261), [JEP 286: Local-Variable Type Inference](https://openjdk.org/jeps/286), [JEP 321: HTTP Client](https://openjdk.org/jeps/321): the Java 9 to 11 features.
- [JEP 361: Switch Expressions](https://openjdk.org/jeps/361), [JEP 378: Text Blocks](https://openjdk.org/jeps/378), [JEP 395: Records](https://openjdk.org/jeps/395), [JEP 394: Pattern Matching for instanceof](https://openjdk.org/jeps/394): the Java 14 to 16 features, with the design reasons.
- [JEP 409: Sealed Classes](https://openjdk.org/jeps/409), [JEP 440: Record Patterns](https://openjdk.org/jeps/440), [JEP 441: Pattern Matching for switch](https://openjdk.org/jeps/441), [JEP 431: Sequenced Collections](https://openjdk.org/jeps/431): the Java 17 and 21 features.
- [JEP 12: Preview Features](https://openjdk.org/jeps/12) and [JEP 11: Incubator Modules](https://openjdk.org/jeps/11): the definitions of preview features and incubator modules.
- [JEP 465: String Templates (Third Preview)](https://openjdk.org/jeps/465): status "Closed / Withdrawn". [JDK-8329948](https://bugs.openjdk.org/browse/JDK-8329948) removed string templates in JDK 23.
- [JEP 358: Helpful NullPointerExceptions](https://openjdk.org/jeps/358) and [JDK-8233014](https://bugs.openjdk.org/browse/JDK-8233014): the feature in JDK 14, and the JDK 15 change that turned it on by default.
- [Oracle Java SE Support Roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html): the list of LTS releases and the support dates.
- [JEP 456: Unnamed Variables and Patterns](https://openjdk.org/jeps/456), [JEP 485: Stream Gatherers](https://openjdk.org/jeps/485), [JEP 506: Scoped Values](https://openjdk.org/jeps/506), [JEP 512: Compact Source Files](https://openjdk.org/jeps/512), [JEP 513: Flexible Constructor Bodies](https://openjdk.org/jeps/513): the features that Java 22 to 25 finalized.
- [OpenJDK project pages for JDK 22 to 27](https://openjdk.org/projects/jdk/27/): the JEP list and the date of each release (replace the number for another release).
- [`java.lang.Record` javadoc (JDK 21)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Record.html): the contract of the generated `equals`, `hashCode`, and `toString`.
- [`SequencedCollection` javadoc (JDK 21)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/SequencedCollection.html): the new methods and their exceptions.
- [Local Variable Type Inference: Style Guidelines](https://openjdk.org/projects/amber/guides/lvti-style-guide): when `var` helps and when it hurts.
- [Project Amber](https://openjdk.org/projects/amber/): the project page for records, sealed types, and pattern matching, with links to the design documents.
