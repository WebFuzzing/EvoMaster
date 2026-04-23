import Replacement from "../../../../src/instrumentation/methodreplacement/Replacement";
import ExecutionTracer from "../../../../src/instrumentation/staticstate/ExecutionTracer";
import ObjectiveNaming from "../../../../src/instrumentation/shared/ObjectiveNaming";

test("Test no replacement", () => {

    const obj: { y: number, sum(x: number): number } =
        {
            y: 0,
            sum: function (x){
                return x + this.y;
            }
        };

    obj.y = 42;
    const res = Replacement.replaceCall("id", obj, obj.sum, 8);

    expect(res).toBe(50);
})

test("Test string replacement", () => {

    ExecutionTracer.reset();
    let n = ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT);
    expect(n).toBe(0);


    const id = ObjectiveNaming.methodReplacementObjectiveNameTemplate("bar", 0, 1);
    const obj = "foo";
    const fun = obj.startsWith;
    const input = "f";

    expect(fun.call(obj, input)).toBe(true);

    const res = Replacement.replaceCall(id, obj, fun, input);
    expect(res).toBe(true);

    ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
    n = ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT);
    expect(n).toBe(2);
})

test("Test array includes replacement", () => {

    ExecutionTracer.reset();
    let n = ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT);
    expect(n).toBe(0);

    const id = ObjectiveNaming.methodReplacementObjectiveNameTemplate("test_array", 0, 1);
    const obj = ["foo", 1, 2, 3];
    const fun = obj.includes;
    const input = "foo";

    expect(fun.call(obj, input)).toBe(true);

    const res = Replacement.replaceCall(id, obj, fun, input);
    expect(res).toBe(true);

    ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
    n = ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT);
    expect(n).toBe(2);
})