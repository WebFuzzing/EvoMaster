from typing import Set
import abc

from evomaster_client.instrumentation.execution_tracer import ExecutionTracer, Action, TargetInfo
from evomaster_client.instrumentation.objective_recorder import ObjectiveRecorder


class SutHandler(metaclass=abc.ABCMeta):
    @abc.abstractmethod
    def get_url(self):
        raise NotImplementedError

    @abc.abstractmethod
    def start_sut(self):
        raise NotImplementedError

    @abc.abstractmethod
    def stop_sut(self):
        raise NotImplementedError

    @abc.abstractmethod
    def reset_state_of_sut(self):
        raise NotImplementedError

    @abc.abstractmethod
    def setup_for_generated_test(self):
        raise NotImplementedError

    # --- other abstract methods ---

    @abc.abstractmethod
    def get_info_for_authentication(self):
        raise NotImplementedError

    @abc.abstractmethod
    def get_preferred_output_format(self):
        raise NotImplementedError

    @abc.abstractmethod
    def is_sut_running(self):
        raise NotImplementedError

    @abc.abstractmethod
    def get_problem_info(self):
        raise NotImplementedError

    # --- other methods that MUST not be overriden ----

    def get_additional_info_list(self):
        return ExecutionTracer().additional_info_list

    def get_units_info_dto(self):
        return {
            'unitNames': list(ObjectiveRecorder().units_info.unit_names),
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
            t = objectives.get(descriptive_id)
            if t:
                t.mapped_id = mapped_id
                t.descriptive_id = None
            else:
                t = TargetInfo.not_reached(mapped_id)
            target_infos.append(t)
        # If new targets were found, we add them even if not requested by EM
        for descriptive_id in ObjectiveRecorder().first_time_encountered:
            t = objectives[descriptive_id]
            t.mapped_id = ObjectiveRecorder().get_mapped_id(descriptive_id)
            target_infos.append(t)
        return target_infos
