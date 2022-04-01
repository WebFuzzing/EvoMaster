from evomaster_client.em_test_utils import resolve_location


def test_resolve_location():
    resolved = resolve_location('/news/1', 'http://localhost:5000/news/5/text')
    assert resolved == 'http://localhost:5000/news/1/text'
    # TODO: complete tests
