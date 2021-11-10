using System;
using System.Collections;
using System.Collections.Concurrent;
using System.Collections.Generic;

namespace EvoMaster.Client.Util {
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
            return _dictionary.TryRemove(item ?? throw new ArgumentNullException(nameof(item)), out byte b);
        }

        IEnumerator IEnumerable.GetEnumerator() {
            return _dictionary.Keys.GetEnumerator();
        }
    }
}