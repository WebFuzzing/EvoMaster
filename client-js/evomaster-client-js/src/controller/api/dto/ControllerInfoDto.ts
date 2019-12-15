export default class ControllerInfoDto {

    /**
     * The full qualifying name of the controller.
     * This will be needed when tests are generated, as those
     * will instantiate and start the controller directly
     */
    public fullName: string;

    /**
     * Whether the system under test is running with instrumentation
     * to collect data about its execution
     */
    public isInstrumentationOn: boolean;
}
