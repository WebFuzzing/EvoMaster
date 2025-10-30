package com.foo.spring.rest.opensearch.students;

import com.foo.spring.rest.opensearch.OpenSearchController;
import com.opensearch.students.OpenSearchStudentsApp;

public class OpenSearchStudentsController extends OpenSearchController {
    public OpenSearchStudentsController() {
        super("students", OpenSearchStudentsApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.opensearch.students";
    }
}
