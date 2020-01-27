import h from "../../../../src/instrumentation/heuristic/HeuristicsForBooleans";


test("=== numbers", () => {

    const t = h.compare(42, "===", 42);

    expect(t.getOfTrue()).toBe(1);
    expect(t.getOfFalse()).toBeLessThan(1);
});


test("=== numbers, same distance", () => {

    const a = h.compare(42, "===", 42);
    const b = h.compare(-4, "===", -4);

    expect(a.getOfTrue()).toBe(a.getOfTrue());
    expect(b.getOfFalse()).toBe(b.getOfFalse());
});


test("=== numbers, gradient", () => {

    const a = h.compare(10042, "===", 3);
    const b = h.compare(420, "===", 42);
    const c = h.compare(-5, "===", 10);
    const d = h.compare(2, "===", 3);

    expect(a.getOfFalse()).toBe(1);
    expect(b.getOfFalse()).toBe(1);
    expect(c.getOfFalse()).toBe(1);
    expect(d.getOfFalse()).toBe(1);


    expect(a.getOfTrue()).toBeLessThan(b.getOfTrue());
    expect(b.getOfTrue()).toBeLessThan(c.getOfTrue());
    expect(c.getOfTrue()).toBeLessThan(d.getOfTrue());
});





