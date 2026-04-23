import ObjectiveNaming from "../../../../../src/instrumentation/shared/ObjectiveNaming";
import StringClassReplacement from "../../../../../src/instrumentation/methodreplacement/classes/StringClassReplacement";

/*
    These tests just check for semantic equivalent, and not the branch distance values
 */


const id = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate"


test("Test startsWith", () => {

    const ok = StringClassReplacement.startsWith(id, "Hello World", "Hello");
    expect(ok).toBe(true);

    const wrong = StringClassReplacement.startsWith(id, "Hello World", "World");
    expect(wrong).toBe(false);
});

test("Test endsWith", () => {

    const ok = StringClassReplacement.endsWith(id, "Hello World", "World");
    expect(ok).toBe(true);

    const wrong = StringClassReplacement.endsWith(id, "Hello World", "Hello");
    expect(wrong).toBe(false);
});

test("Test includes", () => {

    const ok = StringClassReplacement.includes(id, "Hello World", "o W");
    expect(ok).toBe(true);

    const wrong = StringClassReplacement.includes(id, "Hello World", "42")
    expect(wrong).toBe(false);
});

test("Test indexOf", () => {

    const ok = StringClassReplacement.indexOf(id, "Hello World", "o W");
    expect(ok).toBe(4);

    const wrong = StringClassReplacement.indexOf(id, "Hello World", "42")
    expect(wrong).toBe(-1);
});

test("Test lastIndexOf", () => {

    const ok = StringClassReplacement.lastIndexOf(id, "Hello World", "o");
    expect(ok).toBe(7);

    const wrong = StringClassReplacement.lastIndexOf(id, "Hello World", "42")
    expect(wrong).toBe(-1);
});



