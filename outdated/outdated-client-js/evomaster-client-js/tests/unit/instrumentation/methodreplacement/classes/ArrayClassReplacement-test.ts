import ObjectiveNaming from "../../../../../src/instrumentation/shared/ObjectiveNaming";
import ArrayClassReplacement from "../../../../../src/instrumentation/methodreplacement/classes/ArrayClassReplacement";

/*
    These tests just check for semantic equivalent, and not the branch distance values
 */

const id = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate"


test("Test Array includes", () => {

    const arrays_string = ["foo", 1, 2, 3, null, undefined, -0, NaN]

    let ok = ArrayClassReplacement.includes(id, arrays_string, "foo")
    expect(ok).toBe(true);

    ok = ArrayClassReplacement.includes(id, arrays_string, "foo", 0)
    expect(ok).toBe(true);

    ok = ArrayClassReplacement.includes(id, arrays_string, null, 4)
    expect(ok).toBe(true);

    ok = ArrayClassReplacement.includes(id, arrays_string, undefined)
    expect(ok).toBe(true);

    ok = ArrayClassReplacement.includes(id, arrays_string, undefined, -3)
    expect(ok).toBe(true);

    ok = ArrayClassReplacement.includes(id, arrays_string, +0)
    expect(ok).toBe(true);

    ok = ArrayClassReplacement.includes(id, arrays_string, NaN)
    expect(ok).toBe(true);

    let wrong = ArrayClassReplacement.includes(id, arrays_string, "bar");
    expect(wrong).toBe(false);

    wrong = ArrayClassReplacement.includes(id, arrays_string, "foo", 1);
    expect(wrong).toBe(false);

    wrong = ArrayClassReplacement.includes(id, arrays_string, null, 5);
    expect(wrong).toBe(false);

});