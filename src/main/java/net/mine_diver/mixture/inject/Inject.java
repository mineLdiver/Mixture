package net.mine_diver.mixture.inject;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Documented
public @interface Inject {

    String method();


}
