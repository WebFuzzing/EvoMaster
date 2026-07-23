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
        List<QueryOperation> results = selectors.stream()
                .map(selector -> selector.getOperation(bsonDocument))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (results.size() != 1) {
            return null;
        }

        return results.get(0);
    }
}
