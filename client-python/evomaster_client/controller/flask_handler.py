import threading
from importlib import import_module
from werkzeug.serving import make_server

from evomaster_client.controller.sut_handler import SutHandler
from evomaster_client.instrumentation.import_hook import install_import_hook

HOST = '127.0.0.1'
PORT = 5001


class ServerThread(threading.Thread):
    def __init__(self, app):
        threading.Thread.__init__(self)
        self.app = app

    def run(self):
        self.srv = make_server(host=HOST, port=PORT, app=self.app)
        self.ctx = self.app.app_context()
        self.ctx.push()
        self.srv.serve_forever()

    def shutdown(self):
        self.srv.shutdown()


class FlaskHandlerError(Exception):
    pass


class FlaskHandler(SutHandler):
    def __init__(self, config):
        self.package_prefixes_to_cover = config['package_prefixes_to_cover']
        self.flask_module = config['flask_module']
        self.flask_app = config['flask_app']
        self.app = self._import_app()
        self.server = None  # SUT is not running

    def _import_app(self):
        print('Importing Flask application')
        with install_import_hook(self.package_prefixes_to_cover):
            module = import_module(self.flask_module)
            app = module.__getattribute__(self.flask_app)
            return app

    def get_url(self):
        return f"{HOST}:{PORT}"

    def start_sut(self):
        if self.is_sut_running():
            print('server is running')
            return
        self.server = ServerThread(self.app)
        self.server.start()

    def stop_sut(self):
        if not self.is_sut_running():
            print('server is not running')
            return
        # Server will stop after the next request. TODO: is there a way to force it?
        self.server.shutdown()
        self.server = None

    def reset_sut_state(self):
        pass

    def setup_for_generated_test(self):
        pass

    def get_info_for_authentication(self):
        # TODO: must be an argument
        return []

    def get_preferred_output_format(self):
        # TODO: must be an argument
        return 'JAVA_JUNIT_5'

    def is_sut_running(self):
        return bool(self.server)

    def get_problem_info(self):
        # TODO: must be an argument
        return {
            'swaggerJsonUrl': self.get_url(),
            'endpointsToSkip': []
        }
