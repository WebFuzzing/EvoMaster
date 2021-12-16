using System;

namespace EvoMaster.Instrumentation_Shared.Collections
{
    public partial class Deque<T>
    {
        #region Node Class

        // Represents a node in the deque.
        [Serializable()]
        private class Node
        {
            private T value;

            private Node previous;

            private Node next;

            public Node(T value)
            {
                this.value = value;
            }

            public T Value
            {
                get
                {
                    return value;
                }
            }

            public Node Previous
            {
                get
                {
                    return previous;
                }
                set
                {
                    previous = value;
                }
            }

            public Node Next
            {
                get
                {
                    return next;
                }
                set
                {
                    next = value;
                }
            }
        }

        #endregion
    }
}