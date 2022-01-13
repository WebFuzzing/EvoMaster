using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Threading;
using EvoMaster.Client.Util;
using EvoMaster.Instrumentation_Shared;

namespace EvoMaster.Instrumentation.StaticState{
    [Serializable]
    public class UnitsInfoRecorder{
        private static UnitsInfoRecorder _singleton;

        static UnitsInfoRecorder(){
            _singleton = new UnitsInfoRecorder();
        }

        //see entries in UnitsInfoDto

        private ConcurrentHashSet<string> _unitNames = new ConcurrentHashSet<string>();
        private int _numberOfLines;
        private int _numberOfBranches;
        private int _numberOfReplacedMethodsInSut;
        private int _numberOfReplacedMethodsInThirdParty;
        private int _numberOfTrackedMethods;
        private int _numberOfInstrumentedNumberComparisons;

        //TODO should consider if also adding info on type, eg JSON vs XML
        //Key -> DTO full name, Value -> OpenAPI object schema
        private ConcurrentDictionary<string, string> _parsedDtos = new ConcurrentDictionary<string, string>();

        private ConcurrentHashSet<string> _alreadyReachedLines = new ConcurrentHashSet<string>();
        

        //Only needed for tests
        public static void Reset(){
            _singleton = new UnitsInfoRecorder();
        }

        public static UnitsInfoRecorder GetInstance(){
            return _singleton;
        }

        public static void MarkNewUnit(string name){
            _singleton._unitNames.Add(name);
        }

        public static void MarkNewLine(){
            // if (_singleton._alreadyReachedLines.Contains(line)) return;
            //
            // _singleton._alreadyReachedLines.Add(line);

            Interlocked.Increment(ref _singleton._numberOfLines);
        }

        public static void MarkNewBranch(){
            // Interlocked.Add(ref _singleton._numberOfBranches, 2);
            Interlocked.Increment(ref _singleton._numberOfBranches);
        }

        public static void MarkNewReplacedMethodInSut(){
            Interlocked.Increment(ref _singleton._numberOfReplacedMethodsInSut);
        }

        public static void MarkNewReplacedMethodInThirdParty(){
            Interlocked.Increment(ref _singleton._numberOfReplacedMethodsInThirdParty);
        }

        public static void MarkNewTrackedMethod(){
            Interlocked.Increment(ref _singleton._numberOfTrackedMethods);
        }

        public static void MarkNewInstrumentedNumberComparison(){
            Interlocked.Increment(ref _singleton._numberOfInstrumentedNumberComparisons);
        }

        public static void RegisterNewParsedDto(string name, string schema){
            if (string.IsNullOrEmpty(name)){
                throw new ArgumentException("Empty dto name");
            }

            if (string.IsNullOrEmpty(schema)){
                throw new ArgumentException("Empty schema");
            }

            _singleton._parsedDtos.TryAdd(name, schema);
        }

        public int GetNumberOfUnits(){
            return _unitNames.Count;
        }

        public ICollection<string> GetUnitNames(){
            return _unitNames;
        }

        public IDictionary<string, string> GetParsedDtos(){
            return _parsedDtos;
        }

        public int GetNumberOfLines(){
            return _numberOfLines;
        }

        public int GetNumberOfBranches(){
            return _numberOfBranches;
        }

        public int GetNumberOfReplacedMethodsInSut(){
            return _numberOfReplacedMethodsInSut;
        }

        public int GetNumberOfReplacedMethodsInThirdParty(){
            return _numberOfReplacedMethodsInThirdParty;
        }

        public int GetNumberOfTrackedMethods(){
            return _numberOfTrackedMethods;
        }

        public int GetNumberOfInstrumentedNumberComparisons(){
            return _numberOfInstrumentedNumberComparisons;
        }
    }
}