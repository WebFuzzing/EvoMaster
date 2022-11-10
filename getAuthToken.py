import getopt
import os

import requests
import json
import sys
from dotenv import load_dotenv


def get_auth_token(url: str, client_id: str, client_secret: str, user_id: str):
    response = requests.post(url, data={'grant_type': 'trusted_client',
                                        'client_id': client_id,
                                        'client_secret': client_secret,
                                        'user_id': user_id})
    print(json.loads(response.text)['access_token'])


def get_client_auth_token(url: str, client_id: str, client_secret: str):
    response = requests.post(url, data={
        'grant_type': 'client_credentials',
        'client_id': client_id,
        'client_secret': client_secret
    })
    print(json.loads(response.text)['access_token'])


def main(argv):
    load_dotenv()
    url = os.environ.get("API_TOKEN")
    opts, args = getopt.getopt(argv, "ci:cs:ui:",
                               ["client_id=", "client_secret=", "user_id="])

    client_id = ''
    client_secret = ''
    user_id = ''
    for opt, arg in opts:
        if opt in ('-ci', '--client_id'):
            client_id = arg
        elif opt in ('-cs', '--client_secret'):
            client_secret = arg
        elif opt in ('-ui', '--user_id'):
            user_id = arg

    if user_id == '':
        get_client_auth_token(url, client_id, client_secret)
    else:
        get_auth_token(url, client_id, client_secret, user_id)


if __name__ == "__main__":
    main(sys.argv[1:])
