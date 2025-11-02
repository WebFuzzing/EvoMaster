using System.Collections.Generic;
using EvoMaster.Instrumentation.Examples.Objects;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Examples.Objects {
    [Collection("Sequential")]
    public class ObjectOperationsTests {
        private readonly ObjectOperations _objectOperations = new ObjectOperations();

        [Fact]
        public void EqualsTest_EqualValues() {
            var a = new Student {Name = "Zagros", Age = 22};
            var b = new Student {Name = "Zagros", Age = 22};

            var res = _objectOperations.CheckEqual(a, b);
            Assert.True(res);
        }

        [Fact]
        public void EqualsTest_UnequalValues() {
            var a = new Student {Name = "Bahoz", Age = 22};
            var b = new Student {Name = "Pervin", Age = 19};

            var res = _objectOperations.CheckEqual(a, b);
            Assert.False(res);
        }

        [Fact]
        public void CheckEqualStringWithObjectTest() {
            string str = "abc";
            object obj = "abc";

            var res = _objectOperations.CheckEqualStringWithObject(str, obj);
            
            Assert.True(res);
        }
    }
}