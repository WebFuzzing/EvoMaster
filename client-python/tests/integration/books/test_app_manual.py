import pytest

from sut.book import repository as repo
from sut.app import app as booksapp


@pytest.fixture()
def app():
    app = booksapp
    app.config.update({
        "TESTING": True,
    })

    # other setup can go here
    repo.create_new_book("The Hitchhiker's Guide to the Galaxy", "Douglas Adams", 1979)
    repo.create_new_book("The Lord of the Rings", "J. R. R. Tolkien", 1954)
    repo.create_new_book("The Last Wish", "Andrzej Sapkowski", 1993)
    repo.create_new_book("A Game of Thrones", "George R. R. Martin", 1996)
    repo.create_new_book("The Call of Cthulhu", "H. P. Lovecraft", 1928)

    yield app

    # clean up / reset resources here
    repo.reset()


@pytest.fixture()
def client(app):
    return app.test_client()


@pytest.fixture()
def runner(app):
    return app.test_cli_runner()


def test_get_all(client):
    response = client.get("/books")
    assert 200 == response.status_code
    assert 5 == len(response.json)


def test_book_not_found(client):
    response = client.get("/books/-3")
    assert 404 == response.status_code


def test_retrieve_each_single_book(client):
    response = client.get("/books")
    assert 200 == response.status_code
    assert 5 == len(response.json)

    books = response.json
    for book in books:
        response = client.get("/books/" + str(book['book_id']))
        assert 200 == response.status_code
        assert book['title'] == response.json['title']


def test_create_book(client):
    response = client.get("/books")
    n = len(response.json)

    response = client.post('/books', json={
        'title': 'foo', 'author': 'bar', 'year': 2010
    })
    assert 201 == response.status_code
    location = response.headers['location']

    response = client.get("/books")
    assert 200 == response.status_code
    assert n+1 == len(response.json)

    response = client.get(location)
    assert 200 == response.status_code
    assert 'foo' == response.json['title']


def test_delete_all_books(client):
    response = client.get("/books")
    assert 200 == response.status_code
    assert 5 == len(response.json)

    books = response.json
    for book in books:
        response = client.delete("/books/" + str(book['book_id']))
        assert 204 == response.status_code

    response = client.get("/books")
    assert 200 == response.status_code
    assert 0 == len(response.json)


def test_update_book(client):
    response = client.post('/books', json={
        'title': 'foo', 'author': 'bar', 'year': 2018
    })
    assert 201 == response.status_code
    location = response.headers['location']

    response = client.get(location)
    book = response.json

    book_id = book['book_id']
    response = client.put(location, json={
        'book_id': book_id, 'title': 'modified', 'author': 'bar', 'year': 2018
    })
    assert 204 == response.status_code

    response = client.get(location)
    assert 200 == response.status_code
    book = response.json
    assert 'modified' == book['title']
