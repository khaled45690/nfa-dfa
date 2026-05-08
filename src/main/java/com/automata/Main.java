package com.automata;

import com.automata.algorithm.*;
import com.automata.model.*;
import com.automata.visualizer.*;
import java.nio.file.*;
import java.util.Scanner;

/** Entry point: converts the built-in NFA to DFA, then back to NFA, and tests a string. */
public final class Main {

    public static void main(String[] args) {
        // ── Step 1: build the NFA for (a|b)*abb ──────────────────────────────
        State q0 = new State("q0"), q1 = new State("q1"),
              q2 = new State("q2"), q3 = new State("q3");

        NFA nfa = new NFA.Builder()
            .addState(q0).addState(q1).addState(q2).addState(q3)
            .addSymbol('a').addSymbol('b')
            .setStart(q0).addAccept(q3)
            .addTransition(q0, 'a', q0).addTransition(q0, 'a', q1)
            .addTransition(q0, 'b', q0)
            .addTransition(q1, 'b', q2)
            .addTransition(q2, 'b', q3)
            .build();

        System.out.println("═══════════════════════════════════════════════");
        System.out.println("  NFA ↔ DFA Converter");
        System.out.println("  Language: (a|b)*abb");
        System.out.println("═══════════════════════════════════════════════\n");

        // ── Step 2: show the NFA ──────────────────────────────────────────────
        System.out.println("── Original NFA ─────────────────────────────");
        System.out.println(nfa);

        // ── Step 3: convert NFA → DFA ─────────────────────────────────────────
        DFA dfa = NFAToDFAConverter.convert(nfa);
        System.out.println("── NFA → DFA (Subset Construction) ──────────");
        NFAToDFAConverter.printTable(nfa, dfa);
        System.out.println(dfa);

        // ── Step 4: convert DFA → NFA ─────────────────────────────────────────
        NFA nfaFromDfa = DFAToNFAConverter.convert(dfa);
        System.out.println("── DFA → NFA (trivial wrap) ──────────────────");
        System.out.println(nfaFromDfa);

        // ── Step 5: export DOT / PNG ──────────────────────────────────────────
        exportAndRender(nfa, dfa);

        // ── Step 6: test a string ─────────────────────────────────────────────
        System.out.print("Enter a string to test (e.g. aabbaabbb): ");
        try (Scanner scanner = new Scanner(System.in)) {
            String input = scanner.nextLine().trim();
            System.out.println("\n\"" + input + "\" → " + (dfa.accepts(input) ? "ACCEPTED" : "REJECTED"));
        }
    }

    private static void exportAndRender(NFA nfa, DFA dfa) {
        try {
            Path nfaDot = Path.of("output", "nfa.dot");
            GraphvizExporter.exportNFA(nfa, nfaDot);
            System.out.println("  [dot] Written: " + nfaDot);
            if (GraphvizRunner.isAvailable()) {
                Path nfaPng = Path.of("output", "nfa.png");
                GraphvizRunner.render(nfaDot, nfaPng, "png");
                System.out.println("  [graphviz] Rendered: " + nfaPng);
            }
            Path dfaDot = Path.of("output", "dfa.dot");
            GraphvizExporter.exportDFA(dfa, dfaDot);
            System.out.println("  [dot] Written: " + dfaDot);
            if (GraphvizRunner.isAvailable()) {
                Path dfaPng = Path.of("output", "dfa.png");
                GraphvizRunner.render(dfaDot, dfaPng, "png");
                System.out.println("  [graphviz] Rendered: " + dfaPng);
            }
        } catch (Exception e) {
            System.out.println("  [dot] Error: " + e.getMessage());
        }
    }
}
