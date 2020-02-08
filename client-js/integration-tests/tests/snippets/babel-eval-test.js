const {transform} = require("@babel/core");
const dedent = require("dedent");
const ET = require("evomaster-client-js").internal.ExecutionTracer;
const OR = require("evomaster-client-js").internal.ObjectiveRecorder;
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


beforeEach(()=>{
    ET.reset();
    expect(ET.getNumberOfObjectives()).toBe(0);
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

    expect(ET.getNumberOfObjectives()).toBe(0);
    expect(ET.getNumberOfObjectives(ON.LINE)).toBe(0);


    let x = 0;

    const code = dedent`
        let k = 42;
        x = k;
    `;

    const instrumented = runPlugin(code).code;
    eval(instrumented);

    expect(x).toBe(42);

    expect(ET.getNumberOfObjectives()).toBe(5); // 2 lines, 1 file and 2 stmt
    expect(ET.getNumberOfObjectives(ON.LINE)).toBe(2);
    expect(ET.getNumberOfObjectives(ON.FILE)).toBe(1);
    expect(ET.getNumberOfObjectives(ON.STATEMENT)).toBe(2);
});



test("=== number", () => {

    expect(ET.getNumberOfObjectives(ON.BRANCH)).toBe(0);

    let f;

    const code = dedent`
       f = function(x){ 
            if(x === 42) return true;
            else return false;          
       };
    `;

    const instrumented = runPlugin(code).code;

    eval(instrumented);

    //function is just declared, but not called
    expect(ET.getNumberOfObjectives(ON.BRANCH)).toBe(0);

    f(0);

    expect(ET.getNumberOfObjectives(ON.BRANCH)).toBe(2);
    expect(ET.getNumberOfNonCoveredObjectives(ON.BRANCH)).toBe(1);
    const id = ET.getNonCoveredObjectives(ON.BRANCH).values().next().value;

    const h0 = ET.getValue(id);
    expect(h0).toBeGreaterThan(0);
    expect(h0).toBeLessThan(1);

    f(7);
    const h7 = ET.getValue(id);
    expect(h7).toBeGreaterThan(h0);
    expect(h7).toBeLessThan(1);

    f(42);
    const h42 = ET.getValue(id);
    expect(h42).toBe(1);
});



test("issue in IF handling", () => {

    expect(ET.getNumberOfObjectives(ON.BRANCH)).toBe(0);

    const code = dedent`
       const k = function(x){ 
            if(x === 42) return true;
            else return false;          
       };
       
       const t = k(42);
    `;

    const instrumented = runPlugin(code).code;

    //should not crash
    eval(instrumented);
});