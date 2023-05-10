package net.mine_diver.mixture.handler;

public final class CallbackInfoReturnable<R> extends CallbackInfo {

    private R returnValue;

    public CallbackInfoReturnable() {
        this.returnValue = null;
    }

    public CallbackInfoReturnable(R returnValue) {
        this.returnValue = returnValue;
    }

    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(byte returnValue) {
        this.returnValue = (R) Byte.valueOf(returnValue);
    }

    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(char returnValue) {
        this.returnValue = (R) Character.valueOf(returnValue);
    }

    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(double returnValue) {
        this.returnValue = (R) Double.valueOf(returnValue);
    }

    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(float returnValue) {
        this.returnValue = (R) Float.valueOf(returnValue);
    }

    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(int returnValue) {
        this.returnValue = (R) Integer.valueOf(returnValue);
    }

    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(long returnValue) {
        this.returnValue = (R) Long.valueOf(returnValue);
    }

    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(short returnValue) {
        this.returnValue = (R) Short.valueOf(returnValue);
    }

    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(boolean returnValue) {
        this.returnValue = (R) Boolean.valueOf(returnValue);
    }

    /**
     * Sets a return value for this callback and cancels the callback (required
     * in order to return the new value)
     *
     * @param returnValue value to return
     */
    public void setReturnValue(R returnValue) {
        super.cancel();

        this.returnValue = returnValue;
    }

    public R getReturnValue() {
        return this.returnValue;
    }

    public byte getReturnValueB() {
        if (this.returnValue == null) {
            return 0;
        }
        return (Byte) this.returnValue;
    }

    public char getReturnValueC() {
        if (this.returnValue == null) {
            return 0;
        }
        return (Character) this.returnValue;
    }

    public double getReturnValueD() {
        if (this.returnValue == null) {
            return 0.0;
        }
        return (Double) this.returnValue;
    }

    public float getReturnValueF() {
        if (this.returnValue == null) {
            return 0.0F;
        }
        return (Float) this.returnValue;
    }

    public int getReturnValueI() {
        if (this.returnValue == null) {
            return 0;
        }
        return (Integer) this.returnValue;
    }

    public long getReturnValueJ() {
        if (this.returnValue == null) {
            return 0;
        }
        return (Long) this.returnValue;
    }

    public short getReturnValueS() {
        if (this.returnValue == null) {
            return 0;
        }
        return (Short) this.returnValue;
    }

    public boolean getReturnValueZ() {
        if (this.returnValue == null) {
            return false;
        }
        return (Boolean) this.returnValue;
    }
}
