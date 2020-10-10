from flask import Flask, Blueprint
from flask_restx import Api, Resource, fields

app = Flask(__name__)
api = Api(app, prefix='/controller/api')

sut_info = api.model('SutInfo', {
    # TODO
})

sut_run = api.model('SutRun', {
    'run': fields.Boolean(),
    'resetState': fields.Boolean(),
    'calculateSqlHeuristics': fields.Boolean(),
    'extractSqlExecutionInfo': fields.Boolean(),
})

test_results = api.model('TestResults', {
    # TODO
})

target_info = api.model('TargetInfo', {
    # TODO
})

controller_info = api.model('ControllerInfo', {
    # TODO
})

action = api.model('Action', {
    # TODO
})

db_command = api.model('DatabaseCommand', {
    # TODO
})

@api.route('/infoSUT')
class SutInfo(Resource):
    def get(self):
        return 'SutInfo'

@api.route('/runSUT')
class SutRun(Resource):
    @api.expect(sut_run)
    def put(self):
        return 'SutRun'

@api.route('/testResults')
class TestResults(Resource):
    def get(self):
        return 'TestResults'

@api.route('/controllerInfo')
class ControllerInfo(Resource):
    def get(self):
        return 'ControllerInfo'

@api.route('/newSearch')
class Search(Resource):
    def post(self):
        return 'Search'

@api.route('/newAction')
class Action(Resource):
    def put(self):
        return 'Action'

@api.route('/databaseCommand')
class DatabaseCommand(Resource):
    def post(self):
        return 'DatabaseCommand'

if __name__ == '__main__':
    app.run(debug=False)
