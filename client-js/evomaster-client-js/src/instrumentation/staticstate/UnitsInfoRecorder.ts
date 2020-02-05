/**
 * Keep track of static info on the SUT related to its classes,
 * eg total number of lines
 *
 */
export default class UnitsInfoRecorder {

    //see entries in UnitsInfoDto

    private static readonly unitNames: Set<string> = new Set<string>();
    private static numberOfLines: number = 0;
    private static numberOfBranches: number = 0;
    // private static readonly numberOfReplacedMethodsInSut: number;
    // private static readonly numberOfReplacedMethodsInThirdParty: number;
    // private static readonly numberOfTrackedMethods: number;


    /**
     * Only need for tests
     */
    public static reset() {
        UnitsInfoRecorder.unitNames.clear();
        UnitsInfoRecorder.numberOfLines = 0;
        UnitsInfoRecorder.numberOfBranches = 0;
    }


    public static markNewUnit(name: string) {
        UnitsInfoRecorder.unitNames.add(name);
    }

    public static markNewLine() {
        UnitsInfoRecorder.numberOfLines++;
    }

    public static markNewBranch() {
        UnitsInfoRecorder.numberOfBranches += 1;
    }

// public static void markNewReplacedMethodInSut(){
//     singleton.numberOfReplacedMethodsInSut.incrementAndGet();
// }
//
// public static void markNewReplacedMethodInThirdParty(){
//     singleton.numberOfReplacedMethodsInThirdParty.incrementAndGet();
// }
//
// public static void markNewTrackedMethod(){
//     singleton.numberOfTrackedMethods.incrementAndGet();
// }


    public static getNumberOfUnits(): number {
        return UnitsInfoRecorder.unitNames.size;
    }

    public static getUnitNames(): Set<string> {
        return new Set(UnitsInfoRecorder.unitNames);
    }

    public static getNumberOfLines(): number {
        return UnitsInfoRecorder.numberOfLines;
    }

    public static getNumberOfBranches(): number {
        return UnitsInfoRecorder.numberOfBranches;
    }

// public  int getNumberOfReplacedMethodsInSut() {
//     return numberOfReplacedMethodsInSut.get();
// }
//
// public  int getNumberOfReplacedMethodsInThirdParty() {
//     return numberOfReplacedMethodsInThirdParty.get();
// }
//
// public  int getNumberOfTrackedMethods() {
//     return numberOfTrackedMethods.get();
// }
}
