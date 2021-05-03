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

