package cech12.ceramicbucket.item;

import cech12.ceramicbucket.api.item.CeramicBucketItems;

import cech12.ceramicbucket.util.CeramicBucketUtils;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;

public class FilledCeramicBucketItem extends AbstractCeramicBucketItem {

    public FilledCeramicBucketItem(Properties builder) {
        super(Fluids.EMPTY.delegate, builder);
    }

    @Nonnull
    @Override
    FluidHandlerItemStack getNewFluidHandlerInstance(@Nonnull ItemStack stack) {
        return new FilledCeramicBucketFluidHandler(stack);
    }

    public ItemStack getFilledInstance(@Nonnull Fluid fluid) {
        return fill(new ItemStack(this), new FluidStack(fluid, FluidAttributes.BUCKET_VOLUME));
    }

    @Nonnull
    @OnlyIn(Dist.CLIENT)
    public ItemStack getDefaultInstance() {
        return this.getFilledInstance(Fluids.WATER);
    }

    /**
     * returns a list of items with the same ID, but different meta (eg: dye returns 16 items)
     */
    @Override
    public void fillItemGroup(@Nonnull ItemGroup group, @Nonnull NonNullList<ItemStack> items) {
        if (this.isInGroup(group)) {
            for (Fluid fluid : ForgeRegistries.FLUIDS) {
                //only add non milk source fluids with a bucket item
                if (fluid.getDefaultState().isSource() && !CeramicBucketUtils.isMilkFluid(fluid, false) && fluid.getFilledBucket() != null) {
                    items.add(getFilledInstance(fluid));
                }
            }
        }
    }

    @Override
    @Nonnull
    public String getTranslationKey() {
        return Util.makeTranslationKey("item", CeramicBucketItems.CERAMIC_BUCKET.getRegistryName());
    }

    @Override
    @Nonnull
    public ITextComponent getDisplayName(@Nonnull ItemStack stack) {
        if (getFluid(stack) == Fluids.EMPTY) {
            return new TranslationTextComponent("item.ceramicbucket.ceramic_bucket");
        } else {
            ITextComponent fluidText;
            if (getFluid(stack) == Fluids.WATER || getFluid(stack) == Fluids.LAVA) {
                //vanilla fluids
                fluidText = getFluid(stack).getDefaultState().getBlockState().getBlock().getTranslatedName();
            } else {
                //fluids registered by mods
                fluidText = new TranslationTextComponent(Util.makeTranslationKey("fluid", ForgeRegistries.FLUIDS.getKey(getFluid(stack))));
            }
            return  new TranslationTextComponent("item.ceramicbucket.filled_ceramic_bucket", fluidText);
        }
    }

}
