const ET = require("evomaster-client-js").internal.ExecutionTracer;
const ON = require("evomaster-client-js").internal.ObjectiveNaming;

class ExampleUtils {

    static checkIncreasingTillCoveredForSingleMethodReplacement(inputs, solution, lambda) {

        ET.reset();
        expect(ET.getNumberOfObjectives()).toBe(0);

        let heuristics = -1;
        let target = null;

        for (let k of inputs) {
            lambda(k);

            const missing = ET.getNonCoveredObjectives(ON.METHOD_REPLACEMENT);
            target = Array.from(missing)[0];
            expect(missing.size).toBe(1);

            const h = ET.getValue(target);
            expect(h >= 0).toBe(true);
            expect(h > heuristics).toBe(true);
            expect(h < 1).toBe(true);
            heuristics = h;
        }

        lambda(solution);

        const missing = ET.getNonCoveredObjectives(ON.METHOD_REPLACEMENT);
        expect(missing.size).toBe(0);
        const covered = ET.getValue(target);
        expect(covered).toBe(1);
    }
}

module.exports = ExampleUtils;