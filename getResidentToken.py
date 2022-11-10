import os

import requests
from lxml import html
from dotenv import load_dotenv


def get_token():
    load_dotenv()
    url = os.environ.get("TOKEN_URL")
    response = requests.get(url)
    tree = html.fromstring(response.content)

    uuid = tree.xpath('//span[@data-key="uuid"]/text()')
    print(uuid[0])


if __name__ == "__main__":
    get_token()
