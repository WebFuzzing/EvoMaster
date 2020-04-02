import h from "../../../../src/instrumentation/heuristic/HeuristicsForBooleans";
import Truthness from "../../../../src/instrumentation/heuristic/Truthness";
import HeuristicsForBooleans from "../../../../src/instrumentation/heuristic/HeuristicsForBooleans";

beforeEach(() => {
    h.clearLastEvaluation();
});

test("=== numbers", () => {

    const t = h.compare(42, "===", 42);

    expect(t.getOfTrue()).toBe(1);
    expect(t.getOfFalse()).toBeLessThan(1);
});


test("=== strings", () => {

    const t = h.compare("foo", "===", "foo");

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

test("=== strings, gradient", () => {

    const a = h.compare("aaaaaaaaaaaaaaaaaaaaa", "===", "foo");
    const b = h.compare("", "===", "foo");
    const c = h.compare("a", "===", "foo");
    const d = h.compare("f", "===", "foo");
    const e = h.compare("fo", "===", "foo");

    expect(a.getOfFalse()).toBe(1);
    expect(b.getOfFalse()).toBe(1);
    expect(c.getOfFalse()).toBe(1);
    expect(d.getOfFalse()).toBe(1);
    expect(e.getOfFalse()).toBe(1);


    expect(a.getOfTrue()).toBeLessThan(b.getOfTrue());
    expect(b.getOfTrue()).toBeLessThan(c.getOfTrue());
    expect(c.getOfTrue()).toBeLessThan(d.getOfTrue());
    expect(d.getOfTrue()).toBeLessThan(e.getOfTrue());
});

test("== and === mixed types", () =>{

    const a = h.compare("42", "===", 42);
    const b = h.compare(42, "===", "42");
    const c = h.compare("42", "==", 42);
    const d = h.compare(42, "==", "42");

    expect(a.getOfFalse()).toBe(1);
    expect(b.getOfFalse()).toBe(1);
    expect(c.getOfTrue()).toBe(1);
    expect(d.getOfTrue()).toBe(1);
});


test("== same types", () =>{

    const a = h.compare("42", "==", "42");
    const b = h.compare(42, "==", 42);

    expect(a.getOfTrue()).toBe(1);
    expect(b.getOfTrue()).toBe(1);
});

test("== not handled types", () =>{

    const a = h.compare("42", "==", []);
    const b = h.compare(42, "==", {});

    expect(a.getOfFalse()).toBe(1);
    expect(b.getOfFalse()).toBe(1);
});

test("!==", () =>{
    const t = h.compare(42, "!==", 66);

    expect(t.getOfTrue()).toBe(1);
    expect(t.getOfFalse()).toBeLessThan(1);
});

test("!= same type", () =>{
    const t = h.compare(42, "!=", 66);

    expect(t.getOfTrue()).toBe(1);
    expect(t.getOfFalse()).toBeLessThan(1);
});


test("!== number, same distance", () =>{

    const a = h.compare(44, "!==", 42);
    const b = h.compare(-6, "!==", -4);

    expect(a.getOfTrue()).toBe(a.getOfTrue());
    expect(b.getOfFalse()).toBe(b.getOfFalse());
});


test("< number", () =>{

    const t = h.compare(42, "<", 66);

    expect(t.getOfTrue()).toBe(1);
    expect(t.getOfFalse()).toBeLessThan(1);
});

test("< number equivalent to inverted >=", () =>{

    const a = h.compare(42, "<", 66);
    const b = h.compare(66, ">=", 42);

    expect(a.getOfTrue()).toBe(b.getOfTrue());
    expect(a.getOfFalse()).toBe(b.getOfFalse());
});

test("<= number equivalent to inverted >", () =>{

    const a = h.compare(42, "<=", 66);
    const b = h.compare(66, ">", 42);

    expect(a.getOfTrue()).toBe(b.getOfTrue());
    expect(a.getOfFalse()).toBe(b.getOfFalse());
});


test("< string", () =>{

    const t = h.compare("aaa", "<", "aab");

    expect(t.getOfTrue()).toBe(1);
    expect(t.getOfFalse()).toBeLessThan(1);
});


test("< string equivalent to inverted >=", () =>{

    const a = h.compare("foo", "<", "bar");
    const b = h.compare("bar", ">=", "foo");

    expect(a.getOfTrue()).toBe(b.getOfTrue());
    expect(a.getOfFalse()).toBe(b.getOfFalse());
});


test("< different types", () => {

    const x = 42;
    const y = {};

    expect(x<y).toBe(false);

    const t = h.compare(x, "<", y);

    expect(t.getOfTrue()).toBeLessThan(1);
    expect(t.getOfFalse()).toBe(1);
});



const or = (left: () => any,
    right: () => any,
    isRightPure: boolean,
    fileName: string,
    line: number,
    branchId: number) : Truthness =>  {

    h.evaluateOr(left, right, isRightPure, fileName, line, branchId);
    return HeuristicsForBooleans.getLastEvaluation();
};


const and = (left: () => any,
             right: () => any,
             isRightPure: boolean,
             fileName: string,
             line: number,
             branchId: number): Truthness => {

    h.evaluateAnd(left, right, isRightPure, fileName, line, branchId);
    return HeuristicsForBooleans.getLastEvaluation();
};

test("|| constants", () =>{

    const a = or(
        () => true,
        () => false,
        false, "",0,0);

    const b = or(
        () => true,
        () => true,
        false, "",0,0);

    const c = or(
        () => false,
        () => true,
        false, "",0,0);

    const d = or(
        () => false,
        () => false,
        false, "",0,0);

    //true
    expect(a.getOfTrue()).toBe(1);
    expect(a.getOfTrue()).toBe(b.getOfTrue());
    expect(a.getOfTrue()).toBe(c.getOfTrue());
    expect(d.getOfTrue()).toBeLessThan(1);

    //false
    expect(a.getOfFalse()).toBeLessThan(1);
    expect(a.getOfFalse()).toBe(b.getOfFalse()); // using not pure, otherwise would be smaller
    expect(a.getOfFalse()).toBeLessThan(c.getOfFalse()); // using not pure
    expect(d.getOfFalse()).toBe(1);
});

test("|| constants, using pure", () =>{

    const a = or(
        () => true,
        () => false,
        true, "",0,0);

    const b = or(
        () => true,
        () => true,
        true, "",0,0);

    const c = or(
        () => false,
        () => true,
        true, "",0,0);


    //false, when marking as pure functions
    expect(a.getOfFalse()).toBeLessThan(1);
    expect(a.getOfFalse()).toBeGreaterThan(b.getOfFalse());
    expect(a.getOfFalse()).toBe(c.getOfFalse());
});

test("|| pure functions", () => {

    const a = or(
        () => {return h.compare("foo","===","bar")},
        () => {return h.compare(42,"==",42)},
        true, "", 0, 0);

    expect(a.getOfTrue()).toBe(1);
    expect(a.getOfFalse()).toBeLessThan(1);
});


test("|| pure functions, gradient true", () => {

    const a = or(
        () => {return h.evaluate(100,"===",110,"",0,0)},
        () => {return h.evaluate(0,"==",10,"",0,0)},
        true, "", 0, 0);

    const b = or(
        () => {return h.evaluate(100,"===",110,"",0,0)},
        () => {return h.evaluate(5,"==",10,"",0,0)},
        true, "", 0, 0);

    const c = or(
        () => {return h.evaluate(102,"===",110,"",0,0)},
        () => {return h.evaluate(5,"==",10,"",0,0)},
        true, "", 0, 0);

    const d = or(
        () => {return h.evaluate(107,"===",110,"",0,0)},
        () => {return h.evaluate(5,"==",10,"",0,0)},
        true, "", 0, 0);

    const e = or(
        () => {return h.evaluate(107,"===",110,"",0,0)},
        () => {return h.evaluate(9,"==",10,"",0,0)},
        true, "", 0, 0);

    expect(a.getOfTrue()).toBeLessThan(1);
    expect(a.getOfTrue()).toBeLessThan(b.getOfTrue());
    expect(b.getOfTrue()).toBe(c.getOfTrue());
    expect(c.getOfTrue()).toBeLessThan(d.getOfTrue());
    expect(d.getOfTrue()).toBeLessThan(e.getOfTrue());
    expect(e.getOfTrue()).toBeLessThan(1);
});



test("&& constants", () =>{

    const a = and(
        () => true,
        () => false,
        false, "",0,0);

    const b = and(
        () => true,
        () => true,
        false, "",0,0);

    const c = and(
        () => false,
        () => true,
        false, "",0,0);

    const d = and(
        () => false,
        () => false,
        false, "",0,0);

    //true
    expect(a.getOfTrue()).toBeLessThan(1);
    expect(b.getOfTrue()).toBe(1);
    expect(c.getOfTrue()).toBeLessThan(a.getOfTrue()); // because not pure
    expect(d.getOfTrue()).toBeLessThan(a.getOfTrue());
    expect(c.getOfTrue()).toBe(d.getOfTrue()); // because not pure

    //false
    expect(a.getOfFalse()).toBe(1);
    expect(b.getOfFalse()).toBeLessThan(1);
    expect(c.getOfFalse()).toBe(1);
    expect(d.getOfFalse()).toBe(1);
});



test("&& constants pure", () =>{

    const a = and(
        () => true,
        () => false,
        true, "",0,0);

    const b = and(
        () => true,
        () => true,
        true, "",0,0);

    const c = and(
        () => false,
        () => true,
        true, "",0,0);

    const d = and(
        () => false,
        () => false,
        true, "",0,0);

    //true
    expect(a.getOfTrue()).toBeLessThan(1);
    expect(b.getOfTrue()).toBe(1);
    expect(c.getOfTrue()).toBe(a.getOfTrue());
    expect(d.getOfTrue()).toBeLessThan(a.getOfTrue());
    expect(c.getOfTrue()).toBeGreaterThan(d.getOfTrue());

    //false
    expect(a.getOfFalse()).toBe(1);
    expect(b.getOfFalse()).toBeLessThan(1);
    expect(c.getOfFalse()).toBe(1);
    expect(d.getOfFalse()).toBe(1);
});


test("|| right exception", () =>{

    const a = h.evaluateOr(
        () => 42,
        () => {throw new Error("foo")},
        false, "",0,0);

    expect(a).toBe(42);
});

test("|| left exception, with pure", () =>{

    const f = () => h.evaluateOr(
        () => {throw new Error("foo")},
        () => {return h.evaluate(42,"===",42,"",0,0)},
        true, "",0,0);

    expect(f).toThrow();
    const t = HeuristicsForBooleans.getLastEvaluation();
    expect(t).not.toBeNull();
    //exception was thrown, so neither true nor false
    expect(t.getOfTrue()).toBeLessThan(1);
    expect(t.getOfFalse()).toBeLessThan(1);
});

test("|| right exception gradient", () =>{

    const x = () => h.evaluateOr(
        () => {return h.evaluate(0,"===",42,"",0,0)},
        () => {throw new Error("foo")},
        false, "",0,0);

    const y = () => h.evaluateOr(
        () => {return h.evaluate(40,"===",42,"",0,0)},
        () => {throw new Error("foo")},
        false, "",0,0);

    expect(x).toThrow();
    const a = HeuristicsForBooleans.getLastEvaluation();

    expect(y).toThrow();
    const b = HeuristicsForBooleans.getLastEvaluation();

    //exception was thrown, so neither true nor false
    expect(a.getOfTrue()).toBeLessThan(1);
    expect(a.getOfFalse()).toBeLessThan(1);
    expect(b.getOfTrue()).toBeLessThan(1);
    expect(b.getOfFalse()).toBeLessThan(1);

    //however, the second is closer to be true
    expect(b.getOfTrue()).toBeGreaterThan(a.getOfTrue());
});




test("&& left exception, pure", () =>{

    const f = () => h.evaluateAnd(
        () => {throw new Error("foo")},
        () => {return h.evaluate(42,"===",42,"",0,0)},
        true, "",0,0);

    expect(f).toThrow();
    const t = HeuristicsForBooleans.getLastEvaluation();
    expect(t).not.toBeNull();
    //exception was thrown, so neither true nor false
    expect(t.getOfTrue()).toBeLessThan(1);
    expect(t.getOfFalse()).toBeLessThan(1);
});

test("&& right exception gradient, pure", () =>{

    const a = and(
        () => {return h.evaluate(0,"===",42,"",0,0)},
        () => {throw new Error("foo")},
        true, "",0,0);

    const b = and(
        () => {return h.evaluate(40,"===",42,"",0,0)},
        () => {throw new Error("foo")},
        true, "",0,0);


    //no exception was thrown, even when evaluating right due to pure
    expect(a.getOfTrue()).toBeLessThan(1);
    expect(a.getOfFalse()).toBe(1);
    expect(b.getOfTrue()).toBeLessThan(1);
    expect(b.getOfFalse()).toBe(1);

    //however, the second is closer to be true
    expect(b.getOfTrue()).toBeGreaterThan(a.getOfTrue());
});

test("&& left true, right exception", () =>{

    const f = () => h.evaluateAnd(
        () => {return h.evaluate(42,"===",42,"",0,0)},
        () => {throw new Error("foo")},
        true, "",0,0);

    expect(f).toThrow();
    const t = HeuristicsForBooleans.getLastEvaluation();
    expect(t).not.toBeNull();
    //exception was thrown, so neither true nor false
    expect(t.getOfTrue()).toBeLessThan(1);
    expect(t.getOfFalse()).toBeLessThan(1);
});



