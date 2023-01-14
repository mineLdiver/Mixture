package net.mine_diver.mixture.test;

import net.mine_diver.mixture.inject.Inject;
import net.mine_diver.mixture.inject.Mixture;
import net.mine_diver.mixture.inject.Reference;

@Mixture(Target.class)
public class TargetMixture {

    @Inject(
            method = @Reference(
                    value = "test()V",
                    overrides = {
                            "mappings:mcp", "mcpTest()V",
                            "mappings:barn", "barnTest()V"
                    }
            )
    )
    public void injectTest() {

    }
}
