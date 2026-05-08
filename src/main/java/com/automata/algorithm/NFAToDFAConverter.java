package com.automata.algorithm;

import com.automata.model.*;
import java.util.*;
import java.util.stream.Collectors;

/** Converts an NFA to an equivalent DFA using the Subset Construction algorithm. */
public final class NFAToDFAConverter {

    /** Converts the given NFA to an equivalent DFA. */
    public static DFA convert(NFA nfa) {
        DFA.Builder builder = new DFA.Builder();
        for (char c : nfa.alphabet()) builder.addSymbol(c);

        Map<Set<State>, State> subsetToState = new LinkedHashMap<>();

        Set<State> startSubset = epsilonClosure(nfa, Set.of(nfa.startState()));
        State dfaStart = stateForSubset(startSubset);
        subsetToState.put(startSubset, dfaStart);
        builder.addState(dfaStart).setStart(dfaStart);
        if (!Collections.disjoint(startSubset, nfa.acceptStates())) builder.addAccept(dfaStart);

        Queue<Set<State>> worklist = new ArrayDeque<>();
        worklist.add(startSubset);

        while (!worklist.isEmpty()) {
            Set<State> subset = worklist.poll();
            State dfaState = subsetToState.get(subset);

            for (char c : nfa.alphabet()) {
                Set<State> moved = move(nfa, subset, c);
                Set<State> closure = epsilonClosure(nfa, moved);
                if (closure.isEmpty()) continue;

                if (!subsetToState.containsKey(closure)) {
                    State newState = stateForSubset(closure);
                    subsetToState.put(closure, newState);
                    builder.addState(newState);
                    if (!Collections.disjoint(closure, nfa.acceptStates())) builder.addAccept(newState);
                    worklist.add(closure);
                }
                builder.addTransition(dfaState, c, subsetToState.get(closure));
            }
        }

        return builder.build();
    }

    /** Computes the epsilon closure of a set of NFA states via BFS. */
    public static Set<State> epsilonClosure(NFA nfa, Set<State> states) {
        Set<State> closure = new LinkedHashSet<>(states);
        Deque<State> stack = new ArrayDeque<>(states);
        while (!stack.isEmpty()) {
            State s = stack.pop();
            for (State t : nfa.transitions(s, NFA.EPSILON)) {
                if (closure.add(t)) stack.push(t);
            }
        }
        return closure;
    }

    /** Returns states reachable from the subset on symbol c (before epsilon closure). */
    private static Set<State> move(NFA nfa, Set<State> subset, char c) {
        Set<State> result = new LinkedHashSet<>();
        for (State s : subset) result.addAll(nfa.transitions(s, c));
        return result;
    }

    /** Creates a DFA State whose name encodes the sorted subset of NFA state names. */
    private static State stateForSubset(Set<State> subset) {
        String name = "{" + subset.stream()
            .map(State::name)
            .sorted()
            .collect(Collectors.joining(",")) + "}";
        return new State(name);
    }

    /** Prints the subset construction table to stdout. */
    public static void printTable(NFA nfa, DFA dfa) {
        List<Character> symbols = new ArrayList<>(nfa.alphabet());

        System.out.println("── Subset Construction Table ─────────────────");
        System.out.printf("  %-22s", "DFA state");
        for (char c : symbols) System.out.printf("  %-15s", "δ(" + c + ")");
        System.out.println();
        System.out.println("  ─────────────────────────────────────────────");

        for (State s : dfa.states()) {
            boolean isStart  = s.equals(dfa.startState());
            boolean isAccept = dfa.acceptStates().contains(s);
            String prefix = (isStart ? "→" : " ") + (isAccept ? "*" : " ");
            System.out.printf("%s %-20s", prefix, s.name());
            for (char c : symbols) {
                State next = dfa.transition(s, c);
                System.out.printf("  %-15s", next != null ? next.name() : "∅");
            }
            System.out.println();
        }
        System.out.println();
    }
}
