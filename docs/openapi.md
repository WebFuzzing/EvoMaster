# OpenAPI Schema

To test a RESTful API, _EvoMaster_ requires the presence of a schema.
There are different ways to write API schemata, where [OpenAPI](https://www.openapis.org/) is 
arguably the most common and used. 
Such format (previously called `Swagger`) is supported by _EvoMaster_, both *v2* and *v3*.

If your API does not have such a schema, you have two options:
1. Write it by hand.
2. Use a tool to automatically generate it from your source code.

This latter option depends on which language and frameworks you are using to implement your API.
For example, for *Spring* applications, you can look at [SpringDoc](https://github.com/springdoc/springdoc-openapi).
Adding a schema then is as easy as just adding such dependency to the classpath.
Then, the schema will be accessible on the endpoint `/v3/api-docs`.  