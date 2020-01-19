const {transform} = require("@babel/core");
const dedent = require("dedent");

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


test("simple", () => {



    let x = 0;

    const code = dedent`
        let k = 42;
        x = k;
    `;

    const instrumented = runPlugin(code).code;

    const i = eval(instrumented);

    expect(x).toBe(42);
});