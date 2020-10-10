import click

from instrumentation.import_hook import install_import_hook
from instrumentation.execution_tracer import ExecutionTracer

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
        from controller.app import app
        app.run()
    # 1. start an instrumented SUT using import_hook.py
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
