package de.tum.in.testuser.subject;

import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import de.tum.in.test.api.security.ArtemisSecurityManager;

public class ThreadPenguin {

	private ThreadPenguin() {

	}

	public static void tryBreakThreadGroup() {
		ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
		for (;;) {
			ThreadGroup parent = threadGroup.getParent();
			if (parent == null)
				break;
			threadGroup = parent;
		}
		new Thread(threadGroup, () -> {
			// nothing
		}).start();
	}

	public static void spawnEndlessThreads() {
		try {
			Thread.sleep(2);
		} catch (@SuppressWarnings("unused") InterruptedException e) {
			// nothing
		}
		for (int i = 0; i < 2000; i++) {
			Thread t = new Thread(ThreadPenguin::spawnEndlessThreads);
			t.start();
		}
	}

	public static void tryThreadWhitelisting() throws Throwable {
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread t = new Thread(() -> Path.of("pom.xml").toFile().canWrite());
		ArtemisSecurityManager.requestThreadWhitelisting(t);
		t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				failure.set(e);
			}
		});
		t.start();
		t.join();
		if (failure.get() != null)
			throw failure.get();
	}
}