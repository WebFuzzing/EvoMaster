from flask import Flask
from evomaster_proxy.em_controller import controller

app = Flask(__name__)
app.register_blueprint(controller)
