package dev.bedrockify.forge.client.gui;

/** Pure row geometry; screens retain their own selection and drag lifecycle. */
public record GridViewport(int columns, int rows) {
    public GridViewport {
        if (columns < 1 || rows < 1) throw new IllegalArgumentException("Grid dimensions must be positive");
    }
    public int maxRow(int entries) { return Math.max(0, (entries + columns - 1) / columns - rows); }
    public int clamp(int row, int entries) { return Math.max(0, Math.min(row, maxRow(entries))); }
    public int scroll(int row, double amount, int entries) { return clamp(row - (int) Math.signum(amount), entries); }
    public int drag(double mouseY, int trackTop, int trackHeight, int thumbHeight, int entries) {
        float fraction = Math.max(0, Math.min(1, (float) (mouseY - trackTop - thumbHeight / 2.0)
                / Math.max(1, trackHeight - thumbHeight)));
        return Math.round(fraction * maxRow(entries));
    }
    public int thumb(int row, int entries, int trackHeight, int thumbHeight) {
        int maximum = maxRow(entries);
        return maximum == 0 ? 0 : Math.round(Math.max(0, trackHeight - thumbHeight) * (float) clamp(row, entries) / maximum);
    }
}
