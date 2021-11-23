from flask import Flask, abort
from flask_restx import Resource, Api

from evomaster_benchmark.ncs.views import triangle_classify, bessj, expint, fisher, GammqImpl, remainder

app = Flask(__name__)
app.config['RESTX_MASK_SWAGGER'] = False
app.url_map.strict_slashes = False

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


@ns.route('/expint/<int:n>/<float:x>')
class Expint(Resource):
    def get(self, n, x):
        try:
            return {'resultAsFloat': expint(n, x)}
        except Exception as e:
            print(e)
            abort(400)


@ns.route('/fisher/<int:m>/<int:n>/<float:x>')
class Fisher(Resource):
    def get(self, m, n, x):
        if m > 1000 or n > 1000:
            abort(400)
        try:
            return {'resultAsFloat': fisher(m, n, x)}
        except Exception as e:
            print(e)
            abort(400)


@ns.route('/gammq/<float:a>/<float:x>')
class Gammq(Resource):
    def get(self, a, x):
        try:
            return {'resultAsFloat': GammqImpl().exe(a, x)}
        except Exception as e:
            print(e)
            abort(400)


@ns.route('/remainder/<int:a>/<int:b>')
class Remainder(Resource):
    def get(self, a, b):
        lim = 10_000
        if a > lim or a < -lim or b > lim or b < -lim:
            abort(400)
        return {'resultAsInt': remainder(a, b)}


if __name__ == '__main__':
    app.run(debug=True, port=8080)
