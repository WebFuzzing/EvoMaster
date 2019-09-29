package org.evomaster.client.java.instrumentation.external;

import org.evomaster.client.java.instrumentation.Action;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.evomaster.client.java.utils.SimpleLogger;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.TargetInfo;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.List;

/**
 * The SutController will start a TCP server, and the Agent in the external
 * SUT process will do a connection to it.
 * <br>
 * Note: the communications are extremely basic. Therefore, to avoid adding
 * unnecessary complexity to the Agent (which runs together with the SUT),
 * no REST or RMI is used here, just basic, old-style TCP raw connections
 * with serialized Java objects.
 */
public class ServerController {

    /*
        Note: for some reasons, different threads access this class, leading sometime to
        nasty StreamCorruptedException.
        Therefore, all public methods in this class are synchronized.
     */

    private ServerSocket server;
    private Socket socket;
    protected ObjectOutputStream out;
    protected ObjectInputStream in;

    public synchronized int startServer() {

        closeServer();

        try {
            server = new ServerSocket(0);
            server.setSoTimeout(10_000);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return server.getLocalPort();
    }


    public synchronized void closeServer() {
        if (server != null) {
            try {
                server.close();
                server = null;
                socket = null;
                in = null;
                out = null;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public synchronized boolean waitForIncomingConnection() {

        try {
            socket = server.accept();
            socket.setSoTimeout(20_000);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

        } catch (InterruptedIOException e) {
            /*
                didn't get a response in time.
                This likely means that the SUT was not started
                with the Java Agent properly initialized
             */
            return false;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return isConnectionOn();
    }

    public synchronized boolean isConnectionOn() {
        /*
            as the Java Agent is the one starting this communication, if we
            have the connection, then it necessarily means that it is working
         */
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public synchronized boolean sendCommand(Command command) {
        return sendObject(command);
    }

    public synchronized boolean sendObject(Object obj) {
        if (!isConnectionOn()) {
            SimpleLogger.error("TCP connection is not on");
            return false;
        }

        try {
            out.writeObject(obj);
            out.reset(); //Note: this is critical, due to caching
        } catch (IOException e) {
            SimpleLogger.error("IO exception while sending object", e);
            return false;
        }

        return true;
    }

    public synchronized Object waitAndGetResponse() {
        if (!isConnectionOn()) {
            SimpleLogger.error("TCP connection is not on");
            return null;
        }

        try {
            Object obj = in.readObject();
            return obj;
        } catch (IOException e) {
            SimpleLogger.error("IO exception while waiting for response", e);
            return null;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public synchronized boolean sendAndExpectACK(Command command) {
        boolean sent = sendCommand(command);
        if (!sent) {
            SimpleLogger.error("Failed to send message");
            return false;
        }

        return waitForAck();
    }

    public synchronized boolean sendWithDataAndExpectACK(Command command, Object data) {

        boolean sent = sendCommand(command);
        if (!sent) {
            SimpleLogger.error("Failed to send message");
            return false;
        }

        sent = sendObject(data);
        if (!sent) {
            SimpleLogger.error("Failed to send message");
            return false;
        }

        return waitForAck();

    }

    private boolean waitForAck() {
        Object response = waitAndGetResponse();
        if (response == null) {
            SimpleLogger.error("Failed to read ACK response");
            return false;
        }
        if (!Command.ACK.equals(response)) {
            throw new IllegalStateException(errorMsgExpectingResponse(response, "an ACK"));
        }

        return true;
    }

    private String errorMsgExpectingResponse(Object response, String expectation) {

        String repMsg = response == null ? "NULL"
                : "an instance of type " + response.getClass()
                + " with value: " + response.toString();

        return "Invalid response."
                + " Expecting " + expectation
                + ", but rather received " + repMsg;
    }

    public boolean resetForNewSearch() {
        return sendAndExpectACK(Command.NEW_SEARCH);
    }

    public boolean resetForNewTest() {
        return sendAndExpectACK(Command.NEW_TEST);
    }

    public boolean setAction(Action action) {
        return sendWithDataAndExpectACK(Command.ACTION_INDEX, action);
    }

    public synchronized List<TargetInfo> getTargetsInfo(Collection<Integer> ids) {
        boolean sent = sendCommand(Command.TARGETS_INFO);
        if (!sent) {
            SimpleLogger.error("Failed to send message");
            return null;
        }

        if(! sendObject(ids)){
            SimpleLogger.error("Failed to send ids");
            return null;
        }

        Object response = waitAndGetResponse();
        if (response == null) {
            SimpleLogger.error("Failed to read response about covered targets");
            return null;
        }

        if (!(response instanceof List<?>)) {
            throw new IllegalStateException(errorMsgExpectingResponse(response, "a List"));
        }

        return (List<TargetInfo>) response;
    }

    public synchronized List<AdditionalInfo> getAdditionalInfoList() {

        boolean sent = sendCommand(Command.ADDITIONAL_INFO);
        if (!sent) {
            SimpleLogger.error("Failed to send message");
            return null;
        }

        Object response = waitAndGetResponse();
        if (response == null) {
            SimpleLogger.error("Failed to read response about additional info");
            return null;
        }

        if (!(response instanceof List<?>)) {
            throw new IllegalStateException(errorMsgExpectingResponse(response, "a List"));
        }

        return (List<AdditionalInfo>) response;
    }

    public synchronized UnitsInfoRecorder getUnitsInfoRecorder(){

        boolean sent = sendCommand(Command.UNITS_INFO);
        if (!sent) {
            SimpleLogger.error("Failed to send message");
            return null;
        }

        Object response = waitAndGetResponse();
        if (response == null) {
            SimpleLogger.error("Failed to read response about units info");
            return null;
        }

        if (!(response instanceof UnitsInfoRecorder)) {
            throw new IllegalStateException(errorMsgExpectingResponse(response, "a UnitsInfoRecorder"));
        }

        return (UnitsInfoRecorder) response;
    }
}
