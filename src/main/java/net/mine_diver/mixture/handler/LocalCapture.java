package net.mine_diver.mixture.handler;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public enum LocalCapture {

    NO_CAPTURE(false, false),
    PRINT(false, true),
    CAPTURE_FAILHARD;

    boolean
            captureLocals,
            printLocals;

    LocalCapture() {
        this(true, false);
    }
}
