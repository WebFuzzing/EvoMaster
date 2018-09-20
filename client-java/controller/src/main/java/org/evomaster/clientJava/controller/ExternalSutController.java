package org.evomaster.clientJava.controller;

import org.evomaster.clientJava.clientUtil.SimpleLogger;
import org.evomaster.clientJava.controller.internal.SutController;
import org.evomaster.clientJava.controller.internal.db.StandardOutputTracker;
import org.evomaster.clientJava.databasespy.P6SpyFormatter;
import org.evomaster.clientJava.instrumentation.InstrumentingAgent;
import org.evomaster.clientJava.instrumentation.TargetInfo;
import org.evomaster.clientJava.instrumentation.external.JarAgentLocator;
import org.evomaster.clientJava.instrumentation.external.ServerController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class ExternalSutController extends SutController {

    /**
     * System property to avoid printing the console output of the SUT.
     */
    public static final String PROP_MUTE_SUT = "em.muteSUT";

    protected volatile Process process;

    private volatile boolean instrumentation;
    private volatile Thread processKillHook;
    private volatile Thread outputPrinter;
    private volatile CountDownLatch latch;
    private volatile ServerController serverController;
    private volatile boolean initialized;

    /*
        If SUT output is mutated, but SUT fails to start, we
        still want to print it for debugging
     */
    private volatile StringBuffer errorBuffer;



    public void setInstrumentation(boolean instrumentation) {
        this.instrumentation = instrumentation;
    }

    /**
     * @return the input parameters with which the system under test
     * should be started
     */
    public abstract String[] getInputParameters();

    /**
     * @return the JVM parameters (eg -X and -D) with which the system
     * under test should be started
     */
    public abstract String[] getJVMParameters();

    /**
     * @return the base URL of the running SUT, eg "http://localhost:8080".
     * Note: this value will likely depend on how getInputParameters() has
     * been implemented
     */
    public abstract String getBaseURL();


    /**
     * @return a String representing either a relative or absolute path
     * to the where the JAR of the system under test is located
     */
    public abstract String getPathToExecutableJar();


    /**
     * @return a string subtext that should be present in the logs (std output)
     * of the system under test to check if the server is up and ready.
     */
    public abstract String getLogMessageOfInitializedServer();

    /**
     * How long (in seconds) we should wait at most to check if SUT is ready
     * and initialized (this related to the getLogMessageOfInitializedServer() method)
     *
     * @return
     */
    public abstract long getMaxAwaitForInitializationInSeconds();

    /**
     * If the SUT needs some third-party processes (eg a non-embedded database),
     * here they can be configured and started.
     * This method is going to be called before we start the SUT.
     */
    public abstract void preStart();


    /**
     * This method is going to be called after the SUT is started.
     */
    public abstract void postStart();


    /**
     * This method is going to be called before the SUT is stopped.
     */
    public abstract void preStop();


    /**
     * If the SUT needs some third-party processes (eg a non-embedded database),
     * here we can shut them down once the SUT has been stopped.
     */
    public abstract void postStop();

    //-------------------------------------------------------------

    @Override
    public String startSut() {

        SimpleLogger.info("Going to start the SUT");

        initialized = false;

        validateJarPath();

        preStart();

        /*
            the following thread is important to make sure that the external process is killed
            when current process ends
        */
        processKillHook = new Thread(() -> killProcess());
        Runtime.getRuntime().addShutdownHook(processKillHook);

        //we need a mechanism to wait until the SUT is ready
        latch = new CountDownLatch(1);


        List<String> command = new ArrayList<>();
        command.add("java");


        if (instrumentation) {
            if (serverController == null) {
                serverController = new ServerController();
            }
            int port = serverController.startServer();
            command.add("-D" + InstrumentingAgent.EXTERNAL_PORT_PROP + "=" + port);

            String driver = getDatabaseDriverName();
            if (driver != null && !driver.isEmpty()) {
                command.add("-D" + InstrumentingAgent.SQL_DRIVER + "=" + driver);
            }

            String jarPath = JarAgentLocator.getAgentJarPath();
            if (jarPath == null) {
                throw new IllegalStateException("Cannot locate JAR file with EvoMaster Java Agent");
            }
            command.add("-javaagent:" + jarPath + "=" + getPackagePrefixesToCover());
        }

        for (String s : getJVMParameters()) {
            if (s != null) {
                String token = s.trim();
                if (!token.isEmpty()) {
                    command.add(token);
                }
            }
        }

        if (command.stream().noneMatch(s -> s.startsWith("-Xmx"))) {
            command.add("-Xmx2G");
        }
        if (command.stream().noneMatch(s -> s.startsWith("-Xms"))) {
            command.add("-Xms1G");
        }

        command.add("-jar");
        command.add(getPathToExecutableJar());

        for (String s : getInputParameters()) {
            if (s != null) {
                String token = s.trim();
                if (!token.isEmpty()) {
                    command.add(token);
                }
            }
        }

        SimpleLogger.info("Going to start SUT with command:\n" + String.join(" ", command));

        // now start the process
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        try {
            process = builder.start();
        } catch (IOException e) {
            SimpleLogger.error("Failed to start external process", e);
            return null;
        }

        //this is not only needed for debugging, but also to check for when SUT is ready
        startExternalProcessPrinter();

        if (instrumentation && serverController != null) {
            boolean connected = serverController.waitForIncomingConnection();
            if (!connected) {
                SimpleLogger.error("Could not establish connection to retrieve code metrics");
                return null;
            }
        }

        //need to block until server is ready
        long timeout = getMaxAwaitForInitializationInSeconds();
        boolean completed;

        try {
            completed = latch.await(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            SimpleLogger.error("Interrupted controller");
            stopSut();
            return null;
        }

        if(! completed){
            SimpleLogger.error("SUT has not started properly within " + timeout + " seconds");
            if(errorBuffer != null) {
                SimpleLogger.error("SUT output:\n" + errorBuffer.toString());
            }
            stopSut();
            return null;
        }

        if (!isSutRunning()) {
            SimpleLogger.error("SUT started but then terminated. Likely a possible misconfiguration");
            if(errorBuffer != null) {
                SimpleLogger.error("SUT output:\n" + errorBuffer.toString());
            }
            //note: actual process might still be running due to Java Agent we started
            stopSut();
            return null;
        }

        if (!initialized) {
            //this could happen if SUT is hanging for some reason
            SimpleLogger.error("SUT is started but not initialized");
            if(errorBuffer != null) {
                SimpleLogger.error("SUT output:\n" + errorBuffer.toString());
            }
            //note: actual process might still be running due to Java Agent we started
            stopSut();
            return null;
        }

        postStart();

        return getBaseURL();
    }

    @Override
    public boolean isSutRunning() {
        return process != null && process.isAlive();
    }


    @Override
    public void stopSut() {

        SimpleLogger.info("Going to stop the SUT");

        preStop();

        if (serverController != null) {
            serverController.closeServer();
        }
        killProcess();
        initialized = false;

        postStop();
    }

    @Override
    public final boolean isInstrumentationActivated() {
        return instrumentation && serverController != null && serverController.isConnectionOn();
    }

    @Override
    public final void newSearch() {
        if (isInstrumentationActivated()) {
            serverController.resetForNewSearch();
        }
    }

    @Override
    public final void newTest() {
        if (isInstrumentationActivated()) {
            serverController.resetForNewTest();
        }
        resetExtraHeuristics();
    }

    @Override
    public final List<TargetInfo> getTargetInfos(Collection<Integer> ids) {
        checkInstrumentation();
        return serverController.getTargetInfos(ids);
    }

    @Override
    public final void newAction(int actionIndex) {
        if (isInstrumentationActivated()) {
            serverController.setActionIndex(actionIndex);
        }
        resetExtraHeuristics();
    }

    //-----------------------------------------

    private void checkInstrumentation() {
        if (!isInstrumentationActivated()) {
            throw new IllegalStateException("Instrumentation is not active");
        }
    }

    private void validateJarPath() {

        String path = getPathToExecutableJar();
        if (!path.endsWith(".jar")) {
            throw new IllegalStateException("Invalid jar path does not end with '.jar': " + path);
        }

        if (!Files.exists(Paths.get(path))) {
            throw new IllegalArgumentException("File does not exist: " + path);
        }
    }

    private void killProcess() {
        try {
            Runtime.getRuntime().removeShutdownHook(processKillHook);
        } catch (Exception e) {
            /* do nothing. this can happen if shutdown is in progress */
        }

        if (process != null) {
            try {
                //be sure streamers are closed, otherwise process might hang on Windows
                process.getOutputStream().close();
                process.getInputStream().close();
                process.getErrorStream().close();
            } catch (Exception t) {
                SimpleLogger.error("Failed to close process stream: " + t.toString());
            }
            process.destroy();
            process = null;
        }
    }

    protected void startExternalProcessPrinter() {

        if (outputPrinter == null || !outputPrinter.isAlive()) {
            outputPrinter = new Thread(() -> {

                try {

                    boolean muted = Boolean.parseBoolean(System.getProperty(PROP_MUTE_SUT));

                    if(muted){
                        errorBuffer = new StringBuffer(4096);
                    }

                    Scanner scanner = new Scanner(new BufferedReader(
                            new InputStreamReader(process.getInputStream())));

                    while (scanner.hasNextLine()) {

                        String line = scanner.nextLine();

                        if(line.startsWith(P6SpyFormatter.PREFIX)){
                            StandardOutputTracker.handleSqlLine(this, line);
                        }

                        if(!muted) {
                            SimpleLogger.info("SUT: " + line);
                        } else if(errorBuffer != null){
                            errorBuffer.append(line);
                            errorBuffer.append("\n");
                        }

                        if (line.contains(getLogMessageOfInitializedServer())) {
                            initialized = true;
                            errorBuffer = null; //no need to keep track of it if everything is ok
                            latch.countDown();
                        }
                    }

                    /*
                        if we arrive here, it means the process has no more output.
                        this could happen if it was started with some misconfiguration
                     */
                    if(! process.isAlive()){
                        SimpleLogger.warn("SUT has terminated");
                    } else {
                        SimpleLogger.warn("SUT is still alive, but its output was closed before" +
                                " producing the initialization message.");
                    }


                    latch.countDown();

                } catch (Exception e) {
                    SimpleLogger.error(e.toString());
                }
            });

            outputPrinter.start();
        }
    }
}
