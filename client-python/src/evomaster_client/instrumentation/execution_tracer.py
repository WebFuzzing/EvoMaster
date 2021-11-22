from typing import Sequence, Set, Mapping

from evomaster_client.instrumentation import objective_naming
from evomaster_client.instrumentation.objective_recorder import ObjectiveRecorder
from evomaster_client.instrumentation.util import Singleton
from evomaster_client.instrumentation.heuristic.truthness import Truthness


class AdditionalInfo:
    def __init__(self) -> None:
        self.query_parameters = set()
        self.headers = set()
        self.last_executed_statement_stack = []
        self.no_exception_statement = None

    def push_last_executed_statement(self, last_line: str) -> None:
        self.no_exception_statement = None
        self.last_executed_statement_stack.append(last_line)

    def pop_last_executed_statement(self) -> str:
        statement = self.last_executed_statement_stack.pop()
        if not self.last_executed_statement_stack:
            self.no_exception_statement = statement
        return statement

    def get_last_executed_statement(self) -> str:
        if self.last_executed_statement_stack:
            return self.last_executed_statement_stack[-1]
        else:
            return self.no_exception_statement

    def to_dto(self):
        return {
            'queryParameters': list(self.query_parameters),
            'headers': list(self.headers),
            'lastExecutedStatement': self.get_last_executed_statement()
        }


class Action:
    def __init__(self, index: int, input_variables: Sequence[str]) -> None:
        self.index = index
        self.input_variables = input_variables


class TargetInfo:
    def __init__(self, mapped_id: int, descriptive_id: str, value: float, action_index: int):
        self.mapped_id = mapped_id
        self.descriptive_id = descriptive_id
        self.value = value
        self.action_index = action_index

    def to_dto(self):
        return {
            'id': self.mapped_id,
            'value': self.value,
            'descriptiveId': self.descriptive_id,
            'actionIndex': self.action_index
        }

    @staticmethod
    def not_reached(mapped_id: int):
        return TargetInfo(mapped_id=mapped_id, descriptive_id=None, value=0.0, action_index=-1)


class ExecutionTracer(Singleton):
    def initialize(self) -> None:
        self.reset()

    def reset(self) -> None:
        self.objective_coverage: Mapping[str, TargetInfo] = {}
        self.action_index = 0
        self.input_variables = set()
        self.additional_info_list = []
        self.additional_info_list.append(AdditionalInfo())

    def set_action(self, action: Action) -> None:
        if action.index != self.action_index:
            self.action_index = action.index
            self.additional_info_list.append(AdditionalInfo())
        if action.input_variables:
            self.input_variables = action.input_variables

    def mark_last_executed_statement(self, last_line: str) -> None:
        self.additional_info_list[self.action_index].push_last_executed_statement(last_line)

    def completed_last_executed_statement(self, last_line: str) -> None:
        self.additional_info_list[self.action_index].pop_last_executed_statement()

    def get_number_of_objectives(self, prefix: str = '') -> int:
        return len([k for k in self.objective_coverage.keys() if k.startsWith(prefix)])

    def get_number_of_non_covered_objectives(self, prefix: str = '') -> int:
        return len(self.get_non_covered_objectives(prefix))

    def get_non_covered_objectives(self, prefix: str = '') -> Set[str]:
        return set(descriptive_id for descriptive_id, target_info in self.objective_coverage.items()
                   if descriptive_id.startsWith(prefix) and target_info.value < 1)

    def get_value(self, descriptive_id: str) -> float:
        return self.objective_coverage[descriptive_id].value

    def update_objective(self, descriptive_id: str, value: float) -> None:
        if value < 0 or value > 1:
            raise ValueError(f'Invalid value: {value}')
        target_info = TargetInfo(None, descriptive_id, value, self.action_index)
        previous = self.objective_coverage.get(descriptive_id)
        if previous:
            if value > previous.value:
                self.objective_coverage[descriptive_id] = target_info
        else:
            self.objective_coverage[descriptive_id] = target_info
        ObjectiveRecorder().update(descriptive_id, value)

    def entering_statement(self, file_name: str, line: int, statement: int) -> None:
        file_id = objective_naming.file_objective_name(file_name)
        line_id = objective_naming.line_objective_name(file_name, line)
        statement_id = objective_naming.statement_objective_name(file_name, line, statement)
        self.update_objective(file_id, 1)
        self.update_objective(line_id, 1)
        self.update_objective(statement_id, 0.5)
        self.mark_last_executed_statement(f"{file_name}_{line}_{statement}")

    def completed_statement(self, file_name: str, line: int, statement: int) -> None:
        statement_id = objective_naming.statement_objective_name(file_name, line, statement)
        self.update_objective(statement_id, 1)
        self.completed_last_executed_statement(f"{file_name}_{line}_{statement}")
        from evomaster_client.instrumentation.heuristic.heuristics import clear_last_evaluation
        clear_last_evaluation()

    def update_branch(self, file_name: str, line: int, branch: int, truthness: Truthness) -> None:
        print(f"Branch: {file_name}:line:{line}:branch:{branch}:truthness:{truthness}")
        then_branch = objective_naming.branch_objective_name(file_name, line, branch, then_branch=True)
        else_branch = objective_naming.branch_objective_name(file_name, line, branch, then_branch=False)
        self.update_objective(then_branch, truthness.ofFalse)
        self.update_objective(else_branch, truthness.ofTrue)
