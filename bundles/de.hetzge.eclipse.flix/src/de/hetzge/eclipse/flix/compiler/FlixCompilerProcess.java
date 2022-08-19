package de.hetzge.eclipse.flix.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;

import de.hetzge.eclipse.utils.Utils;

public final class FlixCompilerProcess implements AutoCloseable {

	private final Rollback rollback;
	private final Process process;

	private FlixCompilerProcess(Process process, Rollback rollback) {
		this.process = process;
		this.rollback = rollback;
	}

	public boolean isAlive() {
		return this.process.isAlive();
	}

	@Override
	public void close() {
		System.out.println("FlixCompilerProcess.close()");
		this.rollback.run();
	}

	public static synchronized FlixCompilerProcess start(File flixJarFile, int port) {
		System.out.println("FlixCompilerProcess.start()");
		return SafeRun.runWithResult((rollback) -> {
			try {
				final File jreExecutableFile = Utils.getJreExecutable();

				System.out.println("Use java from here: " + jreExecutableFile.getAbsolutePath());
				System.out.println("Use flix from here: " + flixJarFile.getAbsolutePath());

				final ProcessBuilder processBuilder = new ProcessBuilder(jreExecutableFile.getAbsolutePath(), "-jar", flixJarFile.getAbsolutePath(), "--lsp", String.valueOf(port));
				processBuilder.redirectErrorStream(true);
				final Process process = processBuilder.start();
				rollback.add(process::destroyForcibly);
				final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line = "";
				while ((line = reader.readLine()) != null) {
					if (line.contains("LSP listening on")) {
						break;
					} else {
						throw new RuntimeException("Failed to start lsp with message: " + line);
					}
				}
				final MonitorThread thread = new MonitorThread(process);
				rollback.add(thread::close);
				thread.start();

				System.out.println("Started flix compiler process on port " + port);

				return new FlixCompilerProcess(process, rollback);
			} catch (final IOException exception) {
				throw new RuntimeException(exception);
			}
		});
	}

	private static class MonitorThread extends Thread implements AutoCloseable {
		private final Process process;
		private boolean done;

		public MonitorThread(Process process) {
			super("Flix Compiler Client");
			this.process = process;
			this.done = false;
			setDaemon(true);
		}

		@Override
		public void run() {
			System.out.println("FlixCompilerProcess.MonitorThread.run()");
			final BufferedReader reader = new BufferedReader(new InputStreamReader(this.process.getInputStream()));
			String line = "";
			try {
				while (!this.done && (line = reader.readLine()) != null) {
					System.out.println("[FLIX LSP PROCESS]::" + line);
				}
			} catch (final IOException exception) {
				throw new RuntimeException(exception);
			}
			System.out.println("Finish flix compiler monitor thread");
		}

		@Override
		public void close() {
			System.out.println("FlixCompilerProcess.MonitorThread.close()");
			this.done = true;
		}
	}
}
