using System;
using System.Collections.Generic;

namespace EvoMaster.Client.Util.Extensions {
    public static class DictionaryExtensions {
        //Intended to do as computeIfAbsent does in Java
        public static TVal ComputeIfAbsent<TKey, TVal>(this IDictionary<TKey, TVal> dictionary, TKey key,
            Func<TKey, TVal> func) {
            var has = dictionary.TryGetValue(key, out var val);

            if (has) return val;

            //TODO: check if it is needed to rethrow exceptions here
            val = func(key);

            if (val != null) {
                dictionary.TryAdd(key, val);
            }

            return val;
        }

        public static void TryAddOrUpdate<TKey, TVal>(this IDictionary<TKey, TVal> dictionary, TKey key, TVal val) {
            var addResult = dictionary.TryAdd(key, val);
            if (!addResult) dictionary[key] = val;
        }
    }
}