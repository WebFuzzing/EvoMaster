from flask import Flask

from evomaster_client.controller.em_controller import controller

app = Flask(__name__)
app.register_blueprint(controller)


def run_em(config, sut_handler) -> None:
    globals()['sut_handler'] = sut_handler
    app.run()
