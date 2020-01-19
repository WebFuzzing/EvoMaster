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
        
        __EM__.enteringStatement(foo, 1, 0);
        
        let x = 0;
        
        __EM__.completedStatement(foo, 1, 0);
    `);
});


test("simple multi assignment, same line", () => {
    const code = dedent`
        let x = 0; let y = 0;
    `;

    const res = runPlugin(code);
    expect(res.code).toEqual(dedent`
        //File instrumented with EvoMaster
        
        const __EM__ = require("evomaster-client-js").InjectedFunctions;

        __EM__.enteringStatement(foo, 1, 0);
        
        let x = 0;
        
        __EM__.completedStatement(foo, 1, 0);
        
        __EM__.enteringStatement(foo, 1, 1);
        
        let y = 0;
        
        __EM__.completedStatement(foo, 1, 1);
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

        __EM__.enteringStatement(foo, 1, 0);
        
        let x = 0;
        
        __EM__.completedStatement(foo, 1, 0);
        
        __EM__.enteringStatement(foo, 3, 1);
        
        let y = 1;
        
        __EM__.completedStatement(foo, 3, 1);
        
        __EM__.enteringStatement(foo, 5, 2);
        
        const sum = function (a, b) {
          __EM__.enteringStatement(foo, 6, 3);

          return a + b;
        
          __EM__.completedStatement(foo, 6, 3);
        };
    
        __EM__.completedStatement(foo, 5, 2);
        
        __EM__.enteringStatement(foo, 9, 4);
        
        const k = sum(x, y);
        
        __EM__.completedStatement(foo, 9, 4);
    `);
});
