using System;

namespace EvoMaster.Controller.Api {
    public class WrappedResponseDto {
        internal WrappedResponseDto() { }

        public string Error { get; set; }
    }

    public class WrappedResponseDto<T> : WrappedResponseDto {
        /**
         * The actual payload we are sending and are "wrapping" here
         */
        public T Data { get; set; }

        /**
         * A message describing the error, if any.
         * If this is not null, then "data" must be null.
         */
        public static WrappedResponseDto<K> WithData<K>(K data) {
            var dto = new WrappedResponseDto<K>();

            dto.Data = data;

            return dto;
        }

        public static WrappedResponseDto WithNoData() => new WrappedResponseDto();

        public static WrappedResponseDto WithError(string error) {
            //TODO: check again
            if (error.Equals(null))
                throw new NullReferenceException("Null error message");

            if (string.IsNullOrEmpty(error)) {
                throw new ArgumentException("Empty error message");
            }

            var dto = new WrappedResponseDto();

            dto.Error = error;

            return dto;
        }
    }
}