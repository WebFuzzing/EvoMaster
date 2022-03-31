from evomaster_client.instrumentation.staticstate.execution_tracer import ExecutionTracer


def test_execution_tracer_singleton():
    ExecutionTracer().mark_last_executed_statement('line1')
    ExecutionTracer().mark_last_executed_statement('line2')
    ExecutionTracer().mark_last_executed_statement('line3')
    stack = ExecutionTracer().additional_info_list[0].last_executed_statement_stack
    assert len(stack) == 3
    assert stack[0] == 'line1'
    assert stack[1] == 'line2'
    assert stack[2] == 'line3'
