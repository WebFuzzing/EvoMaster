import {NodePath, Visitor} from "@babel/traverse";
import * as BabelTypes from "@babel/types";

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

    return {
        visitor: {
            Program: {
                enter(path) {
                    t.addComment(path.node, "leading", "File instrumented with EvoMaster", true);

                },
                exit(path: NodePath) {
                    //
                }
            }
        }
    };
}
