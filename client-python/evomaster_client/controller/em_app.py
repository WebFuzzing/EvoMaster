from flask import Flask

from evomaster_client.controller.em_controller import controller


def run_em(config, sut_handler) -> None:
    app = Flask(__name__)
    app.register_blueprint(controller(sut_handler))
    app.run(port=40100)
