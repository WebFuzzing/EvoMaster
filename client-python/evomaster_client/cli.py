from importlib import import_module

import click

from evomaster_client.instrumentation.import_hook import install_import_hook
from evomaster_client.instrumentation.ast_transformer import FULL_INSTRUMENTATION
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
@click.option('--handler-module', '-m', required=True,
              help='overriden embedded handler class name')
@click.option('--handler-class', '-c', required=True,
              help='overriden embedded handler module')
@click.option('--instrumentation-level', '-i', required=True, default=FULL_INSTRUMENTATION, type=int,
              help='0: only coverage, 1: branch distance for CMP ops, 2: branch distance for BOOL ops')
def run_em_handler(handler_module, handler_class, instrumentation_level):
    from evomaster_client.controller.em_app import run_em
    cls = getattr(import_module(handler_module), handler_class)
    run_em({}, sut_handler=cls(instrumentation_level))


if __name__ == '__main__':
    evomaster()
