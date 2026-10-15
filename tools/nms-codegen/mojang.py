"""
Typed classes for the modules compiled with Mojang names: Spigot 1.17 and later (remapped to Spigot names by the
build) and Paper 1.20.5 and later. Run as `generate.py <module>`, for example `generate.py spigot-1.20.4`.
"""
from codegen import module_package, write, write_class_list, write_source

TARGET = MODULE  # noqa: F821 - set by generate.py
CRAFTBUKKIT_PACKAGE = CRAFTBUKKIT  # noqa: F821 - set by generate.py, None for Paper and Spigot 26.1+


def ver(text):
    return tuple(int(x) for x in text.split("."))


platform, version = TARGET.split("-")
V = ver(version)
M = TARGET
SUFFIX = TARGET.replace("-", "_").replace(".", "_")
PAPER = platform == "paper"
CB = "org.bukkit.craftbukkit" if CRAFTBUKKIT_PACKAGE is None else "org.bukkit.craftbukkit." + CRAFTBUKKIT_PACKAGE

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
# ServerFunctionLibrary#reload lost its profilers in 1.21.2 and takes a SharedState in place of the resource manager
# from 1.21.9.
RELOAD_DROPPED_PROFILERS = V >= (1, 21, 2)
RELOAD_HAS_SHARED_STATE = V >= (1, 21, 9)
# CommandSourceStack#sendSuccess takes a Supplier of the message from 1.20.
SEND_SUCCESS_TAKES_SUPPLIER = V >= (1, 20)

RESOURCE_MANAGER = "server.resources.resourceManager()" if HAS_RELOADABLE_RESOURCES \
    else "server.resources.getResourceManager()"
BARRIER = """new PreparableReloadListener.PreparationBarrier() {
                           @Override
                           public <T> CompletableFuture<T> wait(T value) {
                               return CompletableFuture.completedFuture(value);
                           }
                       }"""
_SEP = ",\n" + " " * 23
if RELOAD_HAS_SHARED_STATE:
    RELOAD_ARGUMENTS = _SEP.join([f"new PreparableReloadListener.SharedState({RESOURCE_MANAGER})", "Runnable::run",
                                  BARRIER, "Runnable::run"])
elif RELOAD_DROPPED_PROFILERS:
    RELOAD_ARGUMENTS = _SEP.join([BARRIER, RESOURCE_MANAGER, "Runnable::run", "Runnable::run"])
else:
    RELOAD_ARGUMENTS = _SEP.join([BARRIER, RESOURCE_MANAGER, "InactiveProfiler.INSTANCE", "InactiveProfiler.INSTANCE",
                                  "Runnable::run", "Runnable::run"])

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


def arg(wrapper, nms, ret, argument, parse, imports=(), throws=True, extra=""):
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
{extra}'''
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
# 1.21.11 renamed ResourceLocation to Identifier.
ID_ARGUMENT = "IdentifierArgument" if V >= (1, 21, 11) else "ResourceLocationArgument"
ID_CLASS = "Identifier" if V >= (1, 21, 11) else "ResourceLocation"
# 1.21 made the constructor of ResourceLocation private.
LOAD_TAG_ID = f'{ID_CLASS}.withDefaultNamespace("load")' if V >= (1, 21) else 'new ResourceLocation("minecraft", "load")'
arg("NMSArgumentNamespacedKey", f"commands.arguments.{ID_ARGUMENT}", "String",
    f"{ID_ARGUMENT}.id()", f"return {ID_ARGUMENT}.getId(CTX, name).toString();", throws=False)
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
    "return NMSVec3D.create(Vec3Argument.getVec3(CTX, name));", throws=False, extra='''
    @Override
    public ArgumentType<?> argument(boolean centerIntegers) {
        return Vec3Argument.vec3(centerIntegers);
    }
''')
# Coordinates#getBlockPos, unlike BlockPosArgument#getLoadedBlockPos, checks neither that the position is loaded nor
# that it is inside the world, as Paper's blockPosition() resolver.
arg("NMSArgumentBlockPosition", "commands.arguments.coordinates.BlockPosArgument", "BlockVector",
    "BlockPosArgument.blockPos()",
    "BlockPos pos = CTX.getArgument(name, Coordinates.class)\n"
    "                           .getBlockPos(CTX.getSource());\n"
    "        return new BlockVector(pos.getX(), pos.getY(), pos.getZ());",
    ["net.minecraft.commands.arguments.coordinates.Coordinates", "net.minecraft.core.BlockPos",
     "org.bukkit.util.BlockVector"], throws=False)
# getItemPredicate resolves item tags, and fails on an unknown one, until 1.19, which resolves them while parsing.
arg("NMSArgumentItemPredicate", "commands.arguments.item.ItemPredicateArgument", "Predicate<ItemStack>",
    "ItemPredicateArgument.itemPredicate(BuildContexts.current())" if HAS_BUILD_CONTEXT
    else "ItemPredicateArgument.itemPredicate()",
    "Predicate<net.minecraft.world.item.ItemStack> predicate = ItemPredicateArgument.getItemPredicate(CTX, name);\n"
    + (" " * 8 if HAS_BUILD_CONTEXT else " " * 12) + "return item -> predicate.test(CraftItemStack.asNMSCopy(item));",
    ["java.util.function.Predicate", "org.bukkit.inventory.ItemStack", f"{CB}.inventory.CraftItemStack"],
    throws=not HAS_BUILD_CONTEXT)
# The block is tested in its world, loading its chunk as /execute if block does. getBlockPredicate fails on an unknown
# block tag.
arg("NMSArgumentBlockPredicate", "commands.arguments.blocks.BlockPredicateArgument", "Predicate<Block>",
    "BlockPredicateArgument.blockPredicate(BuildContexts.current())" if HAS_BUILD_CONTEXT
    else "BlockPredicateArgument.blockPredicate()",
    "Predicate<BlockInWorld> predicate = BlockPredicateArgument.getBlockPredicate(CTX, name);\n"
    "            return block -> predicate.test(new BlockInWorld(((CraftWorld) block.getWorld()).getHandle(),\n"
    "                                                            new BlockPos(block.getX(), block.getY(), block.getZ()),\n"
    "                                                            true));",
    ["java.util.function.Predicate", "org.bukkit.block.Block", "net.minecraft.core.BlockPos",
     "net.minecraft.world.level.block.state.pattern.BlockInWorld", f"{CB}.CraftWorld"])
# MinMaxBounds turned into records with Optional bounds in 1.20.2. `var` keeps its package, which 1.21.11 renamed from
# advancements.critereon to advancements.criterion, out of the imports. 26.2 moved it to advancements.predicates, and
# the 26.x module runs on both, so it does not name the class and reads min() and max() of its interface by reflection.
if V >= (26,):
    INT_BOUNDS = ("Object bounds = CTX.getArgument(name, Object.class);\n"
                  '        return range((Integer) bound(bounds, "min"), (Integer) bound(bounds, "max"));')
    FLOAT_BOUNDS = ("Object bounds = CTX.getArgument(name, Object.class);\n"
                    '        return doubleRange((Number) bound(bounds, "min"), (Number) bound(bounds, "max"));')
else:
    BOUNDS = "bounds.min().orElse(null), bounds.max().orElse(null)" if V >= (1, 20, 2) \
        else "bounds.getMin(), bounds.getMax()"
    INT_BOUNDS = f"var bounds = RangeArgument.Ints.getRange(CTX, name);\n        return range({BOUNDS});"
    FLOAT_BOUNDS = f"var bounds = RangeArgument.Floats.getRange(CTX, name);\n        return doubleRange({BOUNDS});"
arg("NMSArgumentIntRange", "commands.arguments.RangeArgument", "Range<Integer>", "RangeArgument.intRange()",
    INT_BOUNDS, ["com.google.common.collect.Range"], throws=False)
arg("NMSArgumentFloatRange", "commands.arguments.RangeArgument", "Range<Double>", "RangeArgument.floatRange()",
    FLOAT_BOUNDS, ["com.google.common.collect.Range"], throws=False)
# Vanilla components reach Bungee through the JSON that CraftChatMessage writes, which is one object or string, so
# ComponentSerializer.parse returns one component. ComponentArgument takes the registries from 1.20.5 and reads SNBT,
# with getRawComponent in place of getComponent, from 1.21.5.
BUNGEE = [f"{CB}.util.CraftChatMessage", "net.md_5.bungee.api.chat.BaseComponent",
          "net.md_5.bungee.chat.ComponentSerializer"]
arg("NMSArgumentChatComponent", "commands.arguments.ComponentArgument", "BaseComponent",
    "ComponentArgument.textComponent(BuildContexts.current())" if V >= (1, 20, 5)
    else "ComponentArgument.textComponent()",
    "return ComponentSerializer.parse(CraftChatMessage.toJSON(ComponentArgument.GET(CTX, name)))[0];"
    .replace("GET", "getRawComponent" if V >= (1, 21, 5) else "getComponent"), BUNGEE, throws=False)
arg("NMSArgumentChat", "commands.arguments.MessageArgument", "BaseComponent", "MessageArgument.message()",
    "return ComponentSerializer.parse(CraftChatMessage.toJSON(MessageArgument.getMessage(CTX, name)))[0];", BUNGEE)
arg("NMSArgumentNBTTag", "commands.arguments.CompoundTagArgument", "String", "CompoundTagArgument.compoundTag()",
    "return CompoundTagArgument.getCompoundTag(CTX, name).toString();", throws=False)

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

# 26.1 dropped the checkOverStack parameter and always checks the stack size.
w("NMSArgumentPredicateItemStack", '''    public NMSArgumentPredicateItemStack_SUFFIX(Object handle) {
        super(handle, ItemInput.class);
    }

    @Override
    public NMSItemStack createItemStack(int amount, boolean checkOverStack) {
        try {
            return NMSItemStack.create(((ItemInput) requireHandle()).createItemStack(CREATE_ARGUMENTS));
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
'''.replace("CREATE_ARGUMENTS", "amount" if V >= (26,) else "amount, checkOverStack"), [f"{ARGS}.item.ItemInput"])

w("NMSArgumentTileLocation", '''    public NMSArgumentTileLocation_SUFFIX(Object handle) {
        super(handle, BlockInput.class);
    }

    @Override
    public NMSIBlockData getBlockData() {
        return NMSIBlockData.create(((BlockInput) requireHandle()).getState());
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
        return NMSTranslatableContents.create(((MutableComponent) requireHandle()).getContents());
    }
''', ["net.minecraft.network.chat.MutableComponent"])

    w("NMSTranslatableContents", '''    public NMSTranslatableContents_SUFFIX(Object handle) {
        super(handle, TranslatableContents.class);
    }

    @Override
    public String getKey() {
        return ((TranslatableContents) requireHandle()).getKey();
    }

    @Override
    public Object[] getArgs() {
        return ((TranslatableContents) requireHandle()).getArgs();
    }
''', ["net.minecraft.network.chat.contents.TranslatableContents"])
else:
    w("NMSChatMessage", '''    public NMSChatMessage_SUFFIX(Message handle) {
        super(handle, TranslatableComponent.class);
    }

    @Override
    public String getKey() {
        return ((TranslatableComponent) requireHandle()).getKey();
    }

    @Override
    public Object[] getArgs() {
        return ((TranslatableComponent) requireHandle()).getArgs();
    }
''', ["net.minecraft.network.chat.TranslatableComponent", "com.mojang.brigadier.Message"])

if HAS_BUILD_CONTEXT:
    handle("NMSCommandBuildContext", "net.minecraft.commands.CommandBuildContext")

w("NMSCommandDispatcher", '''    public NMSCommandDispatcher_SUFFIX(Object handle) {
        super(handle, Commands.class);
    }

    @Override
    public CommandDispatcher<?> getBrigadier() {
        return ((Commands) requireHandle()).getDispatcher();
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
    @Nullable
    public org.bukkit.entity.Entity getBukkitEntity() {
        Entity entity = source().getEntity();
        return entity != null ? entity.getBukkitEntity() : null;
    }

    @Override
    @Nullable
    public World getBukkitWorld() {
        ServerLevel level = source().getLevel();
        return level != null ? level.getWorld() : null;
    }

    @Override
    @Nullable
    protected NMSVec3D getPosition() {
        Vec3 position = source().getPosition();
        return position != null ? NMSVec3D.create(position) : null;
    }

    @Override
    @Nullable
    protected NMSVec2D getRotation() {
        Vec2 rotation = source().getRotation();
        return rotation != null ? NMSVec2D.create(rotation) : null;
    }

    @Override
    public void sendSuccess(BaseComponent component, boolean broadcastToOps) {
        Component message = component(component);
        SEND_SUCCESS
    }

    @Override
    public Message toMessage(BaseComponent component) {
        return component(component);
    }

    private static Component component(BaseComponent component) {
        return CraftChatMessage.fromJSON(ComponentSerializer.toString(component));
    }

    private CommandSourceStack source() {
        return (CommandSourceStack) requireHandle();
    }
'''.replace("SEND_SUCCESS", "source().sendSuccess(() -> message, broadcastToOps);" if SEND_SUCCESS_TAKES_SUPPLIER
            else "source().sendSuccess(message, broadcastToOps);"),
  [CSS, "net.minecraft.server.level.ServerLevel", "net.minecraft.world.entity.Entity", "net.minecraft.world.phys.Vec2",
   "net.minecraft.world.phys.Vec3", "org.bukkit.World", "org.bukkit.command.CommandSender",
   "net.minecraft.network.chat.Component", f"{CB}.util.CraftChatMessage", "net.md_5.bungee.api.chat.BaseComponent",
   "net.md_5.bungee.chat.ComponentSerializer", "com.mojang.brigadier.Message"])

w("NMSVanillaCommandWrapper", '''    public NMSVanillaCommandWrapper_SUFFIX() {
        super(null, VanillaCommandWrapper.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public BukkitCommand createInstance(NMSCommandDispatcher dispatcher, CommandNode<?> command) {
        return new VanillaCommandWrapper((Commands) dispatcher.requireHandle(), (CommandNode<CommandSourceStack>) command);
    }
''', ["net.minecraft.commands.Commands", CSS, f"{CB}.command.VanillaCommandWrapper",
      "com.mojang.brigadier.tree.CommandNode", "org.bukkit.command.defaults.BukkitCommand"])

handle("NMSParticle", "net.minecraft.core.particles.ParticleType")

w("NMSParticleParam", '''    public NMSParticleParam_SUFFIX(Object handle) {
        super(handle, ParticleOptions.class);
    }

    @Override
    public NMSParticle getParticle() {
        return NMSParticle.create(((ParticleOptions) requireHandle()).getType());
    }
''', ["net.minecraft.core.particles.ParticleOptions"])

w("NMSCraftParticle", ('''    public NMSCraftParticle_SUFFIX() {
        super(null, CraftParticle.class);
    }

    @Override
    public Particle toBukkit(NMSParticleParam nms) {
        return CraftParticle.CONVERSION;
    }
''').replace("CONVERSION", "minecraftToBukkit(((ParticleOptions) nms.requireHandle()).getType())"
             if HAS_PARTICLE_MINECRAFT_TO_BUKKIT else "toBukkit((ParticleOptions) nms.requireHandle())"),
  ["net.minecraft.core.particles.ParticleOptions", f"{CB}.CraftParticle", "org.bukkit.Particle"])

w("NMSCraftServer", '''    public NMSCraftServer_SUFFIX(Server handle) {
        super(handle, CraftServer.class);
    }

    @Override
    public NMSDedicatedServer getServer() {
        return NMSDedicatedServer.create(((CraftServer) requireHandle()).getServer());
    }

    @Override
    public SimpleCommandMap getCommandMap() {
        return ((CraftServer) requireHandle()).getCommandMap();
    }
''', [f"{CB}.CraftServer", "org.bukkit.Server", "org.bukkit.command.SimpleCommandMap"])

# Paper 1.20.6 dropped MinecraftServer.vanillaCommandDispatcher with its Brigadier command API and keeps one dispatcher.
# The Paper 1.20.6 module also runs on Paper 1.20.5, so it reads the CraftBukkit field when the server still has it.
VANILLA_DISPATCHER = '''        DedicatedServer server = (DedicatedServer) requireHandle();
        try {
            return NMSCommandDispatcher.create(MinecraftServer.class.getField("vanillaCommandDispatcher")
                                                                    .get(server));
        } catch (NoSuchFieldException e) {
            return NMSCommandDispatcher.create(server.getCommands());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
''' if PAPER else '''        return NMSCommandDispatcher.create(((DedicatedServer) requireHandle()).vanillaCommandDispatcher);
'''

w("NMSDedicatedServer", f'''    public NMSDedicatedServer_SUFFIX(Object handle) {{
        super(handle, DedicatedServer.class);
    }}

    @Override
    public NMSCommandDispatcher getCommandDispatcher() {{
        return NMSCommandDispatcher.create(((DedicatedServer) requireHandle()).getCommands());
    }}

    @Override
    public NMSCommandDispatcher getVanillaCommandDispatcher() {{
{VANILLA_DISPATCHER}    }}

    @Override
    public NMSDataPackResources getDataPackResources() {{
        return NMSDataPackResources.create(getDataPackResourcesHandle());
    }}

    @Override
    public Object getDataPackResourcesHandle() {{
        return ((DedicatedServer) requireHandle()).{"resources.managers()" if HAS_RELOADABLE_RESOURCES else "resources"};
    }}

    @Override
    public void recompileFunctions(NMSCommandDispatcher dispatcher, boolean reloaded) {{
        DedicatedServer server = (DedicatedServer) requireHandle();
        ServerFunctionLibrary library = server.{"resources.managers()" if HAS_RELOADABLE_RESOURCES else "resources"}.getFunctionLibrary();
        boolean loadMissing = reloaded && loadTagEmpty(library);
        setFunctionDispatcher(library, ServerFunctionLibrary.class, dispatcher);
        library.reload({RELOAD_ARGUMENTS})
               .join();
        keepPendingLoad(server.getFunctions(),
                        () -> server.getFunctions().replaceLibrary(library),
                        loadMissing && !loadTagEmpty(library));
    }}

    private static boolean loadTagEmpty(ServerFunctionLibrary library) {{
        return library.getTag({LOAD_TAG_ID}){".getValues()" if V < (1, 19) else ""}.isEmpty();
    }}

    @Override
    public boolean isRunning() {{
        return ((DedicatedServer) requireHandle()).isRunning();
    }}
''', ["net.minecraft.server.dedicated.DedicatedServer",
      *(["net.minecraft.server.MinecraftServer"] if PAPER else []),
      "net.minecraft.server.ServerFunctionLibrary",
      f"net.minecraft.resources.{ID_CLASS}",
      "net.minecraft.server.packs.resources.PreparableReloadListener",
      *([] if RELOAD_DROPPED_PROFILERS else ["net.minecraft.util.profiling.InactiveProfiler"]),
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
                    return NMSCommandBuildContext.create(field.get(requireHandle()));
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
        return CraftBlockData.fromData((BlockState) nms.requireHandle());
    }
''', ["net.minecraft.world.level.block.state.BlockState", f"{CB}.block.data.CraftBlockData",
      "org.bukkit.block.data.BlockData"])

w("NMSCraftEnchantment", ('''    public NMSCraftEnchantment_SUFFIX() {
        super(null, CraftEnchantment.class);
    }

    @Override
    public Enchantment createInstance(NMSEnchantment nms) {
        return CONVERSION((net.minecraft.world.item.enchantment.Enchantment) nms.requireHandle());
    }
''').replace("CONVERSION", "CraftEnchantment.minecraftToBukkit" if HAS_MINECRAFT_TO_BUKKIT else "new CraftEnchantment"),
  [f"{CB}.enchantments.CraftEnchantment", "org.bukkit.enchantments.Enchantment"])

w("NMSCraftItemStack", '''    public NMSCraftItemStack_SUFFIX() {
        super(null, CraftItemStack.class);
    }

    @Override
    public org.bukkit.inventory.ItemStack asCraftMirror(NMSItemStack nms) {
        return CraftItemStack.asCraftMirror((ItemStack) nms.requireHandle());
    }
''', ["net.minecraft.world.item.ItemStack", f"{CB}.inventory.CraftItemStack"])

w("NMSCraftPotionEffectType", ('''    public NMSCraftPotionEffectType_SUFFIX() {
        super(null, CraftPotionEffectType.class);
    }

    public NMSCraftPotionEffectType_SUFFIX(Object handle) {
        super(handle, CraftPotionEffectType.class);
    }

    @Override
    public PotionEffectType createInstance(NMSMobEffectList nms) {
        return CONVERSION((MobEffect) nms.requireHandle());
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
        return ((Entity) requireHandle()).getBukkitEntity();
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
        return ((PlayerTeam) requireHandle()).getName();
    }
''', ["net.minecraft.world.scores.PlayerTeam"])

w("NMSVec3D", '''    public NMSVec3D_SUFFIX(Object handle) {
        super(handle, Vec3.class);
    }

    @Override
    public double x() {
        return ((Vec3) requireHandle()).x;
    }

    @Override
    public double y() {
        return ((Vec3) requireHandle()).y;
    }

    @Override
    public double z() {
        return ((Vec3) requireHandle()).z;
    }
''', ["net.minecraft.world.phys.Vec3"])

w("NMSVec2D", '''    public NMSVec2D_SUFFIX(Object handle) {
        super(handle, Vec2.class);
    }

    @Override
    public float x() {
        return ((Vec2) requireHandle()).x;
    }

    @Override
    public float y() {
        return ((Vec2) requireHandle()).y;
    }
''', ["net.minecraft.world.phys.Vec2"])

HELPER_PACKAGE = module_package(M)
if HAS_BUILD_CONTEXT:
    write_source(M, HELPER_PACKAGE, "BuildContexts", f"""package {HELPER_PACKAGE};

import dev.marulabs.commandlib.util.nms.server.NMSCraftServer;
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
                                                   .requireHandle();
    }}
}}
""")

# Spigot runs with Mojang names from 26.1, where no remapping can make a Mojang-named class mismatch.
if (1, 20, 5) <= V < (26,) and not PAPER:
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

if V >= (26,) and not PAPER:
    write_source(M, HELPER_PACKAGE, "MappingProbe", f"""package {HELPER_PACKAGE};

/**
 * Tells NMSClassRegistry whether this package runs on Spigot. Paper shares the Mojang names and CraftBukkit package
 * from 26.1 but registers commands its own way, which the paper module handles, so this package matches only where
 * Paper's lifecycle API is missing.
 */
final class MappingProbe {{
    private MappingProbe() {{
    }}

    static boolean matches() {{
        try {{
            Class.forName("io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager",
                          false,
                          MappingProbe.class.getClassLoader());
            return false;
        }} catch (ClassNotFoundException e) {{
            return true;
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
