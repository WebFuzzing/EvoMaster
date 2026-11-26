package org.evomaster.client.java.controller;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Check startup signals when an old process finishes while a replacement starts. */
class ExternalSutControllerTest {
    private static class BlockedOutput extends InputStream {
        final CountDownLatch reading = new CountDownLatch(1);
        final CountDownLatch finish = new CountDownLatch(1);

        @Override
        public int read() throws IOException {
            reading.countDown();
            try {
                if (!finish.await(5, TimeUnit.SECONDS)) {
                    throw new IOException("Test did not release the old process output");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
            return -1;
        }
    }

    private void set(ExternalSutController controller, String field, Object value) throws Exception {
        Field f = ExternalSutController.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(controller, value);
    }

    private Thread printer(ExternalSutController controller) throws Exception {
        Field f = ExternalSutController.class.getDeclaredField("outputPrinter");
        f.setAccessible(true);
        return (Thread) f.get(controller);
    }

    private Process process(InputStream output) {
        Process process = mock(Process.class);
        when(process.getInputStream()).thenReturn(output);
        when(process.isAlive()).thenReturn(true);
        return process;
    }

    @Test
    void oldOutputCannotCompleteTheNextStartup() throws Exception {
        ExternalSutController controller = mock(ExternalSutController.class, CALLS_REAL_METHODS);
        when(controller.getLogMessageOfInitializedServer()).thenReturn("READY");
        BlockedOutput output = new BlockedOutput();
        controller.process = process(output);
        set(controller, "latch", new CountDownLatch(1));
        controller.startExternalProcessPrinter(true);
        Thread oldPrinter = printer(controller);
        CountDownLatch nextStartup = new CountDownLatch(1);
        try {
            assertTrue(output.reading.await(2, TimeUnit.SECONDS));
            controller.process = process(new ByteArrayInputStream(new byte[0]));
            set(controller, "latch", nextStartup);
            output.finish.countDown();
            oldPrinter.join(2000);
            assertFalse(oldPrinter.isAlive());
            assertEquals(1, nextStartup.getCount(), "Old EOF must not release a new startup");
        } finally {
            output.finish.countDown();
            oldPrinter.join(2000);
        }
    }

    @Test
    void nextProcessGetsAPrinterWhileThePreviousOneIsStillExiting() throws Exception {
        ExternalSutController controller = mock(ExternalSutController.class, CALLS_REAL_METHODS);
        when(controller.getLogMessageOfInitializedServer()).thenReturn("READY");
        BlockedOutput output = new BlockedOutput();
        controller.process = process(output);
        set(controller, "latch", new CountDownLatch(1));
        controller.startExternalProcessPrinter(true);
        Thread oldPrinter = printer(controller);
        try {
            assertTrue(output.reading.await(2, TimeUnit.SECONDS));
            controller.process = process(new ByteArrayInputStream("READY\n".getBytes(StandardCharsets.UTF_8)));
            CountDownLatch nextStartup = new CountDownLatch(1);
            set(controller, "latch", nextStartup);
            controller.startExternalProcessPrinter(true);
            assertNotSame(oldPrinter, printer(controller));
            assertTrue(nextStartup.await(2, TimeUnit.SECONDS));
            printer(controller).join(2000);
        } finally {
            output.finish.countDown();
            oldPrinter.join(2000);
        }
    }
}
