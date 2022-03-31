from flask import Flask, request, abort
from flask_restx import Resource, Api, fields

from sut.book import repository as repo


app = Flask(__name__)
app.config['RESTX_MASK_SWAGGER'] = False
app.url_map.strict_slashes = False

api = Api(app)
booksapi = api.namespace('books', description='Books API')

book_model_create = booksapi.model('BookCreateFields', {
    'title': fields.String(),
    'author': fields.String(),
    'year': fields.Integer()
})

book_model = booksapi.clone('Book', book_model_create, {
    'book_id': fields.Integer(),
})


@booksapi.route('/')
class BooksResource(Resource):
    @booksapi.doc('Retrieve list of books')
    @booksapi.marshal_with(book_model)
    def get(self):
        return repo.get_all_books()

    @booksapi.doc('Update an existing news')
    @booksapi.expect(book_model_create)
    def post(self):
        book_json = request.get_json()
        book_id = repo.create_new_book(book_json['title'], book_json['author'], book_json['year'])
        return '', 201, {'location': '/books/' + str(book_id)}


@booksapi.route('/<int:book_id>')
class BookResource(Resource):

    @booksapi.doc('Get a single book specified by id')
    @booksapi.marshal_with(book_model)
    def get(self, book_id):
        book = repo.get_book(book_id)
        if not book:
            abort(404)
        else:
            return book

    @booksapi.doc('Delete a book with the given id')
    def delete(self, book_id):
        deleted = repo.delete_book(book_id)
        if deleted:
            return '', 204
        else:
            abort(404)

    @booksapi.doc('Update an existing news')
    @booksapi.expect(book_model)
    def put(self, book_id):
        book_json = request.get_json()
        if book_json['book_id'] != book_id:
            abort(409, 'Not allowed to change the id of the resource')
        updated = repo.update_book(book_json)
        if updated:
            return '', 204
        else:
            abort(404)


if __name__ == '__main__':
    app.run(debug=True, port=8080)
