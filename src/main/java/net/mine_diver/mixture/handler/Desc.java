package net.mine_diver.mixture.handler;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({})
@Documented
public @interface Desc {

    Class<?> owner() default void.class;
    Reference value();
    Class<?> ret() default void.class;
    Class<?>[] args() default { };
}
