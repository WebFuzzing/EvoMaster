from flask import Flask, abort
from flask_restx import Resource, Api

from evomaster_benchmark.scs.views import *

app = Flask(__name__)
api = Api(app)
ns = api.namespace('api', description='SCS api')


@ns.route('/calc/<string:op>/<float:arg1>/<float:arg2>')
class Calc(Resource):
    def get(self, op, arg1, arg2):
        return calc(op, arg1, arg2)


@ns.route('/cookie/<string:name>/<string:val>/<string:site>')
class Cookie(Resource):
    def get(self, name, val, site):
        return cookie(name, val, site)


@ns.route('/costfuns/<int:i>/<string:s>')
class Costfuns(Resource):
    def get(self, i, s):
        return costfuns(i, s)


if __name__ == '__main__':
    app.run(debug=True, port=8080)
