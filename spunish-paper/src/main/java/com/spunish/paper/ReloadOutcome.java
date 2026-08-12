package com.spunish.paper;

import java.util.List;

public record ReloadOutcome(boolean ok, List<String> errors) {

    public static ReloadOutcome success() {
        return new ReloadOutcome(true, List.of());
    }

    public static ReloadOutcome failure(List<String> errors) {
        return new ReloadOutcome(false, errors);
    }
}
