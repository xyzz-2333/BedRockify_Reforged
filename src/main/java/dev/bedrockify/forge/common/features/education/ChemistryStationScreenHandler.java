package dev.bedrockify.forge.common.features.education;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
public final class ChemistryStationScreenHandler extends ScreenHandler {
    public final String kind;
    private final Inventory inventory;
    private final ChemistryStationBlockEntity station;
    private final PropertyDelegate properties;
    public ChemistryStationScreenHandler(int id,PlayerInventory player,String kind){this(id,player,null,kind);}
    public ChemistryStationScreenHandler(int id,PlayerInventory player,ChemistryStationBlockEntity station,String kind){
        super(EducationContent.STATION_MENU,id);this.station=station;this.kind=kind;
        inventory=station==null?new SimpleInventory(10):station;properties=station==null?new ArrayPropertyDelegate(3):station.properties;addProperties(properties);
        for(int i=0;i<9;i++)addSlot(new Slot(inventory,i,16+i%3*20,28+i/3*20){
            @Override public boolean canInsert(ItemStack stack){return !kind.equals("element_constructor") && (!kind.equals("compound_creator") || stack.getItem() instanceof ElementBlockItem);}
            @Override public boolean isEnabled(){return !kind.equals("element_constructor");}
        });
        addSlot(new Slot(inventory,9,158,48){@Override public boolean canInsert(ItemStack stack){return false;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(player,col+row*9+9,12+col*20,118+row*20));
        for(int col=0;col<9;col++)addSlot(new Slot(player,col,12+col*20,184));
    }
    public int particle(int n){return properties.get(n);}
    @Override public boolean canUse(PlayerEntity player){return inventory.canPlayerUse(player);}
    @Override public boolean onButtonClick(PlayerEntity player,int id){
        if(station==null || !canUse(player))return false;
        if(id==0)return station.combine();
        if(!kind.equals("element_constructor") || id<1 || id>6)return false;
        int n=(id-1)/2;properties.set(n,properties.get(n)+(id%2==0?1:-1));return true;
    }
    @Override public ItemStack quickMove(PlayerEntity player,int index){
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasStack())return ItemStack.EMPTY;
        var stack=slot.getStack();var before=stack.copy();
        if(index<10){if(!insertItem(stack,10,46,true))return ItemStack.EMPTY;}
        else if(!kind.equals("element_constructor") && slots.get(0).canInsert(stack)){if(!insertItem(stack,0,9,false))return ItemStack.EMPTY;}
        else if(index<37){if(!insertItem(stack,37,46,false))return ItemStack.EMPTY;}
        else if(!insertItem(stack,10,37,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setStack(ItemStack.EMPTY);else slot.markDirty();slot.onTakeItem(player,stack);return before;
    }
}
