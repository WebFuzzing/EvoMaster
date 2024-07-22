import unittest
from src.main.resources.em_test_utils import *

import re

class EvoMaster_EM_Test_Utils_Test(unittest.TestCase):

    def test_resolve_location_direct(self):
        template = "http://localhost:12345/a/{id}"
        location = "/a/5"

        res = resolve_location(location, template)
        
        assert res == "http://localhost:12345/a/5"


    def test_resolve_location_indirect(self):
        template = "http://localhost:12345/a/{id}/x"
        location = "/a/5"

        res = resolve_location(location, template)
        assert res == "http://localhost:12345/a/5/x"
    

    def test_resolve_location_fullURI_different_indirect(self):
        template = "http://localhost:12345/a/{id}/x"
        location = "https://127.0.0.1:80/a/5"

        res = resolve_location(location, template)

        assert res == "https://127.0.0.1:80/a/5/x"


    def test_given_an_invalid_location_header_when_resolve_location_then_the_expected_template_is_returned(self):
        template = "http://localhost:12345/a/x"
        location = "/a/\"52\""

        res = resolve_location(location, template)

        assert res == "http://localhost:12345/a/%2252%22"
    

    def test_is_valid_URI(self):
        assert is_valid_uri_or_empty(None)
        assert is_valid_uri_or_empty("    ")
        assert is_valid_uri_or_empty("a")
        assert is_valid_uri_or_empty("/a")
        assert is_valid_uri_or_empty("/a/b")
        assert is_valid_uri_or_empty("/a/b/c?k=4&z=foo")
        assert is_valid_uri_or_empty("http://foo.org/a")
        assert is_valid_uri_or_empty("https://127.0.0.1:443")
        assert is_valid_uri_or_empty("http://.www.foo.bar/")
        assert is_valid_uri_or_empty("/{a}")
        assert is_valid_uri_or_empty("--://///{a}")
        assert is_valid_uri_or_empty("http://foo.org/#")
        assert is_valid_uri_or_empty("http://foo.org/#a")
        assert is_valid_uri_or_empty("http://example.com/|foo")
        assert is_valid_uri_or_empty("http://example.com/?key=value&")
        assert is_valid_uri_or_empty("http://example.com/#fragment?query=value")
        assert not is_valid_uri_or_empty("http://example.com:port")


    def test_resolve_location_null(self):
        template = "http://localhost:12345/a/x"
        location = None

        res = resolve_location(location, template)
        assert res == template
    

if __name__ == '__main__':
    unittest.main()