package net.mine_diver.mixture;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class Mixture {
    private Mixture() {}

    private static final Map<Class<?>, Set<Class<?>>> MIXTURES = new IdentityHashMap<>();
}
