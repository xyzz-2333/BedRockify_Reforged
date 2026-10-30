package dev.bedrockify.forge.common.features.education;

/** Logical GUI pixels shared by menu slots and their client decoration. */
public record ChemistryLayout(int width, int height, int inventoryX, int inventoryY, int hotbarY,
                              int inputX, int inputY, int inputColumns, int outputX, int outputY) {
    public static final ChemistryLayout ELEMENT = new ChemistryLayout(250, 220, 84, 141, 197, 54, 25, 3, 212, 90);
    public static final ChemistryLayout COMPOUND = new ChemistryLayout(184, 166, 12, 86, 143, 54, 25, 3, 138, 43);
    public static final ChemistryLayout LAB = new ChemistryLayout(176, 200, 8, 122, 179, 5, 69, 9, 80, 40);
    public static final ChemistryLayout REDUCER = new ChemistryLayout(205, 200, 22, 111, 168, 94, 18, 1, 42, 53);
    public static ChemistryLayout of(String kind) { return ChemistryStationKind.fromId(kind).layout(); }
    public int inputSlotX(int index) { return inputX + index % inputColumns * 18; }
    public int inputSlotY(int index) { return inputY + index / inputColumns * 18; }
    public static int particleLimit(int index) { return index == 1 ? 200 : 118; }
    public static int particleButton(int index, int value) { return 16 + index * 256 + value; }
}
