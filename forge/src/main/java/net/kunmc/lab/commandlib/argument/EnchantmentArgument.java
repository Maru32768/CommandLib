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
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentArgument extends Argument<Enchantment, EnchantmentArgument> {
    public EnchantmentArgument(String name) {
        //? if >=1.19.3 {
        super(name, ResourceArgument.resource(BuiltInCommandBuildContext.INSTANCE, Registries.ENCHANTMENT));
        //?} else
        /*super(name, ItemEnchantmentArgument.enchantment());*/
    }

    @Override
    public Enchantment cast(Object parsedArgument) {
        return ((Enchantment) parsedArgument);
    }

    @Override
    //? if >=1.19.3 {
    protected Enchantment parseImpl(CommandContext ctx) throws ArgumentParseException, CommandSyntaxException {
        return ResourceArgument.getEnchantment(ctx.getHandle(), name())
                               .value();
    //?} else {
    /*protected Enchantment parseImpl(CommandContext ctx) throws ArgumentParseException {
        return ItemEnchantmentArgument.getEnchantment(ctx.getHandle(), name());
    *///?}
    }
}
