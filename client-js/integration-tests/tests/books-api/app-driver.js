const http  = require("http");
const {AddressInfo}  = require("net");

const app = require("../../src/books-api/app");
const rep  = require("../../src/books-api/repository");

const em = require("evomaster-client-js");


class AppController  extends em.SutController {

    getInfoForAuthentication(){
        return [];
    }

    getPreferredOutputFormat() {
        return em.dto.OutputFormat.JAVA_JUNIT_4; // TODO JavaScript
    }

    getProblemInfo() {
        const dto = new em.dto.RestProblemDto();
        dto.swaggerJsonUrl = "http://localhost:" + this.port + "/swagger.json";

        return dto;
    }

    isSutRunning(){
        if (!this.server) {
            return false;
        }
        return this.server.listening;
    }

    resetStateOfSUT(){
        rep.reset();
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