package com.foo.spring.rest.opensearch.age;

import com.foo.spring.rest.opensearch.OpenSearchController;
import com.opensearch.age.OpenSearchAgeApp;

public class OpenSearchAgeController extends OpenSearchController {
    public OpenSearchAgeController() {
        super("age", OpenSearchAgeApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.opensearch.age";
    }
}
