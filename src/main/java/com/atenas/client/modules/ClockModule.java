package com.atenas.client.modules;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.atenas.client.core.TextHud;

public class ClockModule extends TextHud {
    private final SimpleDateFormat format = new SimpleDateFormat("HH:mm");

    public ClockModule() {
        super("Reloj", "Muestra la hora real de tu PC.", 4, 112);
    }

    @Override
    protected long refreshMs() { return 1000L; }

    @Override
    protected String getText() { return format.format(new Date()); }
}
