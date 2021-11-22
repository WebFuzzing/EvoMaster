import ast
from ast import UnaryOp, BoolOp, Compare, Eq, NotEq, Lt, LtE, Gt, GtE, Is, IsNot, In, NotIn, And, Or, Not
from typing import Any

from evomaster_client.instrumentation.objective_naming import (file_objective_name, line_objective_name,
                                                               statement_objective_name, branch_objective_name)
from evomaster_client.instrumentation.objective_recorder import ObjectiveRecorder
from evomaster_client.instrumentation.heuristic.heuristics import VALID_OPS

NO_INSTRUMENTATION = 0
INSTRUMENTATION_LEVEL_COVERAGE = 1
INSTRUMENTATION_LEVEL_BRANCH_DISTANCE_CMP = 2
INSTRUMENTATION_LEVEL_BRANCH_DISTANCE_BOOLOPS = 3
FULL_INSTRUMENTATION = INSTRUMENTATION_LEVEL_BRANCH_DISTANCE_BOOLOPS


class AstTransformer(ast.NodeTransformer):
    def __init__(self, module: str, instrumentation_level: int = FULL_INSTRUMENTATION):
        self.module = module
        self.instrumentation_level = instrumentation_level
        self.statement_counter = 0
        self.branch_counter = 0

    def visit_Module(self, node):
        node = self.generic_visit(node)  # visit child nodes
        ObjectiveRecorder().register_target(file_objective_name(self.module))
        import_node = ast.ImportFrom(module='evomaster_client.instrumentation.injected_functions',
                                     names=[ast.alias(name='*', asname=None)],
                                     level=0)
        node.body.insert(0, import_node)
        return node

    def visit_Statement(self, node):
        # For nodes that were part of a collection of statements (that applies to all statement nodes),
        # the visitor may also return a list of nodes rather than just a single node.
        node = self.generic_visit(node)  # visit child nodes
        print("Visited node of type: ", node.__class__.__name__, " - line no:", node.lineno)

        if self.instrumentation_level < INSTRUMENTATION_LEVEL_COVERAGE:
            return node

        if hasattr(node, 'body'):
            print("isBlockStatement. no point in instrumenting it. Recall, we still instrument its content anyway.")
            return node

        self.statement_counter += 1
        ObjectiveRecorder().register_target(line_objective_name(self.module, node.lineno))
        ObjectiveRecorder().register_target(statement_objective_name(self.module, node.lineno, self.statement_counter))

        if ((isinstance(node, ast.Return) and not node.value) or
                isinstance(node, ast.Raise) or
                isinstance(node, ast.Pass) or
                isinstance(node, ast.If) or
                isinstance(node, ast.For) or
                isinstance(node, ast.While) or
                isinstance(node, ast.Break) or
                isinstance(node, ast.Continue) or
                isinstance(node, ast.Try) or
                isinstance(node, ast.With) or
                isinstance(node, ast.Yield) or
                isinstance(node, ast.YieldFrom)):
            return [
                ast.Expr(value=ast.Call(func=ast.Name("completion_statement", ast.Load()),
                         args=[ast.Str(self.module), ast.Num(node.lineno), ast.Num(self.statement_counter)],
                         keywords=[])),
                node
            ]

        if isinstance(node, ast.Return):
            return [
                ast.Expr(value=ast.Call(func=ast.Name("entering_statement", ast.Load()),
                         args=[ast.Str(self.module), ast.Num(node.lineno), ast.Num(self.statement_counter)],
                         keywords=[])),
                ast.Return(value=ast.Call(func=ast.Name("completing_statement", ast.Load()),
                           args=[node.value, ast.Str(self.module), ast.Num(node.lineno), ast.Num(self.statement_counter)],
                           keywords=[])),
            ]

        return [
            ast.Expr(value=ast.Call(func=ast.Name("entering_statement", ast.Load()),
                     args=[ast.Str(self.module), ast.Num(node.lineno), ast.Num(self.statement_counter)],
                     keywords=[])),
            node,
            ast.Expr(value=ast.Call(func=ast.Name("completed_statement", ast.Load()),
                     args=[ast.Str(self.module), ast.Num(node.lineno), ast.Num(self.statement_counter)],
                     keywords=[])),
        ]

    def visit_UnaryOp(self, node: UnaryOp) -> Any:
        node = self.generic_visit(node)  # visit child nodes
        if self.instrumentation_level < INSTRUMENTATION_LEVEL_BRANCH_DISTANCE_BOOLOPS:
            return node

        if isinstance(node.op, Not):
            return ast.Call(func=ast.Name("not_statement", ast.Load()),
                            args=[node.operand], keywords=[])
        return node

    def visit_BoolOp(self, node: BoolOp) -> Any:
        node = self.generic_visit(node)  # visit child nodes
        if self.instrumentation_level < INSTRUMENTATION_LEVEL_BRANCH_DISTANCE_BOOLOPS:
            return node

        if isinstance(node.op, And) or isinstance(node.op, Or):
            if len(node.values) > 2:
                # TODO: handle len(values) > 2
                return node
            self.branch_counter += 1
            ObjectiveRecorder().register_target(branch_objective_name(self.module, node.lineno, self.branch_counter, True))
            ObjectiveRecorder().register_target(branch_objective_name(self.module, node.lineno, self.branch_counter, False))
            injected_function = "and_statement" if isinstance(node.op, And) else "or_statement"
            right_pure = self.is_pure(node.values[1])
            empty_args = ast.arguments(args=[], vararg=None, kwarg=None, defaults=[], posonlyargs=[], kwonlyargs=[], kw_defaults=[])
            return ast.Call(func=ast.Name(injected_function, ast.Load()),
                            args=[ast.Lambda(empty_args, node.values[0]), ast.Lambda(empty_args, node.values[1]),
                                  ast.Constant(right_pure), ast.Constant(self.module),
                                  ast.Constant(node.lineno), ast.Constant(self.branch_counter)],
                            keywords=[])
        return node

    def visit_Compare(self, node: Compare) -> Any:
        node = self.generic_visit(node)  # visit child nodes
        if self.instrumentation_level < INSTRUMENTATION_LEVEL_BRANCH_DISTANCE_CMP:
            return node

        operator = self.operator_to_string(node.ops[0])
        if len(node.comparators) > 1:
            # TODO: handle len(comparators) > 1
            return node
        if operator in VALID_OPS:
            self.branch_counter += 1
            left_rec = self.generic_visit(node.left)
            right_rec = self.generic_visit(node.comparators[0])
            ObjectiveRecorder().register_target(branch_objective_name(self.module, node.lineno, self.branch_counter, True))
            ObjectiveRecorder().register_target(branch_objective_name(self.module, node.lineno, self.branch_counter, False))
            return ast.Call(func=ast.Name("compare_statement", ast.Load()),
                            args=[left_rec, ast.Str(operator), right_rec,
                                  ast.Str(self.module), ast.Num(node.lineno), ast.Num(self.branch_counter)],
                            keywords=[])
        return node

    def visit(self, node):
        if isinstance(node, ast.stmt):
            # apply on every statement
            # TODO: review if different visitors per statement type are needed
            return self.visit_Statement(node)
        return super().visit(node)

    @staticmethod
    def operator_to_string(operator):
        if isinstance(operator, Eq):
            return "=="
        elif isinstance(operator, NotEq):
            return "!="
        elif isinstance(operator, Lt):
            return "<"
        elif isinstance(operator, LtE):
            return "<="
        elif isinstance(operator, Gt):
            return ">"
        elif isinstance(operator, GtE):
            return ">="
        elif isinstance(operator, Is):
            return "is"
        elif isinstance(operator, IsNot):
            return "is not"
        elif isinstance(operator, In):
            return "in"
        elif isinstance(operator, NotIn):
            return "not in"

    @staticmethod
    def is_pure(value):
        # TODO: complete implementation
        return False
