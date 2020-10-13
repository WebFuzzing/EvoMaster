from importlib import import_module

import click

from evomaster_client.instrumentation.import_hook import install_import_hook
from evomaster_client.controller.flask_handler import FlaskHandler

CONTEXT_SETTINGS = dict(help_option_names=['-h', '--help'])


@click.group(context_settings=CONTEXT_SETTINGS)
@click.version_option(version='1.0.0')
def evomaster():
    pass  # entry Point


@evomaster.command()
@click.option('--package-prefix', '-p', multiple=True,
              help='package prefix to measure code coverage')
@click.option('--flask-module', '-m', required=True,
              help='module name where the flask application is defined')
@click.option('--flask-app', '-a', default='app',
              help='flask app defined in flask-module')
def run_instrumented(package_prefix, flask_module, flask_app):
    print(f'package_prefix={package_prefix}')
    print(f'flask_module={flask_module}')
    print(f'flask_app={flask_app}')
    with install_import_hook(package_prefix):
        module = import_module(flask_module)
        app = module.__getattribute__(flask_app)
        app.run()


@evomaster.command()
@click.option('--package-prefix', '-p', multiple=True,
              help='package prefix to measure code coverage')
@click.option('--flask-module', '-m', required=True,
              help='module name where the flask application is defined')
@click.option('--flask-app', '-a', default='app',
              help='flask app defined in flask-module')
def run_em(package_prefix, flask_module, flask_app):
    from evomaster_client.controller.em_app import run_em
    config = {
        'package_prefixes_to_cover': package_prefix,
        'flask_module': flask_module,
        'flask_app': flask_app
    }
    run_em({}, sut_handler=FlaskHandler(config))


if __name__ == '__main__':
    evomaster()
