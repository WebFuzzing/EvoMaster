using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Threading.Tasks;
using Controller.Api;

namespace Controller {
  public interface ISutHandler {

    /**
     * There might be different settings based on when the SUT is run during the
     * search of EvoMaster, and when it is later started in the generated tests.
     */
    void SetupForGeneratedTest () { }

    ///<summary>Start a new instance of the SUT. 
    ///This method must be blocking until the SUT is initialized.
    ///How this method is implemented depends on the library/framework in which the application is written.
    ///</summary>
    ///<returns>Returns the process to stop later</returns>
    ///This method in java client is neither async, nor returning Process => String StartSut();
    Task<Process> StartSutAsync ();

    ///<summary>
    ///Stops the SUT by killing the process
    ///How this method is implemented depends on the library/framework in which the application is written.
    ///This method in java client doesn't take the process => void StopSut();
    ///</summary>
    void StopSut (Process process);

    /**
     * <p>
     * Make sure the SUT is in a clean state (eg, reset data in database).
     * </p>
     *
     * <p>
     * A possible (likely very inefficient) way to implement this would be to
     * call {@code stopSUT} followed by {@code startSUT}.
     * </p>
     *
     * <p>
     * When dealing with databases, you can look at the utility functions from
     * the class {@link DbCleaner}.
     * How to access the database depends on the application.
     * To access a {@code java.sql.Connection}, in Spring applications you can use something like:
     * {@code ctx.getBean(JdbcTemplate.class).getDataSource().getConnection()}.
     * </p>
     */
    void ResetStateOfSut ();

    /**
     * Execute the given data insertions into the database (if any)
     *
     * @param insertions DTOs for each insertion to execute
     */
    void ExecInsertionsIntoDatabase (IList<InsertionDto> insertions);
  }

}