using System;
using System.Collections.Generic;

namespace EvoMaster.Client.Util.Extensions
{
    public static class DictionaryExtensions
    {
        //Intended to do as computeIfAbsent does in Java
        public static TVal ComputeIfAbsent<TKey, TVal>(this IDictionary<TKey, TVal> dictionary,TKey key, Func<TKey, TVal> func)
        {
            dictionary.TryGetValue(key, out var val);
            
            if (val != null) return val;
            
            //TODO: check if it is needed to rethrow exceptions here
            val = func(key);

            if (val != null)
            {
                dictionary.Add(key, val);
            }
            
            return val;
        }
    }
}