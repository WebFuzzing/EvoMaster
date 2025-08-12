package com.foo.spring.rest.opensearch.findoneby;

import com.foo.spring.rest.opensearch.OpenSearchController;
import com.opensearch.findoneby.OpenSearchFindOneByApp;

public class OpenSearchFindOneByController extends OpenSearchController {
    public OpenSearchFindOneByController() {
        super("findoneby", OpenSearchFindOneByApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.opensearch.findoneby";
    }

}
