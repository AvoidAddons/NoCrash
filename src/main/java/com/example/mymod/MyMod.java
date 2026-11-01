package com.example.mymod;

import pl.olafcio.avoid.mods.AvoidMod;

public class MyMod extends AvoidMod {
    @Override
    public void onEnable() {
        IO.println("MyMod enabled");
    }
}
