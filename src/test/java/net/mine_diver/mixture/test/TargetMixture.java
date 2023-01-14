package net.mine_diver.mixture.test;

import net.mine_diver.mixture.inject.At;
import net.mine_diver.mixture.inject.Inject;
import net.mine_diver.mixture.inject.Mixture;
import net.mine_diver.mixture.inject.Reference;

@Mixture(Target.class)
public class TargetMixture {

    @Inject(
            method = @Reference(
                    value = "test(Z)V",
                    overrides = {
                            "mappings:mcp", "mcpTest(Z)V",
                            "mappings:barn", "barnTest(Z)V"
                    }
            ),
            at = @At(
                    value = "injection_points:invoke",
                    target = @Reference("Ljava/io/PrintStream;println(Ljava/lang/String;)V")
            )
    )
    public static void injectTest() {
        System.out.println("YOOO!");
    }
}
