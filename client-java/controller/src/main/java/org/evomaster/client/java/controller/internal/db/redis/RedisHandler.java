package org.evomaster.client.java.controller.internal.db.redis;

import org.evomaster.client.java.controller.api.dto.database.execution.RedisExecutionsDto;
import org.evomaster.client.java.controller.api.dto.database.execution.RedisFailedCommand;
import org.evomaster.client.java.controller.api.dto.database.execution.RedisSearchFilterDto;
import org.evomaster.client.java.controller.internal.TaintHandlerExecutionTracer;
import org.evomaster.client.java.controller.redis.*;
import org.evomaster.client.java.instrumentation.RedisCommand;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.*;

import static org.evomaster.client.java.distance.heuristics.DistanceHelper.H_MAX_VALUE;
import static org.evomaster.client.java.instrumentation.RedisCommand.RedisCommandType.*;

/**
 * Class used to act upon Redis commands executed by the SUT
 */
public class RedisHandler {

    /**
     * Info about the executed commands
     */
    private final List<RedisCommand> operations;

    /**
     * The heuristics based on the Redis execution
     */
    private final List<RedisCommandEvaluation> evaluatedRedisCommands = new ArrayList<>();

    /**
     * Commands that have a non-zero positive distance.
     */
    private final List<RedisFailedCommand> failedCommands = new ArrayList<>();

    /**
     * Whether to calculate heuristics based on execution or not
     */
    private volatile boolean calculateHeuristics;

    /**
     * Whether to use execution's info or not
     */
    private volatile boolean extractRedisExecution;

    /**
     * The client must be created given both host and port for Redis DB.
     */
    private ReflectionBasedRedisClient redisClient = null;

    private final RedisHeuristicsCalculator calculator = new RedisHeuristicsCalculator(new TaintHandlerExecutionTracer());

    private static final String REDIS_HASH_TYPE = "hash";
    private static final String REDIS_SET_TYPE = "set";
    private static final String REDIS_STRING_TYPE = "string";

    public RedisHandler() {
        operations = new ArrayList<>();
        calculateHeuristics = true;
        extractRedisExecution = true;
    }

    public void reset() {
        operations.clear();
        evaluatedRedisCommands.clear();
        failedCommands.clear();
    }

    public boolean isExtractRedisExecution() {
        return extractRedisExecution;
    }

    public void setExtractRedisExecution(boolean extractRedisExecution) {
        this.extractRedisExecution = extractRedisExecution;
    }

    public RedisExecutionsDto getExecutionDto() {
        RedisExecutionsDto dto = new RedisExecutionsDto();
        dto.failedCommands.addAll(failedCommands);
        return dto;
    }

    public boolean isCalculateHeuristics() {
        return calculateHeuristics;
    }

    public void setCalculateHeuristics(boolean calculateHeuristics) {
        this.calculateHeuristics = calculateHeuristics;
    }

    public void handle(RedisCommand info) {
        operations.add(info);
    }

    public List<RedisCommandEvaluation> getEvaluatedRedisCommands() {
        operations.stream()
            .filter(command -> command.getType().shouldCalculateHeuristic())
            .forEach(redisCommand -> {
                RedisDistanceWithMetrics distanceWithMetrics = computeDistance(redisCommand, redisClient);
                evaluatedRedisCommands.add(new RedisCommandEvaluation(redisCommand, distanceWithMetrics));
                registerFailedCommand(redisCommand, distanceWithMetrics.getDistance());
            });
        operations.clear();

        return evaluatedRedisCommands;
    }

    private void registerFailedCommand(RedisCommand redisCommand, double distance) {
        RedisCommand.RedisCommandType type = redisCommand.getType();
        if (distance > 0 && (
            type.equals(GET) ||
            type.equals(HGET) ||
            type.equals(HGETALL) ||
            type.equals(KEYS) ||
            type.equals(SINTER) ||
            type.equals(SMEMBERS) ||
            type.equals(FT_SEARCH) ||
            type.equals(FT_AGGREGATE))
        ) {
            // Further commands will be registered in future iterations.
            RedisFailedCommand failedCommand = createFailedCommand(redisCommand);
            if (failedCommand != null) {
                failedCommands.add(failedCommand);
            }
        }
    }

    /**
     * @return the failed command, or null if it is not (yet) actionable for data generation
     * (e.g. an FT.SEARCH/FT.AGGREGATE whose index does not exist).
     */
    private RedisFailedCommand createFailedCommand(RedisCommand redisCommand) {
        RedisCommand.RedisCommandType type = redisCommand.getType();
        List<String> args = redisCommand.extractArgs();
        switch (type) {
            case GET:
            case HGETALL:
            case SINTER:
            case SMEMBERS: {
                if (args.isEmpty()) {
                    throw new IllegalArgumentException("Command " + type.getLabel() + " has invalid arguments.");
                }
                return new RedisFailedCommand(
                        type.getLabel().toUpperCase(),
                        args,
                        null,
                        null);
            }

            case KEYS: {
                if (args.isEmpty()) {
                    throw new IllegalArgumentException("Command KEYS has invalid arguments.");
                }
                return new RedisFailedCommand(
                        type.getLabel().toUpperCase(),
                        Collections.emptyList(),
                        RedisUtils.redisPatternToRegex(args.get(0)),
                        null);
            }

            case HGET: {
                if (args.size() < 2) {
                    throw new IllegalArgumentException("Command HGET has invalid arguments.");
                }
                return new RedisFailedCommand(
                        type.getLabel().toUpperCase(),
                        Collections.singletonList((args.get(0))),
                        null,
                        args.get(1));
            }

            case FT_SEARCH:
            case FT_AGGREGATE:
                return createFtFailedCommand(redisCommand);

            default:
                throw new RuntimeException(
                        "Invalid command registering failed redis commands. Type encountered: " + type);
        }
    }

    /**
     * Note: an FT.SEARCH/FT.AGGREGATE against an index that does not
     * exist currently fails outright (Redis raises "No such index"), so ConnectionClassReplacement
     * never records it as a successfully-executed command, and it never reaches this handler at
     * all. Making that case actionable would need capturing failed executions too, plus a
     * FT.CREATE-based action to generate the missing index before any documents.
     */
    private RedisFailedCommand createFtFailedCommand(RedisCommand redisCommand) {
        RedisCommand.RedisCommandType type = redisCommand.getType();
        List<String> args = redisCommand.extractArgs();
        if (args.size() < 2) {
            throw new IllegalArgumentException("Command " + type.getLabel() + " has invalid arguments.");
        }

        String index = args.get(0);
        String query = args.get(1);

        if (redisClient == null) {
            return null;
        }

        RedisIndexInfo info;
        try {
            info = redisClient.getIndexInfo(index);
        } catch (Exception e) {
            SimpleLogger.warn("Could not fetch index info for " + index + ": " + e.getMessage());
            return null;
        }
        if (info == null || !info.isHashIndex()) {
            // Unknown indexes or one over a non-HASH key type are out of scope.
            return null;
        }

        List<RedisSearchFilter> filters;
        try {
            filters = RedisSearchQueryParser.parse(query);
        } catch (IllegalArgumentException e) {
            // Malformed, or using grammar not supported by RedisSearchQueryParser: not actionable.
            return null;
        }

        List<String> groupByFields = type.equals(FT_AGGREGATE)
                ? redisCommand.extractGroupByFields()
                : Collections.emptyList();

        return new RedisFailedCommand(
                type.name(),
                index,
                info.getPrefixes(),
                info.getAttributes(),
                toFilterDtos(filters),
                groupByFields);
    }

    private static List<RedisSearchFilterDto> toFilterDtos(List<RedisSearchFilter> filters) {
        List<RedisSearchFilterDto> dtos = new ArrayList<>();
        for (RedisSearchFilter filter : filters) {
            if (filter instanceof RedisSearchTagFilter) {
                RedisSearchTagFilter tagFilter = (RedisSearchTagFilter) filter;
                dtos.add(RedisSearchFilterDto.tag(tagFilter.getField(), tagFilter.getValues()));
            } else if (filter instanceof RedisSearchNumericFilter) {
                RedisSearchNumericFilter numericFilter = (RedisSearchNumericFilter) filter;
                dtos.add(RedisSearchFilterDto.numeric(numericFilter.getField(), numericFilter.getMin(), numericFilter.getMax()));
            } else if (filter instanceof RedisSearchTextFilter) {
                RedisSearchTextFilter textFilter = (RedisSearchTextFilter) filter;
                dtos.add(RedisSearchFilterDto.text(textFilter.getField(), textFilter.getTerm()));
            }
        }
        return dtos;
    }

    private RedisDistanceWithMetrics computeDistance(RedisCommand redisCommand, ReflectionBasedRedisClient redisClient) {
        RedisCommand.RedisCommandType type = redisCommand.getType();
        try {
            switch (type) {
                case KEYS:
                case EXISTS: {
                    RedisKeyValueStore redisKeyValueStore = createRedisInfoForAllKeys(redisClient);
                    return calculator.computeDistance(redisCommand, redisKeyValueStore);
                }

                case GET: {
                    RedisKeyValueStore redisKeyValueStore = createRedisInfoForKeysByType(REDIS_STRING_TYPE, redisClient);
                    return calculator.computeDistance(redisCommand, redisKeyValueStore);
                }

                case HGET: {
                    RedisKeyValueStore redisKeyValueStore = createRedisInfoForKeysByField(redisClient);
                    return calculator.computeDistance(redisCommand, redisKeyValueStore);
                }

                case HGETALL: {
                    RedisKeyValueStore redisKeyValueStore = createRedisInfoForKeysByType(REDIS_HASH_TYPE, redisClient);
                    return calculator.computeDistance(redisCommand, redisKeyValueStore);
                }

                case SMEMBERS: {
                    RedisKeyValueStore redisKeyValueStore = createRedisInfoForKeysByType(REDIS_SET_TYPE, redisClient);
                    return calculator.computeDistance(redisCommand, redisKeyValueStore);
                }

                case SINTER: {
                    List<String> keys = redisCommand.extractArgs();
                    RedisKeyValueStore redisKeyValueStore = createRedisInfoForIntersection(keys, redisClient);
                    return calculator.computeDistance(redisCommand, redisKeyValueStore);
                }

                case FT_SEARCH:
                case FT_AGGREGATE: {
                    String index = redisCommand.extractArgs().get(0);
                    RedisKeyValueStore redisKeyValueStore = createRedisInfoForFtIndex(index, redisClient);
                    return calculator.computeDistance(redisCommand, redisKeyValueStore);
                }

                default:
                    return new RedisDistanceWithMetrics(H_MAX_VALUE, 0);
            }
        } catch (Exception e) {
            SimpleLogger.warn("Could not compute distance for " + type + ": " + e.getMessage());
            return new RedisDistanceWithMetrics(H_MAX_VALUE, 0);
        }
    }

    private RedisKeyValueStore createRedisInfoForIntersection(List<String> commandKeys, ReflectionBasedRedisClient redisClient) {
        Set<String> keySet = redisClient.getKeysByType(REDIS_SET_TYPE);

        //A Map structure is introduced here using the same keys that are stored in REDIS.
        //The value for each one, since each key represents a SET, correspond to the members of that given set.
        Map<String, RedisValueData> redisData = new HashMap<>();
        keySet.forEach(
                key -> redisData.put(key, new RedisValueData(redisClient.getSetMembers(key))));
        return new RedisKeyValueStore(redisData);
    }

    private RedisKeyValueStore createRedisInfoForAllKeys(ReflectionBasedRedisClient redisClient) {
        Set<String> keys = redisClient.getAllKeys();

        //A Map structure is introduced here using the same keys that are stored in REDIS.
        //No value is needed in this case.
        Map<String, RedisValueData> redisData = new HashMap<>();
        keys.forEach(
                key -> redisData.put(key, null));
        return new RedisKeyValueStore(redisData);
    }

    private RedisKeyValueStore createRedisInfoForKeysByType(String type, ReflectionBasedRedisClient redisClient) {
        Set<String> keys = redisClient.getKeysByType(type);

        //A Map structure is introduced here using the same keys that are stored in REDIS.
        //No value is needed in this case.
        Map<String, RedisValueData> redisData = new HashMap<>();
        keys.forEach(
                key -> redisData.put(key, null));
        return new RedisKeyValueStore(redisData);
    }

    private RedisKeyValueStore createRedisInfoForKeysByField(ReflectionBasedRedisClient redisClient) {
        Set<String> keys = redisClient.getKeysByType(REDIS_HASH_TYPE);

        //A Map structure is introduced here using the same keys that are stored in REDIS.
        //The value for each one, since each key is of type HASH, correspond to the fields stored for that given key.
        Map<String, RedisValueData> redisData = new HashMap<>();
        keys.forEach(
                key -> redisData.put(key, new RedisValueData(redisClient.getHashFields(key))));
        return new RedisKeyValueStore(redisData);
    }

    /**
     * Builds the candidate document set for a FT.SEARCH/FT.AGGREGATE index: every HASH key whose
     * name matches at least one of the prefixes declared for that index. The index definition is
     * fetched live via FT.INFO, since the FT.CREATE call that declared it may have happened before
     * this handler ever observed it, or not have been observed at all.
     */
    private RedisKeyValueStore createRedisInfoForFtIndex(String index, ReflectionBasedRedisClient redisClient) {
        RedisIndexInfo info = redisClient.getIndexInfo(index);
        List<String> prefixes = (info != null && info.isHashIndex()) ? info.getPrefixes() : Collections.emptyList();
        Set<String> hashKeys = redisClient.getKeysByType(REDIS_HASH_TYPE);

        Map<String, RedisValueData> redisData = new HashMap<>();
        hashKeys.stream()
                .filter(key -> matchesAnyPrefix(key, prefixes))
                .forEach(key -> redisData.put(key, new RedisValueData(redisClient.getHashFields(key))));
        return new RedisKeyValueStore(redisData);
    }

    private boolean matchesAnyPrefix(String key, List<String> prefixes) {
        for (String prefix : prefixes) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public void setRedisClient(ReflectionBasedRedisClient redisClient) {
        this.redisClient = redisClient;
    }
}
