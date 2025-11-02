const AppController = require("./app-driver");
const em = require("evomaster-client-js");


const controller = new em.EMController(new AppController());

controller.setPort(process.env.PORT || 40100)

controller.startTheControllerServer();


