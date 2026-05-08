package com.automata.algorithm;

import com.automata.model.*;
import java.util.*;

/**
 * Converts a regular expression to an NFA using Thompson's construction.
 *
 * Supported syntax:
 *   c       — literal character
 *   r|s     — union / alternation
 *   rs      — concatenation (implicit)
 *   r*      — Kleene star (zero or more)
 *   r+      — one or more
 *   r?      — optional (zero or one)
 *   (r)     — grouping
 *   \c      — escaped literal (use \\ \| \* \+ \? \( \) inside the regex string)
 *
 * Grammar (in order of increasing precedence):
 *   expr   → term ('|' term)*
 *   term   → factor+
 *   factor → atom ('*' | '+' | '?')?
 *   atom   → CHAR | '(' expr ')'
 */
public final class RegexToNFAConverter {

    private final String regex;
    private int pos;
    private int counter;
    private final NFA.Builder builder = new NFA.Builder();
    private final Set<Character> alphabet = new LinkedHashSet<>();

    private RegexToNFAConverter(String regex) {
        this.regex = regex;
    }

    /** Converts the given regular expression to an equivalent NFA. */
    public static NFA convert(String regex) {
        if (regex == null || regex.isEmpty())
            throw new IllegalArgumentException("Regular expression must not be empty");
        RegexToNFAConverter c = new RegexToNFAConverter(regex);
        return c.run();
    }

    private NFA run() {
        Fragment f = parseExpr();
        if (pos < regex.length())
            throw new IllegalArgumentException(
                "Unexpected character '" + regex.charAt(pos) + "' at position " + pos);
        for (char c : alphabet) builder.addSymbol(c);
        builder.setStart(f.start).addAccept(f.accept);
        return builder.build();
    }

    /** A sub-NFA fragment: one entry (start) and one exit (accept) state. */
    private record Fragment(State start, State accept) {}

    private State fresh() {
        State s = new State("s" + counter++);
        builder.addState(s);
        return s;
    }

    private void eps(State from, State to) {
        builder.addTransition(from, NFA.EPSILON, to);
    }

    // ── expr → term ('|' term)* ───────────────────────────────────────────────

    private Fragment parseExpr() {
        Fragment left = parseTerm();
        while (pos < regex.length() && regex.charAt(pos) == '|') {
            pos++;
            Fragment right = parseTerm();
            // New start with ε to both, both accepts → new accept with ε
            State s = fresh(), a = fresh();
            eps(s, left.start);  eps(s, right.start);
            eps(left.accept, a); eps(right.accept, a);
            left = new Fragment(s, a);
        }
        return left;
    }

    // ── term → factor+ (concatenation) ───────────────────────────────────────

    private Fragment parseTerm() {
        // Empty term (e.g. right-hand side of "a|") → ε-only NFA fragment
        if (pos >= regex.length() || regex.charAt(pos) == ')' || regex.charAt(pos) == '|') {
            State s = fresh(), a = fresh();
            eps(s, a);
            return new Fragment(s, a);
        }
        Fragment result = parseFactor();
        while (pos < regex.length()
                && regex.charAt(pos) != ')'
                && regex.charAt(pos) != '|') {
            Fragment next = parseFactor();
            // Chain: result.accept →ε→ next.start
            eps(result.accept, next.start);
            result = new Fragment(result.start, next.accept);
        }
        return result;
    }

    // ── factor → atom quantifier? ─────────────────────────────────────────────

    private Fragment parseFactor() {
        Fragment atom = parseAtom();
        if (pos < regex.length()) {
            char op = regex.charAt(pos);
            if (op == '*') { pos++; return star(atom); }
            if (op == '+') { pos++; return plus(atom); }
            if (op == '?') { pos++; return opt(atom);  }
        }
        return atom;
    }

    // ── atom → '(' expr ')' | '\' CHAR | CHAR ────────────────────────────────

    private Fragment parseAtom() {
        if (pos >= regex.length())
            throw new IllegalArgumentException("Unexpected end of regular expression");
        char c = regex.charAt(pos);
        if (c == '(') {
            pos++;
            Fragment inner = parseExpr();
            if (pos >= regex.length() || regex.charAt(pos) != ')')
                throw new IllegalArgumentException("Missing closing ')' in regex");
            pos++;
            return inner;
        }
        if (c == '\\') {
            if (pos + 1 >= regex.length())
                throw new IllegalArgumentException("Trailing backslash in regex");
            pos += 2;
            return literal(regex.charAt(pos - 1));
        }
        pos++;
        return literal(c);
    }

    // ── Fragment constructors ─────────────────────────────────────────────────

    /** Single-character NFA: s0 --c--> s1 */
    private Fragment literal(char c) {
        alphabet.add(c);
        State s = fresh(), a = fresh();
        builder.addTransition(s, c, a);
        return new Fragment(s, a);
    }

    /** r*  — zero or more: adds bypass and loop ε-transitions. */
    private Fragment star(Fragment f) {
        State s = fresh(), a = fresh();
        eps(s, f.start);  eps(s, a);        // can bypass entirely
        eps(f.accept, f.start);             // can loop back
        eps(f.accept, a);                   // can exit loop
        return new Fragment(s, a);
    }

    /** r+  — one or more: first pass is mandatory, then loop. */
    private Fragment plus(Fragment f) {
        State loop = fresh(), a = fresh();
        eps(f.accept, loop);
        eps(loop, f.start);                 // loop back
        eps(loop, a);                       // exit loop
        return new Fragment(f.start, a);
    }

    /** r?  — zero or one: can bypass or go through once. */
    private Fragment opt(Fragment f) {
        State s = fresh(), a = fresh();
        eps(s, f.start); eps(s, a);         // can bypass
        eps(f.accept, a);                   // exit after one
        return new Fragment(s, a);
    }
}
