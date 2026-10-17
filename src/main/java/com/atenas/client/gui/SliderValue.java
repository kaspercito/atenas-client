package com.atenas.client.gui;

/** Lo implementa (por mixin) el deslizador vanilla GuiOptionSlider para poder leer su valor (0..1) y dibujarlo con el estilo Atenas. */
public interface SliderValue {
    float atenas$sliderValue();
}
