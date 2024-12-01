package de.hetzge.eclipse.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Consumer;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public final class EclipseConsoleUtils {

	private static final ILog LOG = Platform.getLog(EclipseUtils.class);

	private EclipseConsoleUtils() {
	}

	public static Thread startWriteToConsoleThread(Process process, MessageConsole console, Consumer<String> lineConsumer) {
		final MessageConsoleStream consoleStream = console.newMessageStream();
		final Thread thread = new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					lineConsumer.accept(line);
					consoleStream.println(line);
				}
			} catch (final IOException exception) {
				LOG.error("Process output thread problem", exception);
				if (process.isAlive()) {
					process.destroyForcibly();
				}
			}
		});
		process.onExit().thenAccept(ignore -> {
			if (thread.isAlive()) {
				thread.interrupt();
			}
		});
		thread.start();
		return thread;
	}

	public static MessageConsole findConsole(String name) {
		final ConsolePlugin plugin = ConsolePlugin.getDefault();
		final IConsoleManager conMan = plugin.getConsoleManager();
		final IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++) {
			if (name.equals(existing[i].getName())) {
				return (MessageConsole) existing[i];
			}
		}
		// no console found, so create a new one
		final MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}

}
