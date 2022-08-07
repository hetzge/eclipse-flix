package de.hetzge.eclipse.flix.internal;

public class FlixProject implements AutoCloseable {

	private final FlixService service;
	private final FlixLanguageClient languageClient;

	public FlixProject(FlixService service, FlixLanguageClient languageClient) {
		this.service = service;
		this.languageClient = languageClient;
	}

	public FlixService getService() {
		return this.service;
	}

	public FlixLanguageClient getLanguageClient() {
		return this.languageClient;
	}

	@Override
	public void close() throws Exception {
		this.service.close();
		this.languageClient.dispose();
	}
}
