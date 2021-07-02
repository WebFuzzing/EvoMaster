const bodyParser = require("body-parser");
const express = require("express");

const app = express();
app.use(bodyParser.json());


app.get("/a", (req, res) => {
    res.status(200);
    res.json("foo a")
});

app.get("/b", (req, res) => {
    res.status(400);
    res.json("foo b")
});

app.get("/c", (req, res) => {
    res.status(404);
    res.json("foo c")
});

app.get("/d", (req, res) => {
    res.status(500);
    res.json("foo d")
});




app.get("/swagger.json", (req, res) => {

    const swagger = {
        openapi: "3.0.3",
        servers: [{url: "http://localhost:8080"}],
        info: {
            version: "1.0.0",
            title: "status-api"
        },
        paths: {
            "/a": {
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
                    }
                }
            },
            "/b": {
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
                    }
                }
            },
            "/c": {
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
                    }
                }
            },
            "/d": {
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
                    }
                }
            }
        }
    };

    res.status(200);
    res.json(swagger);
});


module.exports = app;

