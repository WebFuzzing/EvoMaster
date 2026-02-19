package com.foo.spring.rest.opensearch.queries;

import com.foo.spring.rest.opensearch.OpenSearchController;
import com.opensearch.queries.OpenSearchQueriesApp;

public class OpenSearchQueriesController extends OpenSearchController {
    public OpenSearchQueriesController() {
        super("products", OpenSearchQueriesApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.opensearch.queries";
    }
}
