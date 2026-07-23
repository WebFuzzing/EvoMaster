package org.evomaster.client.java.controller.mongo;

import org.evomaster.client.java.controller.mongo.operations.QueryOperation;
import org.evomaster.client.java.controller.mongo.selectors.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Determines to which operation a query correspond.
 */
public class QueryParser {

    List<QuerySelector> selectors = Arrays.asList(
            new EqualsSelector(),
            new NotEqualsSelector(),
            new LessThanEqualsSelector(),
            new LessThanSelector(),
            new GreaterThanSelector(),
            new GreaterThanEqualsSelector(),
            new AndSelector(),
            new OrSelector(),
            new NorSelector(),
            new InSelector(),
            new NotInSelector(),
            new AllSelector(),
            new SizeSelector(),
            new ElemMatchSelector(),
            new ModSelector(),
            new NotSelector(),
            new ExistsSelector(),
            new TypeSelector(),
            new NearSphereSelector(),
            new ImplicitSelector()
    );

    public QueryOperation parse(Object bsonDocument) {
        if (bsonDocument == null) {
            return null;
        }

        List<QueryOperation> results = selectors.stream()
                .map(selector -> selector.getOperation(bsonDocument))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (results.isEmpty()) {
            return null;
        }

        if (results.size() > 1) {
            // If multiple selectors match, prioritize those that are NOT ImplicitSelector
            List<QueryOperation> nonImplicit = selectors.stream()
                    .filter(s -> !(s instanceof ImplicitSelector))
                    .map(selector -> selector.getOperation(bsonDocument))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (nonImplicit.size() == 1) {
                return nonImplicit.get(0);
            }
            // If still ambiguous or none, we might have a problem, but let's return the first one
            // or null if it's really ambiguous.
            // For now, return the first non-implicit, or first result.
            return nonImplicit.isEmpty() ? results.get(0) : nonImplicit.get(0);
        }

        return results.get(0);
    }
}
