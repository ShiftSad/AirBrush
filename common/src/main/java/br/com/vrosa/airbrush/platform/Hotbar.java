package br.com.vrosa.airbrush.platform;

public final class Hotbar {

    public static int scrollDirection(int previous, int next) {
        int raw = next - previous;
        if (raw > 4) raw -= 9;
        if (raw < -4) raw += 9;
        return Integer.signum(raw);
    }

    private Hotbar() {}
}
