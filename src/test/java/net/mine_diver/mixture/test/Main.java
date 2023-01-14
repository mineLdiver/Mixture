package net.mine_diver.mixture.test;

import net.mine_diver.mixture.Mixtures;
import net.mine_diver.sarcasm.SarcASM;

public class Main {

    public static void main(String[] args) {
        SarcASM.registerInjector(Target.class, new TargetInjector());
        Mixtures.register(TargetMixture.class);
    }
}
