package net.mine_diver.mixture.handler;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target({
        ElementType.FIELD,
        ElementType.METHOD
})
@Documented
public @interface Shadow {
    String[] overrides() default {};
}
