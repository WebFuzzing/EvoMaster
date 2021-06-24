const request = require("supertest");
const app  = require("../../src/base-graphql/app");


test("Test base query", async ()=> {

    const response = await request(app)
        .post("/graphql")
        .set("Content-Type", "application/json")
        .send('{\"query\": "{getItem{name}}"}')
    ;

    expect(response.status).toBe(200);
    expect(response.body.data.getItem.name).toBe("FOO");
});