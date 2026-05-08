package com.automata.model;

import java.util.Objects;

/** Immutable value type representing a single automaton state. */
public record State(String name) implements Comparable<State> {

    /** Constructs a State, requiring a non-null name. */
    public State {
        Objects.requireNonNull(name, "State name must not be null");
    }

    @Override
    public int compareTo(State other) {
        return this.name.compareTo(other.name);
    }

    @Override
    public String toString() {
        return name;
    }
}
