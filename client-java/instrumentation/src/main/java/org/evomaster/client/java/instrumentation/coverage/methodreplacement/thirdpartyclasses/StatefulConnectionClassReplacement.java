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
import java.util.Objects;

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
            Method toCmdString = commandArgs.getClass().getMethod("toCommandString");
            String fullCmd = (String) toCmdString.invoke(commandArgs);

            String[] args = fullCmd.trim().split("\\s+");

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