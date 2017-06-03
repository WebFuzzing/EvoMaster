package org.evomaster.clientJava.controller.internal.db;

import org.evomaster.clientJava.controller.internal.SutController;
import org.evomaster.clientJava.instrumentation.db.P6SpyFormatter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * We use p6spy to intercept SQL commands.
 * That can be done in 3 ways:
 * (1) to file,
 * (2) with a logger,
 * (3) to standard output.
 * <p>
 * (1) is a recipe for disaster: handling generated files, and file locks on Windows.
 * (2) is a nightmare as well, as who knows what the SUT is configuring.
 * (3) is likely the cleanest, easiest option. But it means we need to
 * handle System.out
 *
 * <br />
 *
 * This class can be used for any analyses of the SUT output
 */
public class StandardOutputTracker extends PrintStream {

    private static final PrintStream DEFAULT_OUT = System.out;

    private static final ByteArrayOutputStream byteStream = new ByteArrayOutputStream(2048);

    private volatile SutController sutController;


    public static void setTracker(boolean on, SutController sutController){
        if(on){
            System.setOut(new StandardOutputTracker(sutController));
        } else {
            System.setOut(DEFAULT_OUT);
        }
    }

    private StandardOutputTracker(SutController sutController) {
        super(new PrintStream(byteStream), true);
    }

    @Override
    public void flush() {

        /*
            Output is written to a buffer.
            Every time it is flushed, we do the actual printing
            on standard output, and analyze its content.
         */

        String data;

        synchronized (this) {
            super.flush();

            data = byteStream.toString();
            byteStream.reset();

            DEFAULT_OUT.print(data);
        }

        if (data != null) {
            Arrays.stream(data.split("\n"))
                    .filter(l -> l.startsWith(P6SpyFormatter.PREFIX))
                    .forEach(l -> {
                        String sql = l.substring(P6SpyFormatter.PREFIX.length());
                        sutController.handleSql(sql);
                    });
        }
    }
}
