package com.foo.spring.rest.opensearch.findstring;

import com.foo.spring.rest.opensearch.OpenSearchController;
import com.opensearch.findstring.OpenSearchFindStringApp;

public class OpenSearchFindStringController extends OpenSearchController {
    public OpenSearchFindStringController() {
        super("findstring", OpenSearchFindStringApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.opensearch.findstring";
    }

}
