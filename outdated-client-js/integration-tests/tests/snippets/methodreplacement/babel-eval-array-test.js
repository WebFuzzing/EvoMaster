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

test("Test includes", () => {

    const target = [1, 2, '16'];

    let f;
    const code = dedent`
        f = function(e,c){ return c.includes(e);}
    `;
    const instrumented = runPlugin(code).code;
    eval(instrumented);

    const lambda = (k) => {
        f(k.e, k.c);
    };

    ExampleUtils.checkIncreasingTillCoveredForSingleMethodReplacement(
        [
            {e: 16, c: target},
            {e: '10', c: target},
            {e: 3, c: target}
        ],
        {e: 1, c: target},
        lambda
    );

});

test("Test includes fromIndex", () => {

    const target = [1, 2, '16'];

    let f;
    const code = dedent`
        f = function(e,c,i){ return c.includes(e,i);}
    `;
    const instrumented = runPlugin(code).code;
    eval(instrumented);

    const lambda = (k) => {
        f(k.e, k.c, k.i);
    };

    ExampleUtils.checkIncreasingTillCoveredForSingleMethodReplacement(
        [
            {e: 16, c: target, i:-1},
            {e: 'abc', c: target, i:-1},
            {e: '27', c: target, i:-1},
            {e: '15', c: target, i:-1}
        ],
        {e: '16', c: target, i:-1},
        lambda
    );

});


