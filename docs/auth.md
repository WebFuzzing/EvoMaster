# Security: Authentication


The API/SUT might require authenticated requests (e.g., when _Spring Security_ is used).
And there are several different mechanism to do authentication.
In _EvoMaster_, few mechanisms are supported, as discussed next.
How to set them up will depend on whether you are doing _white_ or _black_ box testing. 

__NOTE:__ If the type of authentication you need is not currently supported, please open a new feature request on [our issue page](https://github.com/EMResearch/EvoMaster/issues).     


The following documentation is divided on whether the tested API is a REST/GraphQL one, or RPC.

## REST/GraphQL APIs

### Black-Box Testing

Since version `1.3.0`, it is possible to specify custom HTTP headers (e.g., to pass auth tokens), using the options from `--header0` to `--header2` (in case more than one HTTP header is needed).


This works for static information, e.g., think about HTTP Basic (RFC-7617), where userId and password are sent at each call. 
Such credentials (i.e., userId/password) would not change per user, and so can be specified once.

Unfortunately, this would not work for _dynamic tokens_ or _cookies_.
For example, if an auth token needs to be fetched from a login endpoint
(e.g., a POST on a `/login` with username and password),
then such call has to be done manually (and then the token can be passed
to _EvoMaster_ with `--header0` option, e.g., `--header0 "cookie: <token>"`).
Such call could then be done with other tools like Postman and cURL.
Still, this can be tedious, especially if the lifespan of the token is short.

To enable this use case, since version `2.1.0` we support a declarative approach to specify how to make calls to a login endpoint, and how to then use such response to extract an auth token for the following authenticated requests.
Because several settings need to be specified, these auth declarations need to be put in the configuration file (default `./em.yaml`, but TOML format is supported as well).

Let's consider this following example of configuration:

```
auth:
  - name: foo
    loginEndpointAuth:
      payloadRaw: "{\"username\": \"foo\", \"password\": \"123\"}"
  - name: bar
    loginEndpointAuth:
      payloadUserPwd:
        username: bar
        password: "456"
        usernameField: username
        passwordField: password

authTemplate:
  loginEndpointAuth:
    endpoint: /login
    verb: POST
    contentType: application/json
    expectCookies: true
```

Here, 2 example users are specified under `auth`: `foo` and `bar`, with their passwords.
Note: to be able to test the API with such credentials, such users should exist in its database. 

The payload sent to the login endpoint could be either specified as it is (i.e., with `payloadRaw`), or with username/password separately (from which the right payload is automatically derived and formatted based on the `contentType`, e.g., `application/json` or `application/x-www-form-urlencoded`).

There are several pieces of information that would be the same for both users:
* `endpoint`: the path for the endpoint with the login (can use `externalEndpointURL` if it is on a different server).
* `verb`: the HTTP verb used to make the request (typically it is a `POST`).
* `contentType`: specify how the payload will be sent (e.g., JSON in this case).
* `expectCookies`: tell _EvoMaster_ that from the login endpoint we expect to get a cookie for the authentication.

If instead of cookies we have a token to be extracted from the JSON response of the login endpoint, we can use something like:

```
auth:
  loginEndpointAuth:
     # ... other data here
     token:
        headerPrefix="Bearer "
        extractFromField = "/token/authToken"
        httpHeaderName="Authorization"
```

What will happen here is that _EvoMaster_ will make a POST to `/login` and then extract the field `token.authToken` from the JSON response (the entry `extractFromField` is treated as a JSON Pointer (RFC 6901)). 
Assume for example we have `token.authToken = 123456`.
In the following auth requests, then _EvoMaster_ will make requests with HTTP header: `Authorization:Bearer 123456`.


These configuration auth declaration are mapped into the Java class `org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto`.
The different fields are then validated on load.
To read the documentation of such fields, you can look at the [JavaDocs for that class](https://javadoc.io/doc/org.evomaster/evomaster-client-java-controller-api/latest/index.html).
Note: the previous link is for the documentation of released versions of _EvoMaster_. 
If you are using latest SNAPSHOT (e.g., built directly from latest `master` branch of the Git repository), the DTO definitions could be updated (e.g., there was major refactoring after version `2.0.0`). 
In such case, you could directly look at the documentation in the [AuthenticationDto class](https://github.com/EMResearch/EvoMaster/blob/master/client-java/controller-api/src/main/java/org/evomaster/client/java/controller/api/dto/auth/AuthenticationDto.java). 




### White-Box Testing

The same type of auth configuration done for black-box testing does apply here as well for white-box testing.
However, in white-box testing, there is a further option to configure auth info.
This can be done directly in the `driver` classes, specifically by implementing the method  `List<AuthenticationDto> getInfoForAuthentication()`.
Here, a list of `AuthenticationDto` objects is returned.
This DTOs can be instantiated directly. 

The `org.evomaster.client.java.controller.AuthUtils` can be used to simplify the creation of such
configuration objects, e.g., by using methods like `getForDefaultSpringFormLogin()`.
Consider the following example from the `proxyprint` case study
in the [EMB repository](https://github.com/EMResearch/EMB).

```
@Override
public List<AuthenticationDto> getInfoForAuthentication() {
        return Arrays.asList(
                AuthUtils.getForBasic("admin","master","1234"),
                AuthUtils.getForBasic("consumer","joao","1234"),
                AuthUtils.getForBasic("manager","joaquim","1234"),
                AuthUtils.getForBasic("employee","mafalda","1234")
        );
}
```

Here, auth is done with [RFC-7617](https://tools.ietf.org/html/rfc7617) _Basic_.
Four different users are defined.
When _EvoMaster_ generates test cases, it can decide to use some of those auth credentials, and
generate the valid HTTP headers for them.


Although _EvoMaster_ can read and analyze the content of a SQL database, it cannot reverse-engineer the
hashed passwords.
These must be provided with `getInfoForAuthentication()`.
If such auth info is stored in a SQL database, and you are resetting the state of such database in the
`resetStateOfSUT()` method, you will need there to recreate the login/password credentials as well.
You could write such auth setup in a `init_db.sql` SQL script file, and then
in `resetStateOfSUT()` execute:

```
DbCleaner.clearDatabase_H2(connection);
SqlScriptRunnerCached.runScriptFromResourceFile(connection,"/init_db.sql");
```     

__IMPORTANT__: since version `1.5.0`, if delegating the resetting of SQL database to _EvoMaster_ (i.e., without `withDisabledSmartClean()`), then initializing scripts should be set directly on the `DbSpecification` object, e.g., `new DbSpecification(DatabaseType.H2,sqlConnection)
.withInitSqlOnResourcePath("/init_db.sql")`.
Look at the JavaDocs of `DbSpecification` to see all the available utility methods.

Note: at the moment _EvoMaster_ is not able to register new users on the fly with HTTP requests,
and use such info to authenticate its following requests. 


## RPC APIs

<mark>Documentation under construction</mark>


