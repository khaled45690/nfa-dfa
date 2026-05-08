package com.automata.model;

import java.util.*;
import java.util.stream.Collectors;

/** Non-deterministic Finite Automaton; epsilon transitions use NFA.EPSILON ('\0'). */
public final class NFA {

    /** Sentinel character for epsilon transitions. */
    public static final char EPSILON = '\0';

    private final Set<State> states;
    private final Set<Character> alphabet;
    private final State startState;
    private final Set<State> acceptStates;
    private final Map<State, Map<Character, Set<State>>> delta;

    private NFA(Builder b) {
        this.states = Collections.unmodifiableSet(new LinkedHashSet<>(b.states));
        this.alphabet = Collections.unmodifiableSet(new LinkedHashSet<>(b.alphabet));
        this.startState = b.startState;
        this.acceptStates = Collections.unmodifiableSet(new LinkedHashSet<>(b.acceptStates));
        Map<State, Map<Character, Set<State>>> d = new LinkedHashMap<>();
        for (var e : b.delta.entrySet()) {
            Map<Character, Set<State>> inner = new LinkedHashMap<>();
            for (var ie : e.getValue().entrySet()) {
                inner.put(ie.getKey(), Collections.unmodifiableSet(new LinkedHashSet<>(ie.getValue())));
            }
            d.put(e.getKey(), Collections.unmodifiableMap(inner));
        }
        this.delta = Collections.unmodifiableMap(d);
    }

    /** Returns all states in this NFA. */
    public Set<State> states() { return states; }

    /** Returns the input alphabet (does not include epsilon). */
    public Set<Character> alphabet() { return alphabet; }

    /** Returns the start state. */
    public State startState() { return startState; }

    /** Returns the set of accepting states. */
    public Set<State> acceptStates() { return acceptStates; }

    /** Returns the full transition function. */
    public Map<State, Map<Character, Set<State>>> delta() { return delta; }

    /** Returns states reachable from s on symbol c, or empty set if none defined. */
    public Set<State> transitions(State s, char c) {
        var inner = delta.get(s);
        if (inner == null) return Set.of();
        var result = inner.get(c);
        return result == null ? Set.of() : result;
    }

    /** Builder for NFA. */
    public static class Builder {
        private final Set<State> states = new LinkedHashSet<>();
        private final Set<Character> alphabet = new LinkedHashSet<>();
        private State startState;
        private final Set<State> acceptStates = new LinkedHashSet<>();
        private final Map<State, Map<Character, Set<State>>> delta = new LinkedHashMap<>();

        /** Adds a state to the NFA. */
        public Builder addState(State s) { states.add(s); return this; }

        /** Adds a symbol to the input alphabet. */
        public Builder addSymbol(char c) { alphabet.add(c); return this; }

        /** Sets the start state. */
        public Builder setStart(State s) { startState = s; return this; }

        /** Marks a state as accepting. */
        public Builder addAccept(State s) { acceptStates.add(s); return this; }

        /** Adds a transition from state 'from' on symbol c to state 'to'. */
        public Builder addTransition(State from, char c, State to) {
            delta.computeIfAbsent(from, k -> new LinkedHashMap<>())
                 .computeIfAbsent(c, k -> new LinkedHashSet<>())
                 .add(to);
            return this;
        }

        /** Builds the NFA; throws if no start state was set. */
        public NFA build() {
            Objects.requireNonNull(startState, "Start state must be set");
            return new NFA(this);
        }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("NFA\n");
        sb.append("  States:   ").append(formatStateSet(states)).append("\n");
        sb.append("  Alphabet: [").append(
            alphabet.stream().map(String::valueOf).collect(Collectors.joining(", "))
        ).append("]\n");
        sb.append("  Start:    ").append(startState).append("\n");
        sb.append("  Accept:   ").append(formatStateSet(acceptStates)).append("\n");
        sb.append("  Delta:\n");
        for (var e : delta.entrySet()) {
            for (var ie : e.getValue().entrySet()) {
                char c = ie.getKey();
                String sym = c == EPSILON ? "ε" : String.valueOf(c);
                sb.append("    δ(").append(e.getKey()).append(", ").append(sym)
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
