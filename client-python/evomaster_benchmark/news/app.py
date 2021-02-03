from flask import Flask, jsonify, request
from flask_restx import Resource, Api, reqparse, fields
from flask_marshmallow import Marshmallow # new

from evomaster_benchmark.news.country import countries
from evomaster_benchmark.news.repository import *
from evomaster_benchmark.news.model import db, News

app = Flask(__name__)
api = Api(app)
cs = api.namespace('countries', description='API for country data')
ns = api.namespace('news', description='Handling of creating and retrieving news')

news_model = ns.model('News', {
    'authorId': fields.String(),
    'text': fields.String(),
    'creationTime': fields.String(),  # todo: use datetime
    'country': fields.String()
})

app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///news.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = True
db.init_app(app)

with app.app_context():
    db.create_all()

ma = Marshmallow(app)

class NewsSchema(ma.SQLAlchemyAutoSchema):
    class Meta:
        model = News
        load_instance = True
        load_only = ("store",)
        include_fk= True

news_schema = NewsSchema()
news_many_schema = NewsSchema(many=True)


@cs.route('/')
class CountriesResource(Resource):
    def get(self):
        return jsonify(countries())


@ns.route('/')
class NewsListResource(Resource):
    def get(self):
        parser = reqparse.RequestParser()
        parser.add_argument('country', type=str)
        parser.add_argument('authorId', type=str)
        args = {k: v for k,v in parser.parse_args().items() if v}
        news = News.query.filter_by(**args).all()
        return news_many_schema.dump(news)

    @ns.expect(news_model)
    def post(self):
        news_json = request.get_json()
        news = news_schema.load(news_json)
        news.save_to_db()
        return news_schema.dump(news), 201


@ns.route('/<int:news_id>')
class NewsResource(Resource):
    def get(self, news_id):
        news = News.query.filter_by(id=news_id).first_or_404()
        return news_schema.dump(news)

    @ns.expect(news_model)
    def put(self, news_id):
        news_json = request.get_json()
        news = News.query.filter_by(id=news_id).first_or_404()
        news.update(news_json)
        return '', 204

    def delete(self, news_id):
        news = News.query.filter_by(id=news_id).first_or_404()
        news.delete_from_db()
        return '', 204


# TODO: complete update text route
# @ns.route('/<int:news_id>/text')
# class NewsTextResource(Resource):
#     def put(self, news_id):
#         return


if __name__ == '__main__':
    app.run(debug=True, port=8080)
