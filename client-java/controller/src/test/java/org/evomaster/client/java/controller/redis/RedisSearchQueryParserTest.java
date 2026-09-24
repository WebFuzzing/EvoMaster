package org.evomaster.client.java.controller.redis;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RedisSearchQueryParserTest {

    @Test
    void testMatchAllQueryHasNoFilters() {
        assertTrue(RedisSearchQueryParser.parse("*").isEmpty());
    }

    @Test
    void testMatchAllQueryIsTrimmed() {
        assertTrue(RedisSearchQueryParser.parse("  *  ").isEmpty());
    }

    @Test
    void testTagFilterSingleValue() {
        List<RedisSearchFilter> filters = RedisSearchQueryParser.parse("@street:{main}");

        assertEquals(1, filters.size());
        RedisSearchTagFilter filter = (RedisSearchTagFilter) filters.get(0);
        assertEquals("street", filter.getFieldName());
        assertEquals(Arrays.asList("main"), filter.getValues());
    }

    @Test
    void testTagFilterMultipleValues() {
        List<RedisSearchFilter> filters = RedisSearchQueryParser.parse("@street:{main|second|third}");

        RedisSearchTagFilter filter = (RedisSearchTagFilter) filters.get(0);
        assertEquals(Arrays.asList("main", "second", "third"), filter.getValues());
    }

    @Test
    void testNumericFilterBounds() {
        List<RedisSearchFilter> filters = RedisSearchQueryParser.parse("@age:[18 65]");

        assertEquals(1, filters.size());
        RedisSearchNumericFilter filter = (RedisSearchNumericFilter) filters.get(0);
        assertEquals("age", filter.getFieldName());
        assertEquals(18.0, filter.getMin());
        assertEquals(65.0, filter.getMax());
    }

    @Test
    void testNumericFilterAcceptsDecimalBounds() {
        RedisSearchNumericFilter filter = (RedisSearchNumericFilter)
                RedisSearchQueryParser.parse("@price:[9.5 19.99]").get(0);

        assertEquals(9.5, filter.getMin());
        assertEquals(19.99, filter.getMax());
    }

    @Test
    void testNumericFilterInternalWhitespaceDoesNotSplitTheFilter() {
        // the "min WS max" separator inside "[...]" must not be treated as a filter boundary
        List<RedisSearchFilter> filters = RedisSearchQueryParser.parse("@age:[18 65]");
        assertEquals(1, filters.size());
    }

    @Test
    void testTextFilterWithField() {
        List<RedisSearchFilter> filters = RedisSearchQueryParser.parse("@name:alice");

        RedisSearchTextFilter filter = (RedisSearchTextFilter) filters.get(0);
        assertEquals("name", filter.getFieldName());
        assertEquals("alice", filter.getTerm());
    }

    @Test
    void testTextFilterWithFieldAndPrefixTerm() {
        RedisSearchTextFilter filter = (RedisSearchTextFilter)
                RedisSearchQueryParser.parse("@name:ali*").get(0);

        assertEquals("name", filter.getFieldName());
        assertEquals("ali*", filter.getTerm(), "the trailing '*' is kept for the caller to interpret");
    }

    @Test
    void testBareTextFilterHasNoField() {
        RedisSearchTextFilter filter = (RedisSearchTextFilter)
                RedisSearchQueryParser.parse("redis").get(0);

        assertNull(filter.getFieldName());
        assertEquals("redis", filter.getTerm());
    }

    @Test
    void testAtFieldWithoutColonIsTreatedAsABareTerm() {
        // "@field" alone isn't part of the grammar (no ":"), so it degrades to a literal text term
        RedisSearchTextFilter filter = (RedisSearchTextFilter)
                RedisSearchQueryParser.parse("@name").get(0);

        assertNull(filter.getFieldName());
        assertEquals("@name", filter.getTerm());
    }

    @Test
    void testMultipleFiltersAreWhitespaceSeparatedAsAnAndConjunction() {
        List<RedisSearchFilter> filters = RedisSearchQueryParser.parse("@name:alice @age:[18 65] @street:{main}");

        assertEquals(3, filters.size());
        assertInstanceOf(RedisSearchTextFilter.class, filters.get(0));
        assertInstanceOf(RedisSearchNumericFilter.class, filters.get(1));
        assertInstanceOf(RedisSearchTagFilter.class, filters.get(2));
    }

    @Test
    void testExtraWhitespaceBetweenAndInsideFiltersIsTolerated() {
        List<RedisSearchFilter> filters = RedisSearchQueryParser.parse("  @name:alice     @age:[ 18   65 ]  ");

        assertEquals(2, filters.size());
        RedisSearchNumericFilter numeric = (RedisSearchNumericFilter) filters.get(1);
        assertEquals(18.0, numeric.getMin());
        assertEquals(65.0, numeric.getMax());
    }

    @Test
    void testUnbalancedTagBracketsThrow() {
        assertThrows(IllegalArgumentException.class, () -> RedisSearchQueryParser.parse("@street:{main"));
    }

    @Test
    void testUnbalancedNumericBracketsThrow() {
        assertThrows(IllegalArgumentException.class, () -> RedisSearchQueryParser.parse("@age:[18 65"));
    }

    @Test
    void testNumericFilterWithWrongNumberOfBoundsThrows() {
        assertThrows(IllegalArgumentException.class, () -> RedisSearchQueryParser.parse("@age:[18]"));
        assertThrows(IllegalArgumentException.class, () -> RedisSearchQueryParser.parse("@age:[18 30 40]"));
    }

    @Test
    void testNumericFilterWithNonNumericBoundsThrows() {
        // open-ended ("+inf"/"-inf") and exclusive ("(" prefix) ranges are out of scope: neither
        // parses as a plain double, so both are rejected the same way as any other malformed bound
        assertThrows(IllegalArgumentException.class, () -> RedisSearchQueryParser.parse("@age:[+inf -inf]"));
        assertThrows(IllegalArgumentException.class, () -> RedisSearchQueryParser.parse("@age:[(18 65]"));
    }

    @Test
    void testNullQueryThrows() {
        assertThrows(IllegalArgumentException.class, () -> RedisSearchQueryParser.parse(null));
    }
}
