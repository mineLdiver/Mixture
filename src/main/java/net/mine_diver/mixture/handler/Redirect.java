package net.mine_diver.mixture.handler;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Documented
public @interface Redirect {

	Reference method();
	At at();
	boolean locals() default false;
	String predicate() default "";
}
