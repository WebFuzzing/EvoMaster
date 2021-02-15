namespace Controller.Api
{
  //TODO: Review possible values for this enum
  /*
    Note: this enum must be kept in sync with what declared in
    org.evomaster.core.output.OutputFormat
 */
  public enum OutputFormat {
        JAVA_JUNIT_5,
        JAVA_JUNIT_4,
        KOTLIN_JUNIT_4,
        KOTLIN_JUNIT_5,
        JS_JEST
    }
}