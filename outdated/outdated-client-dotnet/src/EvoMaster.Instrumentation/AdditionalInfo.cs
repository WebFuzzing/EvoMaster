using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading;
using EvoMaster.Client.Util;
using EvoMaster.Instrumentation.StaticState;
using EvoMaster.Instrumentation_Shared;
using EvoMaster.Instrumentation_Shared.Collections;

namespace EvoMaster.Instrumentation {
    public class AdditionalInfo {
        /**
     * In REST APIs, it can happen that some query parameters do not
     * appear in the schema if they are indirectly accessed via
     * objects like WebRequest.
     * But we can track at runtime when such kind of objects are used
     * to access the query parameters
     */
        private readonly ICollection<string> _queryParameters = new ConcurrentHashSet<string>();


        /**
     * In REST APIs, it can happen that some HTTP headers do not
     * appear in the schema if they are indirectly accessed via
     * objects like WebRequest.
     * But we can track at runtime when such kind of objects are used
     * to access the query parameters
     */
        private readonly ICollection<string> _headers = new ConcurrentHashSet<string>();

        /**
     * Map from taint input name to string specializations for it
     */
        private readonly IDictionary<string, ICollection<StringSpecializationInfo>> _stringSpecializations =
            new ConcurrentDictionary<string, ICollection<StringSpecializationInfo>>();


        /**
     * Keep track of the last executed statement done in the SUT.
     * But not in the third-party libraries, just the business logic of the SUT.
     * The statement is represented with a descriptive unique id, like the class name and line number.
     *
     * We need to use a stack to handle method call invocations, as we can know when a statement
     * starts, but not so easily when it ends.
     * For example:
     * foo(bar(), x.npe)
     * here, if x is null, we would end up wrongly marking the last line in bar() as last-statement,
     * whereas it should be the one for foo()
     *
     * Furthermore, we need a stack per execution thread, based on their name.
     */
        private readonly ConcurrentDictionary<string, Deque<StatementMethod>> _lastExecutedStatementStacks =
            new ConcurrentDictionary<string, Deque<StatementMethod>>();

        /**
     * In case we pop all elements from stack, keep track of last one separately.
     */
        private StatementMethod _noExceptionStatement;


        /**
     * Check if the business logic of the SUT (and not a third-party library) is
     * accessing the raw bytes of HTTP body payload (if any) directly
     */
        private bool _rawAccessOfHttpBodyPayload;


        /**
     * The name of all DTO that have been parsed (eg, with GSON and Jackson).
     * Note: the actual content of schema is queried separately.
     * Reasons: does not change (DTO classes are static), and quite expensive
     * to send at each action evaluation
     */
        private readonly ICollection<string> _parsedDtoNames = new ConcurrentHashSet<string>();

        private string _lastExecutingThread;

        private readonly ICollection<SqlInfo> _sqlInfoData = new ConcurrentHashSet<SqlInfo>();

        public IReadOnlyCollection<SqlInfo> GetSqlInfoData() => (IReadOnlyCollection<SqlInfo>) _sqlInfoData;

        public void AddSqlInfo(SqlInfo info) => _sqlInfoData.Add(info);

        public IReadOnlyCollection<string> GetParsedDtoNamesView() => (IReadOnlyCollection<string>) _parsedDtoNames;

        public void AddParsedDtoName(string name) => _parsedDtoNames.Add(name);

        public bool IsRawAccessOfHttpBodyPayload() => _rawAccessOfHttpBodyPayload;

        public void SetRawAccessOfHttpBodyPayload(bool rawAccessOfHttpBodyPayload) =>
            _rawAccessOfHttpBodyPayload = rawAccessOfHttpBodyPayload;

        public void AddSpecialization(string taintInputName, StringSpecializationInfo info) {
            if (!ExecutionTracer.GetTaintType(taintInputName).IsTainted()) {
                throw new ArgumentException("No valid input name: " + taintInputName);
            }

            if (info.Equals(null)) throw new NullReferenceException();

            _stringSpecializations.TryAdd(taintInputName, new ConcurrentHashSet<StringSpecializationInfo>());
            var set = _stringSpecializations[taintInputName];
            set.Add(info);
        }

        public IReadOnlyDictionary<string, ICollection<StringSpecializationInfo>> GetStringSpecializationsView() {
            //TODO: check if this does not prevent modifying the sets inside it
            return (IReadOnlyDictionary<string, ICollection<StringSpecializationInfo>>) _stringSpecializations;
        }

        public void AddQueryParameter(string param) {
            if (!string.IsNullOrEmpty(param)) {
                _queryParameters.Add(param);
            }
        }

        public IReadOnlyCollection<string> GetQueryParametersView() {
            return (IReadOnlyCollection<string>) _queryParameters;
        }

        public void AddHeader(string header) {
            if (!string.IsNullOrEmpty(header)) {
                _headers.Add(header);
            }
        }

        public IReadOnlyCollection<string> GetHeadersView() {
            return (IReadOnlyCollection<string>) _headers;
        }

        public string GetLastExecutedStatement() {
//        if(lastExecutedStatementStacks.values().stream().allMatch(s -> s.isEmpty())){
            /*
                TODO: not super-sure about this... we could have several threads in theory, but hard to
                really say if the last one executing a statement of the SUT is always the one we are really
                interested into... would need to check if there are cases in which this is not the case
             */

            Deque<StatementMethod> stack = null;
            if (_lastExecutingThread != null) {
                stack = _lastExecutedStatementStacks[_lastExecutingThread];
            }

            if (_lastExecutingThread == null || stack == null || stack.IsEmpty()) {
                return _noExceptionStatement?.StatementId;
            }

            var peeked = stack.PeekFront();
            return peeked != null ? stack.PeekFront().StatementId : null;
        }

        public void PushLastExecutedStatement(string statementId, string lastMethod) {
            var key = GetThreadIdentifier();
            _lastExecutingThread = key;
            _lastExecutedStatementStacks.TryAdd(key,
                new Deque<StatementMethod>()); //TODO: check TryAdd(putIfAbsent) and Deque(ArrayDeque_
            var stack = _lastExecutedStatementStacks[key];

            _noExceptionStatement = null;

            StatementMethod current;

            try {
                current = stack.PeekFront();
            }
            catch (InvalidOperationException e) {
                current = null;
            }

            //if some method, then replace top of stack
            if (current != null && lastMethod.Equals(current.Method)) {
                stack.PopFront();
            }

            stack.PushFront(new StatementMethod {Method = lastMethod, StatementId = statementId});
        }

        private string GetThreadIdentifier() => Thread.CurrentThread.ManagedThreadId.ToString();

        public void PopLastExecutedStatement() {
            var key = GetThreadIdentifier();

            var stack = _lastExecutedStatementStacks[key];

            if (stack == null || stack.IsEmpty()) {
                //throw new IllegalStateException("[ERROR] EvoMaster: invalid stack pop on thread " + key);
                SimpleLogger.Warn("EvoMaster instrumentation was left in an inconsistent state." +
                                  " This could happen if you have threads executing business logic in your instrumented" +
                                  " classes after an action is completed (e.g., an HTTP call)." +
                                  " This is not a problem, as long as this warning appears only seldom in the logs.");
                /*
                    This problem should not really happen in SpringBoot applications, but for example
                    it does happen in LanguageTool, as it handles the HTTP connections manually in
                    the business logic
                 */
                return;
            }

            var statementDescription = stack.PopFront();

            if (stack.IsEmpty()) {
                _noExceptionStatement = statementDescription;
            }
        }
    }

    internal class StatementMethod {
        public string StatementId { get; set; }
        public string Method { get; set; }
    }
}