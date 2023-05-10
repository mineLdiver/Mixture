package net.mine_diver.mixture.handler;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({})
@Documented
public @interface At {

    String value();

    Reference target() default @Reference("");

    Desc desc() default @Desc(@Reference(""));

    Shift shift() default Shift.UNSET;

    int ordinal() default -1;

    int opcode() default -1;

    enum Shift {
        BEFORE,
        UNSET,
        AFTER
    }
}
