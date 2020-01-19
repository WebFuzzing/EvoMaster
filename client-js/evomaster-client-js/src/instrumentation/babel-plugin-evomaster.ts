import {NodePath, Visitor} from "@babel/traverse";
import * as BabelTypes from "@babel/types";
import template from "@babel/template";
import {Statement} from "@babel/types";
import InjectedFunctions from "./InjectedFunctions";

/*
    https://github.com/jamiebuilds/babel-handbook
 */

export interface PluginOptions {
    opts?: {
        target?: string;
        runtime?: string;
    };
    file: {
        path: NodePath;
    };
}

export interface Babel {
    types: typeof BabelTypes;
}


const ref = "__EM__";


export default function evomaster(
    babel: Babel,
): {visitor: Visitor<PluginOptions>} {

    const t = babel.types;

    let statementCounter = 0;

    const fileName = "foo"; //TODO

    function addLineProbeIfNeeded(path: NodePath){

        if(! t.isStatement(path.node)){
            throw Error("Node is not a Statement: " + path.node);
        }

        if(t.isBlockStatement(path.node)){
            //no point in instrumenting it. Recall, we still instrument its content though
            return;
        }

        const stmt = path.node as Statement;

        /*
            TODO: need better, more explicit way to skip traversing
            new nodes we are adding
         */
        if(! stmt.loc){
            return;
        }

        const l = stmt.loc.start.line;

        const enter = template.ast(
            `${ref}.${InjectedFunctions.enteringStatement.name}(${fileName},${l},${statementCounter})`);
        path.insertBefore(enter);

        const completed = template.ast(
            `${ref}.${InjectedFunctions.completedStatement.name}(${fileName},${l},${statementCounter})`);
        path.insertAfter(completed);

        statementCounter++;
    }


    return {
        visitor: {
            // File: {
            //   enter(path){
            //       //FIXME does not seem this is actually reached in the tests
            //       statementCounter = 0;
            //   }
            // },
            Program: {
                enter(path: NodePath) {
                    t.addComment(path.node, "leading", "File instrumented with EvoMaster", true);

                    statementCounter = 0;

                    const emImport = template.ast(
                        "const "+ref+" = require(\"evomaster-client-js\").InjectedFunctions;"
                    );

                    path.unshiftContainer('body', emImport);
                },
                exit(path) {
                }
            },
            Statement: {
                enter(path){
                    addLineProbeIfNeeded(path);
                }
            }
        }
    };
}

