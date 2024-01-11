package com.blert.json;

import com.blert.raid.Hitpoints;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

@Getter
@Setter
public class Npc {
    private int id;
    private int roomId;
    private @Nullable Hitpoints hitpoints = null;
}
