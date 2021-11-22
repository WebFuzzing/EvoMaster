from typing import List, Set, Mapping

from evomaster_client.instrumentation.util import Singleton
from evomaster_client.instrumentation.objective_naming import (FILE, LINE, STATEMENT, BRANCH,
                                                               get_file_id_from_objective_name)


class ObjectiveRecorder(Singleton):
    def initialize(self) -> None:
        self.reset()

    def reset(self) -> None:
        self.max_objective_coverage: Mapping[int, float] = {}
        self.all_targets: Set[str] = set()
        self.id_mapping: Mapping[str, int] = {}
        self.reversed_id_mapping: Mapping[int, str] = {}
        self.id_mapping_counter = 0
        self.first_time_encountered: List[str] = []
        self.units_info = UnitsInfo()

    def register_target(self, target: str) -> None:
        self.all_targets.add(target)
        if target.startswith(FILE):
            self.units_info.unit_names.add(get_file_id_from_objective_name(target))
        elif target.startswith(LINE):
            self.units_info.lines_count += 1
        elif target.startswith(BRANCH):
            self.units_info.branch_count += 1
        elif target.startswith(STATEMENT):
            self.units_info.statement_count += 1

    def clearFirstTimeEncountered(self) -> None:
        self.first_time_encountered = []

    def update(self, descriptive_id: str, value: float) -> None:
        if value < 0 or value > 1:
            raise ValueError(f"Invalid value {value}, out of range [0,1]")
        mapped_id = self.get_mapped_id(descriptive_id)
        if mapped_id not in self.max_objective_coverage:
            self.first_time_encountered.append(descriptive_id)
            self.max_objective_coverage[mapped_id] = value
        else:
            old = self.max_objective_coverage[mapped_id]
            if value > old:
                self.max_objective_coverage[mapped_id] = value

    def get_mapped_id(self, descriptive_id: str) -> int:
        if descriptive_id not in self.id_mapping:
            self.id_mapping[descriptive_id] = self.id_mapping_counter
            self.id_mapping_counter += 1
        mapped_id = self.id_mapping[descriptive_id]
        if mapped_id not in self.reversed_id_mapping:
            self.reversed_id_mapping[mapped_id] = descriptive_id
        return mapped_id

    def get_descriptive_id(self, mapped_id: int) -> str:
        if mapped_id not in self.reversed_id_mapping:
            raise ValueError(f"Id {mapped_id} is not mapped")
        return self.reversed_id_mapping[mapped_id]


class UnitsInfo:
    def __init__(self) -> None:
        self.unit_names: Set[str] = set()
        self.lines_count = 0
        self.branch_count = 0
        self.statement_count = 0
