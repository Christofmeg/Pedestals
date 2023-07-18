package com.mowmaster.pedestals.items.upgrades.pedestal;

import com.mowmaster.mowlib.Capabilities.Dust.DustMagic;
import com.mowmaster.mowlib.Capabilities.Dust.IDustHandler;
import com.mowmaster.pedestals.blocks.pedestal.BasePedestalBlockEntity;
import com.mowmaster.pedestals.items.mechanicalonlystorage.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

public class ItemUpgradeUnPackager extends ItemUpgradeBase implements IHasModeTypes
{
    public ItemUpgradeUnPackager(Properties p_41383_) {
        super(new Properties());
    }

    @Override
    public boolean canModifySpeed(ItemStack upgradeItemStack) {
        return true;
    }

    @Override
    public int getUpgradeWorkRange(ItemStack coinUpgrade) { return 0; }

    @Override
    public void updateAction(Level world, BasePedestalBlockEntity pedestal) {
        if(pedestal.getItemInPedestal().getItem() instanceof BaseItemBulkStorageItem
                || pedestal.getItemInPedestal().getItem() instanceof BaseFluidBulkStorageItem
                || pedestal.getItemInPedestal().getItem() instanceof BaseEnergyBulkStorageItem
                || pedestal.getItemInPedestal().getItem() instanceof BaseXpBulkStorageItem
                || pedestal.getItemInPedestal().getItem() instanceof BaseDustBulkStorageItem)
        {
            super.updateAction(world, pedestal);
        }
    }

    @Override
    public void upgradeAction(Level level, BasePedestalBlockEntity pedestal, BlockPos pedestalPos, ItemStack coin)
    {
        ItemStack stackInPed = pedestal.getItemInPedestal();
        ItemStack initialStackCopy = stackInPed.copy();
        int fluidSpace = pedestal.spaceForFluid();
        int energySpace = pedestal.spaceForEnergy();
        int xpSpace = pedestal.spaceForExperience();
        int dustSpace = pedestal.spaceForDust();

        if(stackInPed.getItem() instanceof BaseItemBulkStorageItem packagedItems && canTransferItems(coin))
        {
            List<ItemStack> newList = new ArrayList<>();
            List<ItemStack> stacks = packagedItems.getStacksList(stackInPed);
            if(stacks.size()>0)
            {
                if(pedestal.addItemStack(stacks.get(0),true).isEmpty())
                {
                    for(ItemStack stack:stacks)
                    {
                        if(pedestal.addItem(stack,true))
                        {
                            pedestal.addItem(stack,false);
                        }
                        else
                        {
                            newList.add(stack);
                        }
                    }

                    if(newList.size()>0)
                    {
                        packagedItems.setStacksList(initialStackCopy,newList);
                        pedestal.removeItemStack(stackInPed,false);
                        pedestal.addItemStack(initialStackCopy,false);
                    }
                    else
                    {
                        packagedItems.setStacksList(initialStackCopy,new ArrayList<>());
                        pedestal.removeItemStack(stackInPed,false);
                    }
                }
            }
            else
            {
                pedestal.removeItemStack(stackInPed,false);
            }
        }
        if(stackInPed.getItem() instanceof BaseFluidBulkStorageItem fluidItem && canTransferFluids(coin))
        {
            if(fluidSpace>0)
            {
                FluidStack fluidInItemCopy = fluidItem.getFluid(stackInPed).copy();
                if(pedestal.addFluid(fluidInItemCopy, IFluidHandler.FluidAction.SIMULATE)>0)
                {
                    if(!pedestal.removeItemStack(stackInPed,true).isEmpty())
                    {
                        //extract Fluid
                        int toRemove = pedestal.addFluid(fluidInItemCopy, IFluidHandler.FluidAction.EXECUTE);
                        if(toRemove < fluidInItemCopy.getAmount())
                        {
                            fluidItem.removeFluid(stackInPed,toRemove);
                        }
                        else
                        {
                            pedestal.removeItemStack(stackInPed,false);
                        }

                        stackInPed = pedestal.getItemInPedestal().copy();
                    }
                }
            }
        }
        if(stackInPed.getItem() instanceof BaseEnergyBulkStorageItem energyItem && canTransferEnergy(coin))
        {
            if(energySpace>0)
            {
                int energyInItem = energyItem.getEnergy(stackInPed);
                if(pedestal.addEnergy(energyInItem,true)>0)
                {
                    if(!pedestal.removeItemStack(stackInPed,true).isEmpty())
                    {
                        //extract Fluid
                        int toRemove = pedestal.addEnergy(energyInItem,false);
                        if(toRemove < energyInItem)
                        {
                            energyItem.removeEnergy(stackInPed,toRemove);
                        }
                        else
                        {
                            pedestal.removeItemStack(stackInPed,false);
                        }

                        stackInPed = pedestal.getItemInPedestal().copy();
                    }
                }
            }
        }
        if(stackInPed.getItem() instanceof BaseXpBulkStorageItem xpItem && canTransferXP(coin))
        {
            if(xpSpace>0)
            {
                int xpInItem = xpItem.getXp(stackInPed);
                if(pedestal.addExperience(xpInItem,true)>0)
                {
                    if(!pedestal.removeItemStack(stackInPed,true).isEmpty())
                    {
                        //extract Fluid
                        int toRemove = pedestal.addExperience(xpInItem,false);
                        if(toRemove < xpInItem)
                        {
                            xpItem.removeXp(stackInPed,toRemove);
                        }
                        else
                        {
                            pedestal.removeItemStack(stackInPed,false);
                        }

                        stackInPed = pedestal.getItemInPedestal().copy();
                    }
                }
            }
        }
        if(stackInPed.getItem() instanceof BaseDustBulkStorageItem && canTransferDust(coin))
        {
            if(dustSpace>0)
            {
                DustMagic dustInItemCopy = DustMagic.getDustMagicInItemStack(stackInPed).copy();
                if(pedestal.addDust(dustInItemCopy, IDustHandler.DustAction.SIMULATE)>0)
                {
                    if(!pedestal.removeItemStack(stackInPed,true).isEmpty())
                    {
                        //extract Fluid
                        int toRemove = pedestal.addDust(dustInItemCopy, IDustHandler.DustAction.EXECUTE);
                        if(toRemove < dustInItemCopy.getDustAmount())
                        {
                            DustMagic.setDustMagicInStack(stackInPed,new DustMagic(dustInItemCopy.getDustColor(),(dustInItemCopy.getDustAmount()-toRemove)));
                        }
                        else
                        {
                            pedestal.removeItemStack(stackInPed,false);
                        }
                    }
                }
            }
        }
    }
}
