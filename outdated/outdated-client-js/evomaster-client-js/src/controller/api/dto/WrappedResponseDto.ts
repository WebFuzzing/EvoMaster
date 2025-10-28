/**
 * In REST, when we have an error, at most we would see a HTTP
 * status code.
 * But it can be very useful to get an actual description of the error.
 * So, it is a common practice to have "Wrapped Responses", which can
 * contain the error message (if any)
 *
 */
export default class WrappedResponseDto<T> {

    public static withData<K>(data: K): WrappedResponseDto<K> {
        const dto = new WrappedResponseDto<K>();
        dto.data = data;
        return dto;
    }

    public static withNoData(): WrappedResponseDto<void> {
        return new WrappedResponseDto<void>();
    }

    public static withError(error: string): WrappedResponseDto<void> {

        if (!error || error.length === 0) {
            throw new Error("Empty error message");
        }

        const dto = new WrappedResponseDto<void>();
        dto.error = error;
        return dto;
    }

    /**
     * The actual payload we are sending and are "wrapping" here
     */
    public data: T;

    /**
     * A message describing the error, if any.
     * If this is not null, then "data" must be null.
     */
    public error: string;
}
