package dev.marulabs.commandlib.annotation;

import dev.marulabs.commandlib.Command;
import dev.marulabs.commandlib.CommandContext;
import dev.marulabs.commandlib.CommonArgument;
import dev.marulabs.commandlib.argument.*;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.loot.LootTable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
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
     * {@link IntegerArgument} and {@link Player} to {@link PlayerArgument}, and resolves {@link CommandSender},
     * {@link ConsoleCommandSender} and a {@link Sender} or omitted {@link Optional} {@link Player} from the sender.
     * Register further types and annotations on it before parsing.
     */
    @NotNull
    public static AnnotationParser<CommandContext, Command> parser() {
        return new AnnotationParser<CommandContext, Command>(CommandContext.class,
                                                             Command::new).registerArgument(String.class,
                                                                                            p -> p.annotation(Greedy.class) == null
                                                                                                    ? new StringArgument(p.name())
                                                                                                    : new StringArgument(p.name(),
                                                                                                                         StringArgument.Type.PHRASE))
                                                                          .registerArgument(Integer.class, p -> {
                                                                              Range range = p.annotation(Range.class);
                                                                              return range == null
                                                                                      ? new IntegerArgument(p.name())
                                                                                      : new IntegerArgument(p.name(),
                                                                                                            (int) range.min(),
                                                                                                            (int) range.max());
                                                                          })
                                                                          .registerArgument(Long.class, p -> {
                                                                              Range range = p.annotation(Range.class);
                                                                              return range == null
                                                                                      ? new LongArgument(p.name())
                                                                                      : new LongArgument(p.name(),
                                                                                                         (long) range.min(),
                                                                                                         (long) range.max());
                                                                          })
                                                                          .registerArgument(Float.class, p -> {
                                                                              Range range = p.annotation(Range.class);
                                                                              return range == null
                                                                                      ? new FloatArgument(p.name())
                                                                                      : new FloatArgument(p.name(),
                                                                                                          (float) Math.max(range.min(),
                                                                                                                           -Float.MAX_VALUE),
                                                                                                          (float) Math.min(range.max(),
                                                                                                                           Float.MAX_VALUE));
                                                                          })
                                                                          .registerArgument(Double.class, p -> {
                                                                              Range range = p.annotation(Range.class);
                                                                              return range == null
                                                                                      ? new DoubleArgument(p.name())
                                                                                      : new DoubleArgument(p.name(),
                                                                                                           range.min(),
                                                                                                           range.max());
                                                                          })
                                                                          .registerArgument(Boolean.class,
                                                                                            p -> new BooleanArgument(p.name()))
                                                                          .registerArgument(Enum.class,
                                                                                            AnnotatedCommands::enumArgument)
                                                                          .registerArgument(Duration.class,
                                                                                            p -> new DurationArgument(p.name()))
                                                                          .registerArgument(UUID.class,
                                                                                            p -> new UUIDArgument(p.name()))
                                                                          .registerArgument(Player.class,
                                                                                            p -> new PlayerArgument(p.name()))
                                                                          .registerArgument(OfflinePlayer.class,
                                                                                            p -> new OfflinePlayerArgument(p.name()))
                                                                          .registerArgument(Entity.class,
                                                                                            p -> new EntityArgument(p.name()))
                                                                          .registerArgument(World.class,
                                                                                            p -> new WorldArgument(p.name()))
                                                                          .registerArgument(Location.class,
                                                                                            p -> new LocationArgument(p.name()))
                                                                          .registerArgument(GameMode.class,
                                                                                            p -> new GameModeArgument(p.name()))
                                                                          .registerArgument(EntityType.class,
                                                                                            p -> new EntityTypeArgument(p.name()))
                                                                          .registerArgument(Biome.class,
                                                                                            p -> new BiomeArgument(p.name()))
                                                                          .registerArgument(Sound.class,
                                                                                            p -> new SoundArgument(p.name()))
                                                                          .registerArgument(Attribute.class,
                                                                                            p -> new AttributeArgument(p.name()))
                                                                          .registerArgument(Enchantment.class,
                                                                                            p -> new EnchantmentArgument(p.name()))
                                                                          .registerArgument(Particle.class,
                                                                                            p -> new ParticleArgument(p.name()))
                                                                          .registerArgument(PotionEffect.class,
                                                                                            p -> new PotionEffectArgument(p.name()))
                                                                          .registerArgument(ItemStack.class,
                                                                                            p -> new ItemStackArgument(p.name()))
                                                                          .registerArgument(BlockData.class,
                                                                                            p -> new BlockDataArgument(p.name()))
                                                                          .registerArgument(NamespacedKey.class,
                                                                                            p -> new NamespacedKeyArgument(p.name()))
                                                                          .registerArgument(Advancement.class,
                                                                                            p -> new AdvancementArgument(p.name()))
                                                                          .registerArgument(LootTable.class,
                                                                                            p -> new LootTableArgument(p.name()))
                                                                          .registerArgument(Recipe.class,
                                                                                            p -> new RecipeArgument(p.name()))
                                                                          .registerArgument(Team.class,
                                                                                            p -> new TeamArgument(p.name()))
                                                                          .registerArgument(Objective.class,
                                                                                            p -> new ObjectiveArgument(p.name()))
                                                                          .registerArgument(DisplaySlot.class,
                                                                                            p -> new ScoreboardDisplaySlotArgument(p.name()))
                                                                          .registerArgument(ChatColor.class,
                                                                                            p -> new ChatColorArgument(p.name()))
                                                                          .registerContextValue(CommandSender.class,
                                                                                                CommandContext::getSender)
                                                                          .registerContextValue(Player.class,
                                                                                                Command.playerRequirer())
                                                                          .registerContextValue(ConsoleCommandSender.class,
                                                                                                Command.consoleRequirer());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static CommonArgument<?, CommandContext, ?> enumArgument(AnnotatedParameter parameter) {
        return new EnumArgument(parameter.name(), parameter.type());
    }
}
