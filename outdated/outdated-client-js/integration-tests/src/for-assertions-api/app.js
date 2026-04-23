const bodyParser = require("body-parser");
const express = require("express");

const app = express();
app.use(bodyParser.json());

app.get("/data", (req, res) => {

    const obj = {
        a: 42,
        b: "hello",
        c: [1000, 2000, 3000],
        d: {
            e: 66,
            f: "bar",
            g: {
                h: ["xvalue", "yvalue"]
            }
        },
        i: true,
        l: false
    };

    res.status(200);
    res.json(obj);
});

app.post("/data", (req, res) => {
    res.status(201);
    res.send();
});

app.get("/simpleNumber", (req, res) => {
    res.status(200);
    res.json(42)
});

app.get("/simpleString", (req, res) => {
    res.status(200);
    res.json("simple-string")
});

app.get("/simpleText", (req, res) => {
    res.status(200);
    res.set('Content-Type', 'text/plain');
    res.send("simple-text");
});

app.get("/array", (req, res) => {
    res.status(200);
    res.json([123, 456]);
});

app.get("/arrayObject", (req, res) => {
    res.status(200);
    res.json([{x: 777}, {x: 888}]);
});

app.get("/arrayEmpty", (req, res) => {
    res.status(200);
    res.json([]);
});

app.get("/objectEmpty", (req, res) => {
    res.status(200);
    res.json({});
});


app.get("/swagger.json", (req, res) => {

    const swagger = {
        openapi: "3.0.3",
        servers: [{url: "http://localhost:8080"}],
        info: {
            version: "1.0.0",
            title: "for-assertions-api"
        },
        paths: {
            "/simpleNumber": {
                get: {
                    response: {
                        "200": "OK",
                        content: {
                            "application/json": {
                                schema: {
                                    type: "integer"
                                }
                            }
                        }
                    }
                }
            },
            "/simpleString": {
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
            "/simpleText": {
                get: {
                    response: {
                        "200": "OK",
                        content: {
                            "text/plain": {
                                schema: {
                                    type: "string"
                                }
                            }
                        }
                    }
                }
            },
            "/array": {
                get: {
                    response: {
                        "200": "OK",
                        content: {
                            "application/json": {
                                schema: {
                                    type: "array",
                                    items: {
                                        type: "integer"
                                    }
                                }
                            }
                        }
                    }
                }
            },
            "/arrayEmpty": {
                get: {
                    response: {
                        "200": "OK",
                        content: {
                            "application/json": {
                                schema: {
                                    type: "array",
                                    items: {
                                        type: "integer"
                                    }
                                }
                            }
                        }
                    }
                }
            },
            "/arrayObject": {
                get: {
                    response: {
                        "200": "OK",
                        content: {
                            "application/json": {
                                schema: {
                                    type: "array",
                                    items: {
                                        type: "object",
                                        properties: {
                                            "x": {type: "integer"}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            "/emptyObject": {
                get: {
                    response: {
                        "200": "OK",
                        content: {
                            "application/json": {
                                schema: {
                                    type: "object",
                                    properties: {
                                        "x": {type: "integer"}
                                    }
                                }
                            }
                        }
                    }
                }
            },
            "/data": {
                post: {
                    responses: {
                        "201": {description: "Created"}
                    },
                    requestBody: {
                        content: {
                            "application/json": {
                                schema: {
                                    type: "string"
                                }
                            }
                        },
                        required: true
                    }
                },
                get: {
                    responses: {
                        "200": {
                            description: "OK",
                            content: {
                                "application/json": {
                                    schema: {
                                        type: "object",
                                        properties: {
                                            "a": {type: "integer"},
                                            "b": {type: "string"},
                                            "c": {
                                                type: "array",
                                                items: {
                                                    type: "integer"
                                                }
                                            },
                                            "d": {
                                                type: "object",
                                                properties: {
                                                    "e": {type: "integer"},
                                                    "f": {type: "string"},
                                                    "g": {
                                                        type: "object",
                                                        properties: {
                                                            "h": {
                                                                type: "array",
                                                                items: {
                                                                    type: "string"
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                }
            }
        }
    };

    res.status(200);
    res.json(swagger);
});


module.exports = app;

