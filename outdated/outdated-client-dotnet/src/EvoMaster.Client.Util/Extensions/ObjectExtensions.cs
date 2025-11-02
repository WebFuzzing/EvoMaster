using System;

namespace EvoMaster.Client.Util.Extensions {
    public static class ObjectExtensions {
        public static T RequireNonNull<T>(this object obj) {
            if (obj == null)
                throw new NullReferenceException();

            return (T) obj;
        }
    }
}