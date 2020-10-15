from typing import Set

from evomaster_client.instrumentation.execution_tracer import ExecutionTracer, Action
from evomaster_client.instrumentation.objective_recorder import ObjectiveRecorder


class SutHandler:
    def get_url(self):
        raise NotImplementedError

    def start_sut(self):
        raise NotImplementedError

    def stop_sut(self):
        raise NotImplementedError

    def reset_sut_state(self):
        raise NotImplementedError

    def setup_for_generated_test(self):
        raise NotImplementedError

    # --- other abstract methods ---

    def get_info_for_authentication(self):
        raise NotImplementedError

    def get_preferred_output_format(self):
        raise NotImplementedError

    def is_sut_running(self):
        raise NotImplementedError

    def get_problem_info(self):
        raise NotImplementedError

    # --- other methods that MUST not be overriden ----

    def get_additional_info_list(self):
        return ExecutionTracer().additional_info_list

    def get_units_info_dto(self):
        return {
            'unitNames': ObjectiveRecorder().units_info.unit_names,
            'numberOfLines': ObjectiveRecorder().units_info.lines_count,
            'numberOfBranches': ObjectiveRecorder().units_info.branch_count
        }

    def is_instrumentation_activated(self) -> bool:
        return bool(ObjectiveRecorder().all_targets)

    def new_search(self):
        ExecutionTracer().reset()
        ObjectiveRecorder().reset()

    def new_test(self):
        ExecutionTracer().reset()
        ObjectiveRecorder().clearFirstTimeEncountered()

    def new_action(self, action):
        ExecutionTracer().set_action(Action(action['index'], action['inputVariables']))

    def get_target_infos(self, ids: Set[int]):
        target_infos = []
        objectives = ExecutionTracer().objective_coverage
        for mapped_id in ids:
            descriptive_id = ObjectiveRecorder().get_descriptive_id(mapped_id)
            # TODO: review what's the purpose of withMappedId and withNoDescriptiveId ?
            target_infos.append(objectives[descriptive_id])
        # If new targets were found, we add them even if not requested by EM
        for descriptive_id in ObjectiveRecorder().first_time_encountered:
            target_infos.append(objectives[descriptive_id])
        return target_infos
