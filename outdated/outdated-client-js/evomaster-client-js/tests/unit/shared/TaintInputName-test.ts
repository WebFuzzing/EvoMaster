import {TaintInputName} from "../../../src/instrumentation/shared/TaintInputName";


test("test base", () => {
    const name = TaintInputName.getTaintName(0);
    expect(TaintInputName.isTaintInput(name)).toBe(true);
});

test("test negative id", () => {
    expect(() => TaintInputName.getTaintName(-1)).toThrow();
});

test("test invalid names", () => {
    expect(TaintInputName.isTaintInput("foo")).toBe(false);
    expect(TaintInputName.isTaintInput("")).toBe(false);
    expect(TaintInputName.isTaintInput("evomaster")).toBe(false);
    expect(TaintInputName.isTaintInput("evomaster_input")).toBe(false);
    expect(TaintInputName.isTaintInput("evomaster__input")).toBe(false);
    expect(TaintInputName.isTaintInput("evomaster_a_input")).toBe(false);

    expect(TaintInputName.isTaintInput("_EM_42_XYZ_")).toBe(true);
});

test("test includes", () => {
    const name = TaintInputName.getTaintName(0);
    const text = "some prefix " + name + " some postfix";

    expect(TaintInputName.isTaintInput(text)).toBe(false);
    expect(TaintInputName.includesTaintInput(text)).toBe(true);

});

test("test upper/lower cases", () => {
    const name = TaintInputName.getTaintName(0);

    expect(TaintInputName.isTaintInput(name)).toBe(true);
    expect(TaintInputName.includesTaintInput(name)).toBe(true);

    expect(TaintInputName.isTaintInput(name.toLowerCase())).toBe(true);
    expect(TaintInputName.includesTaintInput(name.toLowerCase())).toBe(true);

    expect(TaintInputName.isTaintInput(name.toUpperCase())).toBe(true);
    expect(TaintInputName.includesTaintInput(name.toUpperCase())).toBe(true);

});

