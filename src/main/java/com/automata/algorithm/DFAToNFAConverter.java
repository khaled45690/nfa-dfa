package com.automata.algorithm;

import com.automata.model.*;

/** Converts a DFA to an NFA (trivial wrap — every DFA is a degenerate NFA). */
public final class DFAToNFAConverter {

    /** Wraps the DFA as an equivalent NFA with identical transitions. */
    public static NFA convert(DFA dfa) {
        NFA.Builder builder = new NFA.Builder();
        for (State s : dfa.states()) builder.addState(s);
        for (char c : dfa.alphabet()) builder.addSymbol(c);
        builder.setStart(dfa.startState());
        for (State s : dfa.acceptStates()) builder.addAccept(s);
        for (var e : dfa.delta().entrySet()) {
            for (var ie : e.getValue().entrySet()) {
                builder.addTransition(e.getKey(), ie.getKey(), ie.getValue());
            }
        }
        return builder.build();
    }
}
