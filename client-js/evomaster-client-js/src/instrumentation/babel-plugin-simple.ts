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

export default function simple(
    babel: Babel,
): {visitor: Visitor<PluginOptions>} {

    const t = babel.types;

    return {
        visitor: {
            Program: {
                enter(path) {
                    console.log("Before");
                    t.addComment(path.node, "leading", "BEFORE", true);
                },
                exit(path: NodePath) {
                    console.log("After");
                    t.addComment(path.node, "trailing", "AFTER", true);
                }
            }
        }
    };
}
