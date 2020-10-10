from flask import Flask, Blueprint
from evomaster_client.controller.em_controller import controller

app = Flask(__name__)
app.register_blueprint(controller)
