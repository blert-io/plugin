package com.blert.json;

import com.blert.raid.Hitpoints;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.annotation.Nullable;

@RequiredArgsConstructor
@Getter
public class Npc {
    final private int id;
    final private int roomId;
    @Setter
    private @Nullable Hitpoints hitpoints = null;
}
