const request = require("supertest");
const app  = require("../../src/base-graphql/app");


test("Test base query", async ()=> {

    const response = await request(app)
        .post("/graphql")
        .send('{\"query\": "{getItem{name}}"}')
        .set("Content-Type", "application/json")
    ;

    expect(response.status).toBe(200);
    expect(response.body.data.getItem.name).toBe("FOO");
});