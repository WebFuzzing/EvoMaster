package org.evomaster.client.java.controller.redis;

/**
 * A single filter parsed out of a FT.SEARCH/FT.AGGREGATE query string.
 * Implementations are {@link RedisSearchTagFilter}, {@link RedisSearchNumericFilter} and
 * {@link RedisSearchTextFilter}, mirroring the tagFilter/numericFilter/textFilter productions
 * of the supported query grammar.
 */
public interface RedisSearchFilter {
}
