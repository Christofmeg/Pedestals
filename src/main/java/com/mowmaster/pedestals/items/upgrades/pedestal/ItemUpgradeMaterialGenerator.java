package com.mowmaster.pedestals.items.upgrades.pedestal;

import com.mowmaster.mowlib.Capabilities.Dust.DustMagic;
import com.mowmaster.mowlib.Capabilities.Dust.IDustHandler;
import com.mowmaster.mowlib.MowLibUtils.MowLibCompoundTagUtils;
import com.mowmaster.mowlib.MowLibUtils.MowLibReferences;
import com.mowmaster.mowlib.Networking.MowLibPacketHandler;
import com.mowmaster.mowlib.Networking.MowLibPacketParticles;
import com.mowmaster.pedestals.blocks.pedestal.BasePedestalBlockEntity;
import com.mowmaster.pedestals.configs.PedestalConfig;
import com.mowmaster.pedestals.pedestalutils.References;
import com.mowmaster.pedestals.recipes.CobbleGenRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.lang.ref.WeakReference;
import java.util.*;

public class ItemUpgradeMaterialGenerator extends ItemUpgradeBase {

    public ItemUpgradeMaterialGenerator(Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public boolean canModifySpeed(ItemStack upgradeItemStack) { return true; }

    @Override
    public boolean canModifyItemCapacity(ItemStack upgradeItemStack) { return true; }

    @Override
    public int getUpgradeWorkRange(ItemStack coinUpgrade) { return 0; }

    @Override
    public ItemStack getUpgradeDefaultTool() { return new ItemStack(Items.STONE_PICKAXE); }

    public List<ItemStack> getItemsToGenerate(Level level, BasePedestalBlockEntity pedestal, BlockPos pedestalPos, ItemStack blockToBreak) {
        Block generatedBlock = Block.byItem(blockToBreak.getItem());
        if(generatedBlock != Blocks.AIR) {
            ItemStack toolStack = pedestal.getToolStack();

            LootContext.Builder builder = new LootContext.Builder((ServerLevel) level)
                    .withRandom(level.random)
                    .withParameter(LootContextParams.ORIGIN, new Vec3(pedestalPos.getX(), pedestalPos.getY(), pedestalPos.getZ()))
                    .withParameter(LootContextParams.TOOL, toolStack);

            WeakReference<FakePlayer> fakePlayerReference = pedestal.getPedestalPlayer(pedestal);
            if (fakePlayerReference != null && fakePlayerReference.get() != null) {
                builder.withOptionalParameter(LootContextParams.THIS_ENTITY, fakePlayerReference.get());
            }
            return generatedBlock.defaultBlockState().getDrops(builder);
        }

        return new ArrayList<>();
    }

    public void resetCachedRecipe(ItemStack upgrade) {
        CompoundTag tag = upgrade.getOrCreateTag();
        MowLibCompoundTagUtils.removeCustomTagFromNBT(References.MODID, tag, "_cobblegen_result");
        MowLibCompoundTagUtils.removeCustomTagFromNBT(References.MODID, tag, "_fluidStack");
        MowLibCompoundTagUtils.removeCustomTagFromNBT(References.MODID, tag, "_energyNeeded");
        MowLibCompoundTagUtils.removeCustomTagFromNBT(References.MODID, tag, "_xpNeeded");
        MowLibCompoundTagUtils.removeCustomTagFromNBT(MowLibReferences.MODID, tag, "_dustMagicColor");
        MowLibCompoundTagUtils.removeCustomTagFromNBT(MowLibReferences.MODID, tag, "_dustMagicAmount");
        MowLibCompoundTagUtils.removeCustomTagFromNBT(References.MODID, tag, "_cobblegen_cached");
        // TODO [1.20]: get rid of these lines as they remove the previous NBT tags used.
        MowLibCompoundTagUtils.removeCustomTagFromNBT(References.MODID, tag, "_stackList");
    }

    public void lookupAndCacheCobbleGenResult(Level level, ItemStack input, ItemStack upgrade) {
        Container container = new SimpleContainer(input);
        Optional<CobbleGenRecipe> result = level.getRecipeManager().getRecipeFor(CobbleGenRecipe.Type.INSTANCE, container, level);
        CompoundTag tag = upgrade.getOrCreateTag();
        if (result.isPresent()) {
            CobbleGenRecipe recipe = result.get();
            MowLibCompoundTagUtils.writeItemStackToNBT(References.MODID, tag, recipe.getResultItem(),"_cobblegen_result");
            MowLibCompoundTagUtils.writeFluidStackToNBT(References.MODID, tag, recipe.getResultFluidNeeded(),"_fluidStack");
            MowLibCompoundTagUtils.writeIntegerToNBT(References.MODID, tag, recipe.getResultEnergyNeeded(), "_energyNeeded");
            MowLibCompoundTagUtils.writeIntegerToNBT(References.MODID, tag, recipe.getResultExperienceNeeded(), "_xpNeeded");
            DustMagic.setDustMagicInTag(tag, recipe.getResultDustNeeded());
        }
        // even if there was no recipe, denote we've done the lookup since we already reset this when the block below changes
        MowLibCompoundTagUtils.writeBooleanToNBT(References.MODID, tag, true, "_cobblegen_cached");
    }

    @Override
    public void actionOnNeighborBelowChange(BasePedestalBlockEntity pedestal, BlockPos belowBlock) {
        resetCachedRecipe(pedestal.getCoinOnPedestal());
    }

    @Override
    public void actionOnRemovedFromPedestal(BasePedestalBlockEntity pedestal, ItemStack coinInPedestal) {
        resetCachedRecipe(coinInPedestal);
    }

    private ItemStack getGeneratorRecipeResult(Level level, BasePedestalBlockEntity pedestal, BlockPos pedestalPos, ItemStack upgrade) {
        CompoundTag tag = upgrade.getOrCreateTag();
        if (!MowLibCompoundTagUtils.readBooleanFromNBT(References.MODID, tag, "_cobblegen_cached")) {
            BlockPos posBelow = getPosOfBlockBelow(level, pedestalPos, 1);
            lookupAndCacheCobbleGenResult(level, new ItemStack(level.getBlockState(posBelow).getBlock().asItem()), upgrade);
        }
        return MowLibCompoundTagUtils.readItemStackFromNBT(References.MODID, tag, "_cobblegen_result");
    }

    private int getGeneratorMultiplier(BasePedestalBlockEntity pedestal, ItemStack upgrade, FluidStack fluidStackNeeded, int energyNeeded, int experienceNeeded, DustMagic dustNeeded) {
        int multiplier = Math.max(1, getItemCapacityIncrease(upgrade));
        if (!fluidStackNeeded.isEmpty()) {
            FluidStack storedFluid = pedestal.getStoredFluid();
            if (!storedFluid.isFluidEqual(fluidStackNeeded)) {
                multiplier = 0;
            } else {
                multiplier = Math.min(multiplier, storedFluid.getAmount() / fluidStackNeeded.getAmount());
            }
        }
        if (energyNeeded > 0) {
            multiplier = Math.min(multiplier, pedestal.getStoredEnergy() / energyNeeded);
        }
        if (experienceNeeded > 0) {
            multiplier = Math.min(multiplier, pedestal.getStoredExperience() / experienceNeeded);
        }
        if (!dustNeeded.isEmpty()) {
            DustMagic storedDust = pedestal.getStoredDust();
            if (storedDust.getDustColor() != dustNeeded.getDustColor()) {
                multiplier = 0;
            } else {
                multiplier = Math.min(multiplier, storedDust.getDustAmount() / dustNeeded.getDustAmount());
            }
        }
        if (PedestalConfig.COMMON.cobbleGeneratorDamageTools.get()) {
            multiplier = Math.min(multiplier, pedestal.getDurabilityRemainingOnInsertedTool());
        }
        return multiplier;
    }

    private void consumeFuel(BasePedestalBlockEntity pedestal, FluidStack fluidStackNeeded, int energyNeeded, int experienceNeeded, DustMagic dustNeeded, int multiplier) {
        if (!fluidStackNeeded.isEmpty()) {
            FluidStack toRemove = fluidStackNeeded.copy();
            toRemove.setAmount(fluidStackNeeded.getAmount() * multiplier);
            pedestal.removeFluid(toRemove, IFluidHandler.FluidAction.EXECUTE);
        }
        if (energyNeeded > 0) {
            pedestal.removeEnergy(energyNeeded * multiplier, false);
        }
        if (experienceNeeded > 0) {
            pedestal.removeExperience(experienceNeeded * multiplier, false);
        }
        if (!dustNeeded.isEmpty()) {
            DustMagic toRemove = dustNeeded.copy();
            toRemove.setDustAmount(dustNeeded.getDustColor() * multiplier);
            pedestal.removeDust(toRemove, IDustHandler.DustAction.EXECUTE);
        }
        if (PedestalConfig.COMMON.cobbleGeneratorDamageTools.get()) {
            pedestal.damageInsertedTool(multiplier, false);
        }
    }

    @Override
    public void upgradeAction(Level level, BasePedestalBlockEntity pedestal, BlockPos pedestalPos, ItemStack coin) {
        ItemStack recipeResult = getGeneratorRecipeResult(level, pedestal, pedestalPos, coin);
        if (recipeResult.isEmpty()) {
            if (pedestal.canSpawnParticles()) {
                MowLibPacketHandler.sendToNearby(level, pedestalPos, new MowLibPacketParticles(MowLibPacketParticles.EffectType.ANY_COLOR_CENTERED, pedestalPos.getX(), pedestalPos.getY() + 1.0f, pedestalPos.getZ(), 50, 50, 50));
            }
            return;
        }

        List<ItemStack> getCobbleGenOutputs = getItemsToGenerate(level, pedestal, pedestalPos, recipeResult);
        if (getCobbleGenOutputs.isEmpty()) {
            return;
        }

        FluidStack fluidStackNeeded = MowLibCompoundTagUtils.readFluidStackFromNBT(References.MODID,coin.getTag(),"_fluidStack");
        int energyNeeded = MowLibCompoundTagUtils.readIntegerFromNBT(References.MODID,coin.getTag(),"_energyNeeded");
        int experienceNeeded = MowLibCompoundTagUtils.readIntegerFromNBT(References.MODID,coin.getTag(),"_xpNeeded");
        DustMagic dustNeeded = DustMagic.getDustMagicInTag(coin.getTag());

        int multiplier = getGeneratorMultiplier(pedestal, coin, fluidStackNeeded, energyNeeded, experienceNeeded, dustNeeded);
        if (multiplier == 0) {
            if(pedestal.canSpawnParticles()) {
                MowLibPacketHandler.sendToNearby(level, pedestalPos, new MowLibPacketParticles(MowLibPacketParticles.EffectType.ANY_COLOR_CENTERED, pedestalPos.getX(), pedestalPos.getY() + 1.0f, pedestalPos.getZ(), 255, 255, 255));
            }
            return;
        }

        for (ItemStack output : getCobbleGenOutputs) {
            if (output.isEmpty()) continue;

            if (pedestal.hasSpaceForItem(output)) {
                int numToAdd = Math.min(output.getCount() * multiplier, output.getMaxStackSize());
                ItemStack toAdd = output.copy();
                toAdd.setCount(numToAdd);
                ItemStack remainder = pedestal.addItemStack(toAdd, false);
                int fuelConsumedMultiplier = (int)Math.ceil((double)(toAdd.getCount() - remainder.getCount()) / output.getCount());
                consumeFuel(pedestal, fluidStackNeeded, energyNeeded, experienceNeeded, dustNeeded, fuelConsumedMultiplier);
            }
        }
    }

}
