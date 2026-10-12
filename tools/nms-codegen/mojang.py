"""
Typed classes for the modules compiled with Mojang names: Spigot 1.17 and later (remapped to Spigot names by the
build) and Paper 1.20.5 and later. Run as `generate.py <module>`, for example `generate.py spigot-1.20.4`.
"""
from codegen import module_package, write, write_class_list, write_source

TARGET = MODULE  # noqa: F821 - set by generate.py
CRAFTBUKKIT_PACKAGE = CRAFTBUKKIT  # noqa: F821 - set by generate.py, None for Paper


def ver(text):
    return tuple(int(x) for x in text.split("."))


platform, version = TARGET.split("-")
V = ver(version)
M = TARGET
SUFFIX = TARGET.replace("-", "_").replace(".", "_")
PAPER = platform == "paper"
CB = "org.bukkit.craftbukkit" if PAPER else "org.bukkit.craftbukkit." + CRAFTBUKKIT_PACKAGE

# API changes between the supported releases.
HAS_BUILD_CONTEXT = V >= (1, 19)
HAS_RESOURCE_ARGUMENT = V >= (1, 19, 3)
HAS_COMPONENT_CONTENTS = V >= (1, 19)
# ArgumentTypeInfos replaced ArgumentTypes, the registry of argument serializers, in 1.19.
HAS_ARGUMENT_TYPE_INFOS = V >= (1, 19)
# MinecraftServer.resources holds the ReloadableResources record from 1.18.2 and the ServerResources before.
HAS_RELOADABLE_RESOURCES = V >= (1, 18, 2)
# CraftBukkit revision v1_20_R3, shared by 1.20.3 and 1.20.4, added the minecraftToBukkit converters.
HAS_MINECRAFT_TO_BUKKIT = V >= (1, 20, 3)
# CraftParticle switched to minecraftToBukkit earlier than the other Craft* converters.
HAS_PARTICLE_MINECRAFT_TO_BUKKIT = V >= (1, 20, 2)
# Spigot keeps the build context of ReloadableServerResources in a private field. From 1.20.5 the modules build one from
# public API instead, as the reflection implementation for Paper, whose names those releases already used, did.
BUILDS_CONTEXT_FROM_PUBLIC_API = V >= (1, 20, 5)

classes = []
CTX = "CommandContext<CommandSourceStack>"
CSS = "net.minecraft.commands.CommandSourceStack"
ARGS = "net.minecraft.commands.arguments"
RESOURCES = "net.minecraft.server.ReloadableServerResources" if HAS_RELOADABLE_RESOURCES \
    else "net.minecraft.server.ServerResources"


def w(wrapper, body, imports=()):
    classes.append(write(M, wrapper, body.replace("SUFFIX", SUFFIX), imports))


def simple(name):
    return name.split(".")[-1]


def arg(wrapper, nms, ret, argument, parse, imports=(), throws=True):
    parse = parse.replace("CTX", "context")
    cast = f"        {CTX} context = ({CTX}) ctx;\n"
    if throws:
        parse_body = cast + f"""        try {{
            {parse}
        }} catch (CommandSyntaxException e) {{
            throw new UncheckedCommandSyntaxException(e);
        }}"""
    else:
        parse_body = cast + f"        {parse}"
    argument_body = f"        return {argument};"
    body = f'''    public {wrapper}_SUFFIX() {{
        super(null, {simple(nms)}.class);
    }}

    @Override
    public ArgumentType<?> argument() {{
{argument_body}
    }}

    @Override
    @SuppressWarnings("unchecked")
    protected {ret} parseImpl(CommandContext<?> ctx, String name) {{
{parse_body}
    }}
'''
    w(wrapper, body, [f"net.minecraft.{nms}", CSS, *imports])


def handle(wrapper, nms):
    """A wrapper that only holds a handle of the given class."""
    w(wrapper, f'''    public {wrapper}_SUFFIX(Object handle) {{
        super(handle, {simple(nms)}.class);
    }}
''', [nms])


arg("NMSArgumentDimension", "commands.arguments.DimensionArgument", "World", "DimensionArgument.dimension()",
    "return DimensionArgument.getDimension(CTX, name).getWorld();", ["org.bukkit.World"])
if HAS_RESOURCE_ARGUMENT:
    arg("NMSArgumentEnchantment", "commands.arguments.ResourceArgument", "NMSEnchantment",
        "ResourceArgument.resource(BuildContexts.current(), Registries.ENCHANTMENT)",
        "return NMSEnchantment.create(ResourceArgument.getEnchantment(CTX, name)\n"
        "                                                         .value());",
        ["net.minecraft.core.registries.Registries"])
else:
    arg("NMSArgumentEnchantment", "commands.arguments.ItemEnchantmentArgument", "NMSEnchantment",
        "ItemEnchantmentArgument.enchantment()",
        "return NMSEnchantment.create(ItemEnchantmentArgument.getEnchantment(CTX, name));", throws=False)
arg("NMSArgumentEntities", "commands.arguments.EntityArgument", "List<org.bukkit.entity.Entity>",
    "EntityArgument.entities()",
    "return EntityArgument.getEntities(CTX, name).stream()\n"
    "                                 .map(x -> ((org.bukkit.entity.Entity) x.getBukkitEntity()))\n"
    "                                 .collect(Collectors.toList());")
arg("NMSArgumentEntity", "commands.arguments.EntityArgument", "org.bukkit.entity.Entity", "EntityArgument.entity()",
    "return EntityArgument.getEntity(CTX, name).getBukkitEntity();")
arg("NMSArgumentItemStack", "commands.arguments.item.ItemArgument", "NMSArgumentPredicateItemStack",
    "ItemArgument.item(BuildContexts.current())" if HAS_BUILD_CONTEXT else "ItemArgument.item()",
    "return NMSArgumentPredicateItemStack.create(ItemArgument.getItem(CTX, name));",
    throws=False)
if HAS_RESOURCE_ARGUMENT:
    arg("NMSArgumentMobEffect", "commands.arguments.ResourceArgument", "NMSMobEffectList",
        "ResourceArgument.resource(BuildContexts.current(), Registries.MOB_EFFECT)",
        "return NMSMobEffectList.create(ResourceArgument.getMobEffect(CTX, name)\n"
        "                                                           .value());",
        ["net.minecraft.core.registries.Registries"])
else:
    arg("NMSArgumentMobEffect", "commands.arguments.MobEffectArgument", "NMSMobEffectList", "MobEffectArgument.effect()",
        "return NMSMobEffectList.create(MobEffectArgument.getEffect(CTX, name));", throws=False)
arg("NMSArgumentNamespacedKey", "commands.arguments.ResourceLocationArgument", "String",
    "ResourceLocationArgument.id()", "return ResourceLocationArgument.getId(CTX, name).toString();", throws=False)
arg("NMSArgumentParticle", "commands.arguments.ParticleArgument", "NMSParticleParam",
    "ParticleArgument.particle(BuildContexts.current())" if HAS_RESOURCE_ARGUMENT else "ParticleArgument.particle()",
    "return NMSParticleParam.create(ParticleArgument.getParticle(CTX, name));",
    throws=False)
arg("NMSArgumentPlayer", "commands.arguments.EntityArgument", "Player", "EntityArgument.player()",
    "return EntityArgument.getPlayer(CTX, name).getBukkitEntity();", ["org.bukkit.entity.Player"])
arg("NMSArgumentPlayers", "commands.arguments.EntityArgument", "List<Player>", "EntityArgument.players()",
    "return EntityArgument.getPlayers(CTX, name).stream()\n"
    "                                 .map(x -> ((Player) x.getBukkitEntity()))\n"
    "                                 .collect(Collectors.toList());", ["org.bukkit.entity.Player"])
arg("NMSArgumentScoreboardTeam", "commands.arguments.TeamArgument", "NMSScoreboardTeam", "TeamArgument.team()",
    "return NMSScoreboardTeam.create(TeamArgument.getTeam(CTX, name));")
arg("NMSArgumentTile", "commands.arguments.blocks.BlockStateArgument", "NMSArgumentTileLocation",
    "BlockStateArgument.block(BuildContexts.current())" if HAS_BUILD_CONTEXT else "BlockStateArgument.block()",
    "return NMSArgumentTileLocation.create(BlockStateArgument.getBlock(CTX, name));", throws=False)
arg("NMSArgumentVec3D", "commands.arguments.coordinates.Vec3Argument", "NMSVec3D", "Vec3Argument.vec3()",
    "return NMSVec3D.create(Vec3Argument.getVec3(CTX, name));", throws=False)

w("NMSArgumentProfile", '''    public NMSArgumentProfile_SUFFIX() {
        super(null, GameProfileArgument.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return GameProfileArgument.gameProfile();
    }

    @Override
    protected Object parseImpl(CommandContext<?> ctx, String name) {
        throw new UnsupportedOperationException();
    }
''', [f"{ARGS}.GameProfileArgument"])

w("NMSArgumentPredicateItemStack", '''    public NMSArgumentPredicateItemStack_SUFFIX(Object handle) {
        super(handle, ItemInput.class);
    }

    @Override
    public NMSItemStack createItemStack(int amount, boolean checkOverStack) {
        try {
            return NMSItemStack.create(((ItemInput) getHandle()).createItemStack(amount, checkOverStack));
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
''', [f"{ARGS}.item.ItemInput"])

w("NMSArgumentTileLocation", '''    public NMSArgumentTileLocation_SUFFIX(Object handle) {
        super(handle, BlockInput.class);
    }

    @Override
    public NMSIBlockData getBlockData() {
        return NMSIBlockData.create(((BlockInput) getHandle()).getState());
    }
''', [f"{ARGS}.blocks.BlockInput"])

# The registries keep their map of argument serializers in a private field, which the wrapper finds by content.
if HAS_ARGUMENT_TYPE_INFOS:
    w("NMSArgumentTypeRegistrar", '''    public NMSArgumentTypeRegistrar_SUFFIX() {
        super(ArgumentTypeInfos.class);
    }

    @Override
    public <T extends ArgumentType<?>> void registerAsGreedyString(Class<T> argumentTypeClass, Supplier<T> factory) {
        registerInArgumentTypeInfos(argumentTypeClass, ArgumentTypeInfo.class);
    }
''', ["net.minecraft.commands.synchronization.ArgumentTypeInfo",
      "net.minecraft.commands.synchronization.ArgumentTypeInfos"])
else:
    w("NMSArgumentTypeRegistrar", '''    public NMSArgumentTypeRegistrar_SUFFIX() {
        super(ArgumentTypes.class);
    }

    @Override
    public <T extends ArgumentType<?>> void registerAsGreedyString(Class<T> argumentTypeClass, Supplier<T> factory) {
        registerInArgumentRegistry(argumentTypeClass,
                                   factory,
                                   ArgumentSerializer.class,
                                   new ResourceLocation("brigadier:string"));
    }
''', ["net.minecraft.commands.synchronization.ArgumentSerializer",
      "net.minecraft.commands.synchronization.ArgumentTypes", "net.minecraft.resources.ResourceLocation"])

if HAS_COMPONENT_CONTENTS:
    w("NMSIChatMutableComponent", '''    public NMSIChatMutableComponent_SUFFIX(Object handle) {
        super(handle, MutableComponent.class);
    }

    @Override
    public NMSTranslatableContents getContentsAsTranslatable() {
        return NMSTranslatableContents.create(((MutableComponent) getHandle()).getContents());
    }
''', ["net.minecraft.network.chat.MutableComponent"])

    w("NMSTranslatableContents", '''    public NMSTranslatableContents_SUFFIX(Object handle) {
        super(handle, TranslatableContents.class);
    }

    @Override
    public String getKey() {
        return ((TranslatableContents) getHandle()).getKey();
    }

    @Override
    public Object[] getArgs() {
        return ((TranslatableContents) getHandle()).getArgs();
    }
''', ["net.minecraft.network.chat.contents.TranslatableContents"])
else:
    w("NMSChatMessage", '''    public NMSChatMessage_SUFFIX(Message handle) {
        super(handle, TranslatableComponent.class);
    }

    @Override
    public String getKey() {
        return ((TranslatableComponent) getHandle()).getKey();
    }

    @Override
    public Object[] getArgs() {
        return ((TranslatableComponent) getHandle()).getArgs();
    }
''', ["net.minecraft.network.chat.TranslatableComponent", "com.mojang.brigadier.Message"])

if HAS_BUILD_CONTEXT:
    handle("NMSCommandBuildContext", "net.minecraft.commands.CommandBuildContext")

w("NMSCommandDispatcher", '''    public NMSCommandDispatcher_SUFFIX(Object handle) {
        super(handle, Commands.class);
    }

    @Override
    public CommandDispatcher<?> getBrigadier() {
        return ((Commands) getHandle()).getDispatcher();
    }
''', ["net.minecraft.commands.Commands", "com.mojang.brigadier.CommandDispatcher"])

# NMSCommandListenerWrapper builds the location, with the rotation, from the position and rotation.
w("NMSCommandListenerWrapper", '''    public NMSCommandListenerWrapper_SUFFIX(Object handle) {
        super(handle, CommandSourceStack.class);
    }

    @Override
    public CommandSender getBukkitSender() {
        return source().getBukkitSender();
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity() {
        Entity entity = source().getEntity();
        return entity != null ? entity.getBukkitEntity() : null;
    }

    @Override
    public World getBukkitWorld() {
        ServerLevel level = source().getLevel();
        return level != null ? level.getWorld() : null;
    }

    @Override
    protected NMSVec3D getPosition() {
        Vec3 position = source().getPosition();
        return position != null ? NMSVec3D.create(position) : null;
    }

    @Override
    protected NMSVec2D getRotation() {
        Vec2 rotation = source().getRotation();
        return rotation != null ? NMSVec2D.create(rotation) : null;
    }

    private CommandSourceStack source() {
        return (CommandSourceStack) getHandle();
    }
''', [CSS, "net.minecraft.server.level.ServerLevel", "net.minecraft.world.entity.Entity", "net.minecraft.world.phys.Vec2",
      "net.minecraft.world.phys.Vec3", "org.bukkit.World", "org.bukkit.command.CommandSender"])

w("NMSVanillaCommandWrapper", '''    public NMSVanillaCommandWrapper_SUFFIX() {
        super(null, VanillaCommandWrapper.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public BukkitCommand createInstance(NMSCommandDispatcher dispatcher, CommandNode<?> command) {
        return new VanillaCommandWrapper((Commands) dispatcher.getHandle(), (CommandNode<CommandSourceStack>) command);
    }
''', ["net.minecraft.commands.Commands", CSS, f"{CB}.command.VanillaCommandWrapper",
      "com.mojang.brigadier.tree.CommandNode", "org.bukkit.command.defaults.BukkitCommand"])

handle("NMSParticle", "net.minecraft.core.particles.ParticleType")

w("NMSParticleParam", '''    public NMSParticleParam_SUFFIX(Object handle) {
        super(handle, ParticleOptions.class);
    }

    @Override
    public NMSParticle getParticle() {
        return NMSParticle.create(((ParticleOptions) getHandle()).getType());
    }
''', ["net.minecraft.core.particles.ParticleOptions"])

w("NMSCraftParticle", ('''    public NMSCraftParticle_SUFFIX() {
        super(null, CraftParticle.class);
    }

    @Override
    public Particle toBukkit(NMSParticleParam nms) {
        return CraftParticle.CONVERSION;
    }
''').replace("CONVERSION", "minecraftToBukkit(((ParticleOptions) nms.getHandle()).getType())"
             if HAS_PARTICLE_MINECRAFT_TO_BUKKIT else "toBukkit((ParticleOptions) nms.getHandle())"),
  ["net.minecraft.core.particles.ParticleOptions", f"{CB}.CraftParticle", "org.bukkit.Particle"])

w("NMSCraftServer", '''    public NMSCraftServer_SUFFIX(Server handle) {
        super(handle, CraftServer.class);
    }

    @Override
    public NMSDedicatedServer getServer() {
        return NMSDedicatedServer.create(((CraftServer) getHandle()).getServer());
    }

    @Override
    public SimpleCommandMap getCommandMap() {
        return ((CraftServer) getHandle()).getCommandMap();
    }
''', [f"{CB}.CraftServer", "org.bukkit.Server", "org.bukkit.command.SimpleCommandMap"])

# Paper 1.20.6 dropped MinecraftServer.vanillaCommandDispatcher with its Brigadier command API and keeps one dispatcher.
# The Paper 1.20.6 module also runs on Paper 1.20.5, so it reads the CraftBukkit field when the server still has it.
VANILLA_DISPATCHER = '''        DedicatedServer server = (DedicatedServer) getHandle();
        try {
            return NMSCommandDispatcher.create(MinecraftServer.class.getField("vanillaCommandDispatcher")
                                                                    .get(server));
        } catch (NoSuchFieldException e) {
            return NMSCommandDispatcher.create(server.getCommands());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
''' if PAPER else '''        return NMSCommandDispatcher.create(((DedicatedServer) getHandle()).vanillaCommandDispatcher);
'''

w("NMSDedicatedServer", f'''    public NMSDedicatedServer_SUFFIX(Object handle) {{
        super(handle, DedicatedServer.class);
    }}

    @Override
    public NMSCommandDispatcher getCommandDispatcher() {{
        return NMSCommandDispatcher.create(((DedicatedServer) getHandle()).getCommands());
    }}

    @Override
    public NMSCommandDispatcher getVanillaCommandDispatcher() {{
{VANILLA_DISPATCHER}    }}

    @Override
    public NMSDataPackResources getDataPackResources() {{
        return NMSDataPackResources.create(((DedicatedServer) getHandle()).{"resources.managers()" if HAS_RELOADABLE_RESOURCES else "resources"});
    }}

    @Override
    public void recompileFunctions(NMSCommandDispatcher dispatcher) {{
        DedicatedServer server = (DedicatedServer) getHandle();
        ServerFunctionLibrary library = server.{"resources.managers()" if HAS_RELOADABLE_RESOURCES else "resources"}.getFunctionLibrary();
        setFunctionDispatcher(library, ServerFunctionLibrary.class, dispatcher);
        library.reload(new PreparableReloadListener.PreparationBarrier() {{
                           @Override
                           public <T> CompletableFuture<T> wait(T value) {{
                               return CompletableFuture.completedFuture(value);
                           }}
                       }},
                       server.{"resources.resourceManager()" if HAS_RELOADABLE_RESOURCES else "resources.getResourceManager()"},
                       InactiveProfiler.INSTANCE,
                       InactiveProfiler.INSTANCE,
                       Runnable::run,
                       Runnable::run)
               .join();
        server.getFunctions()
              .replaceLibrary(library);
    }}
''', ["net.minecraft.server.dedicated.DedicatedServer",
      *(["net.minecraft.server.MinecraftServer"] if PAPER else []),
      "net.minecraft.server.ServerFunctionLibrary",
      "net.minecraft.server.packs.resources.PreparableReloadListener",
      "net.minecraft.util.profiling.InactiveProfiler",
      "java.util.concurrent.CompletableFuture"])

if BUILDS_CONTEXT_FROM_PUBLIC_API:
    w("NMSDataPackResources", '''    public NMSDataPackResources_SUFFIX(Object handle) {
        super(handle, ReloadableServerResources.class);
    }

    @Override
    public NMSCommandBuildContext getCommandBuildContext() {
        MinecraftServer server = MinecraftServer.getServer();
        return NMSCommandBuildContext.create(CommandBuildContext.simple(server.registryAccess(),
                                                                        server.getWorldData()
                                                                              .enabledFeatures()));
    }
''', [RESOURCES, "net.minecraft.commands.CommandBuildContext", "net.minecraft.server.MinecraftServer"])
elif HAS_BUILD_CONTEXT:
    # The field type is CommandBuildContext up to 1.19.2 and its subtype CommandBuildContext.Configurable after.
    w("NMSDataPackResources", '''    public NMSDataPackResources_SUFFIX(Object handle) {
        super(handle, ReloadableServerResources.class);
    }

    @Override
    public NMSCommandBuildContext getCommandBuildContext() {
        // ReloadableServerResources keeps the build context in a private field.
        for (Field field : ReloadableServerResources.class.getDeclaredFields()) {
            if (CommandBuildContext.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    return NMSCommandBuildContext.create(field.get(getHandle()));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        throw new IllegalStateException("ReloadableServerResources has no CommandBuildContext field");
    }
''', [RESOURCES, "net.minecraft.commands.CommandBuildContext", "java.lang.reflect.Field"])
else:
    w("NMSDataPackResources", f'''    public NMSDataPackResources_SUFFIX(Object handle) {{
        super(handle, {simple(RESOURCES)}.class);
    }}

    @Override
    public NMSCommandBuildContext getCommandBuildContext() {{
        throw new UnsupportedOperationException();
    }}
''', [RESOURCES])

w("NMSCraftBlockData", '''    public NMSCraftBlockData_SUFFIX() {
        super(null, CraftBlockData.class);
    }

    @Override
    public BlockData createData(NMSIBlockData nms) {
        return CraftBlockData.fromData((BlockState) nms.getHandle());
    }
''', ["net.minecraft.world.level.block.state.BlockState", f"{CB}.block.data.CraftBlockData",
      "org.bukkit.block.data.BlockData"])

w("NMSCraftEnchantment", ('''    public NMSCraftEnchantment_SUFFIX() {
        super(null, CraftEnchantment.class);
    }

    @Override
    public Enchantment createInstance(NMSEnchantment nms) {
        return CONVERSION((net.minecraft.world.item.enchantment.Enchantment) nms.getHandle());
    }
''').replace("CONVERSION", "CraftEnchantment.minecraftToBukkit" if HAS_MINECRAFT_TO_BUKKIT else "new CraftEnchantment"),
  [f"{CB}.enchantments.CraftEnchantment", "org.bukkit.enchantments.Enchantment"])

w("NMSCraftItemStack", '''    public NMSCraftItemStack_SUFFIX() {
        super(null, CraftItemStack.class);
    }

    @Override
    public org.bukkit.inventory.ItemStack asCraftMirror(NMSItemStack nms) {
        return CraftItemStack.asCraftMirror((ItemStack) nms.getHandle());
    }
''', ["net.minecraft.world.item.ItemStack", f"{CB}.inventory.CraftItemStack"])

w("NMSCraftPotionEffectType", ('''    public NMSCraftPotionEffectType_SUFFIX() {
        this(null);
    }

    public NMSCraftPotionEffectType_SUFFIX(Object handle) {
        super(handle, CraftPotionEffectType.class);
    }

    @Override
    public PotionEffectType createInstance(NMSMobEffectList nms) {
        return CONVERSION((MobEffect) nms.getHandle());
    }
''').replace("CONVERSION",
             "CraftPotionEffectType.minecraftToBukkit" if HAS_MINECRAFT_TO_BUKKIT else "new CraftPotionEffectType"),
  ["net.minecraft.world.effect.MobEffect", f"{CB}.potion.CraftPotionEffectType", "org.bukkit.potion.PotionEffectType"])

handle("NMSEnchantment", "net.minecraft.world.item.enchantment.Enchantment")

w("NMSEntity", '''    public NMSEntity_SUFFIX(Object handle) {
        super(handle, Entity.class);
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity() {
        return ((Entity) getHandle()).getBukkitEntity();
    }
''', ["net.minecraft.world.entity.Entity"])

handle("NMSIBlockData", "net.minecraft.world.level.block.state.BlockState")
handle("NMSItemStack", "net.minecraft.world.item.ItemStack")
handle("NMSMobEffectList", "net.minecraft.world.effect.MobEffect")

w("NMSScoreboardTeam", '''    public NMSScoreboardTeam_SUFFIX(Object handle) {
        super(handle, PlayerTeam.class);
    }

    @Override
    public String getName() {
        return ((PlayerTeam) getHandle()).getName();
    }
''', ["net.minecraft.world.scores.PlayerTeam"])

w("NMSVec3D", '''    public NMSVec3D_SUFFIX(Object handle) {
        super(handle, Vec3.class);
    }

    @Override
    public double x() {
        return ((Vec3) getHandle()).x;
    }

    @Override
    public double y() {
        return ((Vec3) getHandle()).y;
    }

    @Override
    public double z() {
        return ((Vec3) getHandle()).z;
    }
''', ["net.minecraft.world.phys.Vec3"])

w("NMSVec2D", '''    public NMSVec2D_SUFFIX(Object handle) {
        super(handle, Vec2.class);
    }

    @Override
    public float x() {
        return ((Vec2) getHandle()).x;
    }

    @Override
    public float y() {
        return ((Vec2) getHandle()).y;
    }
''', ["net.minecraft.world.phys.Vec2"])

HELPER_PACKAGE = module_package(M)
if HAS_BUILD_CONTEXT:
    write_source(M, HELPER_PACKAGE, "BuildContexts", f"""package {HELPER_PACKAGE};

import net.kunmc.lab.commandlib.util.nms.server.NMSCraftServer;
import net.minecraft.commands.CommandBuildContext;

final class BuildContexts {{
    private BuildContexts() {{
    }}

    /**
     * Returns the build context of the loaded data packs, which argument types backed by registries need.
     */
    static CommandBuildContext current() {{
        return (CommandBuildContext) NMSCraftServer.create()
                                                   .getServer()
                                                   .getDataPackResources()
                                                   .getCommandBuildContext()
                                                   .getHandle();
    }}
}}
""")

if V >= (1, 20, 5) and not PAPER:
    write_source(M, HELPER_PACKAGE, "MappingProbe", f"""package {HELPER_PACKAGE};

/**
 * Tells NMSClassRegistry whether this package runs on Spigot. Paper shares the version range from 1.20.5 and has
 * modules of its own, which the integration tests run there, so this package matches only where the Spigot name
 * MobEffectList is a class.
 */
final class MappingProbe {{
    private MappingProbe() {{
    }}

    static boolean matches() {{
        try {{
            Class.forName("net.minecraft.world.effect.MobEffectList", false, MappingProbe.class.getClassLoader());
            return true;
        }} catch (ClassNotFoundException e) {{
            return false;
        }}
    }}
}}
""")

if PAPER:
    write_source(M, HELPER_PACKAGE, "MappingProbe", f"""package {HELPER_PACKAGE};

import net.minecraft.world.effect.MobEffect;
import org.bukkit.craftbukkit.CraftServer;

/**
 * Tells NMSClassRegistry whether this package matches the mappings the server loaded the plugin with. Paper remaps a
 * plugin that does not declare Mojang mappings from Spigot names to Mojang names, and the Spigot name MobEffect is the
 * Mojang name of MobEffectInstance. So the Mojang-named package sees MobEffect only when the plugin is not remapped,
 * and the reobfuscated package, which refers to the Spigot name MobEffectList, only when it is.
 *
 * <p>Spigot 1.20.5 and later also have a class named MobEffect, but keep CraftBukkit in a versioned package. Loading
 * the unversioned CraftServer then throws NoClassDefFoundError, which NMSClassRegistry treats as not matching.
 */
final class MappingProbe {{
    private MappingProbe() {{
    }}

    static boolean matches() {{
        return "net.minecraft.world.effect.MobEffect".equals(MobEffect.class.getName())
                && CraftServer.class.getName() != null;
    }}
}}
""")

write_class_list(M, classes)
print(f"{M}: {len(classes)} classes")
