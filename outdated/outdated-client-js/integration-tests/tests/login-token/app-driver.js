const http  = require("http");
const {AddressInfo}  = require("net");

const app = require("../../src/login-token/app");

const em = require("evomaster-client-js");


class AppController extends em.SutController {

    getInfoForAuthentication(){
        let loginJson = new em.dto.JsonTokenPostLoginDto();
        loginJson.endpoint ="/login";
        loginJson.userId="foo";
        loginJson.extractTokenField="/token";
        loginJson.jsonPayload = `{
            "username": "foo",
            "password": "foo"
        }`;
        loginJson.headerPrefix="token ";

        let auth = new em.dto.AuthenticationDto();
        auth.name = "foo-auth";
        auth.jsonTokenPostLogin =loginJson;
        return [auth];
    }

    getPreferredOutputFormat() {
        return em.dto.OutputFormat.JS_JEST;
    }

    getProblemInfo() {
        const dto = new em.dto.RestProblemDto();
        dto.openApiUrl = "http://localhost:" + this.port + "/swagger.json";

        return dto;
    }

    isSutRunning(){
        if (!this.server) {
            return false;
        }
        return this.server.listening;
    }

    resetStateOfSUT(){
        return Promise.resolve();
    }

    startSut(){

        return new Promise( (resolve) => {

            this.server = app.listen(0, "localhost", () => {
                this.port = this.server.address().port;
                resolve("http://localhost:" + this.port);
            });
        });
    }

    stopSut() {
        return new Promise( (resolve) => this.server.close( () => resolve()));
    }

}


module.exports = AppController;