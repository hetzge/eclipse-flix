package de.hetzge.eclipse.flix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;

import de.hetzge.eclipse.utils.EclipseUtils;

public final class FlixUtils {
	private FlixUtils() {
	}

	public synchronized static File loadFlixJarFile() {
		final File flixJarFile = new File("flix.jar");
		if (!flixJarFile.exists()) {
			System.out.println("Download flix.jar");
			try (FileOutputStream outputStream = new FileOutputStream("flix.v0.30.0.jar")) {
				URI.create("https://github.com/flix/flix/releases/download/v0.30.0/flix.jar").toURL().openStream().transferTo(outputStream);
			} catch (final IOException exception) {
				throw new RuntimeException(exception);
			}
		}
		return flixJarFile;
	}

	public static List<IFile> findFlixFiles(IContainer container) {
		final List<IFile> files = new ArrayList<>();
		EclipseUtils.visitFiles(container, file -> {
			if (file.getFileExtension().equals("flix") || file.getFileExtension().equals("fpkg")) {
				files.add(file);
			}
		});
		return files;
	}

}
