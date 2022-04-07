import {NodePath, Visitor} from "@babel/traverse";
import * as BabelTypes from "@babel/types";
import {
    BinaryExpression,
    CallExpression,
    IfStatement,
    LogicalExpression,
    Program,
    ReturnStatement,
    Statement,
    UnaryExpression,
    ConditionalExpression,
    Expression,
    isAwaitExpression,
    MemberExpression,
    isAssignmentExpression,
    AssignmentExpression,
    AssignmentPattern
} from "@babel/types";
import template from "@babel/template";
import InjectedFunctions from "./InjectedFunctions";
import ObjectiveNaming from "./shared/ObjectiveNaming";

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
         // const pure = t.isPureish(exp.right);
        const pure = isPureExpression(exp.right);

        const left = t.arrowFunctionExpression([], exp.left, doesContainAwaitExpression(exp.left));
        const right = t.arrowFunctionExpression([], exp.right, doesContainAwaitExpression(exp.right));

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

    /**
     * @param node to be checked
     * @return whether the expression contains asynchronous functions.
     */
    function doesContainAwaitExpression(node: Expression) : boolean{
        if (t.isAwaitExpression(node))
            return true;
        if (t.isAssignmentExpression(node))
            return doesContainAwaitExpression(node.right);
        if (t.isArrowFunctionExpression(node)) return node.async;
        if (t.isCallExpression(node) || t.isOptionalCallExpression(node) || t.isNewExpression(node)){
            return node.arguments.some((arg) => t.isExpression(arg) && doesContainAwaitExpression(arg))
        }
        // isLogicalExpression should be handled by introduced arrow function
        return false;
    }


    /**
     * @param node to be analyzed
     * @return whether the node is pure
     *
     * we analyze whether the expression in LogicalExpression (from right to left) is pure,
     *      i.e., the expression can be evaluated without any possible exception.
     * here we consider all types listed in https://babeljs.io/docs/en/babel-types#expression
     * Additional info: pureish in babel type https://babeljs.io/docs/en/babel-types#pureish
     *
     */
    function isPureExpression(node: Expression): boolean{

        let pure = false;

        if (t.isStringLiteral(node)
            || t.isNumericLiteral(node) || t.isNullLiteral(node) || t.isBooleanLiteral(node) || t.isBigIntLiteral(node)
            || t.isRegExpLiteral(node)
            || t.isIdentifier(node) // e.g., 'x'
            // || t.isDecimalLiteral(node) //TODO do not find this lib, but it exists https://babeljs.io/docs/en/babel-types#decimalliteral
            || t.isArrayExpression(node)
            || t.isClassExpression(node)
            || t.isObjectExpression(node)
            || t.isJSXElement(node) || t.isJSXFragment(node) // need to discuss
        )
            pure = true;
        else if (t.isParenthesizedExpression(node)){
            pure = isPureExpression(node.expression);
        }else if (t.isSequenceExpression(node)){
            for (let exp of node.expressions){
                pure = pure && isPureExpression(exp);
            }
        } else if (t.isTemplateLiteral(node)){
            // https://babeljs.io/docs/en/babel-types#templateliteral
            for (let exp of node.expressions){
                pure = pure && isPureExpression(exp);
            }
        } else if (t.isTaggedTemplateExpression(node)){
            // https://babeljs.io/docs/en/babel-types#taggedtemplateexpression
            pure = isPureExpression(node.tag)  && isPureExpression(node.quasi);
        } else if (t.isUnaryExpression(node)){
            /*
                https://babeljs.io/docs/en/babel-types#unaryexpression
                "void" | "throw" | "delete" | "!" | "+" | "-" | "~" | "typeof"
             */
            const excludeOp= ["throw", "delete"] // Man: not sure whether to include "void"
            pure = !excludeOp.includes(node.operator) && isPureExpression(node.argument);

        } else if (t.isBinaryExpression(node)){
            /*
                https://babeljs.io/docs/en/babel-types#binaryexpression
                operator: "+" | "-" | "/" | "%" | "*" | "**" | "&" | "|" | ">>" | ">>>" | "<<" | "^"
                        | "==" | "===" | "!=" | "!==" | "in" | "instanceof" | ">" | "<" | ">=" | "<="

                shall we need a further handling based on the operator? maybe not since it will not lead to any exception,
                e.g., for "/", even for instance 5 / 0, it just returns "Infinity".
                As checked, a result of '(5 / 0) && true' is true, a result of 'a / b || false' is Infinity
             */
            pure = isPureExpression(node.right) && isPureExpression(node.left);
        } else if (t.isConditionalExpression(node)){
            // https://babeljs.io/docs/en/babel-types#conditionalexpression
            pure = isPureExpression(node.test) && isPureExpression(node.consequent) && isPureExpression(node.alternate);
        } else if (t.isMemberExpression(node)){
            /*
                https://babeljs.io/docs/en/babel-types#memberexpression
                currently, we only identify 'this.x' as pure
                TODO
                if the object exists in the 'and' expression (e.g., x && x.y), it is false.
                else it is probably true.
             */
            return t.isThisExpression(node.object);
        } else if (t.isOptionalMemberExpression(node)) {
            /*
                https://babeljs.io/docs/en/babel-types#optionalmemberexpression
                since the object is checked, here the pureish depends on its property
             */
            return isPureExpression(node.property);
        } else if (t.isFunctionExpression(node)){
            /*
                https://babeljs.io/docs/en/babel-types#functionexpression
                TODO this needs a further discussion
                var foo = function() { return 5; }
                since this is kind of fun declaration, it may be pure.
             */
            return true;
        } else if (t.isThisExpression(node)){
            /*
                here we identify 'this' pure since it would not be null
             */
            return true;
        } else if (t.isArrowFunctionExpression(node)
            || t.isAwaitExpression(node) // executing it might lead to some side-effect
            || t.isCallExpression(node) || t.isOptionalCallExpression(node) // there might exist throw inside call. without a deep check, we set it false for the moment
            || t.isUpdateExpression(node) // "++" | "--"
            || t.isAssignmentExpression(node)
            || t.isDoExpression(node) // https://babeljs.io/docs/en/babel-types#doexpression
            || t.isMetaProperty(node) || t.isNewExpression(node) // executing it might lead to some side-effect
            || t.isPipelinePrimaryTopicReference(node) // i.e., |>, we set it false for the moment
            || t.isSuper(node) || t.isImport(node)  // need a further check
            /*
                executing following might lead to some side-effect
             */
            || t.isTSAsExpression(node) || t.isTSTypeAssertion(node) ||t.isTSNonNullExpression(node)  || t.isTypeCastExpression(node)
            /*
                executing it might lead to some side-effect, e.g., con1 && con2 && yield x
             */
            || t.isYieldExpression(node)
            // do not find following expression in the lib, but they exist in the list https://babeljs.io/docs/en/babel-types#expression
            // || t.isModuleExpression(node)
            // || t.isRecordExpression(node)
            // || t.isTupleExpression(node)
        ){
            pure = false;
        } else if (t.isLogicalExpression(node)){
            /*
                it is pure only if its right and left are pure

                its def:
                operator: "||" | "&&" | "??" (required)
                left: Expression (required)
                right: Expression (required)

             */
            pure = isPureExpression(node.right) && isPureExpression(node.left);
        } else{
            throw Error("Missing expression type in the pure analysis: " + node.type);
        }
        return pure;
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

    function replaceConditionalExpression(path: NodePath){

        if(!t.isConditionalExpression(path.node)){
            throw Error("Node is not a ConditionalExpression: " + path.node);
        }
        const exp = path.node as ConditionalExpression;

        if(! exp.loc){
            return;
        }

        const l = exp.loc.start.line;
        /*
            test ? consequent : alternate
            test: Expression;
            consequent: Expression;
            alternate: Expression;

            transformed code:
                test? __EM__.ternary(()=>consequent, ... ) : __EM__.ternary(()=>alternate, ...)

            here, we create additional two statements targets for 'consequent' and 'alternate'
            for the statement targets,
                if consequent(/alternate) is executed without exception, h is 1
                otherwise h is 0.5

            Note that we do not further replace 'test' here.
            if it is related to condition, it will be replaced by other existing replacement and
            additional branch will be added there.

         */
        const consequentIsAwait = doesContainAwaitExpression(exp.consequent)
        const consequent = t.arrowFunctionExpression([], exp.consequent, consequentIsAwait);
        const alternateIsAwait = doesContainAwaitExpression(exp.alternate)
        const alternate = t.arrowFunctionExpression([], exp.alternate, alternateIsAwait);


        objectives.push(ObjectiveNaming.statementObjectiveName(fileName, l, statementCounter));
        exp.consequent = t.callExpression(
            t.memberExpression(t.identifier(ref), t.identifier(InjectedFunctions.ternary.name)),
            [consequent,
                t.stringLiteral(fileName), t.numericLiteral(l), t.numericLiteral(statementCounter)]
        );

        if (consequentIsAwait)
            exp.consequent = t.awaitExpression(exp.consequent);


        statementCounter++;

        objectives.push(ObjectiveNaming.statementObjectiveName(fileName, l, statementCounter));
        exp.alternate = t.callExpression(
            t.memberExpression(t.identifier(ref), t.identifier(InjectedFunctions.ternary.name)),
            [alternate,
                t.stringLiteral(fileName), t.numericLiteral(l), t.numericLiteral(statementCounter)]
        );

        if (alternateIsAwait)
            exp.alternate = t.awaitExpression(exp.alternate);

        statementCounter++;
    }

    function replaceMemberExpression(path: NodePath){
        const member = path.node as MemberExpression

        /*
            TODO: need better, more explicit way to skip traversing
            new nodes we are adding
        */
        if(! member.loc ||
            // @ts-ignore
            member.evomaster
        ){
            return;
        }

        // skip to replace it if it is under updateExpression, such as ++, --
        if (path.parent && (t.isUpdateExpression(path.parent) ||
            // skip to replace it if it is left of assignmentExpression
            (t.isAssignmentExpression(path.parent) && ((path.parent as AssignmentExpression).left == path.node)) ||
            // skip for assignmentpattern https://babeljs.io/docs/en/babel-types#assignmentpattern
            (t.isAssignmentPattern(path.parent) && ((path.parent as AssignmentPattern).left == path.node))) ||
            // https://babeljs.io/docs/en/babel-types#arraypattern https://babeljs.io/docs/en/babel-types#lval
            (t.isArrayPattern(path.parent))
        )
            return;



        const pro = member.property
        // we need to handle identifier as well
        // if(pro.type != "NumericLiteral" && pro.type != "StringLiteral")
        //     return;

        const l = member.loc.start.line;
        const obj = member.object

        const replaced = t.callExpression(t.memberExpression(t.identifier(ref), t.identifier(InjectedFunctions.squareBrackets.name)),
            [t.stringLiteral(fileName), t.numericLiteral(l), t.numericLiteral(branchCounter), obj,
                !member.computed ? t.stringLiteral(pro.name) : pro]);
        // method replace boolean with true and false are created
        branchCounter++;

        // @ts-ignore
        member.evomaster = true;

        path.replaceWith(replaced);
    }

    function replaceCallExpression(path: NodePath){

        //if(! t.isExpr) //TODO there is no available check for call expressions???

        const call = path.node as CallExpression;

        /*
            TODO: need better, more explicit way to skip traversing
            new nodes we are adding
        */
        if(! call.loc ||
            // @ts-ignore
            call.evomaster
        ){
            return;
        }

        const l = call.loc.start.line;

        let replaced;

        //TODO only for known names

        if(t.isMemberExpression(call.callee)) {
            replaced = t.callExpression(
                t.memberExpression(t.identifier(ref), t.identifier(InjectedFunctions.callTracked.name)),
                [t.stringLiteral(fileName), t.numericLiteral(l), t.numericLiteral(branchCounter),
                    // @ts-ignore
                    call.callee.object,
                    !call.callee.computed ?
                        t.stringLiteral(call.callee.property.name)
                        : call.callee.property,
                    ...call.arguments]
            );
            branchCounter++;
        } else {
            replaced = t.callExpression(
                t.memberExpression(t.identifier(ref), t.identifier(InjectedFunctions.callBase.name)),
                [t.arrowFunctionExpression([], call, doesContainAwaitExpression(call)) ]
            );
            // @ts-ignore
            call.evomaster = true;
        }

        path.replaceWith(replaced);
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
            || t.isContinueStatement(stmt) //FIXME: did i forget break? or was it included here?
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

                    //@ts-ignore
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
            ConditionalExpression:{
                enter(path: NodePath){
                    replaceConditionalExpression(path);
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
            },
            CallExpression:{
                enter(path: NodePath){
                    replaceCallExpression(path);
                }
            },
            /*
                https://babeljs.io/docs/en/babel-types#memberexpression
                just a reminder here:
                https://babeljs.io/docs/en/babel-types#optionalmemberexpression
                there also exists optionalMemberExpression, x?.a
                but since x?["a"] is not allowed, there might be not need to handle it.
             */
            MemberExpression:{
                enter(path: NodePath){
                    replaceMemberExpression(path);
                }
            }
        }
    };
}

