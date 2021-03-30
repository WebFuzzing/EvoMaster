from evomaster_client.controller.flask_handler import FlaskHandler


class EMHandler(FlaskHandler):
    def package_prefixes_to_cover(self):
        return ['evomaster_benchmark.scs']

    def flask_app(self):
        return 'app'

    def flask_module(self):
        return 'evomaster_benchmark.scs.app'

    def get_problem_info(self):
        return super().get_problem_info()

    def get_url(self):
        return super().get_url()

    def reset_state_of_sut(self):
        pass

    def setup_for_generated_test(self):
        pass

    def get_info_for_authentication(self):
        return []

    def get_preferred_output_format(self):
        return 'PYTHON_UNITTEST'
