package de.hetzge.eclipse.flix.compiler;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.lxtk.util.Disposable;
import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;

public class FlixCompilerLaunch implements Disposable {
	private static final ILog LOG = Platform.getLog(FlixCompilerLaunch.class);

	private final ILaunch launch;
	private final Rollback rollback;
	private boolean connected;
	private boolean ready;

	public FlixCompilerLaunch(ILaunch launch) {
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
		if (text.startsWith("Listen on")) { //$NON-NLS-1$
			this.ready = true;
		} else if (text.startsWith("Client at")) { //$NON-NLS-1$
			this.connected = true;
		}
	}

	public void waitUntilReady() {
		for (int i = 0; i < 500; i++) {
			if (this.ready) {
				return;
			}
			LOG.info("Wait until ready ..."); //$NON-NLS-1$
			try {
				Thread.sleep(500);
			} catch (final InterruptedException exception) {
				throw new RuntimeException(exception);
			}
		}
		throw new RuntimeException("Failed to start language server"); //$NON-NLS-1$
	}

	public void waitUntilConnected() {
		for (int i = 0; i < 500; i++) {
			if (this.connected) {
				return;
			}
			LOG.info("Wait until started ..."); //$NON-NLS-1$
			try {
				Thread.sleep(500);
			} catch (final InterruptedException exception) {
				throw new RuntimeException(exception);
			}
		}
		throw new RuntimeException("Failed to connect to language server"); //$NON-NLS-1$
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
