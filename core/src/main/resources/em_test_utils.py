from urllib.parse import urlparse
import validators


def resolve_location(location_header: str, expected_template: str) -> str:
    if not location_header:
        return expected_template

    location_uri = urlparse(location_header)
    location_path = location_uri.path
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
    if uri == "":
        return True
    try:
        validators.url(uri, r_ve=True)
    except Exception as e:
        return False
    return True
