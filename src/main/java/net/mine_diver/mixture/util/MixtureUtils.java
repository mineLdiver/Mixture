package net.mine_diver.mixture.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.mine_diver.mixture.handler.CommonInjector;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import sun.reflect.annotation.AnnotationParser;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MixtureUtils {

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static <T> T[] concat(T[] array, T element) {
        array = Arrays.copyOf(array, array.length + 1);
        array[array.length - 1] = element;
        return array;
    }

    public static <A extends Annotation & CommonInjector> A createInjectorAnnotationInstance(AnnotationNode node) {
        Annotation an = createAnnotationInstance(node);
        //noinspection unchecked
        return (A) Proxy.newProxyInstance(an.annotationType().getClassLoader(), new Class[] { an.annotationType(), CommonInjector.class }, Proxy.getInvocationHandler(an));
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
        for (int i = 0; i < node.values.size(); i += 2) {
            String key = (String) node.values.get(i);
            values.put(key, transformValue(methods.get(key).getReturnType(), node.values.get(i + 1)));
            methods.remove(key);
        }
        methods.forEach((s, method) -> values.put(s, method.getDefaultValue()));
        //noinspection unchecked
        return (A) AnnotationParser.annotationForMap(annotationType, values);
    }

    private static Object transformValue(Class<?> originalType, Object value) {
        if (value instanceof List<?>) {
            Class<?> componentType = originalType.getComponentType();
            return ((List<?>) value).stream().map(o -> transformValue(componentType, o)).toArray(length -> (Object[]) Array.newInstance(componentType, length));
        } else if (value instanceof AnnotationNode)
            return createAnnotationInstance((AnnotationNode) value);
        else if (value instanceof Type) try {
            return Class.forName(((Type) value).getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } else if (value instanceof String[]) {
            String[] enumConst = (String[]) value;
            try {
                return Enum.valueOf(Class.forName(Type.getType(enumConst[0]).getClassName()).asSubclass(Enum.class), enumConst[1]);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else return value;
    }
}
