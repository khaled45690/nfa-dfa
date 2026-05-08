package com.automata.algorithm;

import com.automata.model.*;
import java.util.*;
import java.util.stream.Collectors;

/** Minimizes a DFA using Hopcroft's algorithm. */
public final class DFAMinimizer {

    /**
     * Holds the minimized DFA and the sequence of partition snapshots captured
     * during Hopcroft's algorithm (one entry per step where the partition changed).
     */
    public record MinimizationResult(DFA minimized, List<List<Set<State>>> partitionSteps) {}

    /** Returns a minimized DFA equivalent to the input. */
    public static DFA minimize(DFA dfa) {
        return minimizeWithSteps(dfa).minimized();
    }

    /**
     * Minimizes the DFA and records each partition state whenever it changes.
     * Use {@link MinimizationResult#partitionSteps()} to inspect the refinement history.
     */
    public static MinimizationResult minimizeWithSteps(DFA dfa) {
        Set<State> allStates = dfa.states();
        Set<State> accepting = dfa.acceptStates();
        Set<State> rejecting = new LinkedHashSet<>(allStates);
        rejecting.removeAll(accepting);

        List<List<Set<State>>> history = new ArrayList<>();

        // Degenerate: all states are equivalent
        if (accepting.isEmpty() || rejecting.isEmpty()) {
            List<Set<State>> single = List.of(new LinkedHashSet<>(allStates));
            history.add(copyPartition(single));
            return new MinimizationResult(buildFromPartition(dfa, single), history);
        }

        List<Set<State>> partition = new ArrayList<>();
        partition.add(new LinkedHashSet<>(accepting));
        partition.add(new LinkedHashSet<>(rejecting));
        history.add(copyPartition(partition));   // step 0 — initial partition

        Set<Set<State>> worklist = new LinkedHashSet<>();
        worklist.add(new LinkedHashSet<>(accepting));

        while (!worklist.isEmpty()) {
            Set<State> A = worklist.iterator().next();
            worklist.remove(A);

            boolean changed = false;

            for (char c : dfa.alphabet()) {
                // X = set of states whose c-transition lands in A
                Set<State> X = new LinkedHashSet<>();
                for (State s : allStates) {
                    State next = dfa.transition(s, c);
                    if (next != null && A.contains(next)) X.add(s);
                }
                if (X.isEmpty()) continue;

                List<Set<State>> next = new ArrayList<>();
                for (Set<State> Y : partition) {
                    Set<State> inter = new LinkedHashSet<>(Y);
                    inter.retainAll(X);
                    Set<State> diff = new LinkedHashSet<>(Y);
                    diff.removeAll(X);

                    if (!inter.isEmpty() && !diff.isEmpty()) {
                        next.add(inter);
                        next.add(diff);
                        if (worklist.contains(Y)) {
                            worklist.remove(Y);
                            worklist.add(inter);
                            worklist.add(diff);
                        } else {
                            // Add the smaller half — keeps complexity O(n log n)
                            worklist.add(inter.size() <= diff.size() ? inter : diff);
                        }
                        changed = true;
                    } else {
                        next.add(Y);
                    }
                }
                partition = next;
            }

            if (changed) history.add(copyPartition(partition));
        }

        return new MinimizationResult(buildFromPartition(dfa, partition), history);
    }

    /** Deep-copies a partition list so snapshots are immutable. */
    private static List<Set<State>> copyPartition(List<Set<State>> p) {
        List<Set<State>> copy = new ArrayList<>();
        for (Set<State> block : p) copy.add(new LinkedHashSet<>(block));
        return Collections.unmodifiableList(copy);
    }

    /** Constructs the minimized DFA from a given equivalence partition. */
    private static DFA buildFromPartition(DFA dfa, List<Set<State>> partition) {
        Map<State, State> stateToRep = new LinkedHashMap<>();
        Map<Set<State>, State> blockToRep = new LinkedHashMap<>();

        for (Set<State> block : partition) {
            String name = "{" + block.stream()
                .map(State::name).sorted().collect(Collectors.joining(",")) + "}";
            State rep = new State(name);
            blockToRep.put(block, rep);
            for (State s : block) stateToRep.put(s, rep);
        }

        DFA.Builder builder = new DFA.Builder();
        for (char c : dfa.alphabet()) builder.addSymbol(c);

        Set<State> added = new LinkedHashSet<>();
        for (Set<State> block : partition) {
            State rep = blockToRep.get(block);
            if (added.add(rep)) builder.addState(rep);
            for (State s : block) {
                if (dfa.acceptStates().contains(s)) { builder.addAccept(rep); break; }
            }
        }
        builder.setStart(stateToRep.get(dfa.startState()));

        Set<State> processed = new LinkedHashSet<>();
        for (Set<State> block : partition) {
            State rep = blockToRep.get(block);
            if (!processed.add(rep)) continue;
            State any = block.iterator().next();
            for (char c : dfa.alphabet()) {
                State next = dfa.transition(any, c);
                if (next != null) builder.addTransition(rep, c, stateToRep.get(next));
            }
        }

        return builder.build();
    }
}
