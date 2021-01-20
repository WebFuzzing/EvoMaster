from flask import Flask, jsonify, request
from flask_restx import Resource, Api

from evomaster_benchmark.news.country import countries
from evomaster_benchmark.news.repository import *

app = Flask(__name__)
api = Api(app)
cs = api.namespace('countries', description='API for country data')
ns = api.namespace('news', description='Handling of creating and retrieving news')


@cs.route('/')
class Countries(Resource):
    def get():
        return jsonify(countries())


@ns.route('/')
class News(Resource):
    def get(self):
        country = request.args['country']
        author_id = request.args['authorId']
        # TODO: get news from repository


if __name__ == '__main__':
    app.run(debug=True, port=8080)
