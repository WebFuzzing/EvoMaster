const ExampleUtils = require("./ExampleUtils");

const {transform} = require("@babel/core");
const dedent = require("dedent");
const ET = require("evomaster-client-js").internal.ExecutionTracer;
const ON = require("evomaster-client-js").internal.ObjectiveNaming;


function runPlugin(code) {
    const res = transform(code, {
        babelrc: false,
        filename: "test.ts",
        plugins: ["module:evomaster-client-js"],
    });

    if (!res) {
        throw new Error("plugin failed");
    }

    return res;
}


beforeEach(() => {
    ET.reset();
    expect(ET.getNumberOfObjectives()).toBe(0);
});


test("Test startWith: #objectives", () => {

    expect(ET.getNumberOfObjectives(ON.METHOD_REPLACEMENT)).toBe(0);

    let f;

    const code = dedent`
        f = function(s,i,p){ return s.startsWith(i,p);}
    `;

    const instrumented = runPlugin(code).code;

    eval(instrumented);

    expect(ET.getNumberOfObjectives(ON.METHOD_REPLACEMENT)).toBe(0);

    f("Hello", "H");

    expect(ET.getNumberOfObjectives(ON.METHOD_REPLACEMENT)).toBe(2);
    expect(ET.getNumberOfNonCoveredObjectives(ON.METHOD_REPLACEMENT)).toBe(1);

    f("Hello", "42");

    expect(ET.getNumberOfNonCoveredObjectives(ON.METHOD_REPLACEMENT)).toBe(0);
});


test("Test startsWith: heuristic", () => {

    const target = "abc";

    let f;
    const code = dedent`
        f = function(s,i,p){ return s.startsWith(i,p);}
    `;
    const instrumented = runPlugin(code).code;
    eval(instrumented);

    const lambda = (k) => {
        f(k.s, k.i, k.p);
    };

    ExampleUtils.checkIncreasingTillCoveredForSingleMethodReplacement(
        [
            {s: "", i: target},
            {s: "1", i: target},
            {s: "12", i: target},
            {s: "12345", i: target},
            {s: "1b345", i: target},
            {s: "ab345", i: target}
        ],
        {s: target + "12345", i: target},
        lambda
    );

});


test("Test startsWith offset: heuristic", () => {

    const target = "abc";

    let f;
    const code = dedent`
        f = function(s,i,p){ return s.startsWith(i,p);}
    `;
    const instrumented = runPlugin(code).code;
    eval(instrumented);

    const lambda = (k) => {
        f(k.s, k.i, k.p);
    };

    ExampleUtils.checkIncreasingTillCoveredForSingleMethodReplacement(
        [
            {s: "", i: target, p: -100},
            {s: "1", i: target, p: -100},
            {s: "123abc456", i: target, p: -100},
            {s: "123abc456", i: target, p: -90},
            {s: "123abc456", i: target, p: 50},
            {s: "123abc456", i: target, p: 8},
            {s: "123abc456", i: target, p: 7},
            {s: "123abc456", i: target, p: 0},
            {s: "123abc456", i: target, p: 2}
        ],
        {s: "123"+target+456, i: target, p:3},
        lambda
    );

});


test("Test endsWith", () => {

    const target = "123";

    let f;
    const code = dedent`
        f = function(s,i,p){ return s.endsWith(i,p);}
    `;
    const instrumented = runPlugin(code).code;
    eval(instrumented);

    const lambda = (k) => {
        f(k.s, k.i, k.p);
    };

    ExampleUtils.checkIncreasingTillCoveredForSingleMethodReplacement(
        [
            {s: "", i: target},
            {s: "a", i: target},
            {s: "abced", i: target},
            {s: "abce1", i: target},
            {s: "abce2", i: target},
            {s: "abc12", i: target},
            {s: "abc121", i: target}
        ],
        {s: "foobar" + target, i: target},
        lambda
    );

});



test("Test includes", () => {

    const target = "abc";

    let f;
    const code = dedent`
        f = function(s,i,p){ return s.includes(i,p);}
    `;
    const instrumented = runPlugin(code).code;
    eval(instrumented);

    const lambda = (k) => {
        f(k.s, k.i, k.p);
    };

    ExampleUtils.checkIncreasingTillCoveredForSingleMethodReplacement(
        [
            {s: "", i: target},
            {s: "a", i: target},
            {s: "z  ", i: target},
            {s: "z  zbcd", i: target},
            {s: "z  zbcd bbd", i: target},
            {s: "z  zbcd abbd", i: target}
        ],
        {s:target, i: target},
        lambda
    );

});


test("Test indexOf", () => {

    let f;
    const code = dedent`
        f = function(s,i,p){ return s.indexOf(i,p);}
    `;
    const instrumented = runPlugin(code).code;
    eval(instrumented);

    const lambda = (k) => {
        f(k.s, k.i, k.p);
    };

    const target = "42";

    ExampleUtils.checkIncreasingTillCoveredForSingleMethodReplacement(
        [
            {s: "", i: target},
            {s: "a", i: target},
            {s: "z  ", i: target},
            {s: "z  4", i: target},
            {s: "z  40", i: target},
            {s: "z  41  ", i: target}
        ],
        {s:"abc"+target+"foo", i: target},
        lambda
    );

});

test("Test lastIndexOf", () => {

    let f;
    const code = dedent`
        f = function(s,i,p){ return s.indexOf(i,p);}
    `;
    const instrumented = runPlugin(code).code;
    eval(instrumented);

    const lambda = (k) => {
        f(k.s, k.i, k.p);
    };

    const target = "42";

    ExampleUtils.checkIncreasingTillCoveredForSingleMethodReplacement(
        [
            {s: "", i: target, p:-100},
            {s: "a", i: target, p:0},
            {s: "z  ", i: target, p:2},
            {s: "z  1", i: target, p:2},
            {s: "z  12 ", i: target, p:4},
            {s: "z  41    3  ", i: target, p:9},
            {s: "z  41    3  ", i: target, p:0},
        ],
        {s:"abc"+target+"foo", i: target},
        lambda
    );

});

