package net.mine_diver.mixture.util;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public final class Identifier {
	
	private static final Map<String, WeakReference<Identifier>> CACHE = new HashMap<>();
	
	public static final String SEPARATOR = ":";
	
	public static Identifier of(String key) {
		String[] split = key.split(SEPARATOR);
		Namespace namespace;
		String id;
		switch (split.length) {
		case 1:
			namespace = Namespace.GLOBAL;
			id = key;
			break;
		case 2:
			namespace = Namespace.of(split[0]);
			id = split[1];
			break;
		default:
			throw new IllegalArgumentException("Invalid raw identifier string \"" + key + "\"!");
		}
		return of(namespace, id, key);
	}
	
	public static Identifier of(NamespaceProvider provider, String id) {
		return of(Namespace.of(provider), id);
	}
	
	public static Identifier of(Namespace namespace, String id) {
		return of(namespace, id, namespace + SEPARATOR + id);
	}
	
	private static Identifier of(Namespace namespace, String id, String key) {
		if (CACHE.containsKey(key)) {
			Identifier cached = CACHE.get(key).get();
			if (cached != null)
				return cached;
			else
				CACHE.remove(key);
		}
		Identifier toCache = new Identifier(namespace, id, key);
		CACHE.put(key, new WeakReference<>(toCache));
		return toCache;
		
	}
	
	public final Namespace namespace;
	public final String id;
	private final String toString;
	private final int hashCode;
	
	private Identifier(Namespace namespace, String id, String toString) {
		this.namespace = namespace;
		this.id = id;
		this.toString = toString;
		hashCode = toString.hashCode();
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (obj instanceof Identifier && toString.equals(((Identifier) obj).toString));
	}

	@Override
	public String toString() {
		return toString;
	}
}
