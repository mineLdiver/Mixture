package net.mine_diver.mixture.handler;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Documented
public @interface Inject {

    Reference method();
    At at();
    LocalCapture locals() default LocalCapture.NO_CAPTURE;
    String predicate() default "";
}
