package net.mine_diver.mixture.util;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public final class Namespace {

    private static final Map<String, WeakReference<Namespace>> CACHE = new HashMap<>();

    public static final Namespace GLOBAL = of("global");

    public static Namespace of(NamespaceProvider provider) {
        return of(provider.namespace());
    }

    static Namespace of(String namespace) {
        if (CACHE.containsKey(namespace)) {
            Namespace cached = CACHE.get(namespace).get();
            if (cached != null)
                return cached;
            else
                CACHE.remove(namespace);
        }
        Namespace toCache = new Namespace(namespace);
        CACHE.put(namespace, new WeakReference<>(toCache));
        return toCache;
    }

    public final String namespace;
    private final int hashCode;

    private Namespace(String namespace) {
        this.namespace = namespace;
        hashCode = namespace.hashCode();
    }

    public Identifier id(String id) {
        return Identifier.of(this, id);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof Namespace && namespace.equals(((Namespace) obj).namespace));
    }

    @Override
    public String toString() {
        return namespace;
    }
}
