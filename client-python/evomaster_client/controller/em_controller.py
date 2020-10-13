"""EvoMaster controller used to connect with the core."""

from flask import Blueprint

controller = Blueprint('controller', __name__, url_prefix='/controller/api')


# TODO: remove this (used for testing)
@controller.route('/testStartSut', methods=['GET'])
def testStartSut():
    from evomaster_client.controller.em_app import sut_handler
    print('Starting SUT...')
    sut_handler.start_sut()
    return ''


# TODO: remove this (used for testing)
@controller.route('/testStopSut', methods=['GET'])
def testStopSut():
    from evomaster_client.controller.em_app import sut_handler
    print('Stopping SUT...')
    sut_handler.stop_sut()
    return ''


@controller.route('/infoSUT', methods=['GET'])
def infoSUT():
    raise NotImplementedError()


@controller.route('/runSUT', methods=['PUT'])
def runSUT():
    raise NotImplementedError()


@controller.route('/testResults', methods=['GET'])
def testResults():
    raise NotImplementedError()


@controller.route('/controllerInfo', methods=['GET'])
def controllerInfo():
    raise NotImplementedError()


@controller.route('/newSearch', methods=['POST'])
def newSearch():
    raise NotImplementedError()


@controller.route('/newAction', methods=['PUT'])
def newAction():
    raise NotImplementedError()


@controller.route('/databaseCommand', methods=['POST'])
def databaseCommand():
    raise NotImplementedError()
