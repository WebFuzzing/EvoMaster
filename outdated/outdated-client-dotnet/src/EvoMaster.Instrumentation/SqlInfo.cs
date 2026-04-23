using System;

namespace EvoMaster.Instrumentation {
    /**
     * Info related to SQL command execution.
     *
     * This class is IMMUTABLE
    */
    [Serializable]
    public class SqlInfo {
        [NonSerialized] public static readonly long FAILURE_EXTIME = -1L;

        //The actual SQL string with the command that was executed
        private readonly string _command;

        /*
        Whether the command led to any result:
        eg if there was any data returned in SELECT or any rows
        modified in a UPDATE or data added with INSERT.
        Failure here usually/often means that the predicates on WHERE and
        ON clauses were not satisfied
        */
        private readonly bool _noResult;

        //Whether the SQL command failed, for any reason
        private readonly bool _exception;

        private readonly long _executionTime;

        public SqlInfo(string command, bool noResult, bool exception) : this(command, noResult, exception,
            FAILURE_EXTIME) {
        }

        public SqlInfo(string command, bool noResult, bool exception, long executionTime) {
            _command = command;
            _noResult = noResult;
            _exception = exception;
            _executionTime = executionTime;
        }

        public string GetCommand() {
            return _command;
        }

        public bool IsNoResult() {
            return _noResult;
        }

        public bool IsException() {
            return _exception;
        }

        public override bool Equals(object o) {
            if (this == o) return true;
            if (o == null || GetType() != o.GetType()) return false;
            var sqlInfo = (SqlInfo) o;
            return _noResult == sqlInfo._noResult && _exception == sqlInfo._exception &&
                   Equals(_command, sqlInfo._command);
        }

        public override int GetHashCode() {
            return HashCode.Combine(_command, _noResult, _exception);
        }

        public long GetExecutionTime() {
            return _executionTime;
        }
    }
}