package net.mine_diver.mixture.handler;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Documented
public @interface Mixture {

    Class<?> value();
}
