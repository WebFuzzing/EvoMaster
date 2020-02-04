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
        
        const k = sum(x, y);
        
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
        
        const x = __EM__.or(() => true, () => false, false, "test.ts", 1, 0);
        
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
        
        const x = __EM__.and(() => 4, () => foo.bar(), false, "test.ts", 1, 0);
        
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




