"""EvoMaster controller used to connect with the core."""

from flask import Blueprint, request, jsonify, abort

from evomaster_client.controller.sut_handler import SutHandler


def controller(sut_handler: SutHandler) -> Blueprint:
    controller = Blueprint('controller', __name__, url_prefix='/controller/api')

    @controller.route('/infoSUT', methods=['GET'])
    def infoSUT():
        sut_info = {
            'isSutRunning': sut_handler.is_sut_running(),
            'baseUrlOfSUT': sut_handler.get_url(),
            'infoForAuthentication': sut_handler.get_info_for_authentication(),
            'defaultOutputFormat': sut_handler.get_preferred_output_format(),
            'restProblem': sut_handler.get_problem_info(),
            'unitsInfoDto': sut_handler.get_units_info_dto()
        }
        return jsonify({'data': sut_info})

    @controller.route('/runSUT', methods=['PUT'])
    def runSUT():
        args = request.json
        if not args:
            abort(400, {'error': 'No provided JSON payload'})
        if 'run' not in args:
            abort(400, {'error': "Invalid JSON: 'run' field is required"})
        run = args.get('run', False)
        reset_state = 'resetState' in args and args.get('resetState', False)
        if run:
            print("Request to START SUT")
            sut_handler.start_sut()
            if reset_state:
                print("Resetting SUT state")
                sut_handler.reset_state_of_sut()
                sut_handler.new_test()
        else:
            print("Request to STOP SUT")
            if reset_state:
                print("Resetting SUT state")
                abort(400, {'error': 'Invalid JSON: cannot reset state and stop service at same time'})
            sut_handler.stop_sut()
        return jsonify({})

    @controller.route('/testResults', methods=['GET'])
    def testResults():
        ids = set(int(_id) for _id in filter(None, request.args.get('ids', '').split(',')))
        target_infos = sut_handler.get_target_infos(ids)
        # if not target_infos:
        #     abort(500, {'error': f"Failed to collect target information for {len(ids)} ids"})
        additional_infos = sut_handler.get_additional_info_list()
        # if not additional_infos:
        #     abort(500, {'error': 'Failed to collect additional info'})
        test_results = {
            'targets': [ti.to_dto() for ti in target_infos],
            'additionalInfoList': [ai.to_dto() for ai in additional_infos],
            'extraHeuristics': [],
        }
        return jsonify({'data': test_results})

    @controller.route('/controllerInfo', methods=['GET'])
    def controllerInfo():
        controller_info = {
            'fullName': sut_handler.__class__.__module__ + '.' + sut_handler.__class__.__name__,
            'isInstrumentationOn': sut_handler.is_instrumentation_activated()
        }
        return jsonify({'data': controller_info})

    @controller.route('/newSearch', methods=['POST'])
    def newSearch():
        sut_handler.new_search()
        return jsonify({})

    @controller.route('/newAction', methods=['PUT'])
    def newAction():
        if any(arg not in request.json for arg in ['index', 'inputVariables']):
            abort(400)
        sut_handler.new_action(request.json)
        return jsonify({})

    @controller.route('/databaseCommand', methods=['POST'])
    def databaseCommand():
        raise NotImplementedError()

    return controller
