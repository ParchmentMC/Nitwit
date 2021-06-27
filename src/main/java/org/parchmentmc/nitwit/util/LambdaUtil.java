package org.parchmentmc.nitwit.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class for handling lambda expressions which throw exceptions.
 *
 * @see <a href="https://stackoverflow.com/a/27252163">StackOverflow,
 * "Java 8 Lambda function that throws exception?", answer by jlb</a>
 * @see <a href="https://stackoverflow.com/a/46966597">StackOverflow,
 * "Java 8 Lambda function that throws exception?", answer by myui</a>
 */
public final class LambdaUtil {
    private LambdaUtil() { // Prevent instantiation
    }

    /**
     * Converts the given throwing runnable into a regular runnable, rethrowing any exception as needed.
     *
     * @param runnable the throwing runnable to convert
     * @return a runnable which runs the given throwing runnable, rethrowing any exceptions
     * @see #sneakyThrow(Throwable)
     */
    public static Runnable rethrow(final ThrowingRunnable runnable) {
        return runnable;
    }

    /**
     * Converts the given throwing supplier into a regular supplier, rethrowing any exception as needed.
     *
     * @param supplier the throwing supplier to convert
     * @param <T>      the type of results supplied by this supplier
     * @return a supplier which returns the result from the given throwing supplier, rethrowing any exceptions
     * @see #sneakyThrow(Throwable)
     */
    public static <T> Supplier<T> rethrow(final ThrowingSupplier<T> supplier) {
        return supplier;
    }

    /**
     * Converts the given throwing runnable into a regular consumer, rethrowing any exception as needed.
     *
     * @param consumer the throwing consumer to convert
     * @param <T>      the type of the input to the operation
     * @return a runnable which passes to the given throwing consumer, rethrowing any exceptions
     * @see #sneakyThrow(Throwable)
     */
    public static <T> Consumer<T> rethrow(final ThrowingConsumer<T> consumer) {
        return consumer;
    }

    /**
     * Converts the given throwing function into a regular function, rethrowing any exception as needed.
     *
     * @param function the throwing function to convert
     * @param <T>      the type of the input to the function
     * @param <R>      the type of the result of the function
     * @return a function which applies the given throwing function, rethrowing any exceptions
     * @see #sneakyThrow(Throwable)
     */
    public static <T, R> Function<T, R> rethrow(final ThrowingFunction<T, R> function) {
        return function;
    }

    /**
     * Sneakily throws the given exception, bypassing compile-time checks for checked exceptions.
     *
     * <p><strong>This method will never return normally.</strong> The exception passed to the method is always
     * rethrown.</p>
     *
     * @param ex the exception to sneakily rethrow
     */
    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void sneakyThrow(Throwable ex) throws E {
        throw (E) ex;
    }

    /**
     * A specialized version of {@link Runnable} which is extended to allow throwing
     * an exception.
     *
     * @see Runnable
     * @see #rethrow(ThrowingRunnable)
     */
    @FunctionalInterface
    public interface ThrowingRunnable extends Runnable {
        void runThrows() throws Exception;

        @Override
        default void run() {
            try {
                runThrows();
            } catch (Exception e) {
                sneakyThrow(e);
            }
        }
    }

    /**
     * Represents a supplier of results, which may throw an exception.
     *
     * <p>There is no requirement that a new or distinct result be returned each
     * time the supplier is invoked.
     *
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #get()}.
     *
     * @param <T> the type of results supplied by this supplier
     * @see Supplier
     * @see #rethrow(ThrowingSupplier)
     */
    @FunctionalInterface
    public interface ThrowingSupplier<T> extends Supplier<T> {
        T getThrows() throws Exception;

        @Override
        default T get() {
            try {
                return getThrows();
            } catch (Exception e) {
                sneakyThrow(e);
                return null; // Never reached, as previous line always throws
            }
        }
    }

    /**
     * Represents an operation that accepts a single input argument and returns no
     * result, which may throw an exception. Like its non-throwing counterpart,
     * {@code ThrowingConsumer} is expected to operate via side-effects.
     *
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #acceptThrows(Object)}.</p>
     *
     * @param <T> the type of the input to the operation
     * @see Consumer
     * @see #rethrow(ThrowingConsumer)
     */
    @FunctionalInterface
    public interface ThrowingConsumer<T> extends Consumer<T> {
        void acceptThrows(T elem) throws Exception;

        @Override
        default void accept(final T elem) {
            try {
                acceptThrows(elem);
            } catch (final Exception e) {
                sneakyThrow(e);
            }
        }
    }

    /**
     * Represents a function that accepts one argument and produces a result, which
     * may throw an exception.
     *
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #apply(Object)}.
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @see Function
     * @see #rethrow(ThrowingFunction)
     */
    @FunctionalInterface
    public interface ThrowingFunction<T, R> extends Function<T, R> {
        R applyThrows(T t) throws Exception;

        @Override
        default R apply(T t) {
            try {
                return applyThrows(t);
            } catch (final Exception e) {
                sneakyThrow(e);
                return null; // Never reached, as previous line always throws
            }
        }
    }
}
