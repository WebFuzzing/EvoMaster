from flask import Flask, abort
from flask_restx import Resource, Api

from evomaster_benchmark.ncs.views import triangle_classify, bessj

app = Flask(__name__)
api = Api(app)
ns = api.namespace('api', description='NCS api')


@ns.route('/triangle/<int:a>/<int:b>/<int:c>')
class Triangle(Resource):
    def get(self, a, b, c):
        return {'resultAsInt': triangle_classify(a, b, c)}


@ns.route('/bessj/<int:n>/<float:x>')
class Bessj(Resource):
    def get(self, n, x):
        if n <= 2 or n > 1000:
            abort(400)
        return {'resultAsFloat': bessj(n, x)}


if __name__ == '__main__':
    app.run(debug=True, port=8080)
