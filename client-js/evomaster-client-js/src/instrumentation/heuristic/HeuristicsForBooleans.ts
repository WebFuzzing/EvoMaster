import Truthness from "./Truthness";
import TruthnessUtils from "./TruthnessUtils";
import ExecutionTracer from "../staticstate/ExecutionTracer";


export default class HeuristicsForBooleans {

    private static readonly validOps = ["==", "===", "!=", "!==", "<", "<=", ">", ">="];

    private static lastEvaluation: Truthness = null;

    private static readonly FLAG_NO_EXCEPTION = 0.01;
    private static readonly EXCEPTION = HeuristicsForBooleans.FLAG_NO_EXCEPTION / 2;


    public static handleNot(value: any) : any{

        if(HeuristicsForBooleans.lastEvaluation){
            HeuristicsForBooleans.lastEvaluation = HeuristicsForBooleans.lastEvaluation.invert();
        }

        return !value;
    }


    public static evaluateAnd(left: () => any, right: () => any, isRightPure: boolean, fileName: string, line: number, branchId: number): any {

        // (x && y) == ! (!x || !y)

        const base = HeuristicsForBooleans.FLAG_NO_EXCEPTION;

        const negatedLeft = () => {

            //not important that throws exception, it ll be handled later
            const x = left();

            let xT = HeuristicsForBooleans.lastEvaluation;
            if (!xT) {
                xT = new Truthness(!x ? 1 : base, !x ? base : 1);
            } else {
                xT = xT.invert();
            }
            HeuristicsForBooleans.lastEvaluation = xT;
        };

        const negatedRight = () => {

            //not important that throws exception, it ll be handled later
            const x = right();

            let xT = HeuristicsForBooleans.lastEvaluation;
            if (!xT) {
                xT = new Truthness(!x ? 1 : base, !x ? base : 1);
            } else {
                xT = xT.invert();
            }
            HeuristicsForBooleans.lastEvaluation = xT;
        };


        return HeuristicsForBooleans.evaluateOr(negatedLeft, negatedRight, isRightPure, true, fileName, line, branchId);
    }


    public static evaluateOr(left: () => any,
                             right: () => any,
                             isRightPure: boolean,
                             isNegated: boolean,
                             fileName: string,
                             line: number,
                             branchId: number): any {

        HeuristicsForBooleans.lastEvaluation = null;

        const base = HeuristicsForBooleans.FLAG_NO_EXCEPTION;
        const exception = HeuristicsForBooleans.EXCEPTION;

        let x: any;
        let xT: Truthness;
        let xE: any = null;

        try {
            x = left();
            xT = HeuristicsForBooleans.lastEvaluation;
            if (!xT) {
                xT = new Truthness(x ? 1 : base, x ? base : 1);
            } else {
                xT = xT.rescaleFromMin(base);
            }

        } catch (e) {
            xT = new Truthness(exception, exception);
            xE = e;
        }

        const leftIsFalse = (!x && xE === null);

        let h: Truthness;
        let yE: any = null;
        let y: any;

        /*
            Two cases in which we can evaluate the right:
            - it is pure
            - left was false, and without exception
         */
        if (leftIsFalse || isRightPure) {

            HeuristicsForBooleans.lastEvaluation = null;

            let yT: Truthness;

            try {
                y = right();
                yT = HeuristicsForBooleans.lastEvaluation;
                if (!yT) {
                    yT = new Truthness(y ? 1 : base, y ? base : 1);
                } else {
                    yT = yT.rescaleFromMin(base);
                }

            } catch (e) {
                yT = new Truthness(exception, exception);
                yE = e;
            }

            h = new Truthness(
                Math.max(xT.getOfTrue(), yT.getOfTrue()),
                (xT.getOfFalse() / 2) + (yT.getOfFalse() / 2)
            );
        } else {

            // this means either the left threw an exception, or it was true and right is not pure
            h = new Truthness(
                xT.getOfTrue(),
                xT.getOfFalse() / 2
            );
        }

        ExecutionTracer.updateBranch(fileName, line, branchId, h);

        HeuristicsForBooleans.lastEvaluation = h;

        if(xE){
            throw xE;
        }

        if(leftIsFalse && yE){
            throw yE;
        }

        if(x){
            return x;
        }

        return y;
    }


    public static evaluate(left: any, op: string, right: any, fileName: string, line: number, branchId: number): any {

        /*
            Make sure we get exactly the same result
         */
        let res: any;
        if (op === "==") {
            res = left == right;
        } else if (op === "===") {
            res = left === right;
        } else if (op === "!=") {
            res = left != right;
        } else if (op === "!==") {
            res = left !== right;
        } else if (op === "<") {
            res = left < right;
        } else if (op === "<=") {
            res = left <= right;
        } else if (op === ">") {
            res = left > right;
        } else if (op === ">=") {
            res = left >= right;
        }

        const h = HeuristicsForBooleans.compare(left, op, right);

        ExecutionTracer.updateBranch(fileName, line, branchId, h);

        HeuristicsForBooleans.lastEvaluation = h;

        return res;
    }


    public static compare(left: any, op: string, right: any): Truthness {

        if (!HeuristicsForBooleans.validOps.includes(op)) {
            throw new Error("Invalid op: " + op);
        }

        const tl = typeof left;
        const tr = typeof right;

        const isLeftNumber = tl === "number";
        const isLeftString = tl === "string";

        const isRightNumber = tr === "number";
        const isRightString = tr === "string";

        const bothNumbers = isLeftNumber && isRightNumber;
        const bothStrings = isLeftString && isRightString;

        const aNumberAndString = (isLeftString && isRightNumber) || (isLeftNumber && isRightString);

        let h: Truthness;

        if (op === "===") {
            if (isLeftNumber && isRightNumber) {
                h = TruthnessUtils.getEqualityTruthnessNumber(left, right);
            } else if (isLeftString && isRightString) {
                h = TruthnessUtils.getEqualityTruthnessString(left, right);
            } else {
                const b = left === right;
                h = new Truthness(b ? 1 : 0, b ? 0 : 1);
            }
        } else if (op === "!==") {
            h = HeuristicsForBooleans.compare(left, "===", right).invert();
        } else if (op === "==") {
            if (bothNumbers || bothStrings) {
                h = HeuristicsForBooleans.compare(left, "===", right);
            } else if (aNumberAndString) {
                h = HeuristicsForBooleans.compare("" + left, "===", "" + right);
            } else {
                const b = left == right;
                h = new Truthness(b ? 1 : 0, b ? 0 : 1);
            }
        } else if (op === "!=") {
            h = HeuristicsForBooleans.compare(left, "==", right).invert();
        } else if (op === "<") {
            if (bothNumbers) {
                h = TruthnessUtils.getLessThanTruthnessNumber(left, right);
            } else if (bothStrings || aNumberAndString) {
                h = TruthnessUtils.getLessThanTruthnessString("" + left, "" + right);
            } else {
                const b = left < right;
                h = new Truthness(b ? 1 : 0, b ? 0 : 1);
            }
        } else if (op === ">=") {
            h = HeuristicsForBooleans.compare(left, "<", right).invert();
        } else if (op === "<=") {
            // (l <= r)  same as  (r >= l)  same as  !(r < l)
            h = HeuristicsForBooleans.compare(right, "<", left).invert();
        } else if (op === ">") {
            h = HeuristicsForBooleans.compare(left, "<=", right).invert();
        }

        return h;
    }

}