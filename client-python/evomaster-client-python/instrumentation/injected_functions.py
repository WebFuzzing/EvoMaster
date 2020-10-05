def entering_statement(module: str, line: int, statement: int):
    print(f"entering statement: {module}-{line}-{statement}")

def completed_statement(module: str, line: int, statement: int):
    print(f"completed statement: {module}-{line}-{statement}")

def completing_statement(value: any, module: str, line: int, statement: int):
    completed_statement(module, line, statement)
    return value
