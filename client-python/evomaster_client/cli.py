from importlib import import_module, reload, invalidate_caches
from importlib.util import cache_from_source
from pathlib import Path

import click

from evomaster_client.instrumentation.import_hook import install_import_hook
from evomaster_client.instrumentation.execution_tracer import ExecutionTracer

CONTEXT_SETTINGS = dict(help_option_names=['-h', '--help'])


def greeter(**kwargs):
    output = '{0}, {1}!'.format(kwargs['greeting'],
                                kwargs['name'])
    if kwargs['caps']:
        output = output.upper()
    print(output)


@click.group(context_settings=CONTEXT_SETTINGS)
@click.version_option(version='1.0.0')
def evomaster():
    pass  # entry Point


@evomaster.command()
def run(**kwargs):
    print("running evomaster client!")
    with install_import_hook('all', ExecutionTracer()):
        module_path = 'evomaster_client/controller/em_app.py'
        cached_module_path = Path(cache_from_source(str(module_path)))
        print(cached_module_path)
        if cached_module_path.exists():
            cached_module_path.unlink()
        module = import_module('evomaster_client.controller.em_app')
        reload(module)  # in case it is cached
        app = module.__getattribute__('app')
        app.run()
    # 1. start an instrumented SUT using moduimport_hook.py
    # 2. make the instrumented SUT path parametrizable with options
    # the command options are the options that the user needs to manually define in Java extending the EmbeddedController class


@evomaster.command()
@click.argument('name')
@click.option('--greeting', default='Hello', help='word to use for the greeting')
@click.option('--caps', is_flag=True, help='uppercase the output')
def hello(**kwargs):
    greeter(**kwargs)


if __name__ == '__main__':
    evomaster()
