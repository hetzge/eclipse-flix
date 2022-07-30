package de.hetzge.eclipse.flix.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;

public final class FlixCompilerProcess {

	private final Process process;
	private final MonitorThread thread;

	private FlixCompilerProcess(Process process, MonitorThread thread) {
		this.process = process;
		this.thread = thread;
	}

	public static synchronized FlixCompilerProcess start() {
		try {
			System.out.println("Start lsp");

			// /home/hetzge/.sdkman/candidates/java/21.3.0.r17-grl/bin/java -jar
			// /home/hetzge/Downloads/flix.jar --lsp 8000
			final ProcessBuilder processBuilder = new ProcessBuilder("/home/hetzge/.sdkman/candidates/java/21.3.0.r17-grl/bin/java", "-jar", "/home/hetzge/Downloads/flix.jar", "--lsp", "8112");
			processBuilder.redirectErrorStream(true);
			processBuilder.redirectOutput(Redirect.PIPE);
			final Process process = processBuilder.start();
			final BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = "";
			while ((line = stdoutReader.readLine()) != null) {
				if (line.contains("LSP listening on")) {
					break;
				} else {
					throw new RuntimeException("Failed to start lsp with message: " + line);
				}
			}

			System.out.println("No no no");
			System.out.println(process.isAlive());

			final MonitorThread thread = new MonitorThread(process);
			thread.start();
			return new FlixCompilerProcess(process, thread);
		} catch (final IOException exception) {
			throw new RuntimeException(exception);
		}
	}

	public static class MonitorThread extends Thread {
		private final Process process;

		public MonitorThread(Process process) {
			super("Flix Compiler Client");
			this.process = process;
			setDaemon(true);
		}

		@Override
		public void run() {
			super.run();
			final BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(this.process.getInputStream()));
			String line = "";
			try {
				while ((line = stdoutReader.readLine()) != null) {
					System.out.println("[FLIX LSP PROCESS]::" + line);
				}
			} catch (final IOException exception) {
				throw new RuntimeException(exception);
			}
		}
	}

}
