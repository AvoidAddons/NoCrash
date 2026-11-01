package pl.olafcio.nocrash.crashpad.message;

public class QuickFixFailed extends RuntimeException {
    private final int opcode;

    public QuickFixFailed(String message, int opcode) {
        super(message);
        this.opcode = opcode;
    }

    public QuickFixFailed(String message, int opcode, Throwable parent) {
        super(message, parent);
        this.opcode = opcode;
    }

    public int getOpcode() {
        return opcode;
    }
}
