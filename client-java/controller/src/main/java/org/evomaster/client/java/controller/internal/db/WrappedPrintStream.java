package org.evomaster.client.java.controller.internal.db;

import java.io.PrintStream;

public class WrappedPrintStream extends PrintStream {

    // this configuration cannot be accessed
    private final boolean flushSetting;

    public WrappedPrintStream(StandardOutputTracker out, boolean autoFlush) {
        super(out, autoFlush);
        flushSetting = autoFlush;
    }

    private StandardOutputTracker getOut(){
        return (StandardOutputTracker) out;
    }

    public WrappedPrintStream copyWithRestPrintStream(PrintStream printStream){
        return new WrappedPrintStream(getOut().copyWithPrintStream(printStream), flushSetting);
    }
}
