package org.evomaster.clientJava.clientUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * Client library should be the least invasive as possible.
 * Using a logger that could conflict with existing ones of the
 * client would not be a great idea... plus issue with
 * shading and version mismatch.
 * So, here we create a simple, custom one that just print
 * to {@code System.out.println}
 */
public class SimpleLogger {

    enum Level {DEBUG, INFO, WARN, ERROR, OFF}

    /**
     * Property used to control the logging level by system property
     */
    public static final String PROP_LOGGER_LEVEL = "em.logger.level";

    /*
        WARNING: this class has mutable dynamic state.
        Unfortunately, in client library cannot really add
        a DI framework just to handle this case.
        Furthermore, this has no impact on search (just side-effect
        of logging)
     */

    private static Level threshold = Level.INFO;

    private static final Set<String> uniqueMessages = new HashSet<>(1024);


    public static Level getThreshold() {
        return threshold;
    }

    public static void setThreshold(Level threshold) {
        SimpleLogger.threshold = threshold;
    }


    public static void updateThreshold(){
        String level = System.getProperty(PROP_LOGGER_LEVEL);
        if(level != null){
            setThreshold(Level.valueOf(level));
        }
    }


    public static void debug(String message){
        printMessage(Level.DEBUG, message, null);
    }

    public static void info(String message){
        printMessage(Level.INFO, message, null);
    }

    public static void uniqueWarn(String message){
        if(uniqueMessages.contains(message)){
            return;
        }
        uniqueMessages.add(message);
        warn(message);
    }

    public static void warn(String message){
        warn(message, null);
    }

    public static void warn(String message, Throwable t){
        printMessage(Level.WARN, message, t);
    }

    public static void error(String message){
        error(message, null);
    }

    public static void error(String message, Throwable t){
        printMessage(Level.ERROR, message, t);
    }




    private static void printMessage(Level level, String message, Throwable t){

        if(level.compareTo(threshold) >= 0 ){
            if(level.equals(Level.WARN) || level.equals(Level.ERROR)){
                System.out.print(""+ level + " - ");
            }

            System.out.println(message);
            if(t != null){
                t.printStackTrace();
            }
        }
    }
}
