package net.kunmc.lab.commandlib.argument;

//? if >=1.19.3
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kunmc.lab.commandlib.Argument;
import net.kunmc.lab.commandlib.CommandContext;
import net.kunmc.lab.commandlib.exception.ArgumentParseException;
//? if >=1.19.3 {
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
//?} else
/*import net.minecraft.commands.arguments.ItemEnchantmentArgument;*/
//? if >=1.20.5
/*import net.minecraft.core.Holder;*/
import net.minecraft.world.item.enchantment.Enchantment;

// On 1.20.5+ the parsed value is the registry holder: enchantments are data-driven there, and the APIs that apply
// them (ItemStack#enchant, EnchantmentHelper) take a Holder<Enchantment>.
//? if >=1.20.5 {
/*public class EnchantmentArgument extends Argument<Holder<Enchantment>, EnchantmentArgument> {
*///?} else
public class EnchantmentArgument extends Argument<Enchantment, EnchantmentArgument> {
    public EnchantmentArgument(String name) {
        //? if >=1.19.3 {
        super(name, ResourceArgument.resource(BuiltInCommandBuildContext.INSTANCE, Registries.ENCHANTMENT));
        //?} else
        /*super(name, ItemEnchantmentArgument.enchantment());*/
    }

    //? if >=1.20.5 {
    /*@Override
    @SuppressWarnings("unchecked")
    public Holder<Enchantment> cast(Object parsedArgument) {
        return ((Holder<Enchantment>) parsedArgument);
    }

    @Override
    protected Holder<Enchantment> parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ResourceArgument.getEnchantment(ctx.getHandle(), name());
    }
    *///?} elif >=1.19.3 {
    @Override
    public Enchantment cast(Object parsedArgument) {
        return ((Enchantment) parsedArgument);
    }

    @Override
    protected Enchantment parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ResourceArgument.getEnchantment(ctx.getHandle(), name())
                               .value();
    }
    //?} else {
    /*@Override
    public Enchantment cast(Object parsedArgument) {
        return ((Enchantment) parsedArgument);
    }

    @Override
    protected Enchantment parseImpl(CommandContext ctx) throws ArgumentParseException {
        return ItemEnchantmentArgument.getEnchantment(ctx.getHandle(), name());
    }
    *///?}
}
