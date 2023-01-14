package net.mine_diver.mixture.handler;

public final class CallbackInfo {

    private boolean canceled;

    public void cancel() {
        canceled = true;
    }

    public boolean isCanceled() {
        return canceled;
    }
}
