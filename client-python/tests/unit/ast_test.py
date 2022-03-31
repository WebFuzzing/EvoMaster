import ast

# import astunparse
import astor

from evomaster_client.instrumentation.ast_transformer import AstTransformer

# TODO: rewrite this as a test module after completing AST transformer with particular statements
if __name__ == "__main__":
    filename = 'tests/dummymodule.py'

    with open(filename) as f:
        source = f.read()

    # Examples: multiple statements per line
    # my_tree = ast.parse('for i in range(2): print("foo"); print("bar")')
    # my_tree = ast.parse('print("foo") if 5 < 10 else print("bar")')
    # my_tree = ast.parse('1 == 2 > 3 not in 4 in 6 != 8')
    # my_tree = ast.parse('if not x: return True')

    CODE = """
x = 1
if x > 0:
    x += 1
    return x
else:
    x -=1
    return
"""
    my_tree = ast.parse(CODE)
    # my_tree = ast.parse(source, filename=filename)

    t = AstTransformer(filename)
    new_tree = t.visit(my_tree)

    # Walk tree in order traversing the bodies
    # def walk_body(node, prefix=''):
    # print(prefix, "node type: ", node.__class__.__name__, " line no:", node.lineno if hasattr(node, 'lineno')
    #       else "unknown")
    #     if hasattr(node, 'body'):
    #         for subnode in node.body:
    #             walk_body(subnode, prefix + '\t')
    # walk_body(my_tree)
    # for node in my_tree.body:
    #     print("node type: ", node.__class__.__name__, " line no:", node.lineno)
    #     if hasattr(node, 'body'):
    #         for subnode in node.body:
    #             print("\t node type: ", subnode.__class__.__name__, " line no:", subnode.lineno)

    # Use ast.walk to traverse the tree (not in source code order)
    # for node in ast.walk(my_tree):
    #     if isinstance(node, ast.stmt):
    #         print("node type: ", node.__class__.__name__, " line no:", node.lineno if hasattr(node, 'lineno')
    #               else "unknown")

    print("### AST Tree ###")
    # print(astunparse.dump(new_tree))
    print(astor.dump_tree(new_tree))
    print("######")

    print("### Code parsed from AST ###")
    # print(astunparse.unparse(new_tree))
    print(astor.to_source(new_tree))
    print("######")

    # Run instrumented code
    # ast.fix_missing_locations(new_tree)
    # exec(compile(new_tree, filename="<ast>", mode="exec"))
