package com.blert.raid;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Hitpoints {
    private int current;
    private int max;

    public String toString() {
        return String.valueOf(current) + "/" + max;
    }
}
