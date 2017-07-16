package org.evomaster.clientJava.instrumentation.external;

import org.evomaster.clientJava.clientUtil.SimpleLogger;
import org.evomaster.clientJava.instrumentation.InstrumentationController;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collection;

/**
 * Code running in the Java Agent to receive and respond to the
 * requests from the the SUT controller.
 */
public class AgentController {

    private static Socket socket;
    private static Thread thread;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;

    public static void start(int port){

        try{
            socket = new Socket("localhost", port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e){
            SimpleLogger.error("Failure in Java Agent: "+e.getMessage(), e);
        }

        SimpleLogger.info("Connected to EvoMaster controller");

        thread = new Thread(() ->{

            while (! Thread.interrupted() && socket != null){

                Object msg;

                try {
                    msg = in.readObject();
                } catch (IOException e) {
                    SimpleLogger.error("Failure in receiving message: "+e.getMessage());
                    return;
                } catch (ClassNotFoundException e) {
                    SimpleLogger.error("Configuration error: "+e.getMessage());
                    return;
                }

                if(msg == null || ! (msg instanceof Command)){
                    SimpleLogger.error("Received wrong message type: "+msg);
                    continue;
                }

                Command command = (Command) msg;
                long start = System.currentTimeMillis();
                SimpleLogger.debug("Handling command: "+command);

                switch(command){
                    case NEW_SEARCH:
                        InstrumentationController.resetForNewSearch();
                        sendObject(Command.ACK);
                        break;
                    case NEW_TEST:
                        InstrumentationController.resetForNewTest();
                        sendObject(Command.ACK);
                        break;
                    case TARGET_INFOS:
                        handleTargetInfos();
                        break;
                    case ACTION_INDEX:
                        handleActionIndex();
                        sendObject(Command.ACK);
                        break;
                    default:
                        SimpleLogger.error("Unrecognized command: "+command);
                        return;
                }

                long delta = System.currentTimeMillis() - start;
                SimpleLogger.debug("Command took "+delta+" ms");
            }
        });

        thread.start();
    }

    private static void handleActionIndex(){
        try {
            Object msg = in.readObject();
            Integer index = (Integer) msg;
            InstrumentationController.newAction(index);

        } catch (Exception e) {
            SimpleLogger.error("Failure in handling action index: "+e.getMessage());
            return;
        }
    }

    private static void handleTargetInfos() {

        try {
            Object msg = in.readObject();
            Collection<Integer> ids = (Collection<Integer>) msg;
            out.writeObject(InstrumentationController.getTargetInfos(ids));

        } catch (Exception e) {
            SimpleLogger.error("Failure in handling ids: "+e.getMessage());
            return;
        }
    }

    private static void sendObject(Object obj){

        try {
            out.writeObject(obj);
        } catch (IOException e) {
            SimpleLogger.error("Failure in sending message: "+e.getMessage());
        }
    }
}
