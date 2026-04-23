const request = require("supertest");
const app  = require("../../src/for-assertions-api/app");



test("Test get data", async () => {

    const response = await request(app).get("/data");

    expect(response.status).toBe(200);
    expect(response.header["content-type"].startsWith("application/json")).toBe(true)


    expect(response.body.a).toBe(42);
    expect(response.body.b).toBe("hello");
    expect(response.body.c.length).toBe(3);
    expect(response.body.c[0]).toBe(1000);
    expect(response.body.c[1]).toBe(2000);
    expect(response.body.c[2]).toBe(3000);
    expect(response.body.d.e).toBe(66);
    expect(response.body.d.f).toBe("bar");
    expect(response.body.d.g.h.length).toBe(2);
    expect(response.body.d.g.h[0]).toBe("xvalue");
    expect(response.body.d.g.h[1]).toBe("yvalue");
    expect(response.body.i).toBe(true);
    expect(response.body.l).toBe(false);
});

test("Test post data", async () => {

    const response = await request(app).post("/data");

    expect(response.status).toBe(201);
    expect(response.body===null || response.body===undefined || response.body==="" || Object.keys(response.body).length === 0).toBe(true)
});

test("Test simpleNumber", async () =>{

    const response = await request(app).get("/simpleNumber");

    expect(response.status).toBe(200);
    expect(response.body).toBe(42);
});

test("Test simpleString", async () =>{

    const response = await request(app).get("/simpleString");

    expect(response.status).toBe(200);
    expect(response.body).toBe("simple-string");
});

test("Test simpleText", async () =>{

    const response = await request(app).get("/simpleText");

    expect(response.status).toBe(200);
    expect(response.header["content-type"].startsWith("text/plain")).toBe(true)
    expect(response.text).toBe("simple-text");
});

test("Test array", async () =>{

    const response = await request(app).get("/array");

    expect(response.status).toBe(200);
    expect(response.body.length).toBe(2);
    expect(response.body).toContain(123);
    expect(response.body).toContain(456);
});

test("Test array empty", async () =>{

    const response = await request(app).get("/arrayEmpty");

    expect(response.status).toBe(200);
    expect(response.body.length).toBe(0);
});

test("Test object array", async () =>{

    const response = await request(app).get("/arrayObject");

    expect(response.status).toBe(200);
    expect(response.body.length).toBe(2);
    expect(response.body[0].x).toBe(777);
    expect(response.body[1].x).toBe(888);
});

test("Test empty object", async () =>{

    const response = await request(app).get("/objectEmpty");

    expect(response.status).toBe(200);
    expect(Object.keys(response.body).length).toBe(0);
});





