const {transform} = require("@babel/core");
const dedent = require("dedent");
const ET = require("evomaster-client-js").internal.ExecutionTracer;
const OR = require("evomaster-client-js").internal.ObjectiveRecorder;


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


beforeEach(()=>{
    ET.reset();
    ET.actionIndex
});


test("simple, no side effect", () => {

    let x = 0;

    const code = dedent`
        let k = 42;
        x = k;
    `;

    const instrumented = runPlugin(code).code;

    const i = eval(instrumented);

    expect(x).toBe(42);
});


test("simple block", () => {

    expect(ET)

    let x = 0;

    const code = dedent`
        let k = 42;
        x = k;
    `;

    const instrumented = runPlugin(code).code;

    const i = eval(instrumented);

    expect(x).toBe(42);
});