package com.automata;

import com.automata.algorithm.*;
import com.automata.algorithm.DFAMinimizer.MinimizationResult;
import com.automata.model.*;
import com.automata.visualizer.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/** Entry point: full Regex → NFA → DFA → Minimized DFA pipeline with step-by-step output. */
public final class Main {

    // ── ANSI colours (disabled automatically if not a real terminal) ──────────
    private static final boolean COLOR = System.console() != null;
    private static final String RESET  = COLOR ? "\033[0m"  : "";
    private static final String BOLD   = COLOR ? "\033[1m"  : "";
    private static final String CYAN   = COLOR ? "\033[96m" : "";
    private static final String GREEN  = COLOR ? "\033[92m" : "";
    private static final String RED    = COLOR ? "\033[91m" : "";
    private static final String YELLOW = COLOR ? "\033[93m" : "";
    private static final String DIM    = COLOR ? "\033[2m"  : "";

    // ── width of the section ruler ────────────────────────────────────────────
    private static final int WIDTH = 70;

    // ═════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {

        banner();

        try (Scanner sc = new Scanner(System.in)) {

            // ── Ask for regex ─────────────────────────────────────────────────
            System.out.print(BOLD + "  Enter a regular expression" + RESET
                + DIM + " [default: (a|b)*abb]" + RESET + ":  ");
            String input = sc.nextLine().trim();
            if (input.isEmpty()) input = "(a|b)*abb";
            String regex = input;

            // ─────────────────────────────────────────────────────────────────
            step(1, "INPUT — Regular Expression");
            System.out.println("  Expression  : " + CYAN + BOLD + regex + RESET);
            System.out.println();

            // ─────────────────────────────────────────────────────────────────
            step(2, "THOMPSON'S CONSTRUCTION  →  NFA");
            explain(
                "Each sub-expression becomes a small NFA fragment (2 states, 1 transition).",
                "Operators combine fragments using ε-transitions:",
                "  |  →  union:        new start/accept; ε to both sub-NFAs",
                "  ·  →  concatenation: left.accept  →ε→  right.start",
                "  *  →  Kleene star:  ε-bypass + ε-loop-back",
                "  +  →  one-or-more:  mandatory first pass, then loop like *",
                "  ?  →  optional:     ε-bypass OR single pass"
            );

            NFA nfa;
            try {
                nfa = RegexToNFAConverter.convert(regex);
            } catch (IllegalArgumentException e) {
                System.out.println(RED + "  Error: " + e.getMessage() + RESET);
                return;
            }

            System.out.println("  Alphabet  : " + formatAlphabet(nfa.alphabet()));
            System.out.println("  NFA states: " + nfa.states().size()
                + "  (s0 … s" + (nfa.states().size() - 1) + ")");
            System.out.println();
            printNFA(nfa);

            // ─────────────────────────────────────────────────────────────────
            step(3, "SUBSET CONSTRUCTION  →  DFA");
            explain(
                "Every reachable set of NFA states (after ε-closure) becomes one DFA state.",
                "We then rename those verbose subset-names to short labels D0, D1, … for clarity.",
                "The mapping below shows which NFA-subset each Di represents."
            );

            // Build raw DFA, then rename states to D0, D1, …
            DFA rawDfa = NFAToDFAConverter.convert(nfa);
            RenameResult renamed = renameDFA(rawDfa, "D");
            DFA dfa = renamed.dfa();

            // Print subset-construction table (verbose names, for reference)
            System.out.println("  " + DIM + "Subset-construction transition table (verbose):" + RESET);
            printSubsetTable(nfa, rawDfa);

            // Print the D-label mapping
            System.out.println("  " + BOLD + "State renaming:" + RESET);
            for (var e : renamed.mapping().entrySet()) {
                boolean isStart  = e.getKey().equals(dfa.startState());
                boolean isAccept = dfa.acceptStates().stream()
                    .anyMatch(s -> s.name().equals(e.getKey().name()));
                String mark = (isStart ? " →" : "  ") + (isAccept ? "*" : " ");
                System.out.printf("  %s %-6s =  %s%n", mark, e.getKey().name(), e.getValue());
            }
            System.out.println();

            // ─────────────────────────────────────────────────────────────────
            step(4, "DFA  (before minimization)");
            explain(
                "Each Di is one DFA state.  The table shows where each state goes",
                "on each input symbol.  ∅ = dead/trap (no transition defined)."
            );
            printDFA(dfa);

            // ─────────────────────────────────────────────────────────────────
            step(5, "HOPCROFT'S ALGORITHM  →  Minimized DFA");
            explain(
                "Start with two blocks: { F } (accepting) vs { Q\\F } (rejecting).",
                "For each symbol c and splitter block A: find states whose c-transition",
                "goes into A and split any block that has both 'in' and 'out' states.",
                "Repeat until no more splits.  Merge each final block into one state."
            );

            MinimizationResult result = DFAMinimizer.minimizeWithSteps(dfa);
            printMinimizationSteps(result.partitionSteps(), dfa);

            // ─────────────────────────────────────────────────────────────────
            step(6, "MINIMIZED DFA");
            explain(
                "States in the same equivalence class have been merged.",
                "This is the smallest DFA that recognises the same language."
            );
            printDFA(result.minimized());

            // ─────────────────────────────────────────────────────────────────
            step(7, "GRAPHVIZ EXPORT  (.dot / .png)");
            exportAndRender(nfa, dfa, result.minimized());

            // ─────────────────────────────────────────────────────────────────
            step(8, "STRING ACCEPTANCE TESTING");
            explain(
                "Testing against the Minimized DFA.",
                "Type 'quit' (or 'exit') to finish."
            );
            testLoop(sc, result.minimized());
        }
    }

    // ── Pretty printers ───────────────────────────────────────────────────────

    /** Prints the NFA as a transition table (dynamic column widths). */
    private static void printNFA(NFA nfa) {
        List<Character> syms = new ArrayList<>();
        syms.add(NFA.EPSILON);
        syms.addAll(nfa.alphabet());

        // Compute column widths
        int sc0 = "NFA State".length();
        for (State s : nfa.states()) sc0 = Math.max(sc0, s.name().length() + 4);
        int[] cw = new int[syms.size()];
        for (int i = 0; i < syms.size(); i++) {
            cw[i] = ("δ(" + symLabel(syms.get(i)) + ")").length();
            for (State s : nfa.states()) {
                Set<State> ts = nfa.transitions(s, syms.get(i));
                String cell = ts.isEmpty() ? "∅"
                    : ts.stream().map(State::name).sorted()
                         .collect(Collectors.joining(",", "{", "}"));
                cw[i] = Math.max(cw[i], cell.length());
            }
            cw[i] += 2;
        }

        printTableHeader("NFA State", sc0, syms.stream()
            .map(c -> "δ(" + symLabel(c) + ")").toList(), cw);

        for (State s : nfa.states()) {
            boolean isStart  = s.equals(nfa.startState());
            boolean isAccept = nfa.acceptStates().contains(s);
            System.out.printf("  %s%-" + (sc0 - 3) + "s", statePrefix(isStart, isAccept), s.name());
            for (int i = 0; i < syms.size(); i++) {
                Set<State> ts = nfa.transitions(s, syms.get(i));
                String cell = ts.isEmpty() ? "∅"
                    : ts.stream().map(State::name).sorted()
                         .collect(Collectors.joining(",", "{", "}"));
                System.out.printf("  %-" + cw[i] + "s", cell);
            }
            System.out.println();
        }
        System.out.println();
        legend();
    }

    /** Prints the verbose subset-construction table (raw DFA state names = NFA subsets). */
    private static void printSubsetTable(NFA nfa, DFA rawDfa) {
        List<Character> syms = new ArrayList<>(nfa.alphabet());
        int sc0 = "DFA subset".length();
        for (State s : rawDfa.states()) sc0 = Math.max(sc0, s.name().length() + 4);
        int[] cw = new int[syms.size()];
        for (int i = 0; i < syms.size(); i++) {
            cw[i] = ("δ(" + syms.get(i) + ")").length();
            for (State s : rawDfa.states()) {
                State nx = rawDfa.transition(s, syms.get(i));
                cw[i] = Math.max(cw[i], nx != null ? nx.name().length() : 1);
            }
            cw[i] += 2;
        }
        printTableHeader("DFA subset", sc0,
            syms.stream().map(c -> "δ(" + c + ")").toList(), cw);
        for (State s : rawDfa.states()) {
            boolean isStart  = s.equals(rawDfa.startState());
            boolean isAccept = rawDfa.acceptStates().contains(s);
            System.out.printf("  %s%-" + (sc0 - 3) + "s", statePrefix(isStart, isAccept), s.name());
            for (int i = 0; i < syms.size(); i++) {
                State nx = rawDfa.transition(s, syms.get(i));
                System.out.printf("  %-" + cw[i] + "s", nx != null ? nx.name() : "∅");
            }
            System.out.println();
        }
        System.out.println();
        legend();
    }

    /** Prints a DFA as a transition table (dynamic column widths). */
    private static void printDFA(DFA dfa) {
        List<Character> syms = new ArrayList<>(dfa.alphabet());
        int sc0 = "DFA State".length();
        for (State s : dfa.states()) sc0 = Math.max(sc0, s.name().length() + 4);
        int[] cw = new int[syms.size()];
        for (int i = 0; i < syms.size(); i++) {
            cw[i] = ("δ(" + syms.get(i) + ")").length();
            for (State s : dfa.states()) {
                State nx = dfa.transition(s, syms.get(i));
                cw[i] = Math.max(cw[i], nx != null ? nx.name().length() : 1);
            }
            cw[i] += 2;
        }
        printTableHeader("DFA State", sc0,
            syms.stream().map(c -> "δ(" + c + ")").toList(), cw);
        for (State s : dfa.states()) {
            boolean isStart  = s.equals(dfa.startState());
            boolean isAccept = dfa.acceptStates().contains(s);
            System.out.printf("  %s%-" + (sc0 - 3) + "s", statePrefix(isStart, isAccept), s.name());
            for (int i = 0; i < syms.size(); i++) {
                State nx = dfa.transition(s, syms.get(i));
                System.out.printf("  %-" + cw[i] + "s", nx != null ? nx.name() : "∅");
            }
            System.out.println();
        }
        System.out.println();
        legend();
    }

    /** Prints a shared table header row + ruler. */
    private static void printTableHeader(String firstCol, int w0,
                                         List<String> headers, int[] cw) {
        System.out.printf("  %-" + w0 + "s", firstCol);
        for (int i = 0; i < headers.size(); i++)
            System.out.printf("  %-" + cw[i] + "s", headers.get(i));
        System.out.println();
        int total = w0 + 2 + Arrays.stream(cw).map(x -> x + 2).sum();
        System.out.println("  " + DIM + "─".repeat(Math.max(total, WIDTH - 2)) + RESET);
    }

    /** Prints Hopcroft partition-refinement history. */
    private static void printMinimizationSteps(List<List<Set<State>>> steps, DFA dfa) {
        Set<State> accepting = dfa.acceptStates();
        for (int i = 0; i < steps.size(); i++) {
            List<Set<State>> partition = steps.get(i);
            String label = i == 0
                ? "  P₀  (initial partition — accept vs reject):"
                : "  P" + subscript(i) + "  (after refinement " + i + "):";
            System.out.println(YELLOW + label + RESET);
            System.out.print("       {  ");
            for (int j = 0; j < partition.size(); j++) {
                Set<State> block = partition.get(j);
                boolean acc = block.stream().anyMatch(accepting::contains);
                String blockStr = block.stream().map(State::name).sorted()
                    .collect(Collectors.joining(", ", "{", "}"));
                System.out.print((acc ? GREEN : "") + blockStr + (acc ? "*" + RESET : ""));
                if (j < partition.size() - 1) System.out.print("   |   ");
            }
            System.out.println("  }");
        }
        System.out.println();

        int finalSize = steps.get(steps.size() - 1).size();
        int origSize  = steps.get(0).stream().mapToInt(Set::size).sum();
        System.out.println("  " + BOLD
            + "Result: " + origSize + " DFA states → "
            + finalSize + " minimized state(s)  ("
            + (origSize - finalSize) + " merged)" + RESET);
        System.out.println();
    }

    /** String acceptance test loop. */
    private static void testLoop(Scanner sc, DFA dfa) {
        while (true) {
            System.out.print("  " + BOLD + "> " + RESET);
            if (!sc.hasNextLine()) break;
            String w = sc.nextLine().trim();
            if (w.equalsIgnoreCase("quit") || w.equalsIgnoreCase("exit")) break;
            if (w.isEmpty()) { System.out.println(); continue; }
            boolean ok = dfa.accepts(w);
            System.out.println("  " + (ok ? GREEN + "✓" : RED + "✗") + RESET
                + "  \"" + w + "\"  →  "
                + BOLD + (ok ? GREEN + "ACCEPTED" : RED + "REJECTED") + RESET);
        }
        System.out.println();
        System.out.println(DIM + "  Done." + RESET);
    }

    // ── State renaming ────────────────────────────────────────────────────────

    /** Renamed DFA + ordered mapping new-label → original-name. */
    private record RenameResult(DFA dfa, LinkedHashMap<State, String> mapping) {}

    /**
     * Renames DFA states to prefix+0, prefix+1, … in insertion order,
     * returning the new DFA and a map from new-State → original-name.
     */
    private static RenameResult renameDFA(DFA src, String prefix) {
        Map<State, State> old2new = new LinkedHashMap<>();
        LinkedHashMap<State, String> mapping = new LinkedHashMap<>();
        int i = 0;
        for (State s : src.states()) {
            State fresh = new State(prefix + i++);
            old2new.put(s, fresh);
            mapping.put(fresh, s.name());
        }

        DFA.Builder b = new DFA.Builder();
        for (char c : src.alphabet()) b.addSymbol(c);
        for (State s : src.states()) b.addState(old2new.get(s));
        b.setStart(old2new.get(src.startState()));
        for (State s : src.acceptStates()) b.addAccept(old2new.get(s));
        for (State s : src.states()) {
            for (char c : src.alphabet()) {
                State nx = src.transition(s, c);
                if (nx != null) b.addTransition(old2new.get(s), c, old2new.get(nx));
            }
        }
        return new RenameResult(b.build(), mapping);
    }

    // ── Export ────────────────────────────────────────────────────────────────

    private static void exportAndRender(NFA nfa, DFA dfa, DFA minDfa) {
        record Ex(String label, Path dot) {}
        try {
            List<Ex> exports = List.of(
                new Ex("NFA",          Path.of("output", "nfa.dot")),
                new Ex("DFA",          Path.of("output", "dfa.dot")),
                new Ex("Minimized DFA", Path.of("output", "dfa_min.dot"))
            );
            GraphvizExporter.exportNFA(nfa,    exports.get(0).dot());
            GraphvizExporter.exportDFA(dfa,    exports.get(1).dot());
            GraphvizExporter.exportDFA(minDfa, exports.get(2).dot());

            boolean hasDot = GraphvizRunner.isAvailable();
            for (Ex e : exports) {
                System.out.println("  " + DIM + "[dot]" + RESET
                    + "  " + e.label() + "  →  " + e.dot());
                if (hasDot) {
                    Path png = e.dot().resolveSibling(
                        e.dot().getFileName().toString().replace(".dot", ".png"));
                    GraphvizRunner.render(e.dot(), png, "png");
                    System.out.println("  " + DIM + "[png]" + RESET
                        + "  " + e.label() + "  →  " + png);
                }
            }
            if (!hasDot)
                System.out.println("  " + DIM
                    + "(Install Graphviz to auto-render PNG files.)" + RESET);
        } catch (Exception e) {
            System.out.println("  " + RED + "[export error] " + e.getMessage() + RESET);
        }
        System.out.println();
    }

    // ── Console formatting helpers ────────────────────────────────────────────

    private static void banner() {
        String line1 = "  Automata Theory: Regex  →  NFA  →  DFA  →  Min-DFA  ";
        String line2 = "  Thompson's Construction · Subset Construction · Hopcroft  ";
        int w = Math.max(line1.length(), line2.length()) + 2;
        System.out.println();
        System.out.println(CYAN + BOLD + "╔" + "═".repeat(w) + "╗");
        System.out.println("║" + centre(line1, w) + "║");
        System.out.println("║" + centre(line2, w) + "║");
        System.out.println("╚" + "═".repeat(w) + "╝" + RESET);
        System.out.println();
    }

    private static void step(int n, String title) {
        int dashes = Math.max(2, WIDTH - 9 - title.length() - String.valueOf(n).length());
        System.out.println();
        System.out.println(BOLD + CYAN
            + "┌─ STEP " + n + ": " + title + " " + "─".repeat(dashes)
            + RESET);
        System.out.println();
    }

    private static void explain(String... lines) {
        for (String l : lines) System.out.println("  " + DIM + l + RESET);
        System.out.println();
    }

    private static void legend() {
        System.out.println("  " + DIM
            + "→  start state    *  accepting state    ∅  no transition" + RESET);
        System.out.println();
    }

    private static String statePrefix(boolean isStart, boolean isAccept) {
        return (isStart ? "→" : " ") + (isAccept ? "*" : " ") + " ";
    }

    private static String symLabel(char c) {
        return c == NFA.EPSILON ? "ε" : String.valueOf(c);
    }

    private static String formatAlphabet(Set<Character> alpha) {
        return alpha.stream().map(String::valueOf)
            .collect(Collectors.joining(", ", "{ ", " }"));
    }

    private static String centre(String s, int width) {
        int pad = width - s.length();
        int l = pad / 2, r = pad - l;
        return " ".repeat(Math.max(0, l)) + s + " ".repeat(Math.max(0, r));
    }

    private static String subscript(int n) {
        String[] sub = {"₀","₁","₂","₃","₄","₅","₆","₇","₈","₉"};
        StringBuilder sb = new StringBuilder();
        for (char c : String.valueOf(n).toCharArray())
            sb.append(c >= '0' && c <= '9' ? sub[c - '0'] : c);
        return sb.toString();
    }
}
