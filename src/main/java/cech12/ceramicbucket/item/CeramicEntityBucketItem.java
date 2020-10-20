package cech12.ceramicbucket.item;

import cech12.ceramicbucket.api.data.ObtainableEntityType;
import cech12.ceramicbucket.compat.ModCompat;
import cech12.ceramicbucket.config.Config;
import cech12.ceramicbucket.util.CeramicBucketUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.passive.fish.AbstractFishEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class CeramicEntityBucketItem extends FilledCeramicBucketItem {

    public CeramicEntityBucketItem(Item.Properties builder) {
        super(builder);
    }

    @Deprecated
    @Override
    public ItemStack getFilledInstance(@Nonnull Fluid fluid) {
        return ItemStack.EMPTY;
    }

    public ItemStack getFilledInstance(@Nonnull Fluid fluid, @Nonnull Entity entity) {
        ItemStack bucket = (fluid != Fluids.EMPTY) ? super.getFilledInstance(fluid) : new ItemStack(this);
        return putEntityInStack(bucket, entity);
    }

    private ItemStack getFilledInstance(@Nonnull Fluid fluid, @Nonnull EntityType<?> entityType) {
        ItemStack bucket = (fluid != Fluids.EMPTY) ? super.getFilledInstance(fluid) : new ItemStack(this);
        return putEntityTypeInStack(bucket, entityType);
    }

    @Override
    @Nonnull
    public ActionResult<ItemStack> onItemRightClick(@Nonnull World worldIn, PlayerEntity playerIn, @Nonnull Hand handIn) {
        //support empty fluids
        ItemStack itemstack = playerIn.getHeldItem(handIn);
        if (this.getFluid(itemstack) == Fluids.EMPTY) {
            RayTraceResult raytraceresult = rayTrace(worldIn, playerIn, this.getFluid(itemstack) == Fluids.EMPTY ? RayTraceContext.FluidMode.SOURCE_ONLY : RayTraceContext.FluidMode.NONE);
            BlockRayTraceResult blockraytraceresult = (BlockRayTraceResult) raytraceresult;
            //ActionResult<ItemStack> ret = net.minecraftforge.event.ForgeEventFactory.onBucketUse(playerIn, worldIn, itemstack, raytraceresult);
            BlockPos blockpos = blockraytraceresult.getPos().offset(blockraytraceresult.getFace());
            this.onLiquidPlaced(worldIn, itemstack, blockpos);
            ItemStack result = (!playerIn.abilities.isCreativeMode) ? this.getContainerItem(itemstack) : itemstack;
            return new ActionResult<>(ActionResultType.SUCCESS, result);
        }
        return super.onItemRightClick(worldIn, playerIn, handIn);
    }

    public void onLiquidPlaced(World worldIn, @Nonnull ItemStack stack, @Nonnull BlockPos blockPos) {
        if (!worldIn.isRemote) {
            Entity entity = getEntityFromStack(stack, worldIn);
            if (entity != null) {
                entity.setPositionAndRotation(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5, 0, 0);
                worldIn.addEntity(entity);
                if (entity instanceof AbstractFishEntity) {
                    ((AbstractFishEntity)entity).setFromBucket(true);
                } else if (entity instanceof MobEntity) {
                    ((MobEntity)entity).enablePersistence(); //TODO really?
                }
            }
        }
    }

    @Override
    public void playEmptySound(@Nullable PlayerEntity player, @Nonnull IWorld worldIn, @Nonnull BlockPos pos, @Nonnull ItemStack stack) {
        ObtainableEntityType type = ModCompat.getObtainableEntityType(this.getEntityTypeFromStack(stack));
        if (type != null) {
            SoundEvent soundevent = type.getEmptySound();
            worldIn.playSound(player, pos, soundevent, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public void playFillSound(@Nullable PlayerEntity player, @Nonnull ItemStack stack) {
        if (player == null) return;
        ObtainableEntityType type = ModCompat.getObtainableEntityType(this.getEntityTypeFromStack(stack));
        if (type != null) {
            SoundEvent soundevent = type.getFillSound();
            player.playSound(soundevent, 1.0F, 1.0F);
        }
    }

    @Override
    public Collection<ItemGroup> getCreativeTabs() {
        //only add the fish buckets to creative tab if obtaining is enabled
        if (Config.FISH_OBTAINING_ENABLED.get()) {
            //TODO ItemGroup.SEARCH is set before config is loaded!
            return Arrays.asList(ItemGroup.MISC, ItemGroup.SEARCH);
        }
        return Collections.singletonList(null);
    }

    /**
     * returns a list of items with the same ID, but different meta (eg: dye returns 16 items)
     */
    @Override
    public void fillItemGroup(@Nonnull ItemGroup group, @Nonnull NonNullList<ItemStack> items) {
        if (this.isInGroup(group)) {
            for (ObtainableEntityType type : ModCompat.getObtainableEntityTypes()) {
                EntityType<?> entityType = type.getEntityType();
                if (entityType != null) {
                    items.add(this.getFilledInstance(type.getOneFluid(), entityType));
                }
            }
        }
    }

    @Override
    @Nonnull
    public ITextComponent getDisplayName(@Nonnull ItemStack stack) {
        EntityType<?> type = getEntityTypeFromStack(stack);
        ITextComponent name = (type != null) ? type.getName() : new StringTextComponent("?");
        return new TranslationTextComponent("item.ceramicbucket.ceramic_entity_bucket", name);
    }

    public boolean containsEntity(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTag() && stack.getTag().contains("entity");
    }

    public ItemStack putEntityInStack(ItemStack stack, Entity entity) {
        CompoundNBT nbt = stack.getOrCreateTag();
        nbt.putString("entity", EntityType.getKey(entity.getType()).toString());
        entity.writeWithoutTypeId(nbt);
        stack.setTag(nbt);
        entity.remove(true);
        return stack;
    }

    private ItemStack putEntityTypeInStack(ItemStack stack, EntityType<?> type) {
        CompoundNBT nbt = stack.getOrCreateTag();
        nbt.putString("entity", EntityType.getKey(type).toString());
        stack.setTag(nbt);
        return stack;
    }

    @Nullable
    public Entity getEntityFromStack(@Nonnull ItemStack stack, World world) {
        EntityType<?> type = getEntityTypeFromStack(stack);
        if (type != null) {
            Entity entity = type.create(world);
            if (entity != null && stack.hasTag()) {
                entity.read(stack.getTag());
            }
            return entity;
        }
        return null;
    }

    @Nullable
    public EntityType<?> getEntityTypeFromStack(ItemStack stack) {
        if (stack.hasTag()) {
            return EntityType.byKey(stack.getTag().getString("entity")).orElse(null);
        }
        return null;
    }

    public boolean cracksBucket(ItemStack stack) {
        ObtainableEntityType type = ModCompat.getObtainableEntityType(this.getEntityTypeFromStack(stack));
        if (type != null) {
            Boolean cracksBucket = type.cracksBucket();
            if (cracksBucket != null) {
                return cracksBucket;
            }
        }
        return CeramicBucketUtils.isFluidTooHotForCeramicBucket(getFluid(stack));
    }

    @Override
    public boolean hasContainerItem(ItemStack stack) {
        return !this.cracksBucket(stack);
    }
}
