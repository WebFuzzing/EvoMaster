const bodyParser = require("body-parser");
const express = require("express");

const app = express();
app.use(bodyParser.json());


const cities = {
    ["oslo"] : 634463,
    ["Notodden"] : 12359
}

const countries = ["Norway", "China"]

app.get("/populationByCity", (req, res) => {
    if(req.query["value"] && cities[req.query["value"]]){
        res.status(200);
        res.json("OK_FOUND_squareBrackets")
    } else {
        res.status(400);
        res.json("FAILED_squareBrackets")
    }
});

app.get("/findCountry", (req, res) => {
    if(req.query["value"] && countries.includes(req.query["value"])){
        res.status(200);
        res.json("OK_FOUND_array")
    } else {
        res.status(400);
        res.json("FAILED_array")
    }
});


app.get("/swagger.json", (req, res) => {

    const swagger = {
        openapi: "3.0.3",
        servers: [{url: "http://localhost:8080"}],
        info: {
            version: "1.0.0",
            title: "taint-squareBrackets"
        },
        paths: {
            "/populationByCity": {
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
            "/findCountry": {
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

