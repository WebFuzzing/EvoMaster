using System;

namespace EvoMaster.Instrumentation_Shared.Collections {
    public partial class Deque<T> {
        #region Node Class

        // Represents a node in the deque.
        [Serializable()]
        private class Node {
            private T _value;

            private Node _previous;

            private Node _next;

            public Node(T value) {
                this._value = value;
            }

            public T Value {
                get { return _value; }
            }

            public Node Previous {
                get { return _previous; }
                set { _previous = value; }
            }

            public Node Next {
                get { return _next; }
                set { _next = value; }
            }
        }

        #endregion
    }
}