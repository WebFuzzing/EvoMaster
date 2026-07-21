# Adding "Examples" with OAI Overlay 

## Intro

EvoMaster has native support for [OAI Overlay](https://github.com/OAI/Overlay-Specification) files.
This is done with [option parameters](./options.md) such as `--overlay`, `--overlayFileSuffixes` and `--overlayLenient`. 
For example, EvoMaster can be run with:

`evomaster --schema $SCHEMA --overlay $OVERLAY`

with `$OVERLAY` specifying where the Overlay file(s) is/are located.


> Goal: The main reason to have native Overlay support in a fuzzer is to be able to provide `examples` entries to help the fuzzing.

Most fuzzers are able to use data in the `example` and `examples` fields of an OpenAPI schema. 
However, having testers manually adding those entries in the schema for their testing needs (e.g., specific combinations of values, or ids of data in the databases of their testing environment) is not viable. 
What if a new version of the schema come? 
That would require manually re-adding all the example entries to the new version.
Putting such data in an Overlay file solves the problem.

## Problem Statement

In black-box testing of REST APIs, where the only information available is an OpenAPI schema and the dynamic feedback/responses from interacting with the tested API, a fuzzer can struggle to generate good inputs in a reasonable time.
Hence, users can provide "hints" to the fuzzer, e.g., ids existing in the database, to help the fuzzing process.

Currently, most fuzzers already can support this, as they can use `example` and `examples` entries in the OpenAPI schema when generating input data.
The problem, though, is that this would require to modify the schema with test data.
This is not viable in the long run.
As soon as there is a new update in the schema, all the examples would need to be manually re-added to the new schema.
The alternative to use a separated, custom format to provide input data hints to the fuzzer is not a particularly good option.
It would create a "vendor-lockin" for each specific fuzzer.
Also, IDE support for such format (e.g., autocomplete and validation) would be necessarily limited, besides the time needed to invest in learning a new custom format.

A possible solution is to use [OAI Overlay](https://spec.openapis.org/overlay/latest.html).
It provides a way to do define transformation rules that can be applied to an existing OpenAPI schema.
Such transformations can be used to add `examples` entries in the OpenAPI schema of the tested API.
If it works, it would solve the addressed problem, as Overlay is a standard, used also outside of software testing needs.
The transformation could be applied on an OpenAPI schema before giving it as input to the fuzzer (e.g., using any existing Overlay merge tool).
Alternatively, if a fuzzer has "native" support for Overlay (and EvoMaster does!), then it is just a matter of giving the Overlay file as input (and EvoMaster will automatically apply the transformation internally).



## Writing Overlay Files

Overlay is a relative new standard.
Documentation, examples, and IDE-support is still at early stages.
But we can expect things to get better in the next coming years.
This means that, today, we cannot provide you precise instructions or suggestions on how those files should be written (e.g., any good IDE-plugin that support Overlay files).
If there is any error when applying an Overlay file to an existing OpenAPI schema, then EvoMaster will crash and give you an error message.
This is a start, but not as good as having an IDE that validates the files while writing them.

Also note that, as it is an official standard, it is quite likely that your LLM of choice might have been trained on it, and being able to write some draft-template of Overlay transformations for you.

Another things to consider is that you should avoid adding `example` (which is deprecated in OpenAPI) but rather used _named_ `examples`.
Names of examples should be unique.
Names re-used in different parameters (query and path) or body payloads will be treated as the same example combination by EvoMaster.
For example, let's say you have a `POST /api/foo?x=1&y=2` with body `B`.
If you want to test a specific combination of `x`, `y` and `B`, you can create `examples` entries with the same name.

You can search online on tips and instructions on how to write Overlay files.
Here, we provide an example to use as reference.
Consider the following simple API with 1 POST endpoint, having 2 query parameters `x` and `y`, and a body payload:

```
openapi: 3.1.0
info:
  title: Simple API
  version: 1.0.0
servers:
  - url: https://api.example.com/v1
paths:
  /api:
    post:
      parameters:
        - name: x
          in: query
          schema:
            type: string
        - name: y
          in: query
          schema:
            type: string
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                id:
                  type: string
                name:
                  type: string
      responses:
        '200':
          description: Successful response
```

Now, consider adding 2 named examples `foo` and `bar`.
`foo` adds an entry just for the parameter `y`, whereas `bar` adds to the combination `x`, `y` and body.
This can be represented with the following Overlay definition:

```
overlay: 1.1.0
info:
  title: Add Examples to API
  version: 1.0.0
actions:
  - target: '$.paths["/api"].post.parameters[?(@.name=="y")]'
    description: Add foo example to query parameter y
    update:
      examples:
        foo:
          value: Foo

  - target: '$.paths["/api"].post.parameters[?(@.name=="x")]'
    description: Add bar example to query parameter x
    update:
      examples:
        bar:
          value: Bar

  - target: '$.paths["/api"].post.parameters[?(@.name=="y")]'
    description: Add bar example to query parameter y
    update:
      examples:
        bar:
          value: Bar

  - target: '$.paths["/api"].post.requestBody.content["application/json"]'
    update:
      examples:
        bar:
          value:
            id: "bar-id-123"
            name: "Bar Name Example"
```

When these 4 transformation actions are applied, the resulting schema will be:

```
openapi: 3.1.0
info:
  title: Simple API
  version: 1.0.0
servers:
  - url: https://api.example.com/v1
paths:
  /api:
    post:
      parameters:
        - name: x
          in: query
          schema:
            type: string
          examples:
            bar:
              value: Bar
        - name: y
          in: query
          schema:
            type: string
          examples:
            bar:
              value: Bar
            foo:
              value: Foo
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                id:
                  type: string
                name:
                  type: string
            examples:
              bar:
                value:
                  id: "bar-id-123"
                  name: "Bar Name Example"
      responses:
        '200':
          description: Successful response
```