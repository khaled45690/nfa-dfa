package com.automata.parser;

import com.automata.model.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Parses an NFA definition from a plain-text file or a list of lines. */
public final class AutomatonParser {

    /** Parses an NFA from the file at filePath using the project's text format. */
    public static NFA parse(Path filePath) throws IOException {
        return parse(Files.readAllLines(filePath));
    }

    /** Parses an NFA from an already-read list of lines. */
    public static NFA parse(List<String> lines) {
        NFA.Builder builder = new NFA.Builder();
        Map<String, State> stateMap = new LinkedHashMap<>();
        boolean inTransitions = false;

        for (String rawLine : lines) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.startsWith("states:")) {
                inTransitions = false;
                for (String name : line.substring(7).trim().split("\\s+")) {
                    if (!name.isEmpty()) {
                        State s = new State(name);
                        stateMap.put(name, s);
                        builder.addState(s);
                    }
                }
            } else if (line.startsWith("alphabet:")) {
                inTransitions = false;
                for (String sym : line.substring(9).trim().split("\\s+")) {
                    if (!sym.isEmpty() && !sym.equals("eps")) builder.addSymbol(sym.charAt(0));
                }
            } else if (line.startsWith("start:")) {
                inTransitions = false;
                builder.setStart(stateMap.computeIfAbsent(line.substring(6).trim(), State::new));
            } else if (line.startsWith("accept:")) {
                inTransitions = false;
                for (String name : line.substring(7).trim().split("\\s+")) {
                    if (!name.isEmpty())
                        builder.addAccept(stateMap.computeIfAbsent(name, State::new));
                }
            } else if (line.equals("transitions:")) {
                inTransitions = true;
            } else if (inTransitions) {
                String[] parts = line.split("\\s+");
                if (parts.length < 3) continue;
                State from = stateMap.computeIfAbsent(parts[0], State::new);
                char c = parts[1].equals("eps") ? NFA.EPSILON : parts[1].charAt(0);
                for (int i = 2; i < parts.length; i++) {
                    builder.addTransition(from, c, stateMap.computeIfAbsent(parts[i], State::new));
                }
            }
        }
        return builder.build();
    }
}
