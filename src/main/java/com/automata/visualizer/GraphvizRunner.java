package com.automata.visualizer;

import java.io.*;
import java.nio.file.*;

/** Renders DOT files to images using the Graphviz dot CLI. */
public final class GraphvizRunner {

    /** Returns true if the dot CLI is on PATH; never throws. */
    public static boolean isAvailable() {
        try {
            new ProcessBuilder("dot", "-V").redirectErrorStream(true).start().waitFor();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Renders a DOT file to an output image; format should be "png" or "svg". */
    public static void render(Path dotFile, Path outputFile, String format)
            throws IOException, InterruptedException {
        var pb = new ProcessBuilder("dot", "-T" + format, dotFile.toString(),
                                    "-o", outputFile.toString());
        pb.redirectErrorStream(true);
        int exit = pb.start().waitFor();
        if (exit != 0) throw new IOException("dot exited with code " + exit);
    }
}
