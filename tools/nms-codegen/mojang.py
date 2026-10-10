"""
Typed classes for the modules compiled with Mojang names: Spigot 1.17.1 and later (remapped to Spigot names by the
build) and Paper 1.20.5 and later. Run as `generate.py <module>`, for example `generate.py spigot-1.20.4`.
"""
from codegen import module_package, register, write, write_source

TARGET = MODULE  # noqa: F821 - set by generate.py
CRAFTBUKKIT_PACKAGE = CRAFTBUKKIT  # noqa: F821 - set by generate.py, None for Paper


def ver(text):
    return tuple(int(x) for x in text.split("."))


platform, version = TARGET.split("-")
V = ver(version)
M = TARGET
SUFFIX = TARGET.replace("-", "_").replace(".", "_")
PAPER = platform == "paper"
if not PAPER:
    CB = "org.bukkit.craftbukkit." + CRAFTBUKKIT_PACKAGE
    # Spigot names that MinecraftClass looks up at runtime on a Spigot-mapped server.
    NAMES = {
        "DimensionArgument": "commands.arguments.ArgumentDimension",
        "ResourceArgument": "commands.arguments.ResourceArgument",
        "ItemEnchantmentArgument": "commands.arguments.ArgumentEnchantment",
        "MobEffectArgument": "commands.arguments.ArgumentMobEffect",
        "EntityArgument": "commands.arguments.ArgumentEntity",
        "ItemArgument": "commands.arguments.item.ArgumentItemStack",
        "ResourceLocationArgument": "commands.arguments.ArgumentMinecraftKeyRegistered",
        "ParticleArgument": "commands.arguments.ArgumentParticle",
        "GameProfileArgument": "commands.arguments.ArgumentProfile",
        "TeamArgument": "commands.arguments.ArgumentScoreboardTeam",
        "BlockStateArgument": "commands.arguments.blocks.ArgumentTile",
        "Vec3Argument": "commands.arguments.coordinates.ArgumentVec3",
        "ItemInput": "commands.arguments.item.ArgumentPredicateItemStack",
        "BlockInput": "commands.arguments.blocks.ArgumentTileLocation",
        "TranslatableComponent": "network.chat.ChatMessage",
        "MutableComponent": "network.chat.IChatMutableComponent",
        "TranslatableContents": "network.chat.contents.TranslatableContents",
        "Commands": "commands.CommandDispatcher",
        "CommandSourceStack": "commands.CommandListenerWrapper",
        "Holder$Reference": "core.Holder$c",
        "ParticleOptions": "core.particles.ParticleParam",
        "Registries": "core.registries.Registries",
        "ReloadableServerResources": "server.DataPackResources",
        "DedicatedServer": "server.dedicated.DedicatedServer",
        "Entity": "world.entity.Entity",
        "PlayerTeam": "world.scores.ScoreboardTeam",
        "Vec3": "world.phys.Vec3D",
    }
else:
    CB = "org.bukkit.craftbukkit"
    NAMES = {
        "DimensionArgument": "commands.arguments.DimensionArgument",
        "ResourceArgument": "commands.arguments.ResourceArgument",
        "EntityArgument": "commands.arguments.EntityArgument",
        "ItemArgument": "commands.arguments.item.ItemArgument",
        "ResourceLocationArgument": "commands.arguments.ResourceLocationArgument",
        "ParticleArgument": "commands.arguments.ParticleArgument",
        "GameProfileArgument": "commands.arguments.GameProfileArgument",
        "TeamArgument": "commands.arguments.TeamArgument",
        "BlockStateArgument": "commands.arguments.blocks.BlockStateArgument",
        "Vec3Argument": "commands.arguments.coordinates.Vec3Argument",
        "ItemInput": "commands.arguments.item.ItemInput",
        "BlockInput": "commands.arguments.blocks.BlockInput",
        "MutableComponent": "network.chat.MutableComponent",
        "TranslatableContents": "network.chat.contents.TranslatableContents",
        "Commands": "commands.Commands",
        "CommandSourceStack": "commands.CommandSourceStack",
        "Holder$Reference": "core.Holder$Reference",
        "ParticleOptions": "core.particles.ParticleOptions",
        "Registries": "core.registries.Registries",
        "ReloadableServerResources": "server.ReloadableServerResources",
        "DedicatedServer": "server.dedicated.DedicatedServer",
        "Entity": "world.entity.Entity",
        "PlayerTeam": "world.scores.PlayerTeam",
        "Vec3": "world.phys.Vec3",
        "Vec2": "world.phys.Vec2",
    }

# API changes between the supported releases.
HAS_BUILD_CONTEXT = V >= (1, 19)
HAS_RESOURCE_ARGUMENT = V >= (1, 19, 3)
HAS_COMPONENT_CONTENTS = V >= (1, 19)
# MinecraftServer.resources holds the ReloadableResources record from 1.18.2 and the ServerResources before.
HAS_RELOADABLE_RESOURCES = V >= (1, 18, 2)
HAS_MINECRAFT_TO_BUKKIT = V >= (1, 20, 4)
# CraftParticle switched to minecraftToBukkit earlier than the other Craft* converters.
HAS_PARTICLE_MINECRAFT_TO_BUKKIT = V >= (1, 20, 2)

wrappers = []
CTX = "CommandContext<CommandSourceStack>"
CSS = "net.minecraft.commands.CommandSourceStack"
ARGS = "net.minecraft.commands.arguments"


def w(wrapper, body, imports=()):
    write(M, wrapper, body.replace("SUFFIX", SUFFIX), imports)
    wrappers.append(wrapper)


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
        super(null, "{NAMES[nms.split('.')[-1]]}");
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
    "ItemArgument.item(BuildContexts.current())" if HAS_BUILD_CONTEXT else "ItemArgument.item()", "return NMSArgumentPredicateItemStack.create(ItemArgument.getItem(CTX, name));",
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
    "ParticleArgument.particle(BuildContexts.current())" if HAS_RESOURCE_ARGUMENT else "ParticleArgument.particle()", "return NMSParticleParam.create(ParticleArgument.getParticle(CTX, name));",
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

w("NMSArgumentProfile", f'''    public NMSArgumentProfile_SUFFIX() {{
        super(null, "{NAMES["GameProfileArgument"]}");
    }}

    @Override
    public ArgumentType<?> argument() {{
        return GameProfileArgument.gameProfile();
    }}

    @Override
    protected Object parseImpl(CommandContext<?> ctx, String name) {{
        throw new UnsupportedOperationException();
    }}
''', [f"{ARGS}.GameProfileArgument"])

w("NMSArgumentPredicateItemStack", f'''    public NMSArgumentPredicateItemStack_SUFFIX(Object handle) {{
        super(handle, "{NAMES["ItemInput"]}");
    }}

    @Override
    public NMSItemStack createItemStack(int amount, boolean checkOverStack) {{
        try {{
            return NMSItemStack.create(((ItemInput) getHandle()).createItemStack(amount, checkOverStack));
        }} catch (CommandSyntaxException e) {{
            throw new UncheckedCommandSyntaxException(e);
        }}
    }}
''', [f"{ARGS}.item.ItemInput"])

w("NMSArgumentTileLocation", f'''    public NMSArgumentTileLocation_SUFFIX(Object handle) {{
        super(handle, "{NAMES["BlockInput"]}");
    }}

    @Override
    public NMSIBlockData getBlockData() {{
        return NMSIBlockData.create(((BlockInput) getHandle()).getState());
    }}
''', [f"{ARGS}.blocks.BlockInput"])

if HAS_COMPONENT_CONTENTS:
    w("NMSIChatMutableComponent", f'''    public NMSIChatMutableComponent_SUFFIX(Object handle) {{
        super(handle, "{NAMES["MutableComponent"]}");
    }}

    @Override
    public NMSTranslatableContents getContentsAsTranslatable() {{
        return NMSTranslatableContents.create(((MutableComponent) getHandle()).getContents());
    }}
    ''', ["net.minecraft.network.chat.MutableComponent"])

    w("NMSTranslatableContents", f'''    public NMSTranslatableContents_SUFFIX(Object handle) {{
        super(handle, "{NAMES["TranslatableContents"]}");
    }}

    @Override
    public String getKey() {{
        return ((TranslatableContents) getHandle()).getKey();
    }}

    @Override
    public Object[] getArgs() {{
        return ((TranslatableContents) getHandle()).getArgs();
    }}
    ''', ["net.minecraft.network.chat.contents.TranslatableContents"])
else:
    w("NMSChatMessage", f'''    public NMSChatMessage_SUFFIX(Message handle) {{
        super(handle, "{NAMES["TranslatableComponent"]}");
    }}

    @Override
    public String getKey() {{
        return ((TranslatableComponent) getHandle()).getKey();
    }}

    @Override
    public Object[] getArgs() {{
        return ((TranslatableComponent) getHandle()).getArgs();
    }}
''', ["net.minecraft.network.chat.TranslatableComponent", "com.mojang.brigadier.Message"])

w("NMSCommandDispatcher", f'''    public NMSCommandDispatcher_SUFFIX(Object handle) {{
        super(handle, "{NAMES["Commands"]}");
    }}

    @Override
    public CommandDispatcher<?> getBrigadier() {{
        return ((Commands) getHandle()).getDispatcher();
    }}
''', ["net.minecraft.commands.Commands", "com.mojang.brigadier.CommandDispatcher"])

if TARGET == "spigot-1.20.4":
    w("NMSCommandListenerWrapper", f'''    public NMSCommandListenerWrapper_SUFFIX(Object handle) {{
        super(handle, "{NAMES["CommandSourceStack"]}");
    }}

    @Override
    public CommandSender getBukkitSender() {{
        return source().getBukkitSender();
    }}

    @Override
    public org.bukkit.entity.Entity getBukkitEntity() {{
        Entity entity = source().getEntity();
        return entity != null ? entity.getBukkitEntity() : null;
    }}

    @Override
    public World getBukkitWorld() {{
        ServerLevel level = source().getLevel();
        return level != null ? level.getWorld() : null;
    }}

    @Override
    public Location getBukkitLocation() {{
        Vec3 pos = source().getPosition();
        World world = getBukkitWorld();
        return world != null && pos != null ? new Location(world, pos.x, pos.y, pos.z) : null;
    }}

    private CommandSourceStack source() {{
        return (CommandSourceStack) getHandle();
    }}
''', [CSS, "net.minecraft.server.level.ServerLevel", "net.minecraft.world.entity.Entity", "net.minecraft.world.phys.Vec3",
      "org.bukkit.Location", "org.bukkit.World", "org.bukkit.command.CommandSender"])
else:
    w("NMSCommandListenerWrapper", f'''    public NMSCommandListenerWrapper_SUFFIX(Object handle) {{
        super(handle, "{NAMES["CommandSourceStack"]}");
    }}

    @Override
    public CommandSender getBukkitSender() {{
        return source().getBukkitSender();
    }}

    @Override
    public org.bukkit.entity.Entity getBukkitEntity() {{
        Entity entity = source().getEntity();
        return entity != null ? entity.getBukkitEntity() : null;
    }}

    @Override
    public World getBukkitWorld() {{
        return source().getLevel()
                       .getWorld();
    }}

    @Override
    public Location getBukkitLocation() {{
        Vec3 pos = source().getPosition();
        Vec2 rotation = source().getRotation();
        return new Location(getBukkitWorld(), pos.x, pos.y, pos.z, rotation.y, rotation.x);
    }}

    private CommandSourceStack source() {{
        return (CommandSourceStack) getHandle();
    }}
''', [CSS, "net.minecraft.world.entity.Entity", "net.minecraft.world.phys.Vec2", "net.minecraft.world.phys.Vec3",
      "org.bukkit.Location", "org.bukkit.World", "org.bukkit.command.CommandSender"])

w("NMSVanillaCommandWrapper", '''    public NMSVanillaCommandWrapper_SUFFIX() {
        super(null, "command.VanillaCommandWrapper");
    }

    @Override
    @SuppressWarnings("unchecked")
    public BukkitCommand createInstance(NMSCommandDispatcher dispatcher, CommandNode<?> command) {
        return new VanillaCommandWrapper((Commands) dispatcher.getHandle(), (CommandNode<CommandSourceStack>) command);
    }
''', ["net.minecraft.commands.Commands", CSS, f"{CB}.command.VanillaCommandWrapper",
      "com.mojang.brigadier.tree.CommandNode", "org.bukkit.command.defaults.BukkitCommand"])

if HAS_COMPONENT_CONTENTS:
    w("NMSHolder.NMSReference", f'''    public NMSReference_SUFFIX(Object handle) {{
        super(handle, "{NAMES["Holder$Reference"]}");
    }}

    @Override
    public Object value() {{
        return ((Holder.Reference<?>) getHandle()).value();
    }}
    ''', ["net.minecraft.core.Holder", "net.kunmc.lab.commandlib.util.nms.core.NMSHolder"])

w("NMSParticleParam", f'''    public NMSParticleParam_SUFFIX(Object handle) {{
        super(handle, "{NAMES["ParticleOptions"]}");
    }}

    @Override
    public NMSParticle getParticle() {{
        return NMSParticle.create(((ParticleOptions) getHandle()).getType());
    }}
''', ["net.minecraft.core.particles.ParticleOptions"])

if HAS_RESOURCE_ARGUMENT:
    w("NMSRegistries", f'''    public NMSRegistries_SUFFIX() {{
        super(null, "{NAMES["Registries"]}");
    }}

    @Override
    public NMSResourceKey enchantment() {{
        return NMSResourceKey.create(Registries.ENCHANTMENT);
    }}

    @Override
    public NMSResourceKey mobEffect() {{
        return NMSResourceKey.create(Registries.MOB_EFFECT);
    }}
    ''', ["net.minecraft.core.registries.Registries"])

w("NMSCraftParticle", ('''    public NMSCraftParticle_SUFFIX() {
        super(null, "CraftParticle");
    }

    @Override
    public Particle toBukkit(NMSParticleParam nms) {
        return CraftParticle.CONVERSION;
    }
''').replace("CONVERSION", "minecraftToBukkit(((ParticleOptions) nms.getHandle()).getType())" if HAS_PARTICLE_MINECRAFT_TO_BUKKIT
                 else "toBukkit((ParticleOptions) nms.getHandle())"), ["net.minecraft.core.particles.ParticleOptions", f"{CB}.CraftParticle", "org.bukkit.Particle"])

w("NMSCraftServer", '''    public NMSCraftServer_SUFFIX(Server handle) {
        super(handle, "CraftServer");
    }

    @Override
    public NMSDedicatedServer getServer() {
        return NMSDedicatedServer.create(((CraftServer) getHandle()).getServer());
    }
''', [f"{CB}.CraftServer", "org.bukkit.Server"])

w("NMSDedicatedServer", f'''    public NMSDedicatedServer_SUFFIX(Object handle) {{
        super(handle, "{NAMES["DedicatedServer"]}");
    }}

    @Override
    public NMSCommandDispatcher getCommandDispatcher() {{
        return NMSCommandDispatcher.create(((DedicatedServer) getHandle()).getCommands());
    }}

    @Override
    public NMSDataPackResources getDataPackResources() {{
        return NMSDataPackResources.create(((DedicatedServer) getHandle()).{"resources.managers()" if HAS_RELOADABLE_RESOURCES else "resources"});
    }}
''', ["net.minecraft.server.dedicated.DedicatedServer"])

# ReloadableServerResources.commandBuildContext is private on Spigot. Up to 1.20.4 the reflection implementation reads
# it; from 1.20.5, whose reflection implementation assumes Paper, a context is built from public API.
if V >= (1, 20, 5) and not PAPER:
    w("NMSDataPackResources", f'''    public NMSDataPackResources_SUFFIX(Object handle) {{
        super(handle, "{NAMES["ReloadableServerResources"]}");
    }}

    @Override
    public NMSCommandBuildContext getCommandBuildContext() {{
        MinecraftServer server = MinecraftServer.getServer();
        return NMSCommandBuildContext.create(CommandBuildContext.simple(server.registryAccess(),
                                                                        server.getWorldData()
                                                                              .enabledFeatures()));
    }}
''', ["net.minecraft.commands.CommandBuildContext", "net.minecraft.server.MinecraftServer"])
if PAPER:
    w("NMSDataPackResources", f'''    public NMSDataPackResources_SUFFIX(Object handle) {{
        super(handle, "{NAMES["ReloadableServerResources"]}");
    }}

    @Override
    public NMSCommandBuildContext getCommandBuildContext() {{
        return NMSCommandBuildContext.create(PaperCommands.INSTANCE.getBuildContext());
    }}
''', ["io.papermc.paper.command.brigadier.PaperCommands"])

w("NMSCraftBlockData", '''    public NMSCraftBlockData_SUFFIX() {
        super(null, "block.data.CraftBlockData");
    }

    @Override
    public BlockData createData(NMSIBlockData nms) {
        return CraftBlockData.fromData((BlockState) nms.getHandle());
    }
''', ["net.minecraft.world.level.block.state.BlockState", f"{CB}.block.data.CraftBlockData",
      "org.bukkit.block.data.BlockData"])

w("NMSCraftEnchantment", ('''    public NMSCraftEnchantment_SUFFIX() {
        super(null, "enchantments.CraftEnchantment");
    }

    @Override
    public Enchantment createInstance(NMSEnchantment nms) {
        return CONVERSION((net.minecraft.world.item.enchantment.Enchantment) nms.getHandle());
    }
''').replace("CONVERSION", "CraftEnchantment.minecraftToBukkit" if HAS_MINECRAFT_TO_BUKKIT else "new CraftEnchantment"), [f"{CB}.enchantments.CraftEnchantment", "org.bukkit.enchantments.Enchantment"])

w("NMSCraftItemStack", '''    public NMSCraftItemStack_SUFFIX() {
        super(null, "inventory.CraftItemStack");
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
        super(handle, "potion.CraftPotionEffectType");
    }

    @Override
    public PotionEffectType createInstance(NMSMobEffectList nms) {
        return CONVERSION((MobEffect) nms.getHandle());
    }
''').replace("CONVERSION",
              "CraftPotionEffectType.minecraftToBukkit" if HAS_MINECRAFT_TO_BUKKIT else "new CraftPotionEffectType"), ["net.minecraft.world.effect.MobEffect", f"{CB}.potion.CraftPotionEffectType",
      "org.bukkit.potion.PotionEffectType"])

w("NMSEntity", f'''    public NMSEntity_SUFFIX(Object handle) {{
        super(handle, "{NAMES["Entity"]}");
    }}

    @Override
    public org.bukkit.entity.Entity getBukkitEntity() {{
        return ((Entity) getHandle()).getBukkitEntity();
    }}
''', ["net.minecraft.world.entity.Entity"])

w("NMSScoreboardTeam", f'''    public NMSScoreboardTeam_SUFFIX(Object handle) {{
        super(handle, "{NAMES["PlayerTeam"]}");
    }}

    @Override
    public String getName() {{
        return ((PlayerTeam) getHandle()).getName();
    }}
''', ["net.minecraft.world.scores.PlayerTeam"])

w("NMSVec3D", f'''    public NMSVec3D_SUFFIX(Object handle) {{
        super(handle, "{NAMES["Vec3"]}");
    }}

    @Override
    public double x() {{
        return ((Vec3) getHandle()).x;
    }}

    @Override
    public double y() {{
        return ((Vec3) getHandle()).y;
    }}

    @Override
    public double z() {{
        return ((Vec3) getHandle()).z;
    }}
''', ["net.minecraft.world.phys.Vec3"])

if "Vec2" in NAMES:
    w("NMSVec2D", f'''    public NMSVec2D_SUFFIX(Object handle) {{
        super(handle, "{NAMES["Vec2"]}");
    }}

    @Override
    public float x() {{
        return ((Vec2) getHandle()).x;
    }}

    @Override
    public float y() {{
        return ((Vec2) getHandle()).y;
    }}
''', ["net.minecraft.world.phys.Vec2"])

# Wrappers that only hold a handle look their class up by name. From 1.20.5 the reflection implementations use Paper's
# Mojang names, which Spigot does not have, so the Spigot modules provide the Spigot names.
if V >= (1, 20, 5) and not PAPER:
    for wrapper, class_name in [("NMSMobEffectList", "world.effect.MobEffectList"),
                                ("NMSIBlockData", "world.level.block.state.IBlockData"),
                                ("NMSParticle", "core.particles.Particle")]:
        w(wrapper, f'''    public {wrapper}_SUFFIX(Object handle) {{
        super(handle, "{class_name}");
    }}
''')

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
 * Tells NMSClassRegistry whether this package runs on Spigot. Paper shares the version range from 1.20.5, and its
 * remapper translates the Spigot names in this package's bytecode, but not the Spigot class names that the wrappers
 * look up by string. So the package matches only where the Spigot name MobEffectList is a class.
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
 * <p>Spigot 1.20.5-1.20.6 also has a class named MobEffect, but keeps CraftBukkit in a versioned package. Loading the
 * unversioned CraftServer then throws NoClassDefFoundError, which NMSClassRegistry treats as not matching.
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

register(wrappers)
print(f"{M}: {len(wrappers)} classes")
