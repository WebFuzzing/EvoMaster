import {transform} from "@babel/core";
import dedent from "dedent";

function runPlugin(code: string) {
    const res = transform(code, {
        babelrc: false,
        filename: "test.ts",
        plugins: [__dirname + "/../../../src/instrumentation/babel-plugin-evomaster.ts"],
    });

    if (!res) {
        throw new Error("plugin failed");
    }

    return res;
}

test("simple", () => {
    const code = dedent`
        let x = 0;
    `;

    const res = runPlugin(code);
    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster
        
        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["File_test.ts", "Line_test.ts_00001", "Statement_test.ts_00001_0"]);

        __EM__.enteringStatement("test.ts", 1, 0);
        
        let x = 0;
        
        __EM__.completedStatement("test.ts", 1, 0);        
    `);
});

// test("expressions", () => {
//
//     const code = dedent`
//         let x = 0;
//         x;
//         "foo";
//         5;
//         bar.foo();
//     `;
//
//     const res = runPlugin(code);
//     expect(res.code).toEqual(dedent`
//         //File instrumented with EvoMaster
//
//         const __EM__ = require("evomaster-client-js").InjectedFunctions;
//
//         __EM__.enteringStatement("/test.ts", 1, 0);
//
//         let x = 0;
//
//         __EM__.completedStatement("/test.ts", 1, 0);
//
//         __EM__.enteringStatement("/test.ts", 2, 1);
//
//         __EM__.completingStatement(x, "/test.ts", 2, 1);
//
//         __EM__.enteringStatement("/test.ts", 3, 2);
//
//         __EM__.completingStatement("foo", "/test.ts", 3, 2);
//
//         __EM__.enteringStatement("/test.ts", 4, 3);
//
//         __EM__.completingStatement(5, "/test.ts", 4, 3);
//
//         __EM__.enteringStatement("/test.ts", 5, 4);
//
//         __EM__.completingStatement(bar.foo(), "/test.ts", 5, 4);
//     `);
// });


test("simple multi assignment, same line", () => {
    const code = dedent`
        let x = 0; let y = 0;
    `;

    const res = runPlugin(code);
    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster
        
        const __EM__ = require("evomaster-client-js").InjectedFunctions;

        __EM__.registerTargets(["File_test.ts", "Line_test.ts_00001", "Statement_test.ts_00001_0", "Statement_test.ts_00001_1"]);

        __EM__.enteringStatement("test.ts", 1, 0);
        
        let x = 0;
        
        __EM__.completedStatement("test.ts", 1, 0);
        
        __EM__.enteringStatement("test.ts", 1, 1);
        
        let y = 0;
        
        __EM__.completedStatement("test.ts", 1, 1);        
    `);
});

test("return void", () => {
    const code = dedent`
        const x = function(){             
            return;
        };
    `;

    const res = runPlugin(code);
    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster

        const __EM__ = require("evomaster-client-js").InjectedFunctions;

        __EM__.registerTargets(["File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1"]);

        __EM__.enteringStatement("test.ts", 1, 0);
        
        const x = function () {
          __EM__.markStatementForCompletion("test.ts", 2, 1);
        
          return;
        };
        
        __EM__.completedStatement("test.ts", 1, 0);        
    `);
});

test("simple multi lines", () => {
    const code = dedent`
        let x = 0;
        
        let y = 1;
        
        const sum = function(a,b){             
            return a+b;
        }
        
        const k = 
            sum(x,y);
    `;

    const res = runPlugin(code);
    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster
        
        const __EM__ = require("evomaster-client-js").InjectedFunctions;

        __EM__.registerTargets(["File_test.ts", "Line_test.ts_00001", "Line_test.ts_00003", "Line_test.ts_00005", "Line_test.ts_00006", "Line_test.ts_00009", "Statement_test.ts_00001_0", "Statement_test.ts_00003_1", "Statement_test.ts_00005_2", "Statement_test.ts_00006_3", "Statement_test.ts_00009_4"]);

        __EM__.enteringStatement("test.ts", 1, 0);
        
        let x = 0;
        
        __EM__.completedStatement("test.ts", 1, 0);
        
        __EM__.enteringStatement("test.ts", 3, 1);
        
        let y = 1;
        
        __EM__.completedStatement("test.ts", 3, 1);
        
        __EM__.enteringStatement("test.ts", 5, 2);
        
        const sum = function (a, b) {
          __EM__.enteringStatement("test.ts", 6, 3);

          return __EM__.completingStatement(a + b, "test.ts", 6, 3);
        };
    
        __EM__.completedStatement("test.ts", 5, 2);
        
        __EM__.enteringStatement("test.ts", 9, 4);
        
        const k = __EM__.callBase(() => sum(x, y));
        
        __EM__.completedStatement("test.ts", 9, 4);        
    `);
});


test("< branch distance", () => {
    const code = dedent`
        const x = 2 < 5;        
    `;

    const res = runPlugin(code);
    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster
        
        const __EM__ = require("evomaster-client-js").InjectedFunctions;

        __EM__.registerTargets(["Branch_at_test.ts_at_line_00001_position_0_falseBranch", "Branch_at_test.ts_at_line_00001_position_0_trueBranch", "File_test.ts", "Line_test.ts_00001", "Statement_test.ts_00001_0"]);

        __EM__.enteringStatement("test.ts", 1, 0);
        
        const x = __EM__.cmp(2, "<", 5, "test.ts", 1, 0);
        
        __EM__.completedStatement("test.ts", 1, 0);        
    `);
});


test("! branch distance", () => {
    const code = dedent`
        const x = !!true;        
    `;

    const res = runPlugin(code);
    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster
        
        const __EM__ = require("evomaster-client-js").InjectedFunctions;

        __EM__.registerTargets(["File_test.ts", "Line_test.ts_00001", "Statement_test.ts_00001_0"]);

        __EM__.enteringStatement("test.ts", 1, 0);
        
        const x = __EM__.not(__EM__.not(true));
        
        __EM__.completedStatement("test.ts", 1, 0);        
    `);
});


test("|| branch distance", () => {
    const code = dedent`
        const x = true || false;        
    `;

    //TODO purity boolean will need to be fixed, once implemented

    const res = runPlugin(code);
    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster
        
        const __EM__ = require("evomaster-client-js").InjectedFunctions;

        __EM__.registerTargets(["Branch_at_test.ts_at_line_00001_position_0_falseBranch", "Branch_at_test.ts_at_line_00001_position_0_trueBranch", "File_test.ts", "Line_test.ts_00001", "Statement_test.ts_00001_0"]);

        __EM__.enteringStatement("test.ts", 1, 0);
        
        const x = __EM__.or(() => true, () => false, true, "test.ts", 1, 0);
        
        __EM__.completedStatement("test.ts", 1, 0);        
    `);
});


test("&& branch distance", () => {
    const code = dedent`
        const x = 4 && foo.bar();        
    `;

    const res = runPlugin(code);
    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster
        
        const __EM__ = require("evomaster-client-js").InjectedFunctions;

        __EM__.registerTargets(["Branch_at_test.ts_at_line_00001_position_0_falseBranch", "Branch_at_test.ts_at_line_00001_position_0_trueBranch", "File_test.ts", "Line_test.ts_00001", "Statement_test.ts_00001_0"]);

        __EM__.enteringStatement("test.ts", 1, 0);
        
        const x = __EM__.and(() => 4, () => __EM__.callTracked("test.ts", 1, 1, foo, "bar"), false, "test.ts", 1, 0);
        
        __EM__.completedStatement("test.ts", 1, 0);        
    `);
});

test("for loop", () => {
    const code = dedent`
        for(let i=0; i<5; i++) x=i;        
    `;

    const res = runPlugin(code);
    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster
        
        const __EM__ = require("evomaster-client-js").InjectedFunctions;

        __EM__.registerTargets(["Branch_at_test.ts_at_line_00001_position_0_falseBranch", "Branch_at_test.ts_at_line_00001_position_0_trueBranch", "File_test.ts", "Line_test.ts_00001", "Statement_test.ts_00001_0", "Statement_test.ts_00001_1"]);

        __EM__.markStatementForCompletion("test.ts", 1, 0);
        
        for (let i = 0; __EM__.cmp(i, "<", 5, "test.ts", 1, 0); i++) {
          __EM__.enteringStatement("test.ts", 1, 1);

          x = i;

          __EM__.completedStatement("test.ts", 1, 1);
        }        
    `);
});


test("function call simple", () => {

    const code = dedent`
        foo()
    `;

    const res = runPlugin(code);
    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster

        const __EM__ = require("evomaster-client-js").InjectedFunctions;

        __EM__.registerTargets(["File_test.ts", "Line_test.ts_00001", "Statement_test.ts_00001_0"]);

        __EM__.enteringStatement("test.ts", 1, 0);

        __EM__.callBase(() => foo());

        __EM__.completedStatement("test.ts", 1, 0);
    `);

});


test("function call chain", () => {

    const code = dedent`
        a.b.c.foo().bar(x)(y,z)
    `;

    const res = runPlugin(code);
    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster
    
        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["File_test.ts", "Line_test.ts_00001", "Statement_test.ts_00001_0"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        __EM__.callBase(() => __EM__.callTracked("test.ts", 1, 0, __EM__.callTracked("test.ts", 1, 1, __EM__.squareBrackets("test.ts", 1, 2, __EM__.squareBrackets("test.ts", 1, 3, a, "b"), "c"), "foo"), "bar", x)(y, z));
        
        __EM__.completedStatement("test.ts", 1, 0);
    `);

});


test("ternary", () => {

    const code = dedent`
        (x==42) ? foo():b.bar()
    `;

    const res = runPlugin(code);
    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster
    
        const __EM__ = require("evomaster-client-js").InjectedFunctions;
    
        __EM__.registerTargets(["Branch_at_test.ts_at_line_00001_position_0_falseBranch", "Branch_at_test.ts_at_line_00001_position_0_trueBranch", "File_test.ts", "Line_test.ts_00001", "Statement_test.ts_00001_0", "Statement_test.ts_00001_1", "Statement_test.ts_00001_2"]);
    
        __EM__.enteringStatement("test.ts", 1, 0);
    
        __EM__.cmp(x, "==", 42, "test.ts", 1, 0) ? __EM__.ternary(() => __EM__.callBase(() => foo()), "test.ts", 1, 1) : __EM__.ternary(() => __EM__.callTracked("test.ts", 1, 1, b, "bar"), "test.ts", 1, 2);
    
        __EM__.completedStatement("test.ts", 1, 0);
    `);

});


test("ternary throw", () => {

    const code = dedent`
        (x==42) ? foo():()=>{throw new Error(42)}
    `;

    const res = runPlugin(code);

    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster
    
        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["Branch_at_test.ts_at_line_00001_position_0_falseBranch", "Branch_at_test.ts_at_line_00001_position_0_trueBranch", "File_test.ts", "Line_test.ts_00001", "Statement_test.ts_00001_0", "Statement_test.ts_00001_1", "Statement_test.ts_00001_2", "Statement_test.ts_00001_3"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        __EM__.cmp(x, "==", 42, "test.ts", 1, 0) ? __EM__.ternary(() => __EM__.callBase(() => foo()), "test.ts", 1, 1) : __EM__.ternary(() => () => {
          __EM__.markStatementForCompletion("test.ts", 1, 3);
        
          throw new Error(42);
        }, "test.ts", 1, 2);
        
        __EM__.completedStatement("test.ts", 1, 0);
    `);

});


test("purity analysis 'and' and 'or' with literal/binary expression", () => {

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

    const res = runPlugin(code);

    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster

        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["Branch_at_test.ts_at_line_00005_position_0_falseBranch", "Branch_at_test.ts_at_line_00005_position_0_trueBranch", "Branch_at_test.ts_at_line_00006_position_1_falseBranch", "Branch_at_test.ts_at_line_00006_position_1_trueBranch", "Branch_at_test.ts_at_line_00006_position_2_falseBranch", "Branch_at_test.ts_at_line_00006_position_2_trueBranch", "Branch_at_test.ts_at_line_00006_position_3_falseBranch", "Branch_at_test.ts_at_line_00006_position_3_trueBranch", "Branch_at_test.ts_at_line_00006_position_4_falseBranch", "Branch_at_test.ts_at_line_00006_position_4_trueBranch", "Branch_at_test.ts_at_line_00006_position_5_falseBranch", "Branch_at_test.ts_at_line_00006_position_5_trueBranch", "Branch_at_test.ts_at_line_00008_position_10_falseBranch", "Branch_at_test.ts_at_line_00008_position_10_trueBranch", "Branch_at_test.ts_at_line_00008_position_6_falseBranch", "Branch_at_test.ts_at_line_00008_position_6_trueBranch", "Branch_at_test.ts_at_line_00008_position_7_falseBranch", "Branch_at_test.ts_at_line_00008_position_7_trueBranch", "Branch_at_test.ts_at_line_00008_position_8_falseBranch", "Branch_at_test.ts_at_line_00008_position_8_trueBranch", "Branch_at_test.ts_at_line_00008_position_9_falseBranch", "Branch_at_test.ts_at_line_00008_position_9_trueBranch", "File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Line_test.ts_00004", "Line_test.ts_00005", "Line_test.ts_00006", "Line_test.ts_00008", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1", "Statement_test.ts_00004_2", "Statement_test.ts_00005_3", "Statement_test.ts_00006_4", "Statement_test.ts_00008_5"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        foo = function (x) {
          __EM__.enteringStatement("test.ts", 2, 1);
        
          return __EM__.completingStatement(x, "test.ts", 2, 1);
        };
        
        __EM__.completedStatement("test.ts", 1, 0);
        
        __EM__.enteringStatement("test.ts", 4, 2);
        
        bar = function (x) {
          __EM__.markStatementForCompletion("test.ts", 5, 3);
        
          if (__EM__.cmp(x, "===", "and", "test.ts", 5, 0)) {
            __EM__.enteringStatement("test.ts", 6, 4);
        
            return __EM__.completingStatement(__EM__.and(() => __EM__.and(() => __EM__.and(() => false, () => __EM__.cmp(5, "<", 2, "test.ts", 6, 4), true, "test.ts", 6, 3), () => __EM__.cmp("foo", "<", 1, "test.ts", 6, 5), true, "test.ts", 6, 2), () => __EM__.callBase(() => foo(x)), false, "test.ts", 6, 1), "test.ts", 6, 4);
          } else {
            __EM__.enteringStatement("test.ts", 8, 5);
        
            return __EM__.completingStatement(__EM__.or(() => __EM__.or(() => __EM__.or(() => true, () => __EM__.cmp(5, ">", 2, "test.ts", 8, 9), true, "test.ts", 8, 8), () => __EM__.cmp("foo", "!==", 1, "test.ts", 8, 10), true, "test.ts", 8, 7), () => __EM__.callBase(() => foo(x)), false, "test.ts", 8, 6), "test.ts", 8, 5);
          }
        };
        
        __EM__.completedStatement("test.ts", 4, 2);
    `);

});

test("purity analysis identifier", () => {
    const code = dedent`
        const x = foo();
        const y1 = 42 < 0 && x;
        const y2 = 42 > 0 || x;
    `;

    const res = runPlugin(code);

    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster
        
        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["Branch_at_test.ts_at_line_00002_position_0_falseBranch", "Branch_at_test.ts_at_line_00002_position_0_trueBranch", "Branch_at_test.ts_at_line_00002_position_1_falseBranch", "Branch_at_test.ts_at_line_00002_position_1_trueBranch", "Branch_at_test.ts_at_line_00003_position_2_falseBranch", "Branch_at_test.ts_at_line_00003_position_2_trueBranch", "Branch_at_test.ts_at_line_00003_position_3_falseBranch", "Branch_at_test.ts_at_line_00003_position_3_trueBranch", "File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Line_test.ts_00003", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1", "Statement_test.ts_00003_2"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        const x = __EM__.callBase(() => foo());
        
        __EM__.completedStatement("test.ts", 1, 0);
        
        __EM__.enteringStatement("test.ts", 2, 1);
        
        const y1 = __EM__.and(() => __EM__.cmp(42, "<", 0, "test.ts", 2, 1), () => x, true, "test.ts", 2, 0);
        
        __EM__.completedStatement("test.ts", 2, 1);
        
        __EM__.enteringStatement("test.ts", 3, 2);
        
        const y2 = __EM__.or(() => __EM__.cmp(42, ">", 0, "test.ts", 3, 3), () => x, true, "test.ts", 3, 2);
        
        __EM__.completedStatement("test.ts", 3, 2);
    `);
});


test("purity analysis member function", () => {
    const code = dedent`
        const x = foo();
        const y1 = 42 < 0 && x.y;
        const y2 = 42 > 0 || x.y;
    `;

    const res = runPlugin(code);
    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster
    
        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["Branch_at_test.ts_at_line_00002_position_0_falseBranch", "Branch_at_test.ts_at_line_00002_position_0_trueBranch", "Branch_at_test.ts_at_line_00002_position_1_falseBranch", "Branch_at_test.ts_at_line_00002_position_1_trueBranch", "Branch_at_test.ts_at_line_00003_position_3_falseBranch", "Branch_at_test.ts_at_line_00003_position_3_trueBranch", "Branch_at_test.ts_at_line_00003_position_4_falseBranch", "Branch_at_test.ts_at_line_00003_position_4_trueBranch", "File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Line_test.ts_00003", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1", "Statement_test.ts_00003_2"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        const x = __EM__.callBase(() => foo());
        
        __EM__.completedStatement("test.ts", 1, 0);
        
        __EM__.enteringStatement("test.ts", 2, 1);
        
        const y1 = __EM__.and(() => __EM__.cmp(42, "<", 0, "test.ts", 2, 1), () => __EM__.squareBrackets("test.ts", 2, 2, x, "y"), false, "test.ts", 2, 0);
        
        __EM__.completedStatement("test.ts", 2, 1);
        
        __EM__.enteringStatement("test.ts", 3, 2);
        
        const y2 = __EM__.or(() => __EM__.cmp(42, ">", 0, "test.ts", 3, 4), () => __EM__.squareBrackets("test.ts", 3, 5, x, "y"), false, "test.ts", 3, 3);
        
        __EM__.completedStatement("test.ts", 3, 2);
    `);
});

test("purity analysis this and member function", () => {
    const code = dedent`
        const y1 = 42 < 0 && this.x;
        const y2 = 42 > 0 || this.x;
    `;

    const res = runPlugin(code);
    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster
    
        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["Branch_at_test.ts_at_line_00001_position_0_falseBranch", "Branch_at_test.ts_at_line_00001_position_0_trueBranch", "Branch_at_test.ts_at_line_00001_position_1_falseBranch", "Branch_at_test.ts_at_line_00001_position_1_trueBranch", "Branch_at_test.ts_at_line_00002_position_3_falseBranch", "Branch_at_test.ts_at_line_00002_position_3_trueBranch", "Branch_at_test.ts_at_line_00002_position_4_falseBranch", "Branch_at_test.ts_at_line_00002_position_4_trueBranch", "File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        const y1 = __EM__.and(() => __EM__.cmp(42, "<", 0, "test.ts", 1, 1), () => __EM__.squareBrackets("test.ts", 1, 2, this, "x"), true, "test.ts", 1, 0);
        
        __EM__.completedStatement("test.ts", 1, 0);
        
        __EM__.enteringStatement("test.ts", 2, 1);
        
        const y2 = __EM__.or(() => __EM__.cmp(42, ">", 0, "test.ts", 2, 4), () => __EM__.squareBrackets("test.ts", 2, 5, this, "x"), true, "test.ts", 2, 3);
        
        __EM__.completedStatement("test.ts", 2, 1);
    `);
});

test("update and assigment statement in if()", () => {
    const code = dedent`
        let x = 0;
        let y = 0;
        if (x++) y=1;
        if (y=42) x=42;
    `;

    const res = runPlugin(code);

    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster

        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Line_test.ts_00003", "Line_test.ts_00004", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1", "Statement_test.ts_00003_2", "Statement_test.ts_00003_3", "Statement_test.ts_00004_4", "Statement_test.ts_00004_5"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        let x = 0;
        
        __EM__.completedStatement("test.ts", 1, 0);
        
        __EM__.enteringStatement("test.ts", 2, 1);
        
        let y = 0;
        
        __EM__.completedStatement("test.ts", 2, 1);
        
        __EM__.markStatementForCompletion("test.ts", 3, 2);
        
        if (x++) {
          __EM__.enteringStatement("test.ts", 3, 3);
        
          y = 1;
        
          __EM__.completedStatement("test.ts", 3, 3);
        }
        
        __EM__.markStatementForCompletion("test.ts", 4, 4);
        
        if (y = 42) {
          __EM__.enteringStatement("test.ts", 4, 5);
        
          x = 42;
        
          __EM__.completedStatement("test.ts", 4, 5);
        }
    `);
});

test("purity analysis non-pure: update, assigment", () => {
    const code = dedent`
        let x = 0;
        let y = 0;
        if (true && x++) y=1;
        if (false && (y=42)) x=42;
    `;

    const res = runPlugin(code);

    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster

        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["Branch_at_test.ts_at_line_00003_position_0_falseBranch", "Branch_at_test.ts_at_line_00003_position_0_trueBranch", "Branch_at_test.ts_at_line_00004_position_1_falseBranch", "Branch_at_test.ts_at_line_00004_position_1_trueBranch", "File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Line_test.ts_00003", "Line_test.ts_00004", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1", "Statement_test.ts_00003_2", "Statement_test.ts_00003_3", "Statement_test.ts_00004_4", "Statement_test.ts_00004_5"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        let x = 0;
        
        __EM__.completedStatement("test.ts", 1, 0);
        
        __EM__.enteringStatement("test.ts", 2, 1);
        
        let y = 0;
        
        __EM__.completedStatement("test.ts", 2, 1);
        
        __EM__.markStatementForCompletion("test.ts", 3, 2);
        
        if (__EM__.and(() => true, () => x++, false, "test.ts", 3, 0)) {
          __EM__.enteringStatement("test.ts", 3, 3);
        
          y = 1;
        
          __EM__.completedStatement("test.ts", 3, 3);
        }
        
        __EM__.markStatementForCompletion("test.ts", 4, 4);
        
        if (__EM__.and(() => false, () => y = 42, false, "test.ts", 4, 1)) {
          __EM__.enteringStatement("test.ts", 4, 5);
        
          x = 42;
        
          __EM__.completedStatement("test.ts", 4, 5);
        }
    `);
});


test("purity analysis non-pure: new object, yield, type cast", () => {
    const code = dedent`
        function* foo(index) {
            while (index < 10){
                if ( (index < 2) || (yield index))
                    index++;
                else if ((index < 5) && (new Object())){
                    index = 6;
                }
                else if ((index > 5) && (String(new Date('2019-01-22')))){
                    index--;
                }else
                    index=0;
            }
        }
    `;

    const res = runPlugin(code);

    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster

        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["Branch_at_test.ts_at_line_00002_position_0_falseBranch", "Branch_at_test.ts_at_line_00002_position_0_trueBranch", "Branch_at_test.ts_at_line_00003_position_1_falseBranch", "Branch_at_test.ts_at_line_00003_position_1_trueBranch", "Branch_at_test.ts_at_line_00003_position_2_falseBranch", "Branch_at_test.ts_at_line_00003_position_2_trueBranch", "Branch_at_test.ts_at_line_00005_position_3_falseBranch", "Branch_at_test.ts_at_line_00005_position_3_trueBranch", "Branch_at_test.ts_at_line_00005_position_4_falseBranch", "Branch_at_test.ts_at_line_00005_position_4_trueBranch", "Branch_at_test.ts_at_line_00008_position_5_falseBranch", "Branch_at_test.ts_at_line_00008_position_5_trueBranch", "Branch_at_test.ts_at_line_00008_position_6_falseBranch", "Branch_at_test.ts_at_line_00008_position_6_trueBranch", "File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Line_test.ts_00003", "Line_test.ts_00004", "Line_test.ts_00005", "Line_test.ts_00006", "Line_test.ts_00008", "Line_test.ts_00009", "Line_test.ts_00011", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1", "Statement_test.ts_00003_2", "Statement_test.ts_00004_3", "Statement_test.ts_00005_4", "Statement_test.ts_00006_5", "Statement_test.ts_00008_6", "Statement_test.ts_00009_7", "Statement_test.ts_00011_8"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        function* foo(index) {
          __EM__.markStatementForCompletion("test.ts", 2, 1);
        
          while (__EM__.cmp(index, "<", 10, "test.ts", 2, 0)) {
            __EM__.markStatementForCompletion("test.ts", 3, 2);
        
            if (__EM__.or(() => __EM__.cmp(index, "<", 2, "test.ts", 3, 2), () => yield index, false, "test.ts", 3, 1)) {
              __EM__.enteringStatement("test.ts", 4, 3);
        
              index++;
        
              __EM__.completedStatement("test.ts", 4, 3);
            } else {
              __EM__.markStatementForCompletion("test.ts", 5, 4);
        
              if (__EM__.and(() => __EM__.cmp(index, "<", 5, "test.ts", 5, 4), () => new Object(), false, "test.ts", 5, 3)) {
                __EM__.enteringStatement("test.ts", 6, 5);
        
                index = 6;
        
                __EM__.completedStatement("test.ts", 6, 5);
              } else {
                __EM__.markStatementForCompletion("test.ts", 8, 6);
        
                if (__EM__.and(() => __EM__.cmp(index, ">", 5, "test.ts", 8, 6), () => __EM__.callBase(() => String(new Date('2019-01-22'))), false, "test.ts", 8, 5)) {
                  __EM__.enteringStatement("test.ts", 9, 7);
        
                  index--;
        
                  __EM__.completedStatement("test.ts", 9, 7);
                } else {
                  __EM__.enteringStatement("test.ts", 11, 8);
        
                  index = 0;
        
                  __EM__.completedStatement("test.ts", 11, 8);
                }
              }
            }
          }
        }
        
        __EM__.completedStatement("test.ts", 1, 0);
    `);
});


test("purity analysis expint", () => {
    const code = dedent`
        const expint = (n, x) => {
            if (n < 0 || x < 0.0 || (x == 0.0 && (n == 0 || n == 1)))
                throw new Error("error: n < 0 or x < 0");
        }
    `;

    const res = runPlugin(code);

    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster

        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["Branch_at_test.ts_at_line_00002_position_0_falseBranch", "Branch_at_test.ts_at_line_00002_position_0_trueBranch", "Branch_at_test.ts_at_line_00002_position_1_falseBranch", "Branch_at_test.ts_at_line_00002_position_1_trueBranch", "Branch_at_test.ts_at_line_00002_position_2_falseBranch", "Branch_at_test.ts_at_line_00002_position_2_trueBranch", "Branch_at_test.ts_at_line_00002_position_3_falseBranch", "Branch_at_test.ts_at_line_00002_position_3_trueBranch", "Branch_at_test.ts_at_line_00002_position_4_falseBranch", "Branch_at_test.ts_at_line_00002_position_4_trueBranch", "Branch_at_test.ts_at_line_00002_position_5_falseBranch", "Branch_at_test.ts_at_line_00002_position_5_trueBranch", "Branch_at_test.ts_at_line_00002_position_6_falseBranch", "Branch_at_test.ts_at_line_00002_position_6_trueBranch", "Branch_at_test.ts_at_line_00002_position_7_falseBranch", "Branch_at_test.ts_at_line_00002_position_7_trueBranch", "Branch_at_test.ts_at_line_00002_position_8_falseBranch", "Branch_at_test.ts_at_line_00002_position_8_trueBranch", "File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Line_test.ts_00003", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1", "Statement_test.ts_00003_2"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        const expint = (n, x) => {
          __EM__.markStatementForCompletion("test.ts", 2, 1);
        
          if (__EM__.or(() => __EM__.or(() => __EM__.cmp(n, "<", 0, "test.ts", 2, 2), () => __EM__.cmp(x, "<", 0.0, "test.ts", 2, 3), true, "test.ts", 2, 1), () => __EM__.and(() => __EM__.cmp(x, "==", 0.0, "test.ts", 2, 5), () => __EM__.or(() => __EM__.cmp(n, "==", 0, "test.ts", 2, 7), () => __EM__.cmp(n, "==", 1, "test.ts", 2, 8), true, "test.ts", 2, 6), true, "test.ts", 2, 4), true, "test.ts", 2, 0)) {
            __EM__.markStatementForCompletion("test.ts", 3, 2);
        
            throw new Error("error: n < 0 or x < 0");
          }
        };
        
        __EM__.completedStatement("test.ts", 1, 0);
    `);
});


test("ternary with await expression", async () => {

    const code = dedent`
        f = async function (x) {
            return  x > 0
                ? await new Promise((resolve) => resolve(1))
                : await new Promise((resolve) => resolve(2))
        };       
    `;

    const instrumented = runPlugin(code).code;
    expect(instrumented).toEqual(dedent`
        //File instrumented with EvoMaster

        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["Branch_at_test.ts_at_line_00002_position_0_falseBranch", "Branch_at_test.ts_at_line_00002_position_0_trueBranch", "File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1", "Statement_test.ts_00002_2", "Statement_test.ts_00002_3"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        f = async function (x) {
          __EM__.enteringStatement("test.ts", 2, 1);
        
          return __EM__.completingStatement(__EM__.cmp(x, ">", 0, "test.ts", 2, 0) ? await __EM__.ternary(async () => await new Promise(resolve => __EM__.callBase(() => resolve(1))), "test.ts", 2, 2) : await __EM__.ternary(async () => await new Promise(resolve => __EM__.callBase(() => resolve(2))), "test.ts", 2, 3), "test.ts", 2, 1);
        };
        
        __EM__.completedStatement("test.ts", 1, 0);
    `);
});


test("logic expression with await expression", async () => {

    const code = dedent`
        f = async function (x) {
            return  x > 0 || 
                (await new Promise((resolve) => resolve(x > 1))) ||
                (await new Promise((resolve) => resolve(x > 2)));
        };        
    `;

    const instrumented = runPlugin(code).code;
    expect(instrumented).toEqual(dedent`
        //File instrumented with EvoMaster

        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["Branch_at_test.ts_at_line_00002_position_0_falseBranch", "Branch_at_test.ts_at_line_00002_position_0_trueBranch", "Branch_at_test.ts_at_line_00002_position_1_falseBranch", "Branch_at_test.ts_at_line_00002_position_1_trueBranch", "Branch_at_test.ts_at_line_00002_position_2_falseBranch", "Branch_at_test.ts_at_line_00002_position_2_trueBranch", "Branch_at_test.ts_at_line_00003_position_3_falseBranch", "Branch_at_test.ts_at_line_00003_position_3_trueBranch", "Branch_at_test.ts_at_line_00004_position_4_falseBranch", "Branch_at_test.ts_at_line_00004_position_4_trueBranch", "File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        f = async function (x) {
          __EM__.enteringStatement("test.ts", 2, 1);
        
          return __EM__.completingStatement(__EM__.or(() => __EM__.or(() => __EM__.cmp(x, ">", 0, "test.ts", 2, 2), async () => await new Promise(resolve => __EM__.callBase(() => resolve(__EM__.cmp(x, ">", 1, "test.ts", 3, 3)))), false, "test.ts", 2, 1), async () => await new Promise(resolve => __EM__.callBase(() => resolve(__EM__.cmp(x, ">", 2, "test.ts", 4, 4)))), false, "test.ts", 2, 0), "test.ts", 2, 1);
        };
        
        __EM__.completedStatement("test.ts", 1, 0);
    `);
});


test("embedded await expression", async () => {

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
    expect(instrumented).toEqual(dedent`
        //File instrumented with EvoMaster

        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["Branch_at_test.ts_at_line_00002_position_0_falseBranch", "Branch_at_test.ts_at_line_00002_position_0_trueBranch", "Branch_at_test.ts_at_line_00012_position_2_falseBranch", "Branch_at_test.ts_at_line_00012_position_2_trueBranch", "File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Line_test.ts_00007", "Line_test.ts_00008", "Line_test.ts_00011", "Line_test.ts_00012", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1", "Statement_test.ts_00002_2", "Statement_test.ts_00002_3", "Statement_test.ts_00007_4", "Statement_test.ts_00008_5", "Statement_test.ts_00011_6", "Statement_test.ts_00012_7", "Statement_test.ts_00012_8", "Statement_test.ts_00012_9"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        async function afoo(x) {
          __EM__.enteringStatement("test.ts", 2, 1);
        
          return __EM__.completingStatement(__EM__.cmp(x, ">", 5, "test.ts", 2, 0) ? await __EM__.ternary(async () => await new Promise(resolve => __EM__.callBase(() => resolve(x - 1))), "test.ts", 2, 2) : await __EM__.ternary(async () => await new Promise(resolve => __EM__.callBase(() => resolve(x + 1))), "test.ts", 2, 3), "test.ts", 2, 1);
        }
        
        __EM__.completedStatement("test.ts", 1, 0);
        
        __EM__.enteringStatement("test.ts", 7, 4);
        
        function numToString(x) {
          __EM__.enteringStatement("test.ts", 8, 5);
        
          return __EM__.completingStatement(__EM__.callTracked("test.ts", 8, 1, x, "toString"), "test.ts", 8, 5);
        }
        
        __EM__.completedStatement("test.ts", 7, 4);
        
        __EM__.enteringStatement("test.ts", 11, 6);
        
        f = async function (x) {
          __EM__.enteringStatement("test.ts", 12, 7);
        
          return __EM__.completingStatement(__EM__.cmp((await __EM__.callBase(() => afoo(x))), ">", 5, "test.ts", 12, 2) ? await __EM__.ternary(async () => __EM__.callBase(async () => numToString((await __EM__.callBase(() => afoo(x - 1))))), "test.ts", 12, 8) : await __EM__.ternary(async () => __EM__.callBase(async () => numToString((await __EM__.callBase(() => afoo(x + 1))))), "test.ts", 12, 9), "test.ts", 12, 7);
        };
        
        __EM__.completedStatement("test.ts", 11, 6);

    `);
});

test("disease sut", () => {

    const code = dedent`
        async function afoo(key, lastdays) {
            await redis.hexists(key, lastdays)
                ? parsedData = JSON.parse(await redis.hget(key, lastdays))
                : parsedData = JSON.parse(await redis.hget(key, 'data')); 
        }       
    `;

    const instrumented = runPlugin(code).code;
    expect(instrumented).toEqual(dedent`
        //File instrumented with EvoMaster

        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1", "Statement_test.ts_00002_2", "Statement_test.ts_00002_3"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        async function afoo(key, lastdays) {
          __EM__.enteringStatement("test.ts", 2, 1);
        
          (await __EM__.callTracked("test.ts", 2, 0, redis, "hexists", key, lastdays)) ? await __EM__.ternary(async () => parsedData = __EM__.callTracked("test.ts", 3, 1, JSON, "parse", (await __EM__.callTracked("test.ts", 3, 2, redis, "hget", key, lastdays))), "test.ts", 2, 2) : await __EM__.ternary(async () => parsedData = __EM__.callTracked("test.ts", 4, 3, JSON, "parse", (await __EM__.callTracked("test.ts", 4, 4, redis, "hget", key, 'data'))), "test.ts", 2, 3);
        
          __EM__.completedStatement("test.ts", 2, 1);
        }
        
        __EM__.completedStatement("test.ts", 1, 0);
    `);
});


test("await in the inputs params", ()=>{
    /*
        const data = JSON.parse(await redis.get(keys.therapeutics));
     */

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
    expect(instrumented).toEqual(dedent`
        //File instrumented with EvoMaster

        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["Branch_at_test.ts_at_line_00002_position_0_falseBranch", "Branch_at_test.ts_at_line_00002_position_0_trueBranch", "File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Line_test.ts_00007", "Line_test.ts_00008", "Line_test.ts_00011", "Line_test.ts_00012", "Line_test.ts_00013", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1", "Statement_test.ts_00002_2", "Statement_test.ts_00002_3", "Statement_test.ts_00007_4", "Statement_test.ts_00008_5", "Statement_test.ts_00011_6", "Statement_test.ts_00012_7", "Statement_test.ts_00013_8"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        async function afoo(x) {
          __EM__.enteringStatement("test.ts", 2, 1);
        
          return __EM__.completingStatement(__EM__.cmp(x, ">", 5, "test.ts", 2, 0) ? await __EM__.ternary(async () => await new Promise(resolve => __EM__.callBase(() => resolve(x - 1))), "test.ts", 2, 2) : await __EM__.ternary(async () => await new Promise(resolve => __EM__.callBase(() => resolve(x + 1))), "test.ts", 2, 3), "test.ts", 2, 1);
        }
        
        __EM__.completedStatement("test.ts", 1, 0);
        
        __EM__.enteringStatement("test.ts", 7, 4);
        
        function numToString(x) {
          __EM__.enteringStatement("test.ts", 8, 5);
        
          return __EM__.completingStatement(__EM__.callTracked("test.ts", 8, 1, x, "toString"), "test.ts", 8, 5);
        }
        
        __EM__.completedStatement("test.ts", 7, 4);
        
        __EM__.enteringStatement("test.ts", 11, 6);
        
        f = async function (x) {
          __EM__.enteringStatement("test.ts", 12, 7);
        
          const data = __EM__.callBase(async () => numToString((await __EM__.callBase(() => afoo(x)))));
        
          __EM__.completedStatement("test.ts", 12, 7);
        
          __EM__.enteringStatement("test.ts", 13, 8);
        
          return __EM__.completingStatement(data, "test.ts", 13, 8);
        };
        
        __EM__.completedStatement("test.ts", 11, 6);
    `);

});


test("test array includes replacement", ()=>{
    const code = dedent`
        function foo(searchString){
            const x = ["foo", 1, 2, 3];
            return x.includes(searchString);
        }
    `
    const instrumented = runPlugin(code).code;

    expect(instrumented).toEqual(dedent`
        //File instrumented with EvoMaster
    
        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Line_test.ts_00003", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1", "Statement_test.ts_00003_2"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        function foo(searchString) {
          __EM__.enteringStatement("test.ts", 2, 1);
        
          const x = ["foo", 1, 2, 3];
        
          __EM__.completedStatement("test.ts", 2, 1);
        
          __EM__.enteringStatement("test.ts", 3, 2);
        
          return __EM__.completingStatement(__EM__.callTracked("test.ts", 3, 0, x, "includes", searchString), "test.ts", 3, 2);
        }
        
        __EM__.completedStatement("test.ts", 1, 0);
    `);
})

test("test squareBrackets", ()=>{

    const code  = dedent`
        let x = {"foo": "foo", "bar": "bar", 1: 1};
        y = x[1] && x["foo"];
        y = x.foo;
        let z = 1;
        y = x[z];
        x.foo = "new foo";
        x["1"]++;
    `;

    const instrumented = runPlugin(code).code;
    expect(instrumented).toEqual(dedent`
        //File instrumented with EvoMaster
    
        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["Branch_at_test.ts_at_line_00002_position_0_falseBranch", "Branch_at_test.ts_at_line_00002_position_0_trueBranch", "File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Line_test.ts_00003", "Line_test.ts_00004", "Line_test.ts_00005", "Line_test.ts_00006", "Line_test.ts_00007", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1", "Statement_test.ts_00003_2", "Statement_test.ts_00004_3", "Statement_test.ts_00005_4", "Statement_test.ts_00006_5", "Statement_test.ts_00007_6"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        let x = {
          "foo": "foo",
          "bar": "bar",
          1: 1
        };
        
        __EM__.completedStatement("test.ts", 1, 0);
        
        __EM__.enteringStatement("test.ts", 2, 1);
        
        y = __EM__.and(() => __EM__.squareBrackets("test.ts", 2, 1, x, 1), () => __EM__.squareBrackets("test.ts", 2, 2, x, "foo"), false, "test.ts", 2, 0);
        
        __EM__.completedStatement("test.ts", 2, 1);
        
        __EM__.enteringStatement("test.ts", 3, 2);
        
        y = __EM__.squareBrackets("test.ts", 3, 3, x, "foo");
        
        __EM__.completedStatement("test.ts", 3, 2);
        
        __EM__.enteringStatement("test.ts", 4, 3);
        
        let z = 1;
        
        __EM__.completedStatement("test.ts", 4, 3);
        
        __EM__.enteringStatement("test.ts", 5, 4);
        
        y = __EM__.squareBrackets("test.ts", 5, 4, x, z);
        
        __EM__.completedStatement("test.ts", 5, 4);
        
        __EM__.enteringStatement("test.ts", 6, 5);
        
        x.foo = "new foo";
        
        __EM__.completedStatement("test.ts", 6, 5);
        
        __EM__.enteringStatement("test.ts", 7, 6);
        
        x["1"]++;
        
        __EM__.completedStatement("test.ts", 7, 6);
    `);
});

test("test squareBrackets for arrayPattern and assignmentPattern", ()=>{

    const code  = dedent`
        let user = {};
        [user.name, user.surname] = "John Smith".split(' ');
        let users = [user.name];
        user.f = function foo(bar = user.name) {};
    `;

    const instrumented = runPlugin(code).code;
    expect(instrumented).toEqual(dedent`
        //File instrumented with EvoMaster
    
        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Line_test.ts_00003", "Line_test.ts_00004", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1", "Statement_test.ts_00003_2", "Statement_test.ts_00004_3"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        let user = {};
        
        __EM__.completedStatement("test.ts", 1, 0);
        
        __EM__.enteringStatement("test.ts", 2, 1);
        
        [user.name, user.surname] = __EM__.callTracked("test.ts", 2, 0, "John Smith", "split", ' ');
        
        __EM__.completedStatement("test.ts", 2, 1);
        
        __EM__.enteringStatement("test.ts", 3, 2);
        
        let users = [__EM__.squareBrackets("test.ts", 3, 1, user, "name")];
        
        __EM__.completedStatement("test.ts", 3, 2);
        
        __EM__.enteringStatement("test.ts", 4, 3);
        
        user.f = function foo(bar = __EM__.squareBrackets("test.ts", 4, 2, user, "name")) {};
        
        __EM__.completedStatement("test.ts", 4, 3);
    `);
});


test("test length", ()=>{
    const code  = dedent`
        const x = "foo";
        const y = x.length;
    `;
    const instrumented = runPlugin(code).code;
    expect(instrumented).toEqual(dedent`
        //File instrumented with EvoMaster
        
        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        const x = "foo";
        
        __EM__.completedStatement("test.ts", 1, 0);
        
        __EM__.enteringStatement("test.ts", 2, 1);
        
        const y = __EM__.squareBrackets("test.ts", 2, 0, x, "length");
        
        __EM__.completedStatement("test.ts", 2, 1);
    `);
});


test("test array with square brackets", ()=>{
    const code  = dedent`
        let x = [1];
        x.foo = 2;
        const y = x["foo"];
    `;
    const instrumented = runPlugin(code).code;
    expect(instrumented).toEqual(dedent`
        //File instrumented with EvoMaster
    
        const __EM__ = require("evomaster-client-js").InjectedFunctions;
        
        __EM__.registerTargets(["File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Line_test.ts_00003", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1", "Statement_test.ts_00003_2"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        let x = [1];
        
        __EM__.completedStatement("test.ts", 1, 0);
        
        __EM__.enteringStatement("test.ts", 2, 1);
        
        x.foo = 2;
        
        __EM__.completedStatement("test.ts", 2, 1);
        
        __EM__.enteringStatement("test.ts", 3, 2);
        
        const y = __EM__.squareBrackets("test.ts", 3, 0, x, "foo");
        
        __EM__.completedStatement("test.ts", 3, 2);
        
    `);
});



