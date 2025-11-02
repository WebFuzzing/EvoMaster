using System;
using System.Collections;
using System.Collections.Generic;

namespace EvoMaster.Instrumentation_Shared.Collections
{
public partial class Deque<T>
	{
        #region SynchronizedDeque Class

        // Implements a synchronization wrapper around a deque.
        [Serializable()]
        private class SynchronizedDeque : Deque<T>, IEnumerable
        {
            #region SynchronziedDeque Members

            #region Fields

            // The wrapped deque.
            private Deque<T> _deque;

            // The object to lock on.
            private object _root;

            #endregion

            #region Construction

            public SynchronizedDeque(Deque<T> deque)
            {
                #region Require

                if(deque == null)
                {
                    throw new ArgumentNullException("deque");
                }

                #endregion

                this._deque = deque;
                this._root = deque.SyncRoot;
            }

            #endregion

            #region Methods

            public override void Clear()
            {
                lock(_root)
                {
                    _deque.Clear();
                }
            }

            public override bool Contains(T item)
            {
                lock(_root)
                {
                    return _deque.Contains(item);
                }
            }

            public override void PushFront(T item)
            {
                lock(_root)
                {
                    _deque.PushFront(item);
                }
            }

            public override void PushBack(T item)
            {
                lock(_root)
                {
                    _deque.PushBack(item);
                }
            }

            public override T PopFront()
            {
                lock(_root)
                {
                    return _deque.PopFront();
                }
            }

            public override T PopBack()
            {
                lock(_root)
                {
                    return _deque.PopBack();
                }
            }

            public override T PeekFront()
            {
                lock(_root)
                {
                    return _deque.PeekFront();
                }
            }

            public override T PeekBack()
            {
                lock(_root)
                {
                    return _deque.PeekBack();
                }
            }

            public override T[] ToArray()
            {
                lock(_root)
                {
                    return _deque.ToArray();
                }
            }

            public override object Clone()
            {
                lock(_root)
                {
                    return _deque.Clone();
                }
            }

            public override void CopyTo(Array array, int index)
            {
                lock(_root)
                {
                    _deque.CopyTo(array, index);
                }
            }

            public override IEnumerator<T> GetEnumerator()
            {
                lock(_root)
                {
                    return _deque.GetEnumerator();
                }
            }

            /// <summary>
            /// Returns an enumerator that can iterate through the Deque.
            /// </summary>
            /// <returns>
            /// An IEnumerator for the Deque.
            /// </returns>
            IEnumerator IEnumerable.GetEnumerator()
            {
                lock(_root)
                {
                    return ((IEnumerable)_deque).GetEnumerator();
                }
            }

            #endregion

            #region Properties

            public override int Count
            {
                get
                {
                    lock(_root)
                    {
                        return _deque.Count;
                    }
                }
            }

            public override bool IsSynchronized
            {
                get
                {
                    return true;
                }
            }

            #endregion

            #endregion
        }

        #endregion	
	}
}