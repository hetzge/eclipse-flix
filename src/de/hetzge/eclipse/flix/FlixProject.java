package de.hetzge.eclipse.flix;

import org.lxtk.util.SafeRun.Rollback;

public class FlixProject implements AutoCloseable {

	private final Rollback rollback;

	public FlixProject(Rollback rollback) {
		this.rollback = rollback;
	}

	@Override
	public void close() throws Exception {
		System.out.println("FlixProject.close()");
		this.rollback.reset();
	}
}
