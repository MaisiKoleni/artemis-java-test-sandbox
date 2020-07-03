package de.tum.in.test.api;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import de.tum.in.test.api.internal.BlacklistedInvoker;
import de.tum.in.test.api.internal.PrivilegedException;

@API(status = Status.EXPERIMENTAL)
public final class TestUtils {

	private TestUtils() {
	}
	
	public static void invokeBlacklisted(Runnable action) {
		BlacklistedInvoker.invoke(action);
	}

	public static <T, R> R invokeBlacklisted(T t, Function<T, R> action) {
		return BlacklistedInvoker.invoke(t, action);
	}
	
	public static <T> void invokeBlacklisted(T t, Consumer<T> action) {
		BlacklistedInvoker.invoke(t, action);
	}

	public static <R> R invokeBlacklisted(Supplier<R> action) {
		return BlacklistedInvoker.invoke(action);
	}

	public static void privilegedThrow(Runnable possiblyThrowingAction) {
		try {
			possiblyThrowingAction.run();
		} catch (Throwable t) {
			throw new PrivilegedException(t);
		}
	}

	public static <R> R privilegedThrow(Callable<R> possiblyThrowingAction) throws Exception {
		try {
			return possiblyThrowingAction.call();
		} catch (Throwable t) {
			throw new PrivilegedException(t);
		}
	}

	public static void privilegedFail(String message) {
		throw new PrivilegedException(new AssertionError(message));
	}
}
