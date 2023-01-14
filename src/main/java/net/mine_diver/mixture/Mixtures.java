package net.mine_diver.mixture;

import net.mine_diver.mixture.util.Identifier;
import net.mine_diver.sarcasm.SarcASM;
import net.mine_diver.sarcasm.util.ASMHelper;
import net.mine_diver.sarcasm.util.Util;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class Mixtures {
    private Mixtures() {}

    private static final Logger LOGGER = Logger.getLogger("Mixture");
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
                        lr.getMessage()
                );
            }
        });
        LOGGER.addHandler(handler);
    }

    private static final Map<Class<?>, Set<Class<?>>> MIXTURES = new IdentityHashMap<>();
    private static final Map<Identifier, InjectionPoint<?>> INJECTION_POINTS_MUTABLE = new IdentityHashMap<>();
    private static final Map<String, Injector> INJECTORS_MUTABLE = new HashMap<>();
    private static final Set<Identifier> PREDICATES_MUTABLE = Util.newIdentitySet();
    static {
        registerInjectionPoint(Identifier.of("injection_points:invoke"), (insns, at) -> {
            String target = at.getReference("target");
            int ordinal = at.get("ordinal", -1);
            Set<MethodInsnNode> found = Util.newIdentitySet();
            Iterator<AbstractInsnNode> iter = insns.iterator();
            int cur = 0;
            while (iter.hasNext()) {
                AbstractInsnNode insn = iter.next();
                if (insn instanceof MethodInsnNode && target.equals(ASMHelper.toTarget(((MethodInsnNode) insn))) && (ordinal == -1 || ordinal == cur++))
                    found.add((MethodInsnNode) insn);
            }
            return Collections.unmodifiableSet(found);
        });
    }

    public static final Map<Identifier, InjectionPoint<?>> INJECTION_POINTS = Collections.unmodifiableMap(INJECTION_POINTS_MUTABLE);
    public static final Map<String, Injector> INJECTORS = Collections.unmodifiableMap(INJECTORS_MUTABLE);
    public static final Set<Identifier> PREDICATES = Collections.unmodifiableSet(PREDICATES_MUTABLE);

    public static <T> void register(Class<T> mixture) {
        ClassNode mixtureNode = new ClassNode();
        new ClassReader(ASMHelper.readClassBytes(mixture)).accept(mixtureNode, ClassReader.EXPAND_FRAMES);
        MixtureInfo info = new MixtureInfo(mixtureNode);
        try {
            SarcASM.registerTransformer(Class.forName(((Type) info.annotation.get("value")).getClassName()), new MixtureTransformer(info));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends AbstractInsnNode> void registerInjectionPoint(Identifier identifier, InjectionPoint<T> point) {
        if (INJECTION_POINTS_MUTABLE.put(identifier, point) != null)
            LOGGER.warning("Overriding injection point \"" + identifier + "\"! This might be intentional, or may be a result of an unaccounted collision");
    }

    public static void registerInjector(Class<? extends Annotation> annotation, Injector injector) {
        String annDescriptor = Type.getDescriptor(annotation);
        if (INJECTORS_MUTABLE.put(annDescriptor, injector) != null)
            LOGGER.warning("Overriding injector for annotation \"" + annotation.getName() + "\". This is most likely intentional");
    }

    public static void registerPredicate(Identifier predicate) {
        if (!PREDICATES_MUTABLE.add(predicate))
            LOGGER.warning("Registered predicate \"" + predicate + "\" multiple times! This might be intentional, or may be a result of an unaccounted collision");
    }
}
