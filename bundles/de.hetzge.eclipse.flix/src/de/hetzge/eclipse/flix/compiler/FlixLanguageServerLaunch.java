package de.hetzge.eclipse.flix.compiler;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.lxtk.util.Disposable;
import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;

public class FlixLanguageServerLaunch implements Disposable {
	private final ILaunch launch;
	private final Rollback rollback;
	private boolean connected;
	private boolean ready;

	public FlixLanguageServerLaunch(ILaunch launch) {
		this.launch = launch;
		this.rollback = SafeRun.runWithResult(rollback -> {
			rollback.add(() -> terminateLaunch());
			final IStreamMonitor outputStreamMonitor = launch.getProcesses()[0].getStreamsProxy().getOutputStreamMonitor();
			final IStreamMonitor errorStreamMonitor = launch.getProcesses()[0].getStreamsProxy().getErrorStreamMonitor();
			final IStreamListener listener = this::onOutput;
			outputStreamMonitor.addListener(listener);
			errorStreamMonitor.addListener(listener);
			rollback.add(() -> outputStreamMonitor.removeListener(listener));
			rollback.add(() -> errorStreamMonitor.removeListener(listener));
			return rollback;
		});
		this.connected = false;
		this.ready = false;
	}

	private void onOutput(String text, IStreamMonitor monitor) {
		if (text.startsWith("Listen on")) {
			this.ready = true;
		} else if (text.startsWith("Client at")) {
			this.connected = true;
		}
	}

	public void waitUntilReady() {
		for (int i = 0; i < 500; i++) {
			if (this.ready) {
				return;
			}
			System.out.println("Wait until ready ...");
			try {
				Thread.sleep(500);
			} catch (final InterruptedException exception) {
				throw new RuntimeException(exception);
			}
		}
		throw new RuntimeException("Failed to start language server");
	}

	public void waitUntilConnected() {
		for (int i = 0; i < 500; i++) {
			if (this.connected) {
				return;
			}
			System.out.println("Wait until started ...");
			try {
				Thread.sleep(500);
			} catch (final InterruptedException exception) {
				throw new RuntimeException(exception);
			}
		}
		throw new RuntimeException("Failed to connect to language server");
	}

	@Override
	public void dispose() {
		this.rollback.run();
	}

	public boolean isRunning() {
		final IProcess[] processes = this.launch.getProcesses();
		for (int i = 0; i < processes.length; i++) {
			if (processes[i].isTerminated()) {
				return false;
			}
		}
		return true;
	}

	private void terminateLaunch() {
		final IProcess[] processes = this.launch.getProcesses();
		for (int i = 0; i < processes.length; i++) {
			if (processes[i].canTerminate()) {
				try {
					processes[i].terminate();
				} catch (final DebugException exception) {
					throw new RuntimeException(exception);
				}
			}
		}
	}

}
