package com.foo.rpc.examples.spring.fakemockobject.external.fake.api;

import java.util.List;

public interface GetApiData {

	public List<ExApiDto> all();

	public ExApiDto one(String name);
}
