package net.mine_diver.mixture.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.mine_diver.mixture.Mixtures;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import sun.reflect.annotation.AnnotationParser;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MixtureUtil {

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static <T> T[] concat(T[] array, T element) {
        array = Arrays.copyOf(array, array.length + 1);
        array[array.length - 1] = element;
        return array;
    }

    public static <T> T make(T object, Consumer<T> initializer) {
        initializer.accept(object);
        return object;
    }

    public static <T> T make(Supplier<T> factory) {
        return factory.get();
    }

    public static <A extends Annotation> A createAnnotationInstance(AnnotationNode node) {
        Class<A> annotationType;
        try {
            //noinspection unchecked
            annotationType = (Class<A>) Class.forName(Type.getType(node.desc).getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Map<String, Method> methods = new HashMap<>();
        Map<String, Object> values = new HashMap<>();
        for (Method method : annotationType.getDeclaredMethods())
            methods.put(method.getName(), method);
        Map<String, Function<Object, ?>> transformers = ANNOTATION_TRANSFORMATION_CACHE.computeIfAbsent(annotationType, annType -> new HashMap<>());
        if (node.values != null) for (int i = 0; i < node.values.size(); i += 2) {
            String key = (String) node.values.get(i);
            Object value = node.values.get(i + 1);
            values.put(key, transformers.computeIfAbsent(key, s -> createValueTransformer(methods.get(key).getReturnType(), value.getClass())).apply(value));
            methods.remove(key);
        }
        methods.forEach((s, method) -> values.put(s, method.getDefaultValue()));
        //noinspection unchecked
        return (A) AnnotationParser.annotationForMap(annotationType, Collections.unmodifiableMap(values));
    }

    private static <T, V> Function<Object, ?> createValueTransformer(Class<T> originalType, Class<V> valueClass) {
        return ANNOTATION_VALUE_TRANSFORMER_FACTORIES.containsKey(valueClass) ? ANNOTATION_VALUE_TRANSFORMER_FACTORIES.get(valueClass).apply(originalType) : Function.identity();
    }

    private static Type arr(Type type) {
        return Type.getType("[" + type.getDescriptor());
    }

    private static final Map<Type, Class<?>> PRIMITIVE_CLASSES = make(() -> {
        Map<Type, Class<?>> m = new HashMap<>();
        m.put(Type.VOID_TYPE, void.class);
        m.put(Type.BOOLEAN_TYPE, boolean.class);
        m.put(Type.CHAR_TYPE, char.class);
        m.put(Type.BYTE_TYPE, byte.class);
        m.put(Type.SHORT_TYPE, short.class);
        m.put(Type.INT_TYPE, int.class);
        m.put(Type.FLOAT_TYPE, float.class);
        m.put(Type.LONG_TYPE, long.class);
        m.put(Type.DOUBLE_TYPE, double.class);
        m.put(arr(Type.BOOLEAN_TYPE), boolean[].class);
        m.put(arr(Type.CHAR_TYPE), char[].class);
        m.put(arr(Type.BYTE_TYPE), byte[].class);
        m.put(arr(Type.SHORT_TYPE), short[].class);
        m.put(arr(Type.INT_TYPE), int[].class);
        m.put(arr(Type.FLOAT_TYPE), float[].class);
        m.put(arr(Type.LONG_TYPE), long[].class);
        m.put(arr(Type.DOUBLE_TYPE), double[].class);
        return Collections.unmodifiableMap(m);
    });

    private static final Map<Class<? extends Annotation>, Map<String, Function<Object, ?>>> ANNOTATION_TRANSFORMATION_CACHE = new HashMap<>();

    private static final Map<Class<?>, Function<Class<?>, Function<Object, ?>>> ANNOTATION_VALUE_TRANSFORMER_FACTORIES = make(() -> {
        Map<Class<?>, Function<Class<?>, Function<Object, ?>>> m = new IdentityHashMap<>();
        m.put(ArrayList.class, originalType -> {
            Class<?> componentType = originalType.getComponentType();
            return o -> {
                List<?> list = (List<?>) o;
                return list.size() > 0 ? list.stream().map(createValueTransformer(componentType, list.get(0).getClass())).toArray(length -> (Object[]) Array.newInstance(componentType, length)) : Array.newInstance(componentType, 0);
            };
        });
        m.put(AnnotationNode.class, originalType -> o -> createAnnotationInstance((AnnotationNode) o));
        m.put(Type.class, originalType -> o -> {
            Type type = (Type) o;
            try {
                return PRIMITIVE_CLASSES.containsKey(type) ? PRIMITIVE_CLASSES.get(type) : Class.forName(type.getSort() == Type.ARRAY ? "[L" + type.getElementType().getClassName() + ";" : type.getClassName());
            } catch (ClassNotFoundException e) {
                Mixtures.LOGGER.log(Level.WARNING, "Missing class encountered! {0}", type);
                final class MissingClass {
                    private MissingClass() {
                        throw new UnsupportedOperationException();
                    }
                }
                return MissingClass.class;
            }
        });
        m.put(String[].class, originalType -> createEnumTransformer(originalType.asSubclass(Enum.class)));
        return Collections.unmodifiableMap(m);
    });

    private static <T extends Enum<T>> Function<Object, T> createEnumTransformer(Class<T> enumClass) {
        return o -> Enum.valueOf(enumClass, ((String[]) o)[1]);
    }
}
