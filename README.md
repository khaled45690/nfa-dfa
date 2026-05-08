# NFA ↔ DFA Converter

Converts an NFA to a DFA using Subset Construction, then wraps the DFA back to an NFA.
Tests whether a string is accepted by the language **(a|b)\*abb**.

---

## Requirements

| Tool | Download |
|------|----------|
| Java 21 (JDK) | https://adoptium.net — download **Temurin 21**, run the `.msi` installer |
| Graphviz *(optional — for PNG diagrams)* | https://graphviz.org/download — download the Windows installer |

> After installing Java, open a new Command Prompt and run `java -version` to confirm it works.

---

## First-time setup (compile)

Open **Command Prompt** (`Win + R` → type `cmd` → Enter), then:

```cmd
cd path\to\nfa-dfa
```

For example if the folder is on your Desktop:
```cmd
cd %USERPROFILE%\Desktop\nfa-dfa
```

Then compile all the Java files into the `bin` folder:
```cmd
for /r src %f in (*.java) do javac -d bin "%f"
```

You only need to do this once (or again if you change any `.java` file).

---

## Run

```cmd
java -cp bin com.automata.Main
```

---

## What the app does

1. Displays the original **NFA** for the language (a|b)\*abb
2. Converts it to a **DFA** using Subset Construction and prints the transition table
3. Wraps the DFA back into an **NFA**
4. Asks you to enter a string — prints **ACCEPTED** or **REJECTED**

If Graphviz is installed, it also saves diagrams to the `output\` folder:
- `output\nfa.png` — the original NFA
- `output\dfa.png` — the converted DFA

---

## Accepted vs Rejected

The language is **(a|b)\*abb** — a string is accepted **only if it ends in `abb`**.

| String | Result |
|--------|--------|
| `abb` | ACCEPTED |
| `aabb` | ACCEPTED |
| `aabaabb` | ACCEPTED |
| `babb` | ACCEPTED |
| `aab` | REJECTED |
| `abba` | REJECTED |
| `b` | REJECTED |

---

## Optional: install Maven and use `mvn`

If you install Maven (https://maven.apache.org/download.cgi), you can use:

```cmd
mvn compile
mvn exec:java
```

instead of the manual `javac` / `java` commands above.
