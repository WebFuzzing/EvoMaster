from multiprocessing import Process
import requests

import pytest
from requests.adapters import HTTPAdapter, Retry

from evomaster_client.controller.em_app import run_em
from app_driver import EMHandler

DRIVER_HOST = '127.0.0.1'
DRIVER_PORT = 40100


@pytest.fixture(scope="module", autouse=True)
def em_driver():
    print('INITIALIZATION')
    em_driver = EMHandler()

    process = Process(target=run_em, args=(em_driver, DRIVER_HOST, DRIVER_PORT))
    process.daemon = True
    process.start()

    yield em_driver

    print('TEAR DOWN')
    process.terminate()


@pytest.fixture()
def driver_url():
    yield f'http://{DRIVER_HOST}:{DRIVER_PORT}/controller/api'


@pytest.fixture()
def sut_url(em_driver, driver_url):
    sut_url = em_driver.get_url()

    s = requests.Session()
    retries = Retry(total=5, backoff_factor=1)  # use retries to wait until the controller is UP
    s.mount('http://', HTTPAdapter(max_retries=retries))

    # Start SUT
    response = s.put(driver_url + '/runSUT', json={
        'run': True,
        'resetState': True
    })
    assert 204 == response.status_code

    response = s.get(sut_url + '/books')
    assert 0 == len(response.json())

    init_with_some_books(s, sut_url)

    yield sut_url


def init_with_some_books(session, sut_url):
    assert 201 == session.post(
        sut_url + '/books',
        json={'title': "The Hitchhiker's Guide to the Galaxy", 'author': "Douglas Adams", 'year': 1979}).status_code
    assert 201 == session.post(
        sut_url + '/books',
        json={'title': "The Lord of the Rings", 'author': "J. R. R. Tolkien", 'year': 1954}).status_code
    assert 201 == session.post(
        sut_url + '/books',
        json={'title': "The Last Wish", 'author': "Andrzej Sapkowski", 'year': 1993}).status_code
    assert 201 == session.post(
        sut_url + '/books',
        json={'title': "A Game of Thrones", 'author': "George R. R. Martin", 'year': 1996}).status_code
    assert 201 == session.post(
        sut_url + '/books',
        json={'title': "The Call of Cthulhu", 'author': "H. P. Lovecraft", 'year': 1928}).status_code


# --- Controller ---


def test_start_stop_reset_sut(driver_url, sut_url):
    response = requests.get(sut_url + '/books')
    assert 5 == len(response.json())

    # Reset SUT
    response = requests.put(driver_url + '/runSUT', json={
        'run': True,
        'resetState': True
    })
    assert 204 == response.status_code

    response = requests.get(sut_url + '/books')
    assert 0 == len(response.json())

    # Stop SUT
    response = requests.put(driver_url + '/runSUT', json={
        'run': False,
        'resetState': False
    })
    assert 204 == response.status_code


# --- SUT ---


def test_get_all(sut_url):
    response = requests.get(sut_url + '/books')
    assert 5 == len(response.json())


def test_book_not_found(sut_url):
    response = requests.get(sut_url + "/books/-3")
    assert 404 == response.status_code


def test_retrieve_each_single_book(sut_url):
    response = requests.get(sut_url + "/books")
    assert 200 == response.status_code
    assert 5 == len(response.json())

    books = response.json()
    for book in books:
        response = requests.get(sut_url + "/books/" + str(book['book_id']))
        assert 200 == response.status_code
        assert book['title'] == response.json()['title']


def test_create_book(sut_url):
    response = requests.get(sut_url + "/books")
    n = len(response.json())

    response = requests.post(sut_url + '/books', json={
        'title': 'foo', 'author': 'bar', 'year': 2010
    })
    assert 201 == response.status_code
    location = response.headers['location']

    response = requests.get(sut_url + "/books")
    assert 200 == response.status_code
    assert n+1 == len(response.json())

    response = requests.get(sut_url + location)
    assert 200 == response.status_code
    assert 'foo' == response.json()['title']


def test_delete_all_books(sut_url):
    response = requests.get(sut_url + "/books")
    assert 200 == response.status_code
    assert 5 == len(response.json())

    books = response.json()
    for book in books:
        response = requests.delete(sut_url + "/books/" + str(book['book_id']))
        assert 204 == response.status_code

    response = requests.get(sut_url + "/books")
    assert 200 == response.status_code
    assert 0 == len(response.json())


def test_update_book(sut_url):
    response = requests.post(sut_url + '/books', json={
        'title': 'foo', 'author': 'bar', 'year': 2018
    })
    assert 201 == response.status_code
    location = response.headers['location']

    response = requests.get(sut_url + location)
    book = response.json()

    book_id = book['book_id']
    response = requests.put(sut_url + location, json={
        'book_id': book_id, 'title': 'modified', 'author': 'bar', 'year': 2018
    })
    assert 204 == response.status_code

    response = requests.get(sut_url + location)
    assert 200 == response.status_code
    book = response.json()
    assert 'modified' == book['title']
