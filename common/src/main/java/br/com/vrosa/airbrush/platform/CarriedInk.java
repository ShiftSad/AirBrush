package br.com.vrosa.airbrush.platform;

import org.jetbrains.annotations.NotNull;

/** Ink carried over from a quill consumed in a craft to the resulting quill. */
public record CarriedInk(@NotNull String inkId, int rgb, int charge) {}
