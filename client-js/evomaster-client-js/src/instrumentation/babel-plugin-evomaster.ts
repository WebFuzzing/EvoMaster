import {NodePath, Visitor} from "@babel/traverse";
import * as BabelTypes from "@babel/types";
import {
    BinaryExpression,
    IfStatement,
    LogicalExpression, Program,
    ReturnStatement,
    Statement,
    UnaryExpression
} from "@babel/types";
import template from "@babel/template";
import InjectedFunctions from "./InjectedFunctions";
import ObjectiveNaming from "./ObjectiveNaming";

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


export default function evomasterPlugin(
    babel: Babel,
): {visitor: Visitor<PluginOptions>} {

    const t = babel.types;

    let statementCounter = 0;
    let branchCounter = 0;
    const objectives = Array<string>();

    let fileName = "filename";

    function addBlockIfNeeded(path: NodePath){

        if(!t.isFor(path.node) && !t.isWhile(path.node)){
            throw Error("Node is not a For: " + path.node);
        }

        const stmt = path.node;

        if(stmt.body && !t.isBlockStatement(stmt.body)){
            stmt.body = t.blockStatement([stmt.body]);
            path.replaceWith(stmt);
        }
    }

    function addBlocksToIf(path: NodePath){

        if(!t.isIfStatement(path.node)){
            throw Error("Node is not a IfStatement: " + path.node);
        }

        const ifs = path.node as IfStatement;

        if(ifs.consequent && !t.isBlockStatement(ifs.consequent)){
            ifs.consequent = t.blockStatement([ifs.consequent]);
            path.replaceWith(ifs);
        }

        if(ifs.alternate && !t.isBlockStatement(ifs.alternate)){
            ifs.alternate = t.blockStatement([ifs.alternate]);
            path.replaceWith(ifs);
        }
    }

    function  replaceUnaryExpression(path: NodePath){

        if(!t.isUnaryExpression(path.node)){
            throw Error("Node is not a UnaryExpression: " + path.node);
        }

        const exp = path.node as UnaryExpression;

        if(exp.operator !== "!"){
            //only handling negation, for now at least...
            return;
        }

        const call = t.callExpression(
            t.memberExpression(t.identifier(ref), t.identifier(InjectedFunctions.not.name)),
            [exp.argument]
        );

        path.replaceWith(call);
    }



    function replaceLogicalExpression(path: NodePath){

        if(!t.isLogicalExpression(path.node)){
            throw Error("Node is not a LogicalExpression: " + path.node);
        }

        const exp = path.node as LogicalExpression;

        if(exp.operator !== "&&" && exp.operator !== "||"){
            //nothing to do. Note the existence of the "??" operator
            return;
        }

        /*
           TODO: need better, more explicit way to skip traversing
           new nodes we are adding
      */
        if(! exp.loc){
            return;
        }

        const l = exp.loc.start.line;

        const methodName = exp.operator === "&&" ? InjectedFunctions.and.name : InjectedFunctions.or.name;

        /*
            TODO: there is proper documentation on this function.
            Look like is checking for some types, which in theory should always be pure, like literals.
            But very unclear on its features... eg, would it handle as pure "!false" ???
            TODO need to check... furthermore we do not care if throwing exception
         */
        //const pure = t.isPureish(exp.right);
        const pure = false; //TODO

        const left = t.arrowFunctionExpression([], exp.left, false);
        const right = t.arrowFunctionExpression([], exp.right, false);

        const call = t.callExpression(
            t.memberExpression(t.identifier(ref), t.identifier(methodName)),
            [left,  right, t.booleanLiteral(pure),
                t.stringLiteral(fileName), t.numericLiteral(l), t.numericLiteral(branchCounter)]
        );

        objectives.push(ObjectiveNaming.branchObjectiveName(fileName, l, branchCounter, true));
        objectives.push(ObjectiveNaming.branchObjectiveName(fileName, l, branchCounter, false));

        path.replaceWith(call);
        branchCounter++;
    }

    function replaceBinaryExpression(path: NodePath){

        if(!t.isBinaryExpression(path.node)){
            throw Error("Node is not a BinaryExpression: " + path.node);
        }

        const exp = path.node as BinaryExpression;

        const validOps = ["==", "===", "!=", "!==", "<", "<=", ">", ">="];

        if(! validOps.includes(exp.operator)){
            //nothing to do
            return;
        }

        /*
             TODO: need better, more explicit way to skip traversing
             new nodes we are adding
        */
        if(! exp.loc){
            return;
        }

        const l = exp.loc.start.line;

        const call = t.callExpression(
            t.memberExpression(t.identifier(ref), t.identifier(InjectedFunctions.cmp.name)),
            [exp.left, t.stringLiteral(exp.operator), exp.right,
                t.stringLiteral(fileName), t.numericLiteral(l), t.numericLiteral(branchCounter)]
        );

        objectives.push(ObjectiveNaming.branchObjectiveName(fileName, l, branchCounter, true));
        objectives.push(ObjectiveNaming.branchObjectiveName(fileName, l, branchCounter, false));

        path.replaceWith(call);
        branchCounter++;
    }

    function addLineProbeIfNeeded(path: NodePath){

        if(! t.isStatement(path.node)){
            throw Error("Node is not a Statement: " + path.node);
        }

        const stmt = path.node as Statement;

        if(t.isBlockStatement(stmt)){
            //no point in instrumenting it. Recall, we still instrument its content anyway
            return;
        }

        /*
            TODO: need better, more explicit way to skip traversing
            new nodes we are adding
         */
        if(! stmt.loc){
            return;
        }

        const l = stmt.loc.start.line;

        objectives.push(ObjectiveNaming.lineObjectiveName(fileName,l));
        objectives.push(ObjectiveNaming.statementObjectiveName(fileName, l, statementCounter));

        if( (t.isReturnStatement(stmt) && !stmt.argument)
            || t.isContinueStatement(stmt)
            || t.isThrowStatement(stmt)
            /*
                The following are tricky. They might have inside return stmts
                or labeled jumps (continue/break) to outer-loops.
             */
            || t.isFor(stmt)
            || t.isWhile(stmt)
            || t.isIfStatement(stmt)
            || t.isTryStatement(stmt)
        ){

            const mark = template.ast(
                `${ref}.${InjectedFunctions.markStatementForCompletion.name}("${fileName}",${l},${statementCounter})`);
            path.insertBefore(mark);

        } else {

            const enter = template.ast(
                `${ref}.${InjectedFunctions.enteringStatement.name}("${fileName}",${l},${statementCounter})`);
            path.insertBefore(enter);

            if (t.isReturnStatement(stmt)) {

                const rs = stmt as ReturnStatement;
                const call = t.callExpression(
                    t.memberExpression(t.identifier(ref), t.identifier(InjectedFunctions.completingStatement.name)),
                    [rs.argument, t.stringLiteral(fileName), t.numericLiteral(l), t.numericLiteral(statementCounter)]
                );

                path.replaceWith(t.returnStatement(call));

            } else {

                const completed = template.ast(
                    `${ref}.${InjectedFunctions.completedStatement.name}("${fileName}",${l},${statementCounter})`);
                path.insertAfter(completed);
            }
        }

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
                enter(path: NodePath, state) {
                    t.addComment(path.node, "leading", "File instrumented with EvoMaster", true);

                    statementCounter = 0;
                    branchCounter = 0;
                    objectives.length = 0;

                    //@ts-ignore
                    const srcFilePath: string = state.file.opts.filename;
                    //@ts-ignore
                    const root: string = state.file.opts.root;

                    fileName = srcFilePath.substr(root.length, srcFilePath.length);
                    if(fileName.startsWith('/') || fileName.startsWith('\\')){
                        fileName = fileName.substr(1, fileName.length);
                    }
                    fileName = fileName.replace(/\\/g, "/");

                    const emImport = template.ast(
                        "const "+ref+" = require(\"evomaster-client-js\").InjectedFunctions;"
                    );

                    path.unshiftContainer('body', emImport);

                    objectives.push(ObjectiveNaming.fileObjectiveName(fileName));

                },
                exit(path: NodePath) {
                    //once the whole program is instrumented, the content of "objectives" array will be ready to be injected

                    const unique = Array.from(new Set<string>(objectives)).sort();

                    const call = t.callExpression(
                        t.memberExpression(t.identifier(ref), t.identifier(InjectedFunctions.registerTargets.name)),
                        [t.arrayExpression(unique.map(e => t.stringLiteral(e)))]
                    );
                    const stmt = t.expressionStatement(call);

                    const p = path.node as Program;
                    p.body.splice(1, 0, stmt);
                    path.replaceWith(p);
                }
            },
            BinaryExpression:{
                enter(path: NodePath){
                    replaceBinaryExpression(path);
                }
            },
            LogicalExpression:{
                enter(path: NodePath){
                    replaceLogicalExpression(path);
                }
            },
            UnaryExpression:{
                enter(path: NodePath){
                    replaceUnaryExpression(path);
                }
            },
            Statement: {
                enter(path: NodePath){

                    if(t.isIfStatement(path.node)){
                        addBlocksToIf(path);
                    }

                    if(t.isFor(path.node) || t.isWhile(path.node)){
                        addBlockIfNeeded(path);
                    }

                    addLineProbeIfNeeded(path);
                }
            }
        }
    };
}

