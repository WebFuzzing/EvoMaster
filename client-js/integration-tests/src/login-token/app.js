const bodyParser = require("body-parser");
const express = require("express");
const Auth = require("./Auth");

const app = express();
app.use(bodyParser.json());

const token = "3BDD0h9iQ4";

app.post("/login", (req, res) => {

    if (req.body.username === "foo" && req.body.password  === "foo"){
        res.status(200)
        res.json(new Auth("foo", "foo", token))
    }else {
        res.status(401);
        res.send();
    }
});

app.get("/check", (req, res) => {

    const authHeader = req.headers.authorization;
    let status = 401;
    if (authHeader && authHeader.split(' ').length === 2 && authHeader.split(' ')[0] === "token" && authHeader.split(' ')[1] === token){
        status = 200;
    }

    res.status(status);
    res.json("OK_check")

});


app.get("/swagger.json", (req, res) => {

    const swagger = {
        openapi: "3.0.3",
        servers: [{url: "http://localhost:8080"}],
        info: {
            version: "1.0.0",
            title: "login-token"
        },
        paths: {
            "/login": {
                post: {
                    summary: "login",
                    description: "Login",
                    operationId: "Login",
                    parameters: [
                        {
                            name: "body",
                            in: "body",
                            required: true,
                            description: "Credentials to use",
                            schema: {
                                $ref: "#/definitions/Auth"
                            }
                        }
                    ],
                    responses: {
                        200: {
                            description: "OK",
                            schema: {
                                $ref: "#/definitions/Auth"
                            }
                        },
                        401: {
                            description: "Unauthorized"
                        }
                    }
                }
            },
            "/check": {
                get: {
                    summary: "check",
                    description: "check",
                    security: [
                        {
                            Token: []
                        }
                    ],
                    operationId: "check",
                    responses: {
                        200: {
                            description: "OK",
                            schema: {
                                type: "string"
                            }
                        },
                        401: {
                            description: "Unauthorized"
                        }
                    }
                },
            }
        },
        definitions: {
            Auth: {
                type: "object",
                properties: {
                    username: {type: "string", description: "name"},
                    password: {type: "string", description: "password"},
                    token: {type: "string", description: "token"}
                },
                title: "Auth",
                required: [
                    "username",
                    "password"
                ]
            }
        }
    };

    res.status(200);
    res.json(swagger);
});


module.exports = app;

