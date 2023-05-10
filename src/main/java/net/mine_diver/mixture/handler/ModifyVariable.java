package net.mine_diver.mixture.handler;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Documented
public @interface ModifyVariable {

    Reference[] method() default {};

    Desc[] target() default {};

    At at();

    int index();

    boolean argsOnly() default false;

    LocalCapture locals() default LocalCapture.NO_CAPTURE;

    String predicate() default "";
}
