package net.mine_diver.mixture.inject;

public @interface At {

    String value();

    String target() default "";
}
