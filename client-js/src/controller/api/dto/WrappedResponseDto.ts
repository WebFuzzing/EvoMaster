/**
 * In REST, when we have an error, at most we would see a HTTP
 * status code.
 * But it can be very useful to get an actual description of the error.
 * So, it is a common practice to have "Wrapped Responses", which can
 * contain the error message (if any)
 *
 */
export default class WrappedResponseDto<T> {

    /**
     * The actual payload we are sending and are "wrapping" here
     */
    data: T;

    /**
     * A message describing the error, if any.
     * If this is not null, then "data" must be null.
     */
    error: string;


    static withData<K>(data: K): WrappedResponseDto<K> {
        const dto = new WrappedResponseDto<K>();
        dto.data = data;
        return dto;
    }

    static withError(error: string): WrappedResponseDto<any> {

        if (!error || error.length == 0) {
            throw new Error("Empty error message");
        }

        const dto = new WrappedResponseDto();
        dto.error = error;
        return dto;
    }
}

