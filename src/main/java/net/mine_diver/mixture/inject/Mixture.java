package net.mine_diver.mixture.inject;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Documented
public @interface Mixture {

    Class<?> value();
}
