package org.evomaster.clientJava.clientUtil;

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

    private static Level threshold = Level.INFO;

    public static Level getThreshold() {
        return threshold;
    }

    public static void setThreshold(Level threshold) {
        SimpleLogger.threshold = threshold;
    }

    public static void debug(String message){
        printMessage(Level.DEBUG, message, null);
    }

    public static void info(String message){
        printMessage(Level.INFO, message, null);
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
