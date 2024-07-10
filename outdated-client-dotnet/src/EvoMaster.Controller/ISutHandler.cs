using System.Collections.Generic;
using EvoMaster.Controller.Api;

namespace EvoMaster.Controller {
    public interface ISutHandler {
        /**
     * There might be different settings based on when the SUT is run during the
     * search of EvoMaster, and when it is later started in the generated tests.
     */
        void SetupForGeneratedTest() { }

        ///<summary>Start a new instance of the SUT. 
        ///This method must be blocking until the SUT is initialized.
        ///How this method is implemented depends on the library/framework in which the application is written.
        ///</summary>
        ///<returns>Returns the base url of the SUT</returns>
        ///This method in java client is not async
        string StartSut();

        ///<summary>
        ///Stops the SUT
        ///How this method is implemented depends on the library/framework in which the application is written.
        ///This method in java client doesn't take the process => void StopSut();
        ///</summary>
        void StopSut();

        //TODO: edit comment
        ///<summary>
        ///Make sure the SUT is in a clean state (eg, reset data in database).
        ///</summary>
        ///<remarks>
        ///A possible (likely very inefficient) way to implement this would be to
        /// call <code>stopSut</code> followed by <code>startSut</code>.
        ///When dealing with databases, you can look at the utility functions from
        ///the class <code>DbCleaner</code>.
        ///How to access the database depends on the application.
        ///To access a <code>java.sql.Connection</code>, in Spring applications you can use something like:
        ///<code>ctx.getBean(JdbcTemplate.class).getDataSource().getConnection()</code>
        ///</remarks>
        void ResetStateOfSut();

        ///<summary>Execute the given data insertions into the database (if any)</summary>
        ///<param name="insertions">DTOs for each insertion to execute</param>
        void ExecInsertionsIntoDatabase(IList<InsertionDto> insertions);
    }
}