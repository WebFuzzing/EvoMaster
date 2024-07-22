from urllib.parse import urlparse, quote
from rfc3986 import validators, uri_reference

def resolve_location(location_header: str, expected_template: str) -> str:
    if not location_header:
        return expected_template

    location_uri = urlparse(location_header)
    location_path = quote(location_uri.path)
    location_tokens = location_path.split('/')

    normalized_template = expected_template.replace('{', '').replace('}', '')
    template_uri = urlparse(normalized_template)
    template_path = template_uri.path
    template_tokens = template_path.split('/')

    target_path = location_path
    if len(template_tokens) > len(location_tokens):
        for i in range(len(location_tokens), len(template_tokens)):
            target_path += '/' + template_tokens[i]

    target_uri = location_uri if location_uri.hostname else template_uri
    target_uri = target_uri._replace(path=target_path)
    return target_uri.geturl()


def is_valid_uri_or_empty(uri: str):
    if uri is None or uri.strip() == "":
        return True

    validated_components = {
        "scheme": False,
        "userinfo": False,
        "host": False,
        "port": False,
        "path": False,
        "query": False,
        "fragment": False,
    }
    
    try:
        validators.ensure_components_are_valid(uri_reference(uri), validated_components)
    except Exception as e:
        return False
    
    return True
    