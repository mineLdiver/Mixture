package net.mine_diver.mixture.handler;

public enum LocalCapture {

    NO_CAPTURE(false, false),
    PRINT(false, true),
    CAPTURE_FAILHARD;

    private final boolean captureLocals;

    private final boolean printLocals;

    LocalCapture() {
        this(true, false);
    }

    LocalCapture(boolean captureLocals, boolean printLocals) {
        this.captureLocals = captureLocals;
        this.printLocals = printLocals;
    }

    public boolean isCaptureLocals() {
        return this.captureLocals;
    }

    public boolean isPrintLocals() {
        return this.printLocals;
    }
}
