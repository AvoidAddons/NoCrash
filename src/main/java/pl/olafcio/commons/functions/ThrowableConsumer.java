package pl.olafcio.commons.functions;

public interface ThrowableConsumer<A, E extends Throwable> {
    void accept(A value) throws E;
}
