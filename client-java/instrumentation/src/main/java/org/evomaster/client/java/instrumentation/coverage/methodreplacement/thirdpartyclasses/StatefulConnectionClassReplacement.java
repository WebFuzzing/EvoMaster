package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.RedisCommand;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * This replacement captures Redis dispatch operations containing Redis Commands.
 * Should cover each and every RedisCommand used through Lettuce.
 */
public class StatefulConnectionClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final StatefulConnectionClassReplacement singleton =
            new StatefulConnectionClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "io.lettuce.core.api.StatefulConnection";
    }

    private static final String DISPATCH = "dispatch";

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = DISPATCH,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.REDIS,
            castTo = "io.lettuce.core.protocol.RedisCommand")
    public static Object dispatch(Object redis, @ThirdPartyCast(actualType = "io.lettuce.core.protocol.RedisCommand") Object command) {
        try {
            long start = System.currentTimeMillis();

            Method m = getOriginal(singleton, DISPATCH, redis);
            Object result = m.invoke(redis, command);

            long end = System.currentTimeMillis();

            Method typeMethod = command.getClass().getMethod("getType");
            Object typeObj = typeMethod.invoke(command);
            String typeName = typeObj.toString();

            Method argsMethod = command.getClass().getMethod("getArgs");
            Object commandArgs = argsMethod.invoke(command);

            String[] args = parseArgs(commandArgs);

            RedisCommand.RedisCommandType cmdType;
            try {
                cmdType = RedisCommand.RedisCommandType.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                cmdType = RedisCommand.RedisCommandType.OTHER;
            }

            addRedisCommand(cmdType, args, end - start);

            return result;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads CommandArgs's private singularArguments field directly instead of
     * calling its public toCommandString(), which joins every argument into a
     * single space-separated string - unrecoverable if any argument value
     * itself contains a space. Walking the list lets each argument be read on
     * its own via toString(), which Lettuce's KeyArgument/ValueArgument render
     * as "key<...>"/"value<...>" (unwrapped below); every other argument type
     * renders as its plain value.
     */
    private static String[] parseArgs(Object commandArgs) {
        List<?> singularArguments = (List<?>) getField(commandArgs, "singularArguments");
        String[] args = new String[singularArguments.size()];
        for (int i = 0; i < singularArguments.size(); i++) {
            args[i] = unwrapArg(singularArguments.get(i).toString());
        }
        return args;
    }

    private static String unwrapArg(String token) {
        if (token.startsWith("key<") && token.endsWith(">")) {
            return token.substring(4, token.length() - 1);
        }
        if (token.startsWith("value<") && token.endsWith(">")) {
            return token.substring(6, token.length() - 1);
        }
        return token;
    }

    private static void addRedisCommand(RedisCommand.RedisCommandType type, String[] args, long executionTime) {
        RedisCommand cmd = new RedisCommand(
                type,
                args,
                true,
                executionTime
        );

        ExecutionTracer.addRedisCommand(cmd);
    }

}