package net.mine_diver.mixture.test;

import net.mine_diver.mixture.handler.*;

import java.io.PrintStream;

@Mixture(Target.class)
public class TargetMixture {

    @Inject(
            method = @Reference(value = "test(Z)V"),
            at = @At(
                    value = "mixture:injection_points/invoke",
                    target = @Reference("Ljava/io/PrintStream;println(Ljava/lang/String;)V"),
                    ordinal = 1,
                    shift = At.Shift.AFTER
            )
    )
    private void injectTest(boolean condition, CallbackInfo ci) {
        System.out.println("YOOO! " + condition);
        System.out.println("CallbackInfo: " + ci);
        ci.cancel();
    }

    @Redirect(
            method = @Reference(value = "test(Z)V"),
            at = @At(
                    value = "mixture:injection_points/invoke",
                    target = @Reference("Ljava/io/PrintStream;println(Ljava/lang/String;)V"),
                    ordinal = 2
            )
    )
    private void redirectTest(PrintStream stream, String message) {
        System.out.println(stream + " " + message);
    }
}
