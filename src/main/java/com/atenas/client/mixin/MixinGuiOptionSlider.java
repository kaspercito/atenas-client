package com.atenas.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.atenas.client.gui.SliderValue;

import net.minecraft.client.gui.GuiOptionSlider;

/** Deja leer el valor del deslizador vanilla (Campo de vision, sensibilidad, brillo...) para dibujarlo con el estilo Atenas. */
@Mixin(GuiOptionSlider.class)
public abstract class MixinGuiOptionSlider implements SliderValue {
    @Shadow private float sliderValue;

    @Override
    public float atenas$sliderValue() { return sliderValue; }
}
