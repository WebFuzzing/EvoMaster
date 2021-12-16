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
            private Deque<T> deque;

            // The object to lock on.
            private object root;

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

                this.deque = deque;
                this.root = deque.SyncRoot;
            }

            #endregion

            #region Methods

            public override void Clear()
            {
                lock(root)
                {
                    deque.Clear();
                }
            }

            public override bool Contains(T item)
            {
                lock(root)
                {
                    return deque.Contains(item);
                }
            }

            public override void PushFront(T item)
            {
                lock(root)
                {
                    deque.PushFront(item);
                }
            }

            public override void PushBack(T item)
            {
                lock(root)
                {
                    deque.PushBack(item);
                }
            }

            public override T PopFront()
            {
                lock(root)
                {
                    return deque.PopFront();
                }
            }

            public override T PopBack()
            {
                lock(root)
                {
                    return deque.PopBack();
                }
            }

            public override T PeekFront()
            {
                lock(root)
                {
                    return deque.PeekFront();
                }
            }

            public override T PeekBack()
            {
                lock(root)
                {
                    return deque.PeekBack();
                }
            }

            public override T[] ToArray()
            {
                lock(root)
                {
                    return deque.ToArray();
                }
            }

            public override object Clone()
            {
                lock(root)
                {
                    return deque.Clone();
                }
            }

            public override void CopyTo(Array array, int index)
            {
                lock(root)
                {
                    deque.CopyTo(array, index);
                }
            }

            public override IEnumerator<T> GetEnumerator()
            {
                lock(root)
                {
                    return deque.GetEnumerator();
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
                lock(root)
                {
                    return ((IEnumerable)deque).GetEnumerator();
                }
            }

            #endregion

            #region Properties

            public override int Count
            {
                get
                {
                    lock(root)
                    {
                        return deque.Count;
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