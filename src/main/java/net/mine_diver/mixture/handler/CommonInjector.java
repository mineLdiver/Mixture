package net.mine_diver.mixture.handler;

public interface CommonInjector {

    Reference[] method();
    Desc[] target();
    At at();
    LocalCapture locals();
    String predicate();
}
