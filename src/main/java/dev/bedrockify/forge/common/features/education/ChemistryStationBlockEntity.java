package dev.bedrockify.forge.common.features.education;

import dev.bedrockify.forge.platform.item.ItemStackData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.*;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

public final class ChemistryStationBlockEntity extends BlockEntity implements Inventory,NamedScreenHandlerFactory {
    public final StationAccess access = new StationAccess();
    private final DefaultedList<ItemStack> stacks=DefaultedList.ofSize(10,ItemStack.EMPTY);
    private final int[] particles={1,0,1};
    private int reaction;
    public final PropertyDelegate properties=new PropertyDelegate() {
        public int size(){return 4;} public int get(int n){return n==3?reaction:particles[n];}
        public void set(int n,int value){if(n==3)reaction=value;else particles[n]=Math.max(0,Math.min(n==1?200:118,value));markDirty();}
    };
    public ChemistryStationBlockEntity(BlockPos pos,BlockState state){super(EducationContent.STATION_ENTITY,pos,state);}
    public String kind(){return ((ChemistryStationBlock)getCachedState().getBlock()).kind;}
    /** Disposable menu preview: no consumption and no fake output in block saves. */
    public ItemStack previewOutput() {
        if (world == null || world.isClient) return ItemStack.EMPTY;
        if (!getStack(9).isEmpty()) return getStack(9).copy();
        if (kind().equals("element_constructor")) {
            if (particles[0] < 1 || particles[0] != particles[2]) return ItemStack.EMPTY;
            ItemStack output = EducationContent.element(particles[0], 1);
            ItemStackData.setNeutrons(output, particles[1]);
            return output;
        }
        if (kind().equals("lab_table")) return ItemStack.EMPTY;
        return world.getRecipeManager().getAllMatches(ChemistryRecipe.TYPE, this, world).stream()
                .filter(recipe -> recipe.station.equals(kind())).findFirst()
                .map(recipe -> recipe.craft(this, world.getRegistryManager())).orElse(ItemStack.EMPTY);
    }
    public boolean combine() {
        if(world==null || world.isClient || !getStack(9).isEmpty()) return false;
        ItemStack output;
        if(kind().equals("element_constructor")) {
            if(particles[0]<1 || particles[0]!=particles[2]) return false;
            output=EducationContent.element(particles[0],1);
            ItemStackData.setNeutrons(output,particles[1]);
        } else {
            var recipe=world.getRecipeManager().getAllMatches(ChemistryRecipe.TYPE,this,world).stream().filter(r->r.station.equals(kind())).findFirst();
            if(recipe.isPresent()) {output=recipe.get().craft(this,world.getRegistryManager());recipe.get().consume(this);}
            else {
                if(!kind().equals("lab_table")) return false;
                boolean any=false;for(int i=0;i<9;i++) any|=!getStack(i).isEmpty();if(!any) return false;
                output=new ItemStack(EducationContent.ITEMS.get("garbage"));for(int i=0;i<9;i++) stacks.set(i,ItemStack.EMPTY);
            }
        }
        stacks.set(9,output);markDirty();return true;
    }
    public boolean runExperiment() {
        if (!kind().equals("lab_table") || !combine()) return false;
        ItemScatterer.spawn(world,pos.getX()+0.5,pos.getY()+1.1,pos.getZ()+0.5,removeStack(9));
        reaction=(reaction+1)&32767;
        return true;
    }
    @Override public int size(){return 10;}
    @Override public boolean isEmpty(){return stacks.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getStack(int slot){return stacks.get(slot);}
    @Override public ItemStack removeStack(int slot,int count){var result=Inventories.splitStack(stacks,slot,count);if(!result.isEmpty())markDirty();return result;}
    @Override public ItemStack removeStack(int slot){var result=Inventories.removeStack(stacks,slot);markDirty();return result;}
    @Override public void setStack(int slot,ItemStack stack){stacks.set(slot,stack);stack.setCount(Math.min(stack.getCount(),getMaxCountPerStack()));markDirty();}
    @Override public void clear(){stacks.clear();markDirty();}
    @Override public boolean canPlayerUse(PlayerEntity player){return world!=null && world.getBlockEntity(pos)==this && player.squaredDistanceTo(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5)<=64;}
    @Override public boolean isValid(int slot,ItemStack stack){return slot<9 && !kind().equals("element_constructor") && (!kind().equals("compound_creator") || stack.getItem() instanceof ElementBlockItem);}
    @Override public void readNbt(NbtCompound nbt){super.readNbt(nbt);stacks.clear();Inventories.readNbt(nbt,stacks);for(int i=0;i<3;i++)properties.set(i,nbt.getInt("Particle"+i));}
    @Override protected void writeNbt(NbtCompound nbt){super.writeNbt(nbt);Inventories.writeNbt(nbt,stacks);for(int i=0;i<3;i++)nbt.putInt("Particle"+i,particles[i]);}
    @Override public Text getDisplayName(){return getCachedState().getBlock().getName();}
    @Override public ScreenHandler createMenu(int id,PlayerInventory inventory,PlayerEntity player){return new ChemistryStationScreenHandler(id,inventory,this,kind());}
    public void dropContents(){ItemScatterer.spawn(world,pos,this);clear();}
}
