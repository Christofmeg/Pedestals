package com.mowmaster.pedestals.Items.Upgrades.Pedestal;

import com.mowmaster.mowlib.Capabilities.Dust.DustMagic;
import com.mowmaster.mowlib.MowLibUtils.MowLibCompoundTagUtils;
import com.mowmaster.mowlib.Networking.MowLibPacketHandler;
import com.mowmaster.mowlib.Networking.MowLibPacketParticles;
import com.mowmaster.pedestals.Blocks.Pedestal.BasePedestalBlockEntity;
import com.mowmaster.pedestals.Configs.PedestalConfig;
import com.mowmaster.pedestals.Items.Filters.BaseFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.mowmaster.pedestals.PedestalUtils.References.MODID;

public class ItemUpgradePlanter extends ItemUpgradeBase implements ISelectablePoints, ISelectableArea
{
    public ItemUpgradePlanter(Properties p_41383_) {
        super(new Properties());
    }

    //Requires energy

    @Override
    public int baseEnergyCostPerDistance(){ return PedestalConfig.COMMON.upgrade_planter_baseEnergyCost.get(); }
    @Override
    public boolean energyDistanceAsModifier() {return PedestalConfig.COMMON.upgrade_planter_energy_distance_multiplier.get();}
    @Override
    public double energyCostMultiplier(){ return PedestalConfig.COMMON.upgrade_planter_energyMultiplier.get(); }

    @Override
    public int baseXpCostPerDistance(){ return PedestalConfig.COMMON.upgrade_planter_baseXpCost.get(); }
    @Override
    public boolean xpDistanceAsModifier() {return PedestalConfig.COMMON.upgrade_planter_xp_distance_multiplier.get();}
    @Override
    public double xpCostMultiplier(){ return PedestalConfig.COMMON.upgrade_planter_xpMultiplier.get(); }

    @Override
    public DustMagic baseDustCostPerDistance(){ return new DustMagic(PedestalConfig.COMMON.upgrade_planter_dustColor.get(),PedestalConfig.COMMON.upgrade_planter_baseDustAmount.get()); }
    @Override
    public boolean dustDistanceAsModifier() {return PedestalConfig.COMMON.upgrade_planter_dust_distance_multiplier.get();}
    @Override
    public double dustCostMultiplier(){ return PedestalConfig.COMMON.upgrade_planter_dustMultiplier.get(); }

    @Override
    public boolean hasSelectedAreaModifier() { return PedestalConfig.COMMON.upgrade_planter_selectedAllowed.get(); }
    @Override
    public double selectedAreaCostMultiplier(){ return PedestalConfig.COMMON.upgrade_planter_selectedMultiplier.get(); }

    private void buildValidBlockList(BasePedestalBlockEntity pedestal)
    {
        ItemStack coin = pedestal.getCoinOnPedestal();
        List<BlockPos> listed = readBlockPosListFromNBT(coin);
        List<BlockPos> valid = new ArrayList<>();
        for (BlockPos pos:listed) {
            if(selectedPointWithinRange(pedestal, pos))
            {
                valid.add(pos);
            }
        }

        saveBlockPosListCustomToNBT(coin,"_validlist",valid);
    }

    private void buildValidBlockListArea(BasePedestalBlockEntity pedestal)
    {
        ItemStack coin = pedestal.getCoinOnPedestal();
        List<BlockPos> valid = new ArrayList<>();
        AABB area = new AABB(readBlockPosFromNBT(pedestal.getCoinOnPedestal(),1),readBlockPosFromNBT(pedestal.getCoinOnPedestal(),2));

        int maxX = (int)area.maxX;
        int maxY = (int)area.maxY;
        int maxZ = (int)area.maxZ;

        //System.out.println("aabbMaxStuff: "+ maxX+","+maxY+","+maxZ);

        int minX = (int)area.minX;
        int minY = (int)area.minY;
        int minZ = (int)area.minZ;

        //System.out.println("aabbMinStuff: "+ minX+","+minY+","+minZ);

        BlockPos pedestalPos = pedestal.getPos();
        if(minY < pedestalPos.getY())
        {
            for(int i=maxX;i>=minX;i--)
            {
                for(int j=maxZ;j>=minZ;j--)
                {
                    for(int k=maxY;k>=minY;k--)
                    {
                        BlockPos newPoint = new BlockPos(i,k,j);
                        //System.out.println("points: "+ newPoint);
                        if(selectedPointWithinRange(pedestal, newPoint))
                        {
                            valid.add(newPoint);
                        }
                    }
                }
            }
        }
        else
        {
            for(int i= minX;i<=maxX;i++)
            {
                for(int j= minZ;j<=maxZ;j++)
                {
                    for(int k= minY;k<=maxY;k++)
                    {
                        BlockPos newPoint = new BlockPos(i,k,j);
                        //System.out.println("points2: "+ newPoint);
                        if(selectedPointWithinRange(pedestal, newPoint))
                        {
                            valid.add(newPoint);
                        }
                    }
                }
            }
        }

        //System.out.println("validList: "+ valid);
        saveBlockPosListCustomToNBT(coin,"_validlist",valid);
    }

    private List<BlockPos> getValidList(BasePedestalBlockEntity pedestal)
    {
        ItemStack coin = pedestal.getCoinOnPedestal();
        return readBlockPosListCustomFromNBT(coin,"_validlist");
    }

    @Override
    public void actionOnRemovedFromPedestal(BasePedestalBlockEntity pedestal, ItemStack coinInPedestal) {
        super.actionOnRemovedFromPedestal(pedestal, coinInPedestal);
        removeBlockListCustomNBTTags(coinInPedestal, "_validlist");
        MowLibCompoundTagUtils.removeIntegerFromNBT(MODID, coinInPedestal.getTag(),"_numposition");
    }

    @Override
    public void updateAction(Level world, BasePedestalBlockEntity pedestal) {

        ItemStack coin = pedestal.getCoinOnPedestal();
        boolean override = hasTwoPointsSelected(coin);
        List<BlockPos> listed = getValidList(pedestal);

        if(override && pedestal.hasItem())
        {
            if(listed.size()>0)
            {
                //System.out.println("RunAction");
                upgradeAction(world,pedestal);
            }
            else if(selectedAreaWithinRange(pedestal) && !hasBlockListCustomNBTTags(coin,"_validlist"))
            {
                buildValidBlockListArea(pedestal);
                //System.out.println("ListBuilt: "+ getValidList(pedestal));
            }
            else if(!pedestal.getRenderRange())
            {
                pedestal.setRenderRange(true);
            }
        }
        else
        {
            List<BlockPos> getList = readBlockPosListFromNBT(coin);
            if(!override && listed.size()>0)
            {
                upgradeAction(world,pedestal);
            }
            else if(getList.size()>0)
            {
                if(!hasBlockListCustomNBTTags(coin,"_validlist"))
                {
                    BlockPos hasValidPos = IntStream.range(0,getList.size())//Int Range
                            .mapToObj((getList)::get)
                            .filter(blockPos -> selectedPointWithinRange(pedestal, blockPos))
                            .findFirst().orElse(BlockPos.ZERO);
                    if(!hasValidPos.equals(BlockPos.ZERO))
                    {
                        buildValidBlockList(pedestal);
                    }
                }
                else if(!pedestal.getRenderRange())
                {
                    pedestal.setRenderRange(true);
                }
            }
        }

    }

    private int getCurrentPosition(BasePedestalBlockEntity pedestal)
    {
        ItemStack coin = pedestal.getCoinOnPedestal();
        return MowLibCompoundTagUtils.readIntegerFromNBT(MODID, coin.getOrCreateTag(), "_numposition");
    }

    private void setCurrentPosition(BasePedestalBlockEntity pedestal, int num)
    {
        ItemStack coin = pedestal.getCoinOnPedestal();
        MowLibCompoundTagUtils.writeIntegerToNBT(MODID, coin.getOrCreateTag(), num, "_numposition");
    }

    private void iterateCurrentPosition(BasePedestalBlockEntity pedestal)
    {
        ItemStack coin = pedestal.getCoinOnPedestal();
        int current = getCurrentPosition(pedestal);
        MowLibCompoundTagUtils.writeIntegerToNBT(MODID, coin.getOrCreateTag(), (current+1), "_numposition");
    }

    private boolean passesFilter(BasePedestalBlockEntity pedestal, BlockState canMineBlock, BlockPos canMinePos)
    {
        if(pedestal.hasFilter())
        {
            ItemStack filterInPedestal = pedestal.getFilterInPedestal();
            if(filterInPedestal.getItem() instanceof BaseFilter filter)
            {
                if(filter.getFilterDirection().neutral())
                {
                    ItemStack blockToCheck = pedestal.getItemInPedestal();
                    if(Block.byItem(blockToCheck.getItem()) != Blocks.AIR)
                    {
                        return filter.canAcceptItems(filterInPedestal,blockToCheck);
                    }
                }
            }
        }

        return true;
    }

    private boolean canPlace(BasePedestalBlockEntity pedestal)
    {
        ItemStack getPlaceItem = pedestal.getItemInPedestal();
        Block possibleBlock = Block.byItem(getPlaceItem.getItem());
        if(possibleBlock != Blocks.AIR)
        {
            if(!ForgeRegistries.BLOCKS.tags().getTag(BlockTags.create(new ResourceLocation(MODID, "pedestals_cannot_place"))).stream().toList().contains(getPlaceItem))
            {
                if(possibleBlock instanceof IPlantable ||
                        possibleBlock instanceof BushBlock ||
                        possibleBlock instanceof StemBlock ||
                        possibleBlock instanceof BonemealableBlock ||
                        possibleBlock instanceof ChorusFlowerBlock
                )
                {
                    return true;
                }
            }
        }
        else
        {
            return false;
        }

        return false;
    }

    public BlockState getState(Block getBlock, ItemStack itemForBlock)
    {
        BlockState stated = Blocks.AIR.defaultBlockState();

        //Redstone
        if(itemForBlock.getItem() == Items.REDSTONE)
        {
            stated = Blocks.REDSTONE_WIRE.defaultBlockState();
        }
        else
        {
            stated = getBlock.defaultBlockState();
        }

        return stated;
    }

    private BlockPos getPosBasedOnPedestalDirection(BasePedestalBlockEntity pedestalBlockEntity, BlockPos pos)
    {
        Direction ofPedestal = getPedestalFacing(pedestalBlockEntity.getLevel(),pedestalBlockEntity.getPos());
        switch (ofPedestal)
        {
            case UP: return pos.below();
            case DOWN: return pos.above();
            case NORTH: return pos.south();
            case EAST: return pos.west();
            case SOUTH: return pos.north();
            case WEST: return pos.east();
            default: return pos.below();
        }
    }

    public void upgradeAction(Level level, BasePedestalBlockEntity pedestal)
    {
        if(!level.isClientSide())
        {
            List<BlockPos> listed = getValidList(pedestal);
            int currentPosition = getCurrentPosition(pedestal);
            BlockPos currentPoint = listed.get(currentPosition);
            BlockState blockAtPoint = level.getBlockState(currentPoint);
            WeakReference<FakePlayer> getPlayer = pedestal.fakePedestalPlayer(pedestal);
            boolean fuelRemoved = true;

            if(!pedestal.removeItem(1,true).isEmpty())
            {
                if(canPlace(pedestal))
                {
                    if(passesFilter(pedestal, blockAtPoint, currentPoint) && !pedestal.removeItem(1,true).isEmpty())
                    {
                        if(!currentPoint.equals(pedestal.getPos()))
                        {
                            if(level.getBlockState((getPedestalFacing(level,pedestal.getPos()) == Direction.DOWN)?(currentPoint.above()):(currentPoint.below())).canSustainPlant(level,getPosBasedOnPedestalDirection(pedestal,currentPoint),getPedestalFacing(level,pedestal.getPos()),(IPlantable) Block.byItem(pedestal.getItemInPedestal().getItem())))
                            {
                                if(removeFuelForAction(pedestal, getDistanceBetweenPoints(pedestal.getPos(),currentPoint), false))
                                {
                                    UseOnContext blockContext = new UseOnContext(level,getPlayer.get(), InteractionHand.MAIN_HAND, pedestal.getItemInPedestal().copy(), new BlockHitResult(Vec3.ZERO, getPedestalFacing(level,pedestal.getPos()), currentPoint, false));
                                    InteractionResult result = ForgeHooks.onPlaceItemIntoWorld(blockContext);
                                    if (result == InteractionResult.CONSUME) {
                                        if(pedestal.canSpawnParticles()) MowLibPacketHandler.sendToNearby(pedestal.getLevel(),pedestal.getPos(),new MowLibPacketParticles(MowLibPacketParticles.EffectType.ANY_COLOR,currentPoint.getX()+0.5D,currentPoint.getY()+0.5D,currentPoint.getZ()+0.5D,100,255,100));
                                        pedestal.removeItem(1,false);
                                    }
                                }
                                else {
                                    fuelRemoved = false;
                                }
                            }
                        }
                    }
                }
            }

            if((currentPosition+1)>=listed.size())
            {
                setCurrentPosition(pedestal,0);
            }
            else
            {
                if(fuelRemoved){
                    iterateCurrentPosition(pedestal);
                }
            }
        }
    }
}
