package net.mine_diver.mixture.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;

public interface CommonInjector extends RawPredicateProvider {

    static <A extends Annotation & CommonInjector> A of(A ann) {
        //noinspection unchecked
        return (A) Proxy.newProxyInstance(ann.annotationType().getClassLoader(), new Class[] { ann.annotationType(), CommonInjector.class }, Proxy.getInvocationHandler(ann));
    }

    Reference[] method();
    Desc[] target();
    At at();
    LocalCapture locals();
}
