package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.RedisCommand;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This replacement captures Redis command execution through Jedis.
 */
public class ConnectionClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final ConnectionClassReplacement singleton = new ConnectionClassReplacement();

    private static final String EXECUTE_COMMAND = "executeCommand";

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "redis.clients.jedis.Connection";
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = EXECUTE_COMMAND,
            usageFilter = UsageFilter.ANY, category = ReplacementCategory.REDIS)
    public static Object executeCommand(Object connection, @ThirdPartyCast(actualType = "redis.clients.jedis.CommandObject") Object commandObject) {
        try {
            long start = System.currentTimeMillis();

            Method m = getOriginal(singleton, EXECUTE_COMMAND, connection);
            Object result = m.invoke(connection, commandObject);

            long end = System.currentTimeMillis();

            try {
                recordCommand(commandObject, end - start);
            } catch (Exception e) {
                SimpleLogger.uniqueWarn("Failed to record Redis command captured via Connection.executeCommand: " + e.getMessage());
            }

            return result;
        } catch (InvocationTargetException e) {
            // The SUT must see the very exception Jedis threw (e.g. JedisDataException), otherwise
            // any handling it does of Redis errors would silently stop working once instrumented.
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void recordCommand(Object commandObject, long executionTime) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Object commandArguments = commandObject.getClass().getMethod("getArguments").invoke(commandObject);
        Object protocolCommand = commandArguments.getClass().getMethod("getCommand").invoke(commandArguments);
        byte[] rawCommand = (byte[]) protocolCommand.getClass().getMethod("getRaw").invoke(protocolCommand);
        String commandName = new String(rawCommand, StandardCharsets.US_ASCII).toUpperCase().replace('.', '_');

        RedisCommand.RedisCommandType type;
        try {
            type = RedisCommand.RedisCommandType.valueOf(commandName);
        } catch (IllegalArgumentException e) {
            type = RedisCommand.RedisCommandType.OTHER;
        }

        String[] args = extractArgs(commandArguments);

        RedisCommand cmd = new RedisCommand(type, args, true, executionTime);
        ExecutionTracer.addRedisCommand(cmd);
    }

    /**
     * CommandArguments stores the command keyword itself as the first element
     * of the same list it iterates over - already captured as RedisCommandType.
     */
    private static String[] extractArgs(Object commandArguments) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Iterator<?> iterator = (Iterator<?>) commandArguments.getClass().getMethod("iterator").invoke(commandArguments);
        List<String> args = new ArrayList<>();
        if (iterator.hasNext()) {
            iterator.next();
        }
        while (iterator.hasNext()) {
            Object rawable = iterator.next();
            byte[] raw = (byte[]) rawable.getClass().getMethod("getRaw").invoke(rawable);
            args.add(new String(raw, StandardCharsets.US_ASCII));
        }
        return args.toArray(new String[0]);
    }
}
