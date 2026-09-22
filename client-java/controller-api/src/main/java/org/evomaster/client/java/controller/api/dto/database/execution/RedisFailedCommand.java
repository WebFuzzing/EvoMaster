package org.evomaster.client.java.controller.api.dto.database.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Each time a Redis command is executed and returns no data, we keep track of which keys were involved,
 * as well as relevant information such as the command type.
 */
public class RedisFailedCommand {

    /**
     * Command keyword. Corresponds to a RedisCommandType label.
     */
    public String command;

    /**
     * Keys involved. Could be null if the command does not have any key in the arguments. For example: KEYS (pattern).
     */
    public List<String> keys;

    /**
     * Pattern involved. It'd only apply to commands with pattern like KEYS.
     */
    public String pattern;

    /**
     * Field involved. It'd only apply to hash commands with a field like HGET.
     */
    public String field;

    /**
     * FT.SEARCH/FT.AGGREGATE only: the name of the queried index. Its existence (with a HASH
     * key_type) is a precondition for these commands to be actionable, so a failed command is
     * only ever produced for an index that was found via FT.INFO.
     */
    public String indexName;

    /**
     * FT.SEARCH/FT.AGGREGATE only: the key prefixes declared for {@link #indexName}, as reported
     * by FT.INFO. A document is a candidate for the index if its key starts with any of these.
     */
    public List<String> indexPrefixes;

    /**
     * FT.SEARCH/FT.AGGREGATE only: the schema of {@link #indexName}, mapping each indexed hash
     * field to its RediSearch type ({@link RedisSearchFilterDto#TAG}, {@link RedisSearchFilterDto#NUMERIC}
     * or {@link RedisSearchFilterDto#TEXT}).
     */
    public Map<String, String> indexAttributes;

    /**
     * FT.SEARCH/FT.AGGREGATE only: the filters parsed out of the query. An empty list means the
     * query was "*" (no filter).
     */
    public List<RedisSearchFilterDto> filters;

    /**
     * FT.AGGREGATE only: the hash fields referenced by its GROUPBY stage, if any.
     */
    public List<String> groupByFields;

    public RedisFailedCommand() {}

    public RedisFailedCommand(String command, List<String> keys, String pattern, String field) {
        this.command = command;
        this.keys =  new ArrayList<>(keys);
        this.pattern = pattern;
        this.field = field;
    }

    public RedisFailedCommand(String command,
                               String indexName,
                               List<String> indexPrefixes,
                               Map<String, String> indexAttributes,
                               List<RedisSearchFilterDto> filters,
                               List<String> groupByFields) {
        this.command = command;
        this.keys = new ArrayList<>();
        this.indexName = indexName;
        this.indexPrefixes = new ArrayList<>(indexPrefixes);
        this.indexAttributes = indexAttributes;
        this.filters = new ArrayList<>(filters);
        this.groupByFields = new ArrayList<>(groupByFields);
    }
}
