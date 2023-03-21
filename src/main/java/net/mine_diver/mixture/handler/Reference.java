package net.mine_diver.mixture.handler;

import net.mine_diver.mixture.Mixtures;
import net.mine_diver.mixture.util.Identifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

@Retention(RetentionPolicy.CLASS)
@Target({})
@Documented
public @interface Reference {

    String value();
    String[] overrides() default {};

    final class Parser {
        private Parser() {}
        public static String get(Reference reference) {
            String value = reference.value();
            String[] overrides = reference.overrides();
            if (overrides.length > 0) {
                if ((overrides.length & 1) == 1)
                    throw new IllegalArgumentException("Override arrays can't be of odd size! " + Arrays.toString(overrides));
                for (int i = 0; i < overrides.length; i += 2)
                    if (Mixtures.PREDICATES.contains(Identifier.of(overrides[i])))
                        value = overrides[i + 1];
            }
            return value;
        }
    }
}
