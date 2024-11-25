package org.evomaster.client.java.instrumentation.external;

import org.evomaster.client.java.instrumentation.Action;
import org.evomaster.client.java.instrumentation.InstrumentationController;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.evomaster.client.java.utils.SimpleLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collection;
import java.util.List;

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
                        sendCommand(Command.ACK);
                        break;
                    case NEW_TEST:
                        InstrumentationController.resetForNewTest();
                        sendCommand(Command.ACK);
                        break;
                    case TARGETS_INFO:
                        handleTargetInfos();
                        break;
                    case ACTION_INDEX:
                        handleActionIndex();
                        sendCommand(Command.ACK);
                        break;
                    case ADDITIONAL_INFO:
                        handleAdditionalInfo();
                        break;
                    case UNITS_INFO:
                        handleUnitsInfo();
                        break;
                    case KILL_SWITCH:
                        handleKillSwitch();
                        sendCommand(Command.ACK);
                        break;
                    case EXECUTING_INIT_SQL:
                        handleExecutingInitSql();
                        sendCommand(Command.ACK);
                        break;
                    case EXECUTING_INIT_MONGO:
                        handleExecutingInitMongo();
                        sendCommand(Command.ACK);
                        break;
                    case EXECUTING_ACTION:
                        handleExecutingAction();
                        sendCommand(Command.ACK);
                        break;
                    case BOOTING_SUT:
                        handleBootingSut();
                        sendCommand(Command.ACK);
                        break;
                    case BOOT_TIME_INFO:
                        handleBootTimeObjectiveInfo();
                        break;
                    case EXTRACT_JVM_DTO:
                        handleExtractingSpecifiedDto();
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


    private static void sendCommand(Command command){
        try {
            sendObject(command);
        } catch (Exception e) {
            SimpleLogger.error("Failure to send command " + command+": "+e.getMessage());
        }
    }

    private static void handleUnitsInfo() {
        try {
            UnitsInfoRecorder.forceLoadingLazyDataStructures();
            sendObject(UnitsInfoRecorder.getInstance());
        } catch (Exception e) {
            SimpleLogger.error("Failure in handling units info: "+e.getMessage());
        }
    }

    private static void handleActionIndex(){
        try {
            Object msg = in.readObject();
            Action action = (Action) msg;
            InstrumentationController.newAction(action);

        } catch (Exception e) {
            SimpleLogger.error("Failure in handling action index: "+e.getMessage());
        }
    }

    private static void handleKillSwitch() {
        try {
            Object msg = in.readObject();
            Boolean killSwitch = (Boolean) msg;
            InstrumentationController.setKillSwitch(killSwitch);
        } catch (Exception e){
            SimpleLogger.error("Failure in handling kill-switch: "+e.getMessage());
        }
    }

    private static void handleExecutingInitSql() {
        try {
            Object msg = in.readObject();
            Boolean executingInitSql = (Boolean) msg;
            InstrumentationController.setExecutingInitSql(executingInitSql);
        } catch (Exception e){
            SimpleLogger.error("Failure in handling executing-init-sql: "+e.getMessage());
        }
    }

    private static void handleExecutingInitMongo() {
        try {
            Object msg = in.readObject();
            Boolean executingInitMongo = (Boolean) msg;
            InstrumentationController.setExecutingInitMongo(executingInitMongo);
        } catch (Exception e){
            SimpleLogger.error("Failure in handling executing-init-mongo: "+e.getMessage());
        }
    }

    private static void handleExecutingAction() {
        try {
            Object msg = in.readObject();
            Boolean executingAction = (Boolean) msg;
            InstrumentationController.setExecutingAction(executingAction);
        } catch (Exception e){
            SimpleLogger.error("Failure in handling executing-action: "+e.getMessage());
        }
    }

    private static void handleBootingSut() {
        try {
            Object msg = in.readObject();
            Boolean bootingSut = (Boolean) msg;
            InstrumentationController.setBootingSut(bootingSut);
        } catch (Exception e){
            SimpleLogger.error("Failure in handling executing-action: "+e.getMessage());
        }
    }

    private static void handleAdditionalInfo(){
        try {
            sendObject(InstrumentationController.getAdditionalInfoList());
        } catch (Exception e) {
            SimpleLogger.error("Failure in handling additional info: "+e.getMessage());
        }
    }

    private static void handleBootTimeObjectiveInfo(){
        try {
            sendObject(InstrumentationController.getBootTimeObjectiveInfo());
        }catch (Exception e) {
            SimpleLogger.error("Failure in handling Boot-time Objective Info: "+e.getMessage());
        }
    }

    private static void handleExtractingSpecifiedDto(){
        try {
            Object msg = in.readObject();
            List<String> dtoNames = (List<String>) msg;
            InstrumentationController.extractSpecifiedDto(dtoNames);
        } catch (Exception e){
            SimpleLogger.error("Failure in handling extracting specified dto: "+e.getMessage());
        }
    }


    private static void handleTargetInfos() {

        try {
            Object msg = in.readObject();
            TargetInfoRequestDto dto = (TargetInfoRequestDto) msg;

            sendObject(InstrumentationController.getTargetInfos(dto.ids, dto.fullyCovered, dto.descriptiveIds));

        } catch (Exception e) {
            SimpleLogger.error("Failure in handling ids", e);
        }
    }

    private static void sendObject(Object obj) throws IOException{

        try {
            out.writeObject(obj);
            out.reset();
            /*
                Note: reset is critical, see https://www.javaspecialists.eu/archive/Issue088.html
                The "problem" is that Java will cache the objects based on identity...
                if you modify an object and try to send it, it is not sent!!!
                furthermore, sent objects are never GCed, so can run out of memory...
                WTF?!?
                but caching is good for immutable objects like String...
                but as "external" drivers are only for experiments, can afford loss of performance
                to avoid weird bugs when we send a mutable object by mistake
             */
        } catch (IOException e) {
            SimpleLogger.error("Failure in sending message: "+e.getMessage());
            throw  e;
        }
    }
}
