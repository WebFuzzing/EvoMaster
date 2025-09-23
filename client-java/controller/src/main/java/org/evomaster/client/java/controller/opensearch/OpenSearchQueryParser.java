package org.evomaster.client.java.controller.opensearch;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;
import org.evomaster.client.java.controller.opensearch.selectors.TermSelector;
import org.evomaster.client.java.controller.opensearch.selectors.TermsSelector;
import org.evomaster.client.java.controller.opensearch.selectors.TermsSetSelector;
import org.evomaster.client.java.controller.opensearch.selectors.IdsSelector;
import org.evomaster.client.java.controller.opensearch.selectors.RangeSelector;
import org.evomaster.client.java.controller.opensearch.selectors.PrefixSelector;
import org.evomaster.client.java.controller.opensearch.selectors.ExistsSelector;
import org.evomaster.client.java.controller.opensearch.selectors.FuzzySelector;
import org.evomaster.client.java.controller.opensearch.selectors.WildcardSelector;
import org.evomaster.client.java.controller.opensearch.selectors.RegexpSelector;
import org.evomaster.client.java.controller.opensearch.selectors.BoolSelector;
import org.evomaster.client.java.controller.opensearch.selectors.MatchSelector;
import org.evomaster.client.java.controller.opensearch.selectors.QuerySelector;

public class OpenSearchQueryParser {

    List<QuerySelector> selectors = Arrays.asList(
        new TermSelector(),
        new TermsSelector(),
        new TermsSetSelector(),
        new IdsSelector(),
        new RangeSelector(),
        new PrefixSelector(),
        new ExistsSelector(),
        new FuzzySelector(),
        new WildcardSelector(),
        new RegexpSelector(),
        new BoolSelector(),
        new MatchSelector()
    );

    public QueryOperation parse(Object query) {
        return selectors.stream()
            .map(selector -> selector.getOperation(query))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }
}

