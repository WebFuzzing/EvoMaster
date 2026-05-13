package com.foo.rest.examples.spring.openapi.v3.http429long

import com.foo.rest.examples.spring.openapi.v3.Http429Long.Http429LongApplication
import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.http429short.Http429ShortApplication

class Http429LongController : SpringController(Http429LongApplication::class.java)