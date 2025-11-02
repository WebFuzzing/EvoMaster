package com.foo.rpc.examples.spring.fakemockobject.external.fake.db;

import java.util.List;

public interface GetDbData {

	public List<ExDbDto> all();

	public ExDbDto one(String name);
}
