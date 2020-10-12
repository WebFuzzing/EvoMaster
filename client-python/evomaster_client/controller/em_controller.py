"""Controller used to redirect all calls to a diferent URL and trace eacg call arguments and responses."""

import os
import json
import requests

from flask import Blueprint, request, jsonify


EVOMASTER_DRIVER_URL = 'http://127.0.0.1:40101'

controller = Blueprint('controller', __name__, url_prefix='/controller/api')


@controller.route('/infoSUT', methods=['GET'])
def infoSUT():
    r = requests.get(getRedirectUrl())
    r.raise_for_status()
    res = r.json()
    trace(request.path, response=res)
    return jsonify(res)


@controller.route('/runSUT', methods=['PUT'])
def runSUT():
    args = request.json
    r = requests.put(getRedirectUrl(), json=args)
    r.raise_for_status()
    trace(request.path, params=args)
    return ''


@controller.route('/testResults', methods=['GET'])
def testResults():
    r = requests.get(getRedirectUrl())
    r.raise_for_status()
    res = r.json()
    trace(request.path, response=res)
    return jsonify(res)


@controller.route('/controllerInfo', methods=['GET'])
def controllerInfo():
    r = requests.get(getRedirectUrl())
    r.raise_for_status()
    res = r.json()
    trace(request.path, response=res)
    return jsonify(res)


@controller.route('/newSearch', methods=['POST'])
def newSearch():
    r = requests.post(getRedirectUrl())
    r.raise_for_status()
    trace(request.path)
    return ''


@controller.route('/newAction', methods=['PUT'])
def newAction():
    args = request.json
    r = requests.put(getRedirectUrl(), json=args)
    r.raise_for_status()
    trace(request.path, params=args)
    return ''


@controller.route('/databaseCommand', methods=['POST'])
def databaseCommand():
    args = request.json
    r = requests.post(getRedirectUrl(), json=args)
    r.raise_for_status()
    res = r.json()
    trace(request.path, params=args, response=res)
    return jsonify(res)


def getRedirectUrl():
    return f"{EVOMASTER_DRIVER_URL}{request.path}"


trace_call = 0


if os.path.exists("full_trace.txt"):
    os.remove("full_trace.txt")
if os.path.exists("trace.txt"):
    os.remove("trace.txt")


def trace(path, params=None, response=None):
    global trace_call
    trace_call += 1
    d = {
        'call_number': trace_call,
        'path': path,
        'params': params,
        'response': response
    }

    with open('trace.txt', 'a+') as f:
        f.write(f'{path}\n')

    with open('full_trace.txt', 'a+') as f:
        f.write(json.dumps(d, indent=4, sort_keys=False))
        f.write('\n#############################################\n')
