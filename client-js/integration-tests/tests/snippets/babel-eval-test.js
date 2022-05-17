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

test("side-effects function calls", () => {

    let k;

    const code = dedent`
        const a = function(x){return b(x+1);}
        const b = function(x){return x+1;}
        k = a(0);
    `;

    const instrumented = runPlugin(code).code;

    eval(instrumented);

    expect(k).toBe(2);
});

test("ternary simple", () => {

    expect(ET.getNumberOfObjectives(ON.STATEMENT)).toBe(0);

    let foo;
    // two additional statements for ternary
    const code = dedent`
        foo = function(x){ 
            return (x==42)? x: y;      
        };
    `;

    const instrumented = runPlugin(code).code;
    eval(instrumented);

    let res = foo(42);

    expect(ET.getNumberOfObjectives(ON.STATEMENT)).toBe(3);
    const cons = ET.getValue("Statement_test.ts_00002_2")
    expect(res).toBe(42);
    expect(cons).toBe(1);

    let throws = false;
    try {
        foo(1);
    }catch (e){
        throws = true;
    }
    expect(throws);
    expect(ET.getNumberOfObjectives(ON.STATEMENT)).toBe(4);
    const alt = ET.getValue("Statement_test.ts_00002_3")
    expect(alt).toBe(0.5);
});

test("ternary throw", () => {

    expect(ET.getNumberOfObjectives(ON.STATEMENT)).toBe(0);

    let foo;
    // 'throw' is not expression in js
    const code = dedent`
       foo = function(x){
           return (x==42)? x: ()=>{throw new Error(x)};
       };
    `;

    const instrumented = runPlugin(code).code;
    eval(instrumented);

    let res = foo(42);

    expect(ET.getNumberOfObjectives(ON.STATEMENT)).toBe(3);
    const cons = ET.getValue("Statement_test.ts_00002_2")
    expect(res).toBe(42);
    expect(cons).toBe(1);

    let throws = false;
    try {
        foo(1);
    }catch (e){
        throws = true;
    }
    expect(throws);
    expect(ET.getNumberOfObjectives(ON.STATEMENT)).toBe(4);
    const alt = ET.getValue("Statement_test.ts_00002_3")
    expect(alt).toBe(1);
});


test("purity analysis 'and' and 'or' with literal/binary expression", () => {

    expect(ET.getNumberOfObjectives(ON.STATEMENT)).toBe(0);

    let bar;
    const code = dedent`
        foo = function(x){
            return x;
        };
        bar = function(x){
            if (x === "and")
                return false && 5 < 2 && "foo" < 1 && foo(x);
            else
                return true || 5 > 2 || "foo" !== 1 || foo(x)
        }
    `;

    const instrumented = runPlugin(code).code;
    eval(instrumented);

    let res = bar("and");

    /*
        return __EM__.completingStatement(
            __EM__.and(
                () => __EM__.and(
                    () => __EM__.and(
                        () => false,
                        () => __EM__.cmp(5, "<", 2, "test.ts", 6, 4),
                        true, "test.ts", 6, 3),
                    () => __EM__.cmp("foo", "<", 1, "test.ts", 6, 5),
                    true, "test.ts", 6, 2),
                () => __EM__.callBase(() => foo(x)),
                false, "test.ts", 6, 1),
            "test.ts", 6, 4);

        Branch_at_test.ts_at_line_00006_position_1_falseBranch
        Branch_at_test.ts_at_line_00006_position_2_falseBranch
        Branch_at_test.ts_at_line_00006_position_3_falseBranch
        Branch_at_test.ts_at_line_00006_position_4_falseBranch
        Branch_at_test.ts_at_line_00006_position_5_falseBranch

        Statement_test.ts_00006_4
     */
    const branch_6_boolean_int_str_fun = ET.getValue("Branch_at_test.ts_at_line_00006_position_1_falseBranch")
    expect(branch_6_boolean_int_str_fun).toBe(1);
    const branch_6_boolean_int_str = ET.getValue("Branch_at_test.ts_at_line_00006_position_2_falseBranch")
    expect(branch_6_boolean_int_str).toBe(1);
    const branch_6_boolean_int = ET.getValue("Branch_at_test.ts_at_line_00006_position_3_falseBranch")
    expect(branch_6_boolean_int).toBe(1);
    const branch_6_int = ET.getValue("Branch_at_test.ts_at_line_00006_position_4_falseBranch")
    expect(branch_6_int).toBe(1);
    const branch_6_str = ET.getValue("Branch_at_test.ts_at_line_00006_position_5_falseBranch")
    expect(branch_6_str).toBe(1);

    expect(ET.getValue("Statement_test.ts_00006_4")).toBe(1);
    expect(res).toBe(false);

    // foo() was not executed
    expect(ET.isTargetReached("Statement_test.ts_00002_1")).toBe(false);

    res = bar("or");

    /*
    return __EM__.completingStatement(
        __EM__.or(
            () => __EM__.or(
                () => __EM__.or(
                    () => true, () => __EM__.cmp(5, ">", 2, "test.ts", 8, 9),
                    true, "test.ts", 8, 8),
                () => __EM__.cmp("foo", "!==", 1, "test.ts", 8, 10),
                true, "test.ts", 8, 7),
            () => __EM__.callBase(() => foo(x)),
            false, "test.ts", 8, 6),
        "test.ts", 8, 5);

        Branch_at_test.ts_at_line_00008_position_6_trueBranch
        Branch_at_test.ts_at_line_00008_position_7_trueBranch
        Branch_at_test.ts_at_line_00008_position_8_trueBranch
        Branch_at_test.ts_at_line_00008_position_9_trueBranch
        Branch_at_test.ts_at_line_00008_position_10_trueBranch

        Statement_test.ts_00008_5
     */

    const branch_8_boolean_int_str_fun = ET.getValue("Branch_at_test.ts_at_line_00008_position_6_trueBranch")
    expect(branch_8_boolean_int_str_fun).toBe(1);
    const branch_8_boolean_int_str = ET.getValue("Branch_at_test.ts_at_line_00008_position_7_trueBranch")
    expect(branch_8_boolean_int_str).toBe(1);
    const branch_8_boolean_int = ET.getValue("Branch_at_test.ts_at_line_00008_position_8_trueBranch")
    expect(branch_8_boolean_int).toBe(1);
    const branch_8_int = ET.getValue("Branch_at_test.ts_at_line_00008_position_9_trueBranch")
    expect(branch_8_int).toBe(1);
    const branch_8_str = ET.getValue("Branch_at_test.ts_at_line_00008_position_10_trueBranch")
    expect(branch_8_str).toBe(1);

    expect(ET.getValue("Statement_test.ts_00008_5")).toBe(1);
    expect(res).toBe(true);

    // foo() was not executed
    expect(ET.isTargetReached("Statement_test.ts_00002_1")).toBe(false);

});


test("purity analysis 'and' and 'or' with update and assignment", () => {

    expect(ET.getNumberOfObjectives(ON.STATEMENT)).toBe(0);

    let x = 0;
    let y = 0;
    const code = dedent`
        if (true || x < 2) x = 1;
        if (true || x++) y=1;
        if (false && (y=42)) x=42;
    `;

    const instrumented = runPlugin(code).code;
    eval(instrumented);

    /*
        if (__EM__.or(() => true, () => __EM__.cmp(x, "<", 2, "test.ts", 1, 1), true, "test.ts", 1, 0)) {
          __EM__.enteringStatement("test.ts", 1, 1);

          x = 1;

          __EM__.completedStatement("test.ts", 1, 1);
        }

        "Branch_at_test.ts_at_line_00001_position_0_falseBranch",
        "Branch_at_test.ts_at_line_00001_position_0_trueBranch",
        "Branch_at_test.ts_at_line_00001_position_1_falseBranch",
        "Branch_at_test.ts_at_line_00001_position_1_trueBranch"
     */

    expect(ET.getNumberOfObjectives("Branch_at_test.ts_at_line_00001_position_0_trueBranch")).toBe(1);
    // x< 2 is evaluated even it is in shortcircuits
    expect(ET.getNumberOfObjectives("Branch_at_test.ts_at_line_00001_position_1_trueBranch")).toBe(1);
    // x++ was not executed, but y is set to 1
    expect(x).toBe(1);
    // y=42 was not executed
    expect(y).toBe(1);
});


test("function call reference via array access", () => {

    let k;

    const code = dedent`
        const getName = () => "foo";
        const x = {"foo": (x) => {return x*2}};
        k = x.foo(2);
        k = x["foo"](k);
        k = x[getName()](k);        
    `;

    const instrumented = runPlugin(code).code;

    eval(instrumented);

    expect(k).toBe(16);
});


test("ternary with await expression", async () => {

    let f;
    const code = dedent`
        f = async function (x) {
            return  x > 0
                ? await new Promise((resolve) => resolve(1))
                : await new Promise((resolve) => resolve(2));
        };       
    `;

    const instrumented = runPlugin(code).code;
    eval(instrumented);

    let k = await f(0);
    expect(k).toBe(2);

    k = await f(1);
    expect(k).toBe(1);
});

test("logic expression with await expression", async () => {

    let f;
    const code = dedent`
        f = async function (x) {
            return  x > 0 || 
                (await new Promise((resolve) => resolve(x > 1))) ||
                (await new Promise((resolve) => resolve(x > 2)));
        };  
    `;

    const instrumented = runPlugin(code).code;
    eval(instrumented);

    let k = await f(0);
    expect(k).toBe(false);

    k = await f(1);
    expect(k).toBe(true);
});


test("toString", async () => {

    let f;
    const code = dedent`
        f = function (x) {
            return x.toString();
        }
    `;

    const instrumented = runPlugin(code).code;
    eval(instrumented);

    let k = await f(5);
    expect(k).toBe("5");

    k = await f(10);
    expect(k).toBe("10");
});

test("embedded await expression", async () => {

    let f;
    const code = dedent`
        async function afoo (x) {
            return  x > 5
                ? await new Promise((resolve) => resolve(x - 1))
                : await new Promise((resolve) => resolve(x + 1))
        }     
        
        function numToString (x) {
            return x.toString();
        }
        
        f = async function (x) {
            return await afoo(x) > 5 
                ? numToString(await afoo(x - 1))
                : numToString(await afoo(x + 1));
        };    
    `;

    const instrumented = runPlugin(code).code;
    eval(instrumented);

    let k = await f(5);
    expect(k).toBe("5");

    k = await f(10);
    expect(k).toBe("8");
});


test("await in the inputs params", async () => {

    let f;
    const code = dedent`
        async function afoo (x) {
            return  x > 5
                ? await new Promise((resolve) => resolve(x - 1))
                : await new Promise((resolve) => resolve(x + 1))
        }     
        
        function numToString (x) {
            return x.toString();
        }
        
        f = async function (x) {
            const data = numToString(await afoo(x));
            return data;
        }; 
    `;

    const instrumented = runPlugin(code).code;
    eval(instrumented);

    let k = await f(5);
    expect(k).toBe("6");

    k = await f(10);
    expect(k).toBe("9");
});


test("test array includes", async () => {

    expect(ET.getNumberOfObjectives(ON.METHOD_REPLACEMENT)).toBe(0);
    let f;
    const code = dedent`
        f = function foo(searchString){
            const x = ["foo", 1, 2, 3];
            return x.includes(searchString);
        }
    `;

    const instrumented = runPlugin(code).code;
    eval(instrumented);

    let k = f("foo");
    expect(k).toBe(true);
    expect(ET.getNumberOfObjectives(ON.METHOD_REPLACEMENT)).toBe(2);
    expect(ET.getValue("MethodReplacement_at_test.ts_00003_0_BOOLEAN_true")).toBe(1);
    expect(ET.getValue("MethodReplacement_at_test.ts_00003_0_BOOLEAN_false") < 1).toBe(true);

    k = f("bar");
    expect(k).toBe(false);
    expect(ET.getValue("MethodReplacement_at_test.ts_00003_0_BOOLEAN_false")).toBe(1);

});

test("test array includes with null found", async () => {

    expect(ET.getNumberOfObjectives(ON.METHOD_REPLACEMENT)).toBe(0);
    let f;
    const code = dedent`
        f = function foo(searchString){
            const x = ["foo", 1, 2, 3, null];
            return x.includes(searchString);
        }
    `;

    const instrumented = runPlugin(code).code;
    eval(instrumented);

    let k = f(null);
    expect(k).toBe(true);
    expect(ET.getNumberOfObjectives(ON.METHOD_REPLACEMENT)).toBe(2);
    expect(ET.getValue("MethodReplacement_at_test.ts_00003_0_BOOLEAN_true")).toBe(1);
    expect(ET.getValue("MethodReplacement_at_test.ts_00003_0_BOOLEAN_false") < 1).toBe(true);

    k = f('3');
    expect(k).toBe(false);
    expect(ET.getValue("MethodReplacement_at_test.ts_00003_0_BOOLEAN_false")).toBe(1);

});

test("test squareBrackets", async () => {
    expect(ET.getNumberOfObjectives(ON.METHOD_REPLACEMENT)).toBe(0);
    let f;
    const code = dedent`
        f = function foo(property){
            const x = {"foo": "foo", "bar": "bar"};
            return x[property];
        }
    `;

    const instrumented = runPlugin(code).code;
    eval(instrumented);

    let k = f("foo");
    expect(k).toBe("foo");
    expect(ET.getNumberOfObjectives(ON.METHOD_REPLACEMENT)).toBe(2);
    expect(ET.getValue("MethodReplacement_at_test.ts_00003_0_BOOLEAN_true")).toBe(1);
    expect(ET.getValue("MethodReplacement_at_test.ts_00003_0_BOOLEAN_false") < 1).toBe(true);

    k = f("foo2");
    expect(k).toBe(undefined);
    expect(ET.getValue("MethodReplacement_at_test.ts_00003_0_BOOLEAN_false")).toBe(1);
})

test("test squareBrackets with null found", async () => {
    expect(ET.getNumberOfObjectives(ON.METHOD_REPLACEMENT)).toBe(0);
    let f;
    const code = dedent`
        f = function foo(property){
            const x = {null: "foo", "bar": "bar"};
            return x[property];
        }
    `;

    const instrumented = runPlugin(code).code;
    eval(instrumented);

    let k = f(null);
    expect(k).toBe("foo");
    expect(ET.getNumberOfObjectives(ON.METHOD_REPLACEMENT)).toBe(2);
    expect(ET.getValue("MethodReplacement_at_test.ts_00003_0_BOOLEAN_true")).toBe(1);
    expect(ET.getValue("MethodReplacement_at_test.ts_00003_0_BOOLEAN_false") < 1).toBe(true);

    k = f("foo2");
    expect(k).toBe(undefined);
    expect(ET.getValue("MethodReplacement_at_test.ts_00003_0_BOOLEAN_false")).toBe(1);
})


test("test string length", async () => {
    expect(ET.getNumberOfObjectives(ON.METHOD_REPLACEMENT)).toBe(0);
    let f;
    const code = dedent`
        f = function foo(x){
            return x.length;
        }
    `;

    const instrumented = runPlugin(code).code;
    eval(instrumented);

    let k = f("foo");
    expect(k).toBe(3);

})

test("test array with square brackets and length", async () => {
    expect(ET.getNumberOfObjectives(ON.METHOD_REPLACEMENT)).toBe(0);
    let a;
    let s;
    let l;
    const code = dedent`
        a = [1];
        s = function setA(k, v){
            a[k] = v;
            return a["length"];
        }
    `;

    const instrumented = runPlugin(code).code;
    eval(instrumented);

    expect(a["0"]).toBe(1);
    expect(a.length).toBe(1);
    expect(a["length"]).toBe(a.length);

    l = s("foo", 2);
    expect(a["foo"]).toBe(2);
    expect(l).toBe(a.length);

    l = s("bar", "barV");
    expect(a["bar"]).toBe("barV");
    expect(l).toBe(a.length);

    a[2] = 2;
    expect(a.length).toBe(3);
    l = s(1, 2);
    expect(a["1"]).toBe(2);
    expect(l).toBe(3);
})