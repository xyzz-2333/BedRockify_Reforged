package dev.bedrockify.forge.common.features.education;

/** Logical GUI pixels shared by menu slots and their client decoration. */
public record ChemistryLayout(int width, int height, int inventoryX, int inventoryY, int hotbarY) {
    public static final ChemistryLayout ELEMENT = new ChemistryLayout(250, 220, 84, 141, 197);
    public static final ChemistryLayout COMPOUND = new ChemistryLayout(184, 166, 12, 86, 143);
    public static final ChemistryLayout LAB = new ChemistryLayout(176, 200, 8, 122, 179);
    public static final ChemistryLayout REDUCER = new ChemistryLayout(205, 200, 22, 111, 168);
    public static ChemistryLayout of(String kind) {
        return switch (kind) { case "element_constructor" -> ELEMENT; case "compound_creator" -> COMPOUND; default -> LAB; };
    }
    public static int particleLimit(int index) { return index == 1 ? 200 : 118; }
    public static int particleButton(int index, int value) { return 16 + index * 256 + value; }
}
