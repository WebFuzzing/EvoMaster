using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using EvoMaster.Client.Util.Extensions;

namespace EvoMaster.Instrumentation_Shared {
    public class ClassName {
        /*
        Name used in the bytecode, where "/" are used
        instead of "."
        eg
        org.bar.Foo turns into org/bar/Foo
        */
        private readonly string _bytecodeName;

        /*
        What usually returned with Foo.class.getName(),
        eg
        org.bar.Foo
        */
        private readonly string _fullNameWithDots;


        private static readonly IDictionary<Type, ClassName>
            CacheClass = new ConcurrentDictionary<Type, ClassName>(); //TODO: capacity 10_000

        private static readonly IDictionary<string, ClassName> Cachestring =
            new ConcurrentDictionary<string, ClassName>(); //TODO: capacity 10_000

        public static ClassName Get(Type klass) {
            return CacheClass.ComputeIfAbsent(klass, k => new ClassName(k));
            //return new ClassName(klass);
        }

        public static ClassName Get(string name) {
            return Cachestring.ComputeIfAbsent(name, n => new ClassName(n));
            //return new ClassName(name);
        }

        public ClassName(Type klass) : this(klass.RequireNonNull<Type>().Name) { }

        /// <param name="name">name of the class, or path resource</param>
        public ClassName(string name) {
            name.RequireNonNull<string>();

            if (name.EndsWith(".class")) {
                name = name.Substring(0, name.Length - ".class".Length);
            }

            if (name.EndsWith(".cs")) {
                name = name.Substring(0, name.Length - ".cs".Length);
            }

            if (name.Contains("/") && name.Contains(".")) {
                throw new ArgumentException("Do not know how to handle name: " + name);
            }

            if (name.Contains("/")) {
                _bytecodeName = name;
                _fullNameWithDots = name.Replace("/", ".");
            }
            else {
                _bytecodeName = name.Replace(".", "/");
                _fullNameWithDots = name;
            }
        }

        ///<summary>Name of the class as used in the bytecode instructions.
        /// This means that foo.bar.Hello would be foo/bar/Hello</summary>
        public string GetBytecodeName() {
            return _bytecodeName;
        }

        /// <summary>Eg, foo.bar.Hello</summary>
        public string GetFullNameWithDots() {
            return _fullNameWithDots;
        }

        public string GetAsResourcePath() {
            return _bytecodeName + ".class";
        }


        public override string ToString() {
            return "[" + this.GetType().Name + ": " + _fullNameWithDots + "]";
        }
    }
}