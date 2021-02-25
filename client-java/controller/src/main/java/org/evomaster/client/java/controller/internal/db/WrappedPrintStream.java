package org.evomaster.client.java.controller.internal.db;

import java.io.PrintStream;

public class WrappedPrintStream extends PrintStream {
    public WrappedPrintStream(StandardOutputTracker out, boolean autoFlush) {
        super(out, autoFlush);
    }

    public StandardOutputTracker getOut(){
        return (StandardOutputTracker) out;
    }
}
