package org.evomaster.client.java.sql.internal;

import java.io.PrintStream;

@Deprecated
public class WrappedPrintStream extends PrintStream {

    // this configuration cannot be accessed
    private final boolean flushSetting;

    public WrappedPrintStream(StandardOutputTracker out, boolean autoFlush) {
        super(out, autoFlush);
        flushSetting = autoFlush;
    }
//
//    private StandardOutputTracker getOut(){
//        return (StandardOutputTracker) out;
//    }
//
//    public WrappedPrintStream copyWithRestPrintStream(PrintStream printStream){
//        return new WrappedPrintStream(getOut().copyWithPrintStream(printStream), flushSetting);
//    }
}
