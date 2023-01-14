package net.mine_diver.mixture.test;

import net.mine_diver.mixture.handler.At;
import net.mine_diver.mixture.handler.Inject;
import net.mine_diver.mixture.handler.Mixture;
import net.mine_diver.mixture.handler.Reference;

@Mixture(Target.class)
public class TargetMixture {

    @Inject(
            method = @Reference(value = "test(Z)V"),
            at = @At(
                    value = "mixture:injection_points/invoke",
                    target = @Reference("Ljava/io/PrintStream;println(Ljava/lang/String;)V"),
                    ordinal = 1
            )
    )
    private void injectTest(boolean condition) {
        System.out.println("YOOO! " + condition);
    }
}
