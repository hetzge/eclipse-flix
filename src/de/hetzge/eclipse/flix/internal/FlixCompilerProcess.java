package de.hetzge.eclipse.flix.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.net.URI;

import org.lxtk.util.SafeRun;
import org.lxtk.util.SafeRun.Rollback;

import de.hetzge.eclipse.utils.Utils;

public final class FlixCompilerProcess implements AutoCloseable {

	private final Process process;
	private final MonitorThread thread;
	private final Rollback rollback;

	private FlixCompilerProcess(Process process, MonitorThread thread, Rollback rollback) {
		this.process = process;
		this.thread = thread;
		this.rollback = rollback;
	}

	public static synchronized FlixCompilerProcess start() {
		return SafeRun.runWithResult((rollback) -> {
			try {
				System.out.println("Start lsp");
				final File jreExecutableFile = Utils.getJreExecutable();
				final File flixJarFile = new File("flix.jar");
				if (!flixJarFile.exists()) {
					System.out.println("Download flix.jar");
					try (FileOutputStream outputStream = new FileOutputStream("flix.jar")) {
						URI.create("https://github.com/flix/flix/releases/download/v0.30.0/flix.jar").toURL().openStream().transferTo(outputStream);
					}
				}
				System.out.println("Use java from here: " + jreExecutableFile.getAbsolutePath());
				System.out.println("Use flix from here: " + flixJarFile.getAbsolutePath());

				final ProcessBuilder processBuilder = new ProcessBuilder(jreExecutableFile.getAbsolutePath(), "-jar", flixJarFile.getAbsolutePath(), "--lsp", "8112");
				processBuilder.redirectErrorStream(true);
				processBuilder.redirectOutput(Redirect.PIPE);
				final Process process = processBuilder.start();
				rollback.add(process::destroyForcibly);
				final BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line = "";
				while ((line = stdoutReader.readLine()) != null) {
					if (line.contains("LSP listening on")) {
						break;
					} else {
						throw new RuntimeException("Failed to start lsp with message: " + line);
					}
				}
				final MonitorThread thread = new MonitorThread(process);
				rollback.add(thread::close);
				thread.start();
				return new FlixCompilerProcess(process, thread, rollback);
			} catch (final IOException exception) {
				throw new RuntimeException(exception);
			}
		});
	}

	@Override
	public void close() {
		this.rollback.reset();
	}

	public static class MonitorThread extends Thread implements AutoCloseable {
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
			super.run();
			final BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(this.process.getInputStream()));
			String line = "";
			try {
				while (!this.done && (line = stdoutReader.readLine()) != null) {
					System.out.println("[FLIX LSP PROCESS]::" + line);
				}
			} catch (final IOException exception) {
				throw new RuntimeException(exception);
			}
			System.out.println("Finish flix compiler monitor thread");
		}

		@Override
		public void close() {
			this.done = true;
		}
	}
}
