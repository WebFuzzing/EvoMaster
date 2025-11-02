using System;
using System.Collections.Generic;

namespace EvoMaster.Instrumentation_Shared.Collections {
    public partial class Deque<T> {
        #region Enumerator Class

        [Serializable()]
        private class Enumerator : IEnumerator<T> {
            private Deque<T> _owner;

            private Node _currentNode;

            private T _current;

            private bool _moveResult;

            private long _version;

            // A value indicating whether the enumerator has been disposed.
            private bool _disposed;

            public Enumerator(Deque<T> owner) {
                this._owner = owner;
                _currentNode = owner._front;
                this._version = owner._version;
            }

            #region IEnumerator Members

            public void Reset() {
                #region Require

                if (_disposed) {
                    throw new ObjectDisposedException(this.GetType().Name);
                }
                else if (_version != _owner._version) {
                    throw new InvalidOperationException(
                        "The Deque was modified after the enumerator was created.");
                }

                #endregion

                _currentNode = _owner._front;
                _moveResult = false;
            }

            public object Current {
                get {
                    #region Require

                    if (_disposed) {
                        throw new ObjectDisposedException(this.GetType().Name);
                    }
                    else if (!_moveResult) {
                        throw new InvalidOperationException(
                            "The enumerator is positioned before the first " +
                            "element of the Deque or after the last element.");
                    }

                    #endregion

                    return _current;
                }
            }

            public bool MoveNext() {
                #region Require

                if (_disposed) {
                    throw new ObjectDisposedException(this.GetType().Name);
                }
                else if (_version != _owner._version) {
                    throw new InvalidOperationException(
                        "The Deque was modified after the enumerator was created.");
                }

                #endregion

                if (_currentNode != null) {
                    _current = _currentNode.Value;
                    _currentNode = _currentNode.Next;

                    _moveResult = true;
                }
                else {
                    _moveResult = false;
                }

                return _moveResult;
            }

            #endregion

            #region IEnumerator<T> Members

            T IEnumerator<T>.Current {
                get {
                    #region Require

                    if (_disposed) {
                        throw new ObjectDisposedException(this.GetType().Name);
                    }
                    else if (!_moveResult) {
                        throw new InvalidOperationException(
                            "The enumerator is positioned before the first " +
                            "element of the Deque or after the last element.");
                    }

                    #endregion

                    return _current;
                }
            }

            #endregion

            #region IDisposable Members

            public void Dispose() {
                _disposed = true;
            }

            #endregion
        }

        #endregion
    }
}