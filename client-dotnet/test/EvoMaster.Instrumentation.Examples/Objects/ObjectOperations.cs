using System;
using System.Collections.Generic;

namespace EvoMaster.Instrumentation.Examples.Objects {
    public class ObjectOperations {
        public bool CheckEqual(Student student1, Student student2) {
            return student1.Equals(student2);
        }
        
        public bool CheckEqualStringWithObject(string str, object obj) {
            return str.Equals(obj);
        }
        
        public bool CheckStudentListEquality(List<Student> list1, List<Student> list2) {
            return list1.Equals(list2);
        }
    }

    public class Student {
        public string Name { set; get; }
        public int Age { set; get; }

        public override bool Equals(object? obj) {
            if (obj == null) {
                return false;
            }

            if (!(obj is Student)) {
                return false;
            }

            return (this.Name == ((Student) obj).Name)
                   && (this.Age == ((Student) obj).Age);
        }

        public override int GetHashCode() {
            return HashCode.Combine(Name, Age);
        }
    }
}