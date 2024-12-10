package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.utils.SimpleLogger;
//import org.evomaster.client.java.controller.internal.SutController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * We use p6spy to intercept SQL commands.
 * That can be done in 3 ways:
 * (1) to file,
 * (2) with a logger,
 * (3) to standard output.
 *
 * <p>
 * (1) is a recipe for disaster: handling generated files, and file locks on Windows.
 * (2) is a nightmare as well, as who knows what the SUT is configuring.
 * (3) is likely the cleanest, easiest option. But it means we need to
 * handle System.out
 *
 * <p>
 * This class can be used for any analyses of the SUT output
 */
@Deprecated // we are now using testability transformations
public class StandardOutputTracker extends ByteArrayOutputStream{

//    private static final PrintStream DEFAULT_OUT = System.out;
//
//    private volatile SutController sutController;
//
//    private final PrintStream printStream;
//
//    public static void setTracker(boolean on, SutController sutController){
//        if(on){
//            System.setOut(new WrappedPrintStream(new StandardOutputTracker(sutController), true));
//        } else {
//            System.setOut(DEFAULT_OUT);
//        }
//    }
//
//    private StandardOutputTracker(SutController sutController) {
//        this(sutController, null);
//    }
//
//    protected StandardOutputTracker(SutController sutController, PrintStream printStream) {
//        super(2048);
//        this.printStream = printStream;
//        this.sutController = sutController;
//    }
//
//    @Override
//    public void flush() throws IOException {
//
//        /*
//            Output is written to a buffer.
//            Every time it is flushed, we do the actual printing
//            on standard output, and analyze its content.
//         */
//
//        String data;
//
//        synchronized (this) {
//            super.flush();
//
//            data = toString(); //get content of the buffer
//            reset();
//
//            getOut().print(data);
//            getOut().flush();
//        }
//
//        if (data != null) {
//            Arrays.stream(data.split("\n"))
//                    //.filter(l -> l.startsWith(P6SpyFormatter.PREFIX))
//                    .forEach(l -> {
//                        handleSqlLine(sutController, l);
//                    });
//        }
//    }
//
//    public static void handleSqlLine(SutController sc, String line){
//        Objects.requireNonNull(sc);
//        Objects.requireNonNull(line);
//
////        if(! line.startsWith(P6SpyFormatter.PREFIX)){
////            throw new IllegalArgumentException("No P6Spy prefix");
////        }
//
//        String sql = line; //line.substring(P6SpyFormatter.PREFIX.length());
//
//        try {
//            sc.handleSql(sql);
//        } catch (Exception | Error e){
//            SimpleLogger.error("Failed to handle SQL command: '"+sql+"'\n" + e.getMessage());
//        }
//    }
//
//    private PrintStream getOut(){
//        if (printStream == null) return DEFAULT_OUT;
//        return printStream;
//    }
//
//    public StandardOutputTracker copyWithPrintStream(PrintStream printStream){
//        return new StandardOutputTracker(sutController, printStream);
//    }
}
