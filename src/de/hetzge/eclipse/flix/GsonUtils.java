package de.hetzge.eclipse.flix;

import org.eclipse.lsp4j.jsonrpc.json.adapters.CollectionTypeAdapter;
import org.eclipse.lsp4j.jsonrpc.json.adapters.EitherTypeAdapter;
import org.eclipse.lsp4j.jsonrpc.json.adapters.EnumTypeAdapter;
import org.eclipse.lsp4j.jsonrpc.json.adapters.ThrowableTypeAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class GsonUtils {

	private static final Gson GSON = new GsonBuilder() //
			.registerTypeAdapterFactory(new EitherTypeAdapter.Factory()) //
			.registerTypeAdapterFactory(new CollectionTypeAdapter.Factory()) //
			.registerTypeAdapterFactory(new EnumTypeAdapter.Factory()) //
			.registerTypeAdapterFactory(new ThrowableTypeAdapter.Factory()) //
			.create();

	private GsonUtils() {
	}

	public static Gson getGson() {
		return GSON;
	}

}
