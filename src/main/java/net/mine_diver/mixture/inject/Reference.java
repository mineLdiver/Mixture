package net.mine_diver.mixture.inject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.CLASS)
public @interface Reference {

    String value();

    String[] overrides() default {};
}
