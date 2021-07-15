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

        __EM__.callBase(() => __EM__.callTracked("test.ts", 1, 0, __EM__.callTracked("test.ts", 1, 1, a.b.c, "foo"), "bar", x)(y, z));

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
        
        __EM__.registerTargets(["Branch_at_test.ts_at_line_00002_position_0_falseBranch", "Branch_at_test.ts_at_line_00002_position_0_trueBranch", "Branch_at_test.ts_at_line_00002_position_1_falseBranch", "Branch_at_test.ts_at_line_00002_position_1_trueBranch", "Branch_at_test.ts_at_line_00003_position_2_falseBranch", "Branch_at_test.ts_at_line_00003_position_2_trueBranch", "Branch_at_test.ts_at_line_00003_position_3_falseBranch", "Branch_at_test.ts_at_line_00003_position_3_trueBranch", "File_test.ts", "Line_test.ts_00001", "Line_test.ts_00002", "Line_test.ts_00003", "Statement_test.ts_00001_0", "Statement_test.ts_00002_1", "Statement_test.ts_00003_2"]);
        
        __EM__.enteringStatement("test.ts", 1, 0);
        
        const x = __EM__.callBase(() => foo());
        
        __EM__.completedStatement("test.ts", 1, 0);
        
        __EM__.enteringStatement("test.ts", 2, 1);
        
        const y1 = __EM__.and(() => __EM__.cmp(42, "<", 0, "test.ts", 2, 1), () => x.y, false, "test.ts", 2, 0);
        
        __EM__.completedStatement("test.ts", 2, 1);
        
        __EM__.enteringStatement("test.ts", 3, 2);
        
        const y2 = __EM__.or(() => __EM__.cmp(42, ">", 0, "test.ts", 3, 3), () => x.y, false, "test.ts", 3, 2);
        
        __EM__.completedStatement("test.ts", 3, 2);
    `);
});




