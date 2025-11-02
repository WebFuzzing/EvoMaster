import request from "supertest";
import app from "../../sut/books-api/app";
import rep from "../../sut/books-api/repository";

function initWithSomeBooks() {

    rep.reset();

    rep.createNewBook("The Hitchhiker's Guide to the Galaxy", "Douglas Adams", 1979);
    rep.createNewBook("The Lord of the Rings", "J. R. R. Tolkien", 1954);
    rep.createNewBook("The Last Wish", "Andrzej Sapkowski", 1993);
    rep.createNewBook("A Game of Thrones", "George R. R. Martin", 1996);
    rep.createNewBook("The Call of Cthulhu", "H. P. Lovecraft", 1928);
}

beforeEach(() => {initWithSomeBooks(); });

test("Test get all", async () => {

    const response = await request(app).get("/books");

    expect(response.status).toBe(200);
    expect(response.body.length).toBe(5);
});

test("Test not found book", async () => {

    const response = await request(app).get("/books/-3");
    expect(response.status).toBe(404);
});

test("Test retrieve each single book", async () => {

    const responseAll = await request(app).get("/books");
    expect(responseAll.status).toBe(200);

    const books = responseAll.body;
    expect(books.length).toBe(5);

    for (const book of books) {

        const res = await request(app).get("/books/" + book.id);

        expect(res.body.title).toBe(book.title);
    }
});

test("Test create book", async () => {

    let responseAll = await request(app).get("/books");
    const n = responseAll.body.length;

    const title = "foo";

    const resPost = await request(app)
        .post("/books")
        .send({title, author: "bar", year: 2018})
        .set("Content-Type", "application/json");

    expect(resPost.status).toBe(201);
    const location = resPost.header.location;

    // should had been increased by 1
    responseAll = await request(app).get("/books");
    expect(responseAll.body.length).toBe(n + 1);

    const resGet = await request(app).get(location);
    expect(resGet.status).toBe(200);
    expect(resGet.body.title).toBe(title);
});

test("Delete all books", async () => {

    let responseAll = await request(app).get("/books");
    expect(responseAll.status).toBe(200);

    const books = responseAll.body;
    expect(books.length).toBe(5);

    for (const book of books) {

        const res = await request(app).delete("/books/" + book.id);
        expect(res.status).toBe(204);
    }

    responseAll = await request(app).get("/books");
    expect(responseAll.status).toBe(200);
    expect(responseAll.body.length).toBe(0);
});

test("Update book", async () => {

    const title = "foo";

    // create a book
    const resPost = await request(app)
        .post("/books")
        .send({title, author: "bar", year: 2018})
        .set("Content-Type", "application/json");
    expect(resPost.status).toBe(201);
    const location = resPost.header.location;

    // get it back
    let resGet = await request(app).get(location);
    expect(resGet.status).toBe(200);
    expect(resGet.body.title).toBe(title);

    const modified = "bar";
    const id = location.substring(location.lastIndexOf("/") + 1, location.length);

    // modify it with PUT
    const resPut = await request(app)
        .put(location)
        .send({id, title: modified, author: "bar", year: 2018})
        .set("Content-Type", "application/json");
    expect(resPut.status).toBe(204);

    // get it back again to verify the change
    resGet = await request(app).get(location);
    expect(resGet.status).toBe(200);
    expect(resGet.body.title).toBe(modified);
});
