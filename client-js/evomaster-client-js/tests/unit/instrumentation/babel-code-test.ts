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

        __EM__.enteringStatement("test.ts", 1, 0);
        
        let x = 0;
        
        __EM__.completedStatement("test.ts", 1, 0);
        
        __EM__.enteringStatement("test.ts", 1, 1);
        
        let y = 0;
        
        __EM__.completedStatement("test.ts", 1, 1);
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
