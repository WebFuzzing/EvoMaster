using System;

namespace EvoMaster.Instrumentation_Shared {
    [Serializable]
    public class StringSpecializationInfo {
        private readonly StringSpecialization _stringSpecialization;

        /**
     * A possible value to provide context to the specialization.
     * For example, if the specialization is a CONSTANT, then the "value" here would
     * the content of the constant
     */
        private readonly string _value;

        private readonly TaintType _type;

        public StringSpecializationInfo(StringSpecialization stringSpecialization, string value, TaintType taintType) {
            if (stringSpecialization.Equals(null)) throw new NullReferenceException();
            _stringSpecialization = stringSpecialization;
            _value = value;
            if (taintType == TaintType.NONE) {
                throw new ArgumentException("Invalid type: " + taintType);
            }

            _type = taintType;
        }

        public StringSpecializationInfo(StringSpecialization stringSpecialization, string value) : this(
            stringSpecialization, value, TaintType.FULL_MATCH) {
        }

        public StringSpecialization GetStringSpecialization() => _stringSpecialization;

        public string GetValue() => _value;

        public TaintType GetTaintType() =>
            _type; //Java counterpart: GetType, name is changed here to prevent hiding GetType of object

        public override bool Equals(object o) {
            if (this == o) return true;

            if (o == null || GetType() != o.GetType()) return false;

            var that = (StringSpecializationInfo) o;

            return _stringSpecialization == that._stringSpecialization &&
                   Equals(_value, that._value) &&
                   _type == that._type;
        }

        public override int GetHashCode() => HashCode.Combine(_stringSpecialization, _value, _type);
    }
}