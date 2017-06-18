package org.evomaster.clientJava.instrumentation.external;

import org.evomaster.clientJava.clientUtil.SimpleLogger;
import org.evomaster.clientJava.instrumentation.TargetInfo;

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

    private ServerSocket server;
    private Socket socket;
    protected ObjectOutputStream out;
    protected ObjectInputStream in;

    public int startServer() {

        closeServer();

        try {
            server = new ServerSocket(0);
            server.setSoTimeout(10_000);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return server.getLocalPort();
    }


    public void closeServer() {
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

    public boolean waitForIncomingConnection() {

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

    public boolean isConnectionOn() {
        /*
            as the Java Agent is the one starting this communication, if we
            have the connection, then it necessarily means that it is working
         */
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public boolean sendCommand(Command command){
        return sendObject(command);
    }

    public boolean sendObject(Object obj){
        if(! isConnectionOn()){
            return false;
        }

        try {
            out.writeObject(obj);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public Object waitAndGetResponse(){
        if(! isConnectionOn()){
            return null;
        }

        try {
            Object obj = in.readObject();
            return obj;
        } catch (IOException e) {
            return null;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean sendAndExpectACK(Command command){
        boolean sent = sendCommand(command);
        if(!sent){
            SimpleLogger.error("Failed to send message");
            return false;
        }

        return waitForAck();
    }

    public boolean sendWithDataAndExpectACK(Command command, Object data){

        boolean sent = sendCommand(command);
        if(!sent){
            SimpleLogger.error("Failed to send message");
            return false;
        }

        sent = sendObject(data);
        if(!sent){
            SimpleLogger.error("Failed to send message");
            return false;
        }

        return waitForAck();

    }

    private boolean waitForAck() {
        Object response = waitAndGetResponse();
        if(response == null){
            SimpleLogger.error("Failed to read ACK response");
            return false;
        }
        if(! Command.ACK.equals(response)){
            throw new IllegalStateException("Invalid response: "+response);
        }

        return true;
    }

    public  boolean resetForNewSearch(){
       return sendAndExpectACK(Command.NEW_SEARCH);
    }

    public  boolean resetForNewTest(){
        return sendAndExpectACK(Command.NEW_TEST);
    }

    public boolean setActionIndex(int actionIndex){
        return sendWithDataAndExpectACK(Command.ACTION_INDEX, actionIndex);
    }

    public List<TargetInfo> getTargetInfos(Collection<Integer> ids){
        boolean sent = sendCommand(Command.TARGET_INFOS);
        if(!sent){
            SimpleLogger.error("Failed to send message");
            return null;
        }

        try {
            out.writeObject(ids);
        } catch (IOException e) {
            SimpleLogger.error("Failed to send ids");
            return null;
        }

        Object response = waitAndGetResponse();
        if(response == null){
            SimpleLogger.error("Failed to read response about covered targets");
            return null;
        }

        if(! (response instanceof List<?>)){
            throw new IllegalStateException("Invalid response: "+response);
        }

        return (List<TargetInfo>) response;
    }
}
