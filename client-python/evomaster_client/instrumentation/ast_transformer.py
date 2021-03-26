import ast
from ast import UnaryOp, BoolOp, Compare, Eq, NotEq, Lt, LtE, Gt, GtE, Is, IsNot, In, NotIn, And, Or, Invert, Not
from typing import Any

from evomaster_client.instrumentation.objective_naming import (file_objective_name, line_objective_name,
                                                               statement_objective_name, branch_objective_name)
from evomaster_client.instrumentation.objective_recorder import ObjectiveRecorder
from evomaster_client.instrumentation.heuristic.heuristics import VALID_OPS

class AstTransformer(ast.NodeTransformer):
    def __init__(self, module: str):
        self.module = module
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
        node = self.generic_visit(node)  # visit child nodes
        print("Visited node of type: ", node.__class__.__name__, " - line no:", node.lineno)

        if hasattr(node, 'body'):
            print("isBlockStatement. no point in instrumenting it. Recall, we still instrument its content anyway.")
            return node

        self.statement_counter += 1
        ObjectiveRecorder().register_target(line_objective_name(self.module, node.lineno))
        ObjectiveRecorder().register_target(statement_objective_name(self.module, node.lineno, self.statement_counter))

        # For nodes that were part of a collection of statements (that applies to all statement nodes),
        # the visitor may also return a list of nodes rather than just a single node.

        # TODO: Consider statements that do not need a completed_statement (return, continue, raise, etc.)
        # TODO: Replace return(something) with a completing_statement(something, ...)
        return [
            ast.Expr(value=ast.Call(func=ast.Name("entering_statement", ast.Load()),
                     args=[ast.Str(self.module), ast.Num(node.lineno), ast.Num(self.statement_counter)],
                     keywords=[]))
        ] + [ node ] + [
            ast.Expr(value=ast.Call(func=ast.Name("completed_statement", ast.Load()),
                     args=[ast.Str(self.module), ast.Num(node.lineno), ast.Num(self.statement_counter)],
                     keywords=[])),
        ]

    def visit_UnaryOp(self, node: UnaryOp) -> Any:
        # TODO: handle Not and Invert
        if isinstance(node.op, Not):
            return ast.Call(func=ast.Name("not_statement", ast.Load()),
                            args=[node.operand], keywords=[])
        return node

    def visit_BoolOp(self, node: BoolOp) -> Any:
        # TODO: handle And and Or
        pass

    def visit_Compare(self, node: Compare) -> Any:
        node =self.generic_visit(node)  # visit child nodes
        operator = self.operatorToString(node.ops[0])
        # TODO: handle len(operators) > 1
        if operator in VALID_OPS:
            self.branch_counter += 1
            ObjectiveRecorder().register_target(branch_objective_name(self.module, node.lineno, self.branch_counter, True))
            ObjectiveRecorder().register_target(branch_objective_name(self.module, node.lineno, self.branch_counter, False))
            return ast.Call(func=ast.Name("compare_statement", ast.Load()),
                            args=[node.left, ast.Str(operator), node.comparators[0],
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
    def operatorToString(operator):
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
