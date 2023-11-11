package net.mine_diver.mixture;

import net.mine_diver.mixture.handler.*;
import net.mine_diver.mixture.inject.*;
import net.mine_diver.mixture.transform.MixtureInfo;
import net.mine_diver.mixture.transform.MixtureTransformer;
import net.mine_diver.mixture.util.MixtureUtil;
import net.mine_diver.sarcasm.SarcASM;
import net.mine_diver.sarcasm.util.Identifier;
import net.mine_diver.sarcasm.util.Namespace;
import net.mine_diver.sarcasm.util.NamespaceProvider;
import net.mine_diver.sarcasm.util.Util;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class Mixtures implements NamespaceProvider {
    private Mixtures() {}

    @Override
    public String namespace() {
        return "mixture";
    }

    public static final Namespace NAMESPACE = Namespace.of(new Mixtures());
    public static final Logger LOGGER = Logger.getLogger("Mixture");

    static {
        LOGGER.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {
            private static final String format = "[%1$tF] [%1$tT] [%2$s] [%3$s#%4$s] [%5$s]: %6$s %n";

            @Override
            public synchronized String format(LogRecord lr) {
                String className = lr.getSourceClassName();
                return String.format(format,
                        new Date(lr.getMillis()),
                        lr.getLoggerName(),
                        className.substring(className.lastIndexOf(".") + 1),
                        lr.getSourceMethodName(),
                        lr.getLevel().getLocalizedName(),
                        formatMessage(lr)
                );
            }
        });
        LOGGER.addHandler(handler);
    }

    private static final Map<Class<?>, MixtureTransformer<?>> MIXTURES = new IdentityHashMap<>();
    private static final Map<Identifier, InjectionPoint<?>> INJECTION_POINTS_MUTABLE = new IdentityHashMap<>();
    private static final Map<String, Injector<?>> INJECTORS_MUTABLE = new HashMap<>();
    private static final Set<Identifier> PREDICATES_MUTABLE = Util.newIdentitySet();

    static {
        registerInjectionPoint(NAMESPACE.id("injection_points/head"), new HeadInjectionPoint());
        registerInjectionPoint(NAMESPACE.id("injection_points/field"), new FieldInjectionPoint());
        registerInjectionPoint(NAMESPACE.id("injection_points/invoke"), new InvokeInjectionPoint());
        registerInjectionPoint(NAMESPACE.id("injection_points/return"), new ReturnInjectionPoint());
        registerInjector(Inject.class, new InjectInjector<>());
        registerInjector(Redirect.class, new RedirectInjector<>());
        registerInjector(ModifyVariable.class, new ModifyVariableInjector<>());
    }

    public static final Map<Identifier, InjectionPoint<?>> INJECTION_POINTS = Collections.unmodifiableMap(INJECTION_POINTS_MUTABLE);
    public static final Map<String, Injector<?>> INJECTORS = Collections.unmodifiableMap(INJECTORS_MUTABLE);
    public static final Set<Identifier> PREDICATES = Collections.unmodifiableSet(PREDICATES_MUTABLE);

    public static <T> void register(Class<T> mixture) {
        MixtureInfo info = new MixtureInfo(mixture);
        Class<?> target = info.annotation.value();
        MIXTURES.computeIfAbsent(target, aClass -> {
            MixtureTransformer<?> transformer = new MixtureTransformer<>(target);
            SarcASM.registerTransformer(aClass, transformer);
            return transformer;
        }).add(info);

        // transformer is invalidated. reinitializing
        SarcASM.invalidateProxyClass(target);
        SarcASM.initProxyFor(target);
    }

    public static <T extends AbstractInsnNode> void registerInjectionPoint(Identifier identifier, InjectionPoint<T> point) {
        if (INJECTION_POINTS_MUTABLE.put(identifier, point) != null)
            LOGGER.warning("Overriding injection point \"" + identifier + "\"! This might be intentional, or may be a result of an unaccounted collision");
    }

    public static <A extends Annotation, T extends Annotation & CommonInjector> void registerInjector(Class<A> annotation, Injector<T> injector) {
        String annDescriptor = Type.getDescriptor(annotation);
        if (INJECTORS_MUTABLE.put(annDescriptor, injector) != null)
            LOGGER.warning("Overriding injector for annotation \"" + annotation.getName() + "\". This is most likely intentional");
    }

    public static void registerPredicate(final Identifier predicate) {
        if (!PREDICATES_MUTABLE.add(predicate))
            LOGGER.warning("Registered predicate \"" + predicate + "\" multiple times! This might be intentional, or may be a result of an unaccounted collision");
    }

    public static boolean containsPredicate(final Identifier predicate) {
        return Mixtures.PREDICATES.contains(predicate);
    }

    public static boolean containsPredicate(final RawPredicateProvider rawPredicateProvider) {
        final String rawPredicate = rawPredicateProvider.predicate();
        return MixtureUtil.isNullOrEmpty(rawPredicate) || containsPredicate(Identifier.of(rawPredicate));
    }
}
