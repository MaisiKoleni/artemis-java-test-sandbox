package de.tum.in.test.api.security;

import java.io.PrintStream;
import java.util.Set;
import java.util.function.BiConsumer;

import de.tum.in.test.api.util.BlacklistedInvoker;

public final class SecurityConstants {

	private SecurityConstants() {

	}

	static final PrintStream SYSTEM_OUT = System.out;
	static final PrintStream SYSTEM_ERR = System.err;
	static final Thread MAIN_THREAD = Thread.currentThread();
	static final String SECURITY_PACKAGE_NAME = SecurityConstants.class.getPackageName();

	static final Set<String> STACK_WHITELIST = Set.of("java.", "org.junit.", "jdk.", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"org.eclipse.", "com.intellij", "org.assertj", "org.opentest4j.", // $NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"com.sun.", "sun.", "org.apache.", "de.tum.in.test.", "net.jqwik", SECURITY_PACKAGE_NAME); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	static final Set<String> STACK_BLACKLIST = Set.of(BlacklistedInvoker.class.getName());

	static final Set<String> PACKAGE_USE_BLACKLIST = Set.of(SECURITY_PACKAGE_NAME, "java.lang.reflect",
			"de.tum.in.test.api.internal");

	static final BiConsumer<String, Object> ON_SUPPRESSED_MOD = (method, object) -> SYSTEM_ERR.format(
			"[WARNING] addSuppressed, %s called with %s%n", method, object == null ? "null" : object.getClass()); //$NON-NLS-1$ //$NON-NLS-2$
}