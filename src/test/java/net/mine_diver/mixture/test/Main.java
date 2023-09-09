package net.mine_diver.mixture.test;

import net.mine_diver.mixture.Mixtures;
import net.mine_diver.mixture.util.Identifier;
import net.mine_diver.sarcasm.SarcASM;

public class Main {

    public static void main(String[] args) {
        SarcASM.registerInjector(Target.class, new TargetInjector());
        Target.INSTANCE.test(true);
        Target.INSTANCE.test(false);
        System.out.println(Target.INSTANCE.testReturnable());
        Mixtures.registerPredicate(Identifier.of("test:test_predicate"));
        Mixtures.register(TargetMixture.class);
        Target.INSTANCE.test(true);
        Target.INSTANCE.test(false);
        System.out.println(Target.INSTANCE.testReturnable());
    }
}
