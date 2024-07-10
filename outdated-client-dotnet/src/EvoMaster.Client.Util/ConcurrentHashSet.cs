using System;
using System.Collections;
using System.Collections.Concurrent;
using System.Collections.Generic;

namespace EvoMaster.Client.Util {
    //This class has been added because there's no pre-defined data structure for concurrent hashsets in .net (at the time of writing this)
    //But luckily .net contains ConcurrentDictionary and ConcurrentHashSet is nothing but a wrapper around ConcurrentDictionary
    //The inserted value is of type byte which is always set to zero
    public class ConcurrentHashSet<T> : ICollection<T>, IReadOnlyCollection<T> {
        private readonly ConcurrentDictionary<T, byte> _dictionary = new ConcurrentDictionary<T, byte>();
        public int Count => _dictionary.Keys.Count;
        public bool IsReadOnly => false;

        public void Add(T item) {
            _dictionary.TryAdd(item ?? throw new ArgumentNullException(nameof(item)), 0);
        }

        public void Clear() {
            _dictionary.Clear();
        }

        public bool Contains(T item) {
            return _dictionary.ContainsKey(item ?? throw new ArgumentNullException(nameof(item)));
        }

        public void CopyTo(T[] array, int arrayIndex) {
            _dictionary.Keys.CopyTo(array, arrayIndex);
        }

        public IEnumerator<T> GetEnumerator() {
            return _dictionary.Keys.GetEnumerator();
        }

        public bool Remove(T item) {
            return _dictionary.TryRemove(item ?? throw new ArgumentNullException(nameof(item)), out var b);
        }

        IEnumerator IEnumerable.GetEnumerator() {
            return _dictionary.Keys.GetEnumerator();
        }
    }
}