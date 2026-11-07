package pl.olafcio.commons.functions;

public interface ThrowableSupplier<R, E extends Throwable> {
    R get() throws E;
}
