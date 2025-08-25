package org.evomaster.client.java.controller.opensearch;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;
import org.evomaster.client.java.controller.opensearch.selectors.RangeSelector;
import org.evomaster.client.java.controller.opensearch.selectors.TermSelector;
import org.evomaster.client.java.controller.opensearch.selectors.QuerySelector;

public class OpenSearchQueryParser {

  List<QuerySelector> selectors = Arrays.asList(new TermSelector(), new RangeSelector());

    public QueryOperation parse(Object query) {
        return selectors.stream()
            .map(selector -> selector.getOperation(query))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }
}

