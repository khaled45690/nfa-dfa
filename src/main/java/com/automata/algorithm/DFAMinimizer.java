package com.automata.algorithm;

import com.automata.model.*;
import java.util.*;
import java.util.stream.Collectors;

/** Minimizes a DFA using Hopcroft's algorithm. */
public final class DFAMinimizer {

    /** Returns a minimized DFA equivalent to the input DFA. */
    public static DFA minimize(DFA dfa) {
        Set<State> allStates  = dfa.states();
        Set<State> accepting  = dfa.acceptStates();
        Set<State> rejecting  = new LinkedHashSet<>(allStates);
        rejecting.removeAll(accepting);

        // Degenerate: all states equivalent
        if (accepting.isEmpty() || rejecting.isEmpty()) {
            return buildFromPartition(dfa, List.of(new LinkedHashSet<>(allStates)));
        }

        List<Set<State>> partition = new ArrayList<>();
        partition.add(new LinkedHashSet<>(accepting));
        partition.add(new LinkedHashSet<>(rejecting));

        // Worklist: track partition blocks to refine against
        Set<Set<State>> worklist = new LinkedHashSet<>();
        worklist.add(new LinkedHashSet<>(accepting));

        while (!worklist.isEmpty()) {
            Set<State> A = worklist.iterator().next();
            worklist.remove(A);

            for (char c : dfa.alphabet()) {
                // X = states whose c-transition lands inside A
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
                            // Add the smaller half to keep complexity O(n log n)
                            worklist.add(inter.size() <= diff.size() ? inter : diff);
                        }
                    } else {
                        next.add(Y);
                    }
                }
                partition = next;
            }
        }

        return buildFromPartition(dfa, partition);
    }

    /** Constructs a DFA from a given equivalence partition of the original DFA's states. */
    private static DFA buildFromPartition(DFA dfa, List<Set<State>> partition) {
        // Assign a representative State to each partition block
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

        // Use any state in each block to derive the outgoing transitions
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
