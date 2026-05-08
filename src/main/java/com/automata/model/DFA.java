package com.automata.model;

import java.util.*;
import java.util.stream.Collectors;

/** Deterministic Finite Automaton. */
public final class DFA {

    private final Set<State> states;
    private final Set<Character> alphabet;
    private final State startState;
    private final Set<State> acceptStates;
    private final Map<State, Map<Character, State>> delta;

    private DFA(Builder b) {
        this.states = Collections.unmodifiableSet(new LinkedHashSet<>(b.states));
        this.alphabet = Collections.unmodifiableSet(new LinkedHashSet<>(b.alphabet));
        this.startState = b.startState;
        this.acceptStates = Collections.unmodifiableSet(new LinkedHashSet<>(b.acceptStates));
        Map<State, Map<Character, State>> d = new LinkedHashMap<>();
        for (var e : b.delta.entrySet()) {
            d.put(e.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(e.getValue())));
        }
        this.delta = Collections.unmodifiableMap(d);
    }

    /** Returns all states. */
    public Set<State> states() { return states; }

    /** Returns the input alphabet. */
    public Set<Character> alphabet() { return alphabet; }

    /** Returns the start state. */
    public State startState() { return startState; }

    /** Returns accepting states. */
    public Set<State> acceptStates() { return acceptStates; }

    /** Returns the full transition function. */
    public Map<State, Map<Character, State>> delta() { return delta; }

    /** Returns the next state from s on c, or null if no transition is defined. */
    public State transition(State s, char c) {
        var inner = delta.get(s);
        if (inner == null) return null;
        return inner.get(c);
    }

    /** Returns true if the DFA accepts string w. */
    public boolean accepts(String w) {
        State current = startState;
        for (char c : w.toCharArray()) {
            current = transition(current, c);
            if (current == null) return false;
        }
        return acceptStates.contains(current);
    }

    /** Builder for DFA. */
    public static class Builder {
        private final Set<State> states = new LinkedHashSet<>();
        private final Set<Character> alphabet = new LinkedHashSet<>();
        private State startState;
        private final Set<State> acceptStates = new LinkedHashSet<>();
        private final Map<State, Map<Character, State>> delta = new LinkedHashMap<>();

        /** Adds a state. */
        public Builder addState(State s) { states.add(s); return this; }

        /** Adds a symbol to the alphabet. */
        public Builder addSymbol(char c) { alphabet.add(c); return this; }

        /** Sets the start state. */
        public Builder setStart(State s) { startState = s; return this; }

        /** Marks a state as accepting. */
        public Builder addAccept(State s) { acceptStates.add(s); return this; }

        /** Adds a deterministic transition from state 'from' on symbol c to state 'to'. */
        public Builder addTransition(State from, char c, State to) {
            delta.computeIfAbsent(from, k -> new LinkedHashMap<>()).put(c, to);
            return this;
        }

        /** Builds the DFA; throws if no start state was set. */
        public DFA build() {
            Objects.requireNonNull(startState, "Start state must be set");
            return new DFA(this);
        }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("DFA\n");
        sb.append("  States:   ").append(formatStateSet(states)).append("\n");
        sb.append("  Alphabet: [").append(
            alphabet.stream().map(String::valueOf).collect(Collectors.joining(", "))
        ).append("]\n");
        sb.append("  Start:    ").append(startState).append("\n");
        sb.append("  Accept:   ").append(formatStateSet(acceptStates)).append("\n");
        sb.append("  Delta:\n");
        for (var e : delta.entrySet()) {
            for (var ie : e.getValue().entrySet()) {
                sb.append("    δ(").append(e.getKey()).append(", ").append(ie.getKey())
                  .append(") = ").append(ie.getValue()).append("\n");
            }
        }
        return sb.toString();
    }

    private String formatStateSet(Set<State> s) {
        return "[" + s.stream()
            .map(st -> st.name() + (acceptStates.contains(st) ? "*" : ""))
            .collect(Collectors.joining(", ")) + "]";
    }
}
