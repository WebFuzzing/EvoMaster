import EMController from "../../../src/controller/EMController";
import AppController from "./app-driver";

import superagent from "superagent";
import rep from "../../sut/books-api/repository";

import c from "../../../src/controller/api/ControllerConstants";
import SutRunDto from "../../../src/controller/api/dto/SutRunDto";

function initWithSomeBooks() {

    rep.reset();

    rep.createNewBook("The Hitchhiker's Guide to the Galaxy", "Douglas Adams", 1979);
    rep.createNewBook("The Lord of the Rings", "J. R. R. Tolkien", 1954);
    rep.createNewBook("The Last Wish", "Andrzej Sapkowski", 1993);
    rep.createNewBook("A Game of Thrones", "George R. R. Martin", 1996);
    rep.createNewBook("The Call of Cthulhu", "H. P. Lovecraft", 1928);
}

const controller = new EMController(new AppController());

let controllerPort: number;
let controllerUrl: string;
let sutUrl: string;

beforeAll( async () => {
    controller.setPort(0);
    await controller.startTheControllerServer();
    controllerPort = controller.getActualPort();
    controllerUrl = "http://localhost:" + controllerPort + c.BASE_PATH;

    expect(controllerPort).not.toBe(0);
});

afterAll( async () => {
   await controller.stopTheControllerServer();
});

beforeEach(async () => {
    sutUrl = await startSut();
    initWithSomeBooks();
});

async function startSut(): Promise<string> {

    const dto = new SutRunDto();
    dto.run = true;
    dto.resetState = true;

    const res = await superagent.put(controllerUrl  + c.RUN_SUT_PATH)
        .send(dto);

    expect(res.status).toBe(204);

    const url = controller.getBaseUrlOfSUT();

    return Promise.resolve(url);
}

async function stopSut(): Promise<void> {

    const dto = new SutRunDto();
    dto.run = false;
    dto.resetState = false;

    const res = await superagent.put(controllerUrl  + c.RUN_SUT_PATH)
        .send(dto);

    expect(res.status).toBe(204);
}

async function resetSut(): Promise<void> {
    await startSut();
}

// ---- Controller -----------

test("Test start SUT", async () => {

    const url = await startSut();
    expect(url).toBeTruthy();
});

test("Test start/stop/reset SUT", async () => {

    const url = await startSut();
    initWithSomeBooks();

    let response = await superagent.get(sutUrl + "/books");
    expect(response.status).toBe(200);
    expect(response.body.length).toBe(5);

    await resetSut();

    response = await superagent.get(sutUrl + "/books");
    expect(response.status).toBe(200);
    expect(response.body.length).toBe(0);

    await stopSut();

    /*
        WARNING: NodeJS server.close() does not close shit... it only prevents new connections
        https://github.com/nodejs/node/issues/2642
     */

    /*
    expect( async () => {
        await superagent.get(sutUrl + "/books");}
    ).toThrow();
    */
});

// ---- SUT ------------------

test("Test get all", async () => {

    const response = await superagent.get(sutUrl + "/books");
    expect(response.status).toBe(200);
    expect(response.body.length).toBe(5);
});

test("Test not found book", async () => {

    const response = await superagent
        .get(sutUrl + "/books/-3")
        .ok((res) => res.status < 600);
    expect(response.status).toBe(404);
});

test("Test retrieve each single book", async () => {

    const responseAll = await superagent.get(sutUrl + "/books");
    expect(responseAll.status).toBe(200);

    const books = responseAll.body;
    expect(books.length).toBe(5);

    for (const book of books) {

        const res = await superagent.get(sutUrl + "/books/" + book.id);

        expect(res.body.title).toBe(book.title);
    }
});

test("Test create book", async () => {

    let responseAll = await superagent.get(sutUrl + "/books");
    const n = responseAll.body.length;

    const title = "foo";

    const resPost = await superagent
        .post(sutUrl + "/books")
        .send({title, author: "bar", year: 2018})
        .set("Content-Type", "application/json");

    expect(resPost.status).toBe(201);
    const location = resPost.header.location;

    // should had been increased by 1
    responseAll = await superagent.get(sutUrl + "/books");
    expect(responseAll.body.length).toBe(n + 1);

    const resGet = await superagent.get(sutUrl + location);
    expect(resGet.status).toBe(200);
    expect(resGet.body.title).toBe(title);
});

test("Delete all books", async () => {

    let responseAll = await superagent.get(sutUrl + "/books");
    expect(responseAll.status).toBe(200);

    const books = responseAll.body;
    expect(books.length).toBe(5);

    for (const book of books) {

        const res = await superagent.delete(sutUrl + "/books/" + book.id);
        expect(res.status).toBe(204);
    }

    responseAll = await superagent.get(sutUrl + "/books");
    expect(responseAll.status).toBe(200);
    expect(responseAll.body.length).toBe(0);
});

test("Update book", async () => {

    const title = "foo";

    // create a book
    const resPost = await superagent
        .post(sutUrl + "/books")
        .send({title, author: "bar", year: 2018})
        .set("Content-Type", "application/json");
    expect(resPost.status).toBe(201);
    const location = resPost.header.location;

    // get it back
    let resGet = await superagent.get(sutUrl + location);
    expect(resGet.status).toBe(200);
    expect(resGet.body.title).toBe(title);

    const modified = "bar";
    const id = location.substring(location.lastIndexOf("/") + 1, location.length);

    // modify it with PUT
    const resPut = await superagent
        .put(sutUrl + location)
        .send({id, title: modified, author: "bar", year: 2018})
        .set("Content-Type", "application/json");
    expect(resPut.status).toBe(204);

    // get it back again to verify the change
    resGet = await superagent.get(sutUrl + location);
    expect(resGet.status).toBe(200);
    expect(resGet.body.title).toBe(modified);
});
