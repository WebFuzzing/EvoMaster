const bodyParser = require("body-parser");
const express = require("express");

const app = express();
app.use(bodyParser.json());


app.get("/constantEqual", (req, res) => {

    if(req.query["value"] === "Hello world!!! Even if this is a long string, it will be trivial to cover with taint analysis"){
        res.status(200);
        res.json("OK_hello")
    } else {
        res.status(400);
        res.json("FAILED")
    }
});

app.get("/constantDifferent", (req, res) => {

    if(req.query["value"] !== "FooBar!!! Even if this is a long string, it will be trivial to cover with taint analysis"){
        res.status(400);
        res.json("FAILED")
    } else {
        res.status(200);
        res.json("OK_foo")
    }
});


app.get("/swagger.json", (req, res) => {

    const swagger = {
        openapi: "3.0.3",
        servers: [{url: "http://localhost:8080"}],
        info: {
            version: "1.0.0",
            title: "taint-string"
        },
        paths: {
            "/constantEqual": {
                get: {
                    response: {
                        "200": "OK",
                        content: {
                            "application/json": {
                                schema: {
                                    type: "string"
                                }
                            }
                        }
                    },
                    parameters: [
                        {
                            name: "value",
                            in: "query",
                            required: true,
                            schema: {
                                type: "string"
                            }
                        }
                    ]
                }
            },
            "/constantDifferent": {
                get: {
                    response: {
                        "200": "OK",
                        content: {
                            "application/json": {
                                schema: {
                                    type: "string"
                                }
                            }
                        }
                    },
                    parameters: [
                        {
                            name: "value",
                            in: "query",
                            required: true,
                            schema: {
                                type: "string"
                            }
                        }
                    ]
                }
            }
        }
    };

    res.status(200);
    res.json(swagger);
});


module.exports = app;

