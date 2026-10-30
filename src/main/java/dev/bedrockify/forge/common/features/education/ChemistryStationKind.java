package dev.bedrockify.forge.common.features.education;

import net.minecraft.item.ItemStack;

/** Internal station rules; external registration, packet and recipe IDs remain unchanged. */
public enum ChemistryStationKind {
    ELEMENT_CONSTRUCTOR("element_constructor"), COMPOUND_CREATOR("compound_creator"), LAB_TABLE("lab_table");

    private final String id;
    ChemistryStationKind(String id) { this.id = id; }
    public String id() { return id; }
    public ChemistryLayout layout() {
        return switch (this) {
            case ELEMENT_CONSTRUCTOR -> ChemistryLayout.ELEMENT;
            case COMPOUND_CREATOR -> ChemistryLayout.COMPOUND;
            case LAB_TABLE -> ChemistryLayout.LAB;
        };
    }
    public boolean accepts(ItemStack stack) {
        return this == LAB_TABLE || this == COMPOUND_CREATOR && stack.getItem() instanceof ElementBlockItem;
    }
    public static ChemistryStationKind fromId(String id) {
        for (var kind : values()) if (kind.id.equals(id)) return kind;
        throw new IllegalArgumentException("Unknown chemistry station: " + id);
    }
}
