package org.evomaster.client.java.instrumentation.external;

import org.evomaster.client.java.instrumentation.Action;
import org.evomaster.client.java.instrumentation.InstrumentationController;
import org.evomaster.client.java.instrumentation.TargetInfo;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.evomaster.client.java.utils.SimpleLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Code running in the Java Agent to receive and respond to the
 * requests from the SUT controller.
 */
public class AgentController {

    private static Socket socket;
    private static Thread thread;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;

    /**
     * How long the agent waits for the driver to send the id mapping of the targets when it connects.
     * The SUT does not start while it waits, so a driver that never sends it must not block it forever.
     */
    private static final int ID_MAPPING_TIMEOUT_MS = 10_000;

    public static void start(int port){

        try{
            socket = new Socket("localhost", port);
            //essential, due to 40ms ack-delay in Linux (but no issue on Windows nor Mac)
            socket.setTcpNoDelay(true);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e){
            SimpleLogger.error("Failure in Java Agent: "+e.getMessage(), e);
        }

        SimpleLogger.info("Connected to EvoMaster controller");

        /*
            The driver first sends the ids that earlier processes of the SUT gave to their targets.
            They are loaded here, before the SUT classes run, so that no target gets a new id first.
         */
        try {
            socket.setSoTimeout(ID_MAPPING_TIMEOUT_MS);
            Object mapping = in.readObject();
            if (mapping instanceof Map) {
                ObjectiveRecorder.restoreIdMapping((Map<String, Integer>) mapping);
            } else {
                SimpleLogger.error("Expected the id mapping of the targets, but received: " + mapping);
            }
        } catch (Exception e) {
            SimpleLogger.error("Failure in reading the id mapping of the targets: " + e.getMessage(), e);
        } finally {
            try {
                if (socket != null) {
                    socket.setSoTimeout(0);
                }
            } catch (IOException e) {
                SimpleLogger.error("Failure in resetting the socket timeout: " + e.getMessage(), e);
            }
        }

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
                if(SimpleLogger.isDebug()) {
                    SimpleLogger.debug("Handling command: " + command);
                }

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
                    case EXECUTING_INIT_CASSANDRA:
                        handleExecutingInitCassandra();
                        sendCommand(Command.ACK);
                        break;
                    case EXECUTING_INIT_REDIS:
                        handleExecutingInitRedis();
                        sendCommand(Command.ACK);
                        break;
                    case EXECUTING_INIT_NEO4J:
                        handleExecutingInitNeo4j();
                        sendCommand(Command.ACK);
                        break;
                    case EXECUTING_INIT_DYNAMODB:
                        handleExecutingInitDynamoDb();
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
                    case OBJECTIVE_UPDATE_COUNT:
                        handleObjectiveUpdateCount();
                        break;
                    default:
                        SimpleLogger.error("Unrecognized command: "+command);
                        return;
                }

                long delta = System.currentTimeMillis() - start;
                if(SimpleLogger.isDebug()) {
                    SimpleLogger.debug("Command took " + delta + " ms");
                }
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

    private static void handleExecutingInitCassandra() {
        try {
            Object msg = in.readObject();
            Boolean executingInitCassandra = (Boolean) msg;
            InstrumentationController.setExecutingInitCassandra(executingInitCassandra);
        } catch (Exception e){
            SimpleLogger.error("Failure in handling executing-init-cassandra: "+e.getMessage());
        }
    }

    private static void handleExecutingInitRedis() {
        try {
            Object msg = in.readObject();
            Boolean executingInitRedis = (Boolean) msg;
            InstrumentationController.setExecutingInitRedis(executingInitRedis);
        } catch (Exception e){
            SimpleLogger.error("Failure in handling executing-init-redis: "+e.getMessage());
        }
    }

    private static void handleExecutingInitNeo4j() {
        try {
            Object msg = in.readObject();
            Boolean executingInitNeo4j = (Boolean) msg;
            InstrumentationController.setExecutingInitNeo4j(executingInitNeo4j);
        } catch (Exception e){
            SimpleLogger.error("Failure in handling executing-init-neo4j: "+e.getMessage());
        }
    }

    private static void handleExecutingInitDynamoDb() {
        try {
            Object msg = in.readObject();
            InstrumentationController.setExecutingInitDynamoDb((Boolean) msg);
        } catch (Exception e){
            SimpleLogger.error("Failure in handling executing-init-dynamodb: " + e.getMessage());
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

    private static void handleObjectiveUpdateCount(){
        try {
            sendObject(InstrumentationController.getObjectiveUpdateCount());
        } catch (Exception e) {
            SimpleLogger.error("Failure in handling objective update count: "+e.getMessage());
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
            /*
                the driver is blocked waiting for a list: without an answer it times out, and every
                message after that is read out of order
             */
            try {
                sendObject(new ArrayList<TargetInfo>());
            } catch (IOException io) {
                SimpleLogger.error("Failure in answering the request for ids", io);
            }
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
