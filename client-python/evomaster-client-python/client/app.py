from flask import Flask, Blueprint
from controller import controller


app = Flask(__name__)
app.register_blueprint(controller)
