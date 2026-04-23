import StringSpecializationInfoDto from "./StringSpecializationInfoDto";

export default class AdditionalInfoDto {

    /**
     * In REST APIs, it can happen that some query parameters do not
     * appear in the schema if they are indirectly accessed via
     * objects like WebRequest.
     * But we can track at runtime when such kind of objects are used
     * to access the query parameters
     */
    public queryParameters: Array<string>  = new Array<string>();

    /**
     * In REST APIs, it can happen that some HTTP headers do not
     * appear in the schema if they are indirectly accessed via
     * objects like WebRequest.
     * But we can track at runtime when such kind of objects are used
     * to access the query parameters
     */
    public headers: Array<string> = new Array<string>();

    /**
     * Information for taint analysis.
     * When some string inputs are recognized of a specific type (eg,
     * they are used as integers or dates), we keep track of it.
     * The key in this map is the value of the tainted input.
     * The associated list is its possible specializations (which usually
     * will be at most 1).
     */
     //public stringSpecializations = new Map<string, StringSpecializationInfoDto[]>();
     /*
        Note: JSON marshallers in JS/TS are pretty crappy, as they SILENTLY ignore string-key maps...
      */
    public stringSpecializations  = new Object();

    /**
     * Keep track of the last executed statement done in the SUT.
     * But not in the third-party libraries, just the business logic of the SUT.
     * The statement is represented with a descriptive unique id, like the class name and line number.
     */
     public lastExecutedStatement: string;
}
