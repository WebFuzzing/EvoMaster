
/**
 * Information about the "units" in the SUT.
 * In case of OO languages like Java and Kotlin, those will be "classes"
 *
 */
export default class UnitsInfoDto{

    /**
     * Then name of all the units (eg classes) in the SUT
     */
    unitNames = new Set<String>();

    /**
     * The total number of lines/statements/instructions in all
     * units of the whole SUT
     */
    numberOfLines: number;


    /**
     * The total number of branches in all
     * units of the whole SUT
     */
    numberOfBranches: number;

    /**
     * Number of replaced method testability transformations.
     * But only for SUT units.
     */
    numberOfReplacedMethodsInSut: number;

    /**
     * Number of replaced method testability transformations.
     * But only for third-party library units (ie all units not in the SUT).
     */
    numberOfReplacedMethodsInThirdParty: number;


    /**
     * Number of tracked methods. Those are special methods for which
     * we explicitly keep track of how they are called (eg their inputs).
     */
    numberOfTrackedMethods: number;
}


