import bodyParser from "body-parser";
import express from "express";
import repository from "./repository";

const app = express();
// to handle JSON payloads
app.use(bodyParser.json());

/*
    Return (ie GET) all the books.
    Can filter by publication year using the query parameter "since".
    The books will be returned as a list of JSON objects.
 */
app.get("/books", (req, res) => {

    /*
        Read the query parameters, if any, eg:

        http://localhost:8080/books?since=2001
     */
    const since = req.query.since;

    if (since !== undefined && since !== null) {
        res.json(repository.getAllBooksSince(since));
    } else {
        res.json(repository.getAllBooks());
    }
});

/*
    Note the use of ":" to represent a variable placeholder.
    Here we return a specific book with a specific id, eg
    "http://localhost:8080/books/42"
 */
app.get("/books/:id", (req, res) => {

    const book = repository.getBook(req.params.id);

    if (book === undefined || book === null) {
        res.status(404);
        res.send();
    } else {
        res.json(book);
    }
    /*
        Either "send()" or "json()" needs to be called, otherwise the
        call of the API will hang waiting for the HTTP response.
        The "json()" also setups the other needed headers related to the
        body, eg things like content-type and content-length
     */
});

/*
    Handle HTTP DELETE request on a book specified by id
 */
app.delete("/books/:id", (req, res) => {

    const deleted = repository.deleteBook(req.params.id);
    if (deleted) {
        res.status(204);
    } else {
        // this can happen if book already deleted or does not exist
        res.status(404);
    }
    res.send();
});

/*
    Create a new book. The id will be chosen by the server.
    Such method should return the "location" header telling
    where such book can be retrieved (ie its URL)
 */
app.post("/books", (req, res) => {

    const dto = req.body;

    const id = repository.createNewBook(dto.title, dto.author, dto.year);

    res.status(201); // created
    res.header("location", "/books/" + id);
    res.send();
});

/*
    Handle PUT request, which completely replace the resource
    with a new one
 */
app.put("/books/:id", (req, res) => {

    if (req.params.id !== req.body.id) {
        res.status(409);
        res.send();
        return;
    }

    const updated = repository.updateBook(req.body);

    if (updated) {
        res.status(204);
    } else {
        // this can happen if entity did not exist
        res.status(404);
    }
    res.send();
});

app.get("/swagger.json", (req, res) => {

    const swagger = {
        swagger: "2.0",
        info: {
            description: "Some description",
            version: "1.0",
            title: "API"
        },
        // host: "localhost:8080",
        basePath: "/",
        tags: [{
            name: "BookApi",
            description: "Book API example"
        }],
        paths: {
            "/books": {
                get: {
                    tags: ["BookApi"],
                    summary: "Get all books",
                    operationId: "getBooks",
                    produces: ["application/json"],
                    responses: {
                        200: {
                            description: "OK",
                            schema: {
                                type: "array",
                                items: {$ref: "#/definitions/BookDto"}
                            }}
                    },
                    parameters: [{
                        name: "since",
                        in: "query",
                        description: "Filter on year",
                        required: false,
                        type: "number"
                    }]
                },
                post: {
                    tags: ["BookApi"],
                    summary: "Create book",
                    operationId: "createBook",
                    consumes: ["application/json"],
                    produces: ["application/json"],
                    parameters: [{
                        in: "body",
                        name: "dto",
                        description: "book payload",
                        required: true,
                        schema: {$ref: "#/definitions/BookDto"}
                    }],
                    responses: {201: {description: "Created"}}
                }
            },
            "/books/{id}": {
                get: {
                    tags: ["BookApi"],
                    summary: "Get a single book specified by id",
                    operationId: "getBookById",
                    produces: ["application/json"],
                    parameters: [
                        {
                            name: "id",
                            in: "path",
                            description: "The id of the book",
                            required: false,
                            type: "string"
                        }
                    ],
                    responses: {
                        200: {
                            description: "OK",
                            schema: {$ref: "#/definitions/BookDto"}
                        }
                    }
                },
                put: {
                    tags: ["BookApi"],
                    summary: "Update an existing book",
                    operationId: "updateUsingPUT",
                    consumes: ["application/json"],
                    produces: ["application/json"],
                    parameters: [
                        {
                            in: "body",
                            name: "dto",
                            description: "The book that will replace the old one. Cannot change its id though.",
                            required: true,
                            schema: {$ref: "#/definitions/BookDto"}
                        },
                        {
                            name: "id",
                            in: "path",
                            description: "The id of the book",
                            required: true,
                            type: "string"
                        }
                    ],
                    responses: {
                        200: {description: "OK",
                                schema: {
                                    type: "object"
                                }
                            }
                        }
                },
                delete: {
                    tags: ["BookApi"],
                    summary: "Delete a book with the given id",
                    operationId: "deleteUsingDELETE",
                    produces: ["application/json"],
                    parameters: [
                        {
                            name: "id",
                            in: "path",
                            description: "The id of the book",
                            required: true,
                            type: "string"
                        }
                    ],
                    responses: {
                        204: {
                            description: "OK",
                            schema: {
                                type: "object"
                            }
                        }
                    }
                }
            }
        },
        definitions: {
            BookDto: {
                type: "object",
                properties: {
                    id: {type: "string", description: "TODO"},
                    title: {type: "string", description: "TODO"},
                    author: {type: "string", description: "TODO"},
                    year: {type: "numberic", description: "TODO"},
                },
                title: "BookDto"
            }
        }
    };

    res.status(200);
    res.json(swagger);
});

export default app;
