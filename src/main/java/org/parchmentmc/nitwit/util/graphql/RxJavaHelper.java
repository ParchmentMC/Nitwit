package org.parchmentmc.nitwit.util.graphql;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.functions.Supplier;

public final class RxJavaHelper {
    private RxJavaHelper() { // Prevent instantiation
    }

    public static Supplier<NullPointerException> npe(String message) {
        return () -> new NullPointerException(message);
    }

    public static Supplier<NullPointerException> npe(String message, Object... args) {
        return npe(message.formatted(args));
    }

    public static <I, O> Function<I, Observable<O>> nullableMapO(Function<I, O> mapper,
                                                                 Supplier<? extends Throwable> exceptionSupplier) {
        return nullableMap(Observable::just, Observable::error, mapper, exceptionSupplier);
    }

    public static <I, O> Function<I, Single<O>> nullableMapS(Function<I, O> mapper,
                                                             Supplier<? extends Throwable> exceptionSupplier) {
        return nullableMap(Single::just, Single::error, mapper, exceptionSupplier);
    }

    static <I, O, U> Function<I, U> nullableMap(Function<O, U> normalMapper,
                                                Function<Supplier<? extends Throwable>, U> exceptionalMapper,
                                                Function<I, O> mapper,
                                                Supplier<? extends Throwable> exceptionSupplier) {
        return input -> {
            final O output = mapper.apply(input);
            if (output == null) {
                return exceptionalMapper.apply(exceptionSupplier);
            } else {
                return normalMapper.apply(output);
            }
        };
    }
}
