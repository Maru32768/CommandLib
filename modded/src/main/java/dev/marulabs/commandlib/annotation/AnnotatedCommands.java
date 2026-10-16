package dev.marulabs.commandlib.annotation;

import com.mojang.authlib.GameProfile;
import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.argument.*;
import dev.marulabs.commandlib.util.Location;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.NotNull;
//? if >=1.20.2 {
/*import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.scores.DisplaySlot;
*///?} else {
import net.minecraft.advancements.Advancement;
import net.minecraft.world.item.crafting.Recipe;
//?}
//? if >=1.20.5 {
/*import dev.marulabs.commandlib.CommonArgument;
import net.minecraft.core.Holder;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
*///?}

import java.util.UUID;

/**
 * Builds commands from classes annotated with {@link dev.marulabs.commandlib.annotation.Command}. See
 * {@link AnnotationParser} for the mapping.
 */
public final class AnnotatedCommands {
    private AnnotatedCommands() {
    }

    /**
     * Builds the command that {@code instance}'s class declares, with the parser of {@link #parser()}.
     *
     * @throws IllegalArgumentException if the class is not annotated or a method cannot be mapped
     */
    @NotNull
    public static Command parse(@NotNull Object instance) {
        return parser().parse(instance);
    }

    /**
     * Returns a new parser that maps the types of the built-in arguments, such as {@code int} to
     * {@link IntegerArgument} and {@link ServerPlayer} to {@link PlayerArgument}, and resolves
     * {@link CommandSourceStack} and a {@link Sender} or omitted {@link Optional} {@link ServerPlayer} from the source.
     * On 1.20.5+ a {@code Holder} of an enchantment, a mob effect or an attribute is the argument of that registry, as
     * those arguments return holders there. Register further types and annotations on it before parsing.
     */
    @NotNull
    public static AnnotationParser<CommandContext, Command> parser() {
        AnnotationParser<CommandContext, Command> parser = new AnnotationParser<>(CommandContext.class, Command::new);
        parser.registerStandardArguments(StringArgument::new,
                                         name -> new StringArgument(name, StringArgument.Type.PHRASE),
                                         IntegerArgument::new,
                                         LongArgument::new,
                                         FloatArgument::new,
                                         DoubleArgument::new,
                                         BooleanArgument::new,
                                         EnumArgument::new,
                                         DurationArgument::new)
              .registerArgument(UUID.class, p -> new UUIDArgument(p.name()))
              .registerArgument(ServerPlayer.class, p -> new PlayerArgument(p.name()))
              .registerArgument(GameProfile.class, p -> new GameProfileArgument(p.name()))
              .registerArgument(Entity.class, p -> new EntityArgument(p.name()))
              .registerArgument(ServerLevel.class, p -> new WorldArgument(p.name()))
              .registerArgument(Location.class, p -> new LocationArgument(p.name()))
              .registerArgument(BlockPos.class, p -> new BlockPositionArgument(p.name()))
              .registerArgument(GameType.class, p -> new GameModeArgument(p.name()))
              .registerArgument(EntityType.class, p -> new EntityTypeArgument(p.name()))
              .registerArgument(Biome.class, p -> new BiomeArgument(p.name()))
              .registerArgument(SoundEvent.class, p -> new SoundArgument(p.name()))
              .registerArgument(ParticleOptions.class, p -> new ParticleArgument(p.name()))
              .registerArgument(ItemStack.class, p -> new ItemStackArgument(p.name()))
              .registerArgument(BlockInput.class, p -> new BlockStateArgument(p.name()))
              .registerArgument(CompoundTag.class, p -> new NBTCompoundArgument(p.name()))
              .registerArgument(ResourceLocation.class, p -> new ResourceLocationArgument(p.name()))
              .registerArgument(LootTable.class, p -> new LootTableArgument(p.name()))
              .registerArgument(PlayerTeam.class, p -> new TeamArgument(p.name()))
              .registerArgument(Objective.class, p -> new ObjectiveArgument(p.name()))
              .registerArgument(ChatFormatting.class, p -> new ChatColorArgument(p.name()))
              .registerContextValue(CommandSourceStack.class, CommandContext::getSender)
              .registerContextValue(ServerPlayer.class, Command.playerRequirer());
        //? if >=1.20.5 {
        /*parser.registerArgument(Holder.class, AnnotatedCommands::holderArgument);
        *///?} else {
        parser.registerArgument(Enchantment.class, p -> new EnchantmentArgument(p.name()))
              .registerArgument(MobEffect.class, p -> new EffectArgument(p.name()))
              .registerArgument(Attribute.class, p -> new AttributeArgument(p.name()));
        //?}
        //? if >=1.20.2 {
        /*parser.registerArgument(AdvancementHolder.class, p -> new AdvancementArgument(p.name()))
              .registerArgument(RecipeHolder.class, p -> new RecipeArgument(p.name()))
              .registerArgument(DisplaySlot.class, p -> new ScoreboardDisplaySlotArgument(p.name()));
        *///?} else {
           // The display slot is an int before 1.20.2, which is the integer argument.
        parser.registerArgument(Advancement.class, p -> new AdvancementArgument(p.name()))
              .registerArgument(Recipe.class, p -> new RecipeArgument(p.name()));
        //?}
        return parser;
    }

    //? if >=1.20.5 {
    /*private static CommonArgument<?, CommandContext, ?> holderArgument(AnnotatedParameter parameter) {
        Type type = parameter.parameter()
                             .getParameterizedType();
        // The holder of a variadic argument is the element of the List.
        if (type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() == List.class) {
            type = ((ParameterizedType) type).getActualTypeArguments()[0];
        }
        Type value = type instanceof ParameterizedType ? ((ParameterizedType) type).getActualTypeArguments()[0] : type;
        if (value == Enchantment.class) {
            return new EnchantmentArgument(parameter.name());
        }
        if (value == MobEffect.class) {
            return new EffectArgument(parameter.name());
        }
        if (value == Attribute.class) {
            return new AttributeArgument(parameter.name());
        }
        throw new IllegalArgumentException("parameter " + parameter.name() + " has no argument type for "
                + type.getTypeName() + "; register one with registerArgument");
    }
    *///?}
}
