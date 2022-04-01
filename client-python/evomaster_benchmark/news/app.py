from flask import Flask, jsonify, request, abort
from flask_restx import Resource, Api, fields
from flask_marshmallow import Marshmallow  # new
# from datetime import datetime

from evomaster_benchmark.news.country import countries
from evomaster_benchmark.news.model import db, News

app = Flask(__name__)
app.config['RESTX_MASK_SWAGGER'] = False
app.url_map.strict_slashes = False

api = Api(app)
cs = api.namespace('countries', description='API for country data')
ns = api.namespace('news', description='Handling of creating and retrieving news')

news_model_create = ns.model('NewsCreateFields', {
    'authorId': fields.String(),
    'text': fields.String(),
    'country': fields.String()
})

news_model = ns.clone('News', news_model_create, {
    'id': fields.Integer(),
    # 'creationTime': fields.DateTime(), TODO: skip assertions on timestamp fields
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
        include_fk = True


news_schema = NewsSchema()
news_many_schema = NewsSchema(many=True)


@cs.route('/')
class CountriesResource(Resource):
    @cs.doc('Retrieve list of country names')
    def get(self):
        return jsonify(countries())


news_filter_parser = ns.parser()
news_filter_parser.add_argument('country', type=str)
news_filter_parser.add_argument('authorId', type=str)


@ns.route('/')
class NewsListResource(Resource):

    @ns.doc('Get all the news')
    @ns.expect(news_filter_parser)
    @ns.marshal_with(news_model, as_list=True)
    def get(self):
        args = {k: v for k, v in news_filter_parser.parse_args().items() if v}
        return News.query.filter_by(**args).all()

    @ns.doc('Create a news')
    @ns.expect(news_model_create)
    @ns.marshal_with(news_model)
    def post(self):
        news_json = request.get_json()
        # news_json['creationTime'] = str(datetime.now())
        news = news_schema.load(news_json)
        news.save_to_db()
        return news


@ns.route('/<int:news_id>')
class NewsResource(Resource):

    @ns.doc('Get a single news specified by id')
    @ns.marshal_with(news_model)
    def get(self, news_id):
        return News.query.filter_by(id=news_id).first_or_404()

    @ns.doc('Update an existing news')
    @ns.expect(news_model)
    def put(self, news_id):
        news_json = request.get_json()
        if news_json['id'] != news_id:
            abort(409, 'Not allowed to change the id of the resource')
        news = News.query.filter_by(id=news_id).first_or_404()
        news.update(news_schema.load(news_json))
        return '', 204

    @ns.doc('Delete a news with the given id')
    def delete(self, news_id):
        news = News.query.filter_by(id=news_id).first_or_404()
        news.delete_from_db()
        return '', 204


@ns.route('/<int:news_id>/text')
class NewsTextResource(Resource):

    @ns.doc('Update the text content of an existing news"')
    def put(self, news_id):
        text = request.get_data(as_text=True)
        news = News.query.filter_by(id=news_id).first_or_404()
        news.update_text(text)
        return '', 204


if __name__ == '__main__':
    app.run(debug=True, port=8080)
