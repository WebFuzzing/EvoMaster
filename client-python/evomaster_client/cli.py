import logging
from importlib import import_module

import click

from evomaster_client.instrumentation.import_hook import install_import_hook
from evomaster_client.instrumentation.ast_transformer import FULL_INSTRUMENTATION

CONTEXT_SETTINGS = dict(help_option_names=['-h', '--help'])


@click.group(context_settings=CONTEXT_SETTINGS)
@click.version_option(version='0.0.2')
def evomaster():
    pass  # entry Point


@evomaster.command()
@click.option('--package-prefix', '-p', multiple=True,
              help='package prefix to measure code coverage')
@click.option('--flask-module', '-m', required=True,
              help='module name where the flask application is defined')
@click.option('--flask-app', '-a', default='app',
              help='flask app defined in flask-module')
@click.option('--instrumentation-level', '-i', required=True, default=FULL_INSTRUMENTATION, type=int,
              help='0: only coverage, 1: branch distance for CMP ops, 2: branch distance for BOOL ops')
@click.option('--log', '-l', default='INFO', type=str, help='log level: DEBUG,INFO,WARN,ERROR')
def run_instrumented(package_prefix, flask_module, flask_app, instrumentation_level, log):
    set_log_level(log)
    with install_import_hook(package_prefix, instrumentation_level):
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
@click.option('--log', '-l', default='INFO', type=str, help='log level: DEBUG,INFO,WARN,ERROR')
def run_em_handler(handler_module, handler_class, instrumentation_level, log):
    set_log_level(log)
    from evomaster_client.controller.em_app import run_em
    cls = getattr(import_module(handler_module), handler_class)
    run_em(sut_handler=cls(instrumentation_level))


def set_log_level(loglevel: str):
    numeric_level = getattr(logging, loglevel.upper(), None)
    if not isinstance(numeric_level, int):
        raise ValueError('Invalid log level: %s' % loglevel)
    logging.basicConfig(format='%(asctime)s - %(levelname)s - %(message)s', level=numeric_level)


if __name__ == '__main__':
    evomaster()
