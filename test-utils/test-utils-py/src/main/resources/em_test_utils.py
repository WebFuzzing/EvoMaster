from urllib.parse import urlparse, quote
from rfc3986 import validators, uri_reference
import time
import math

# Loaded only once at module loading.
# Seed is still going to incremented with += 1 at each use.
# The idea is to force each value unique during a session, even when generating hundreds of thousands of tests.
# However, when running again in generated test suite, a new starting seed might reduce chances of clashes,
# albeit cannot guarantee removal of them
_seed = int(time.time() * 1000)


def create_string(min_length=None, max_length=None, prefix=None, postfix=None):
    """
    Generate a unique string with optional constraints.

    Args:
        min_length: Optional minimum length of the generated string
        max_length: Optional maximum length of the generated string
        prefix: Optional fixed prefix shared by all generated strings
        postfix: Optional fixed postfix shared by all generated strings

    Returns:
        Generated string
    """
    global _seed

    if min_length is not None and min_length < 0:
        raise ValueError(f"Negative minimum length: {min_length}")
    if max_length is not None and max_length < 0:
        raise ValueError(f"Negative maximum length: {max_length}")

    min_val = 0
    if min_length is not None:
        min_val = min_length

    length = 0
    if prefix is not None:
        length += len(prefix)
    if postfix is not None:
        length += len(postfix)
    min_val = max(min_val, length)

    # Actual check on inputs
    if max_length is not None and max_length < length:
        raise ValueError(
            f"Maximum length {max_length} does not cover minimum prefix+postfix length: {prefix}{postfix}"
        )

    # Recompute with default values if not specified
    if prefix is None:
        prefix = "u"
    if postfix is None:
        postfix = ""
    length = len(prefix) + len(postfix)

    max_digits = 6  # 999 999 values
    if max_digits + length < min_val:
        max_digits = min_val - length
    if max_length is not None and max_digits + length > max_length:
        max_digits = max_length - length

    mask = 1
    for i in range(max_digits):
        mask = mask * 10

    value = _seed % mask
    _seed += 1

    return f"{prefix}{value}{postfix}"


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
    