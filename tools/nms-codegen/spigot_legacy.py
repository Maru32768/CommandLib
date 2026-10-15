"""
Typed classes for Spigot 1.16.5, which compiles against the Spigot-mapped server jar. Run as
`generate.py spigot-1.16.5`.
"""
from codegen import write, write_class_list

M = MODULE  # noqa: F821 - set by generate.py
SUFFIX = M.replace("-", "_").replace(".", "_")
NMS = "net.minecraft.server." + CRAFTBUKKIT  # noqa: F821 - set by generate.py
CB = "org.bukkit.craftbukkit." + CRAFTBUKKIT  # noqa: F821
classes = []


def w(wrapper, body, imports=()):
    classes.append(write(M, wrapper, body.replace("SUFFIX", SUFFIX), imports))


CTX = "CommandContext<CommandListenerWrapper>"


def arg(wrapper, nms_class, class_name, ret, argument, parse, extra_imports=(), throws=True):
    cast = f"        {CTX} context = ({CTX}) ctx;\n"
    if throws:
        parse_body = cast + f"""        try {{
            {parse}
        }} catch (CommandSyntaxException e) {{
            throw new UncheckedCommandSyntaxException(e);
        }}"""
    else:
        parse_body = cast + f"        {parse}"
    body = f'''    public {wrapper}_SUFFIX() {{
        super(null, {class_name}.class);
    }}

    @Override
    public ArgumentType<?> argument() {{
        return {argument};
    }}

    @Override
    @SuppressWarnings("unchecked")
    protected {ret} parseImpl(CommandContext<?> ctx, String name) {{
{parse_body}
    }}
'''
    w(wrapper, body, [f"{NMS}.{nms_class}", f"{NMS}.CommandListenerWrapper", *extra_imports])


def c(expr):
    return expr.replace("CTX", "context")


arg("NMSArgumentDimension", "ArgumentDimension", "ArgumentDimension", "World", "ArgumentDimension.a()",
    c("return ArgumentDimension.a(CTX, name).getWorld();"), ["org.bukkit.World"])
arg("NMSArgumentEnchantment", "ArgumentEnchantment", "ArgumentEnchantment", "NMSEnchantment",
    "ArgumentEnchantment.a()", c("return NMSEnchantment.create(ArgumentEnchantment.a(CTX, name));"), throws=False)
arg("NMSArgumentEntities", "ArgumentEntity", "ArgumentEntity", "List<Entity>", "ArgumentEntity.multipleEntities()",
    c("return ArgumentEntity.b(CTX, name).stream()\n"
      "                                 .map(x -> ((Entity) x.getBukkitEntity()))\n"
      "                                 .collect(Collectors.toList());"), ["org.bukkit.entity.Entity"])
arg("NMSArgumentEntity", "ArgumentEntity", "ArgumentEntity", "Entity", "ArgumentEntity.a()",
    c("return ArgumentEntity.a(CTX, name).getBukkitEntity();"), ["org.bukkit.entity.Entity"])
arg("NMSArgumentItemStack", "ArgumentItemStack", "ArgumentItemStack", "NMSArgumentPredicateItemStack",
    "ArgumentItemStack.a()", c("return NMSArgumentPredicateItemStack.create(ArgumentItemStack.a(CTX, name));"), throws=False)
arg("NMSArgumentMobEffect", "ArgumentMobEffect", "ArgumentMobEffect", "NMSMobEffectList",
    "new ArgumentMobEffect()", c("return NMSMobEffectList.create(ArgumentMobEffect.a(CTX, name));"))
arg("NMSArgumentNamespacedKey", "ArgumentMinecraftKeyRegistered", "ArgumentMinecraftKeyRegistered", "String",
    "ArgumentMinecraftKeyRegistered.a()", c("return ArgumentMinecraftKeyRegistered.e(CTX, name).toString();"), throws=False)
arg("NMSArgumentParticle", "ArgumentParticle", "ArgumentParticle", "NMSParticleParam", "new ArgumentParticle()",
    c("return NMSParticleParam.create(ArgumentParticle.a(CTX, name));"), throws=False)
arg("NMSArgumentPlayer", "ArgumentEntity", "ArgumentEntity", "Player", "ArgumentEntity.c()",
    c("return ArgumentEntity.e(CTX, name).getBukkitEntity();"), ["org.bukkit.entity.Player"])
arg("NMSArgumentPlayers", "ArgumentEntity", "ArgumentEntity", "List<Player>", "ArgumentEntity.d()",
    c("return ArgumentEntity.f(CTX, name).stream()\n"
      "                                 .map(x -> ((Player) x.getBukkitEntity()))\n"
      "                                 .collect(Collectors.toList());"), ["org.bukkit.entity.Player"])
arg("NMSArgumentScoreboardTeam", "ArgumentScoreboardTeam", "ArgumentScoreboardTeam", "NMSScoreboardTeam",
    "new ArgumentScoreboardTeam()", c("return NMSScoreboardTeam.create(ArgumentScoreboardTeam.a(CTX, name));"))
arg("NMSArgumentTile", "ArgumentTile", "ArgumentTile", "NMSArgumentTileLocation", "ArgumentTile.a()",
    c("return NMSArgumentTileLocation.create(ArgumentTile.a(CTX, name));"), throws=False)
arg("NMSArgumentVec3D", "ArgumentVec3", "ArgumentVec3", "NMSVec3D", "ArgumentVec3.a()",
    c("return NMSVec3D.create(ArgumentVec3.a(CTX, name));"))
# ArgumentCriterionValue is Mojang's RangeArgument and CriterionConditionValue its MinMaxBounds, whose a() and b() are
# getMin() and getMax(). The float range has no static getter.
arg("NMSArgumentIntRange", "ArgumentCriterionValue", "ArgumentCriterionValue", "Range<Integer>",
    "ArgumentCriterionValue.a()",
    c("CriterionConditionValue.IntegerRange bounds = ArgumentCriterionValue.b.a(CTX, name);\n"
      "        return range(bounds.a(), bounds.b());"),
    ["com.google.common.collect.Range", f"{NMS}.CriterionConditionValue"], throws=False)
arg("NMSArgumentFloatRange", "ArgumentCriterionValue", "ArgumentCriterionValue", "Range<Double>",
    "ArgumentCriterionValue.b()",
    c("CriterionConditionValue.FloatRange bounds = CTX.getArgument(name, CriterionConditionValue.FloatRange.class);\n"
      "        return doubleRange(bounds.a(), bounds.b());"),
    ["com.google.common.collect.Range", f"{NMS}.CriterionConditionValue"], throws=False)

w("NMSArgumentProfile", '''    public NMSArgumentProfile_SUFFIX() {
        super(null, ArgumentProfile.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return new ArgumentProfile();
    }

    @Override
    protected Object parseImpl(CommandContext<?> ctx, String name) {
        throw new UnsupportedOperationException();
    }
''', [f"{NMS}.ArgumentProfile"])

w("NMSArgumentPredicateItemStack", '''    public NMSArgumentPredicateItemStack_SUFFIX(Object handle) {
        super(handle, ArgumentPredicateItemStack.class);
    }

    @Override
    public NMSItemStack createItemStack(int amount, boolean checkOverStack) {
        try {
            return NMSItemStack.create(((ArgumentPredicateItemStack) requireHandle()).a(amount, checkOverStack));
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
''', [f"{NMS}.ArgumentPredicateItemStack"])

w("NMSArgumentTileLocation", '''    public NMSArgumentTileLocation_SUFFIX(Object handle) {
        super(handle, ArgumentTileLocation.class);
    }

    @Override
    public NMSIBlockData getBlockData() {
        return NMSIBlockData.create(((ArgumentTileLocation) requireHandle()).a());
    }
''', [f"{NMS}.ArgumentTileLocation"])

w("NMSChatMessage", '''    public NMSChatMessage_SUFFIX(Message handle) {
        super(handle, ChatMessage.class);
    }

    @Override
    public String getKey() {
        return ((ChatMessage) requireHandle()).getKey();
    }

    @Override
    public Object[] getArgs() {
        return ((ChatMessage) requireHandle()).getArgs();
    }
''', [f"{NMS}.ChatMessage", "com.mojang.brigadier.Message"])

w("NMSCommandDispatcher", '''    public NMSCommandDispatcher_SUFFIX(Object handle) {
        super(handle, CommandDispatcher.class);
    }

    @Override
    public com.mojang.brigadier.CommandDispatcher<?> getBrigadier() {
        return ((CommandDispatcher) requireHandle()).a();
    }
''', [f"{NMS}.CommandDispatcher"])

w("NMSCommandListenerWrapper", '''    public NMSCommandListenerWrapper_SUFFIX(Object handle) {
        super(handle, CommandListenerWrapper.class);
    }

    @Override
    public CommandSender getBukkitSender() {
        return wrapper().getBukkitSender();
    }

    @Override
    @Nullable
    public org.bukkit.entity.Entity getBukkitEntity() {
        Entity entity = wrapper().getEntity();
        return entity != null ? entity.getBukkitEntity() : null;
    }

    @Override
    @Nullable
    public World getBukkitWorld() {
        WorldServer world = wrapper().getWorld();
        return world != null ? world.getWorld() : null;
    }

    @Override
    @Nullable
    protected NMSVec3D getPosition() {
        Vec3D position = wrapper().getPosition();
        return position != null ? NMSVec3D.create(position) : null;
    }

    @Override
    @Nullable
    protected NMSVec2D getRotation() {
        Vec2F rotation = wrapper().i();
        return rotation != null ? NMSVec2D.create(rotation) : null;
    }

    @Override
    public void sendSuccess(BaseComponent component, boolean broadcastToOps) {
        wrapper().sendMessage(component(component), broadcastToOps);
    }

    @Override
    public Message toMessage(BaseComponent component) {
        return component(component);
    }

    private static IChatBaseComponent component(BaseComponent component) {
        return CraftChatMessage.fromJSON(ComponentSerializer.toString(component));
    }

    private CommandListenerWrapper wrapper() {
        return (CommandListenerWrapper) requireHandle();
    }
''', [f"{NMS}.CommandListenerWrapper", f"{NMS}.Entity", f"{NMS}.Vec2F", f"{NMS}.Vec3D", f"{NMS}.WorldServer",
      "org.bukkit.World", "org.bukkit.command.CommandSender", f"{NMS}.IChatBaseComponent", f"{CB}.util.CraftChatMessage",
      "net.md_5.bungee.api.chat.BaseComponent", "net.md_5.bungee.chat.ComponentSerializer",
      "com.mojang.brigadier.Message"])

w("NMSVanillaCommandWrapper", '''    public NMSVanillaCommandWrapper_SUFFIX() {
        super(null, VanillaCommandWrapper.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public BukkitCommand createInstance(NMSCommandDispatcher dispatcher, CommandNode<?> command) {
        return new VanillaCommandWrapper((CommandDispatcher) dispatcher.requireHandle(),
                                         (CommandNode<CommandListenerWrapper>) command);
    }
''', [f"{NMS}.CommandDispatcher", f"{NMS}.CommandListenerWrapper", f"{CB}.command.VanillaCommandWrapper",
      "com.mojang.brigadier.tree.CommandNode", "org.bukkit.command.defaults.BukkitCommand"])

w("NMSParticleParam", '''    public NMSParticleParam_SUFFIX(Object handle) {
        super(handle, ParticleParam.class);
    }

    @Override
    public NMSParticle getParticle() {
        return NMSParticle.create(((ParticleParam) requireHandle()).getParticle());
    }
''', [f"{NMS}.ParticleParam"])

w("NMSCraftParticle", '''    public NMSCraftParticle_SUFFIX() {
        super(null, CraftParticle.class);
    }

    @Override
    public Particle toBukkit(NMSParticleParam nms) {
        return CraftParticle.toBukkit((ParticleParam) nms.requireHandle());
    }
''', [f"{NMS}.ParticleParam", f"{CB}.CraftParticle", "org.bukkit.Particle"])

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

w("NMSDedicatedServer", '''    public NMSDedicatedServer_SUFFIX(Object handle) {
        super(handle, DedicatedServer.class);
    }

    @Override
    public NMSCommandDispatcher getCommandDispatcher() {
        return NMSCommandDispatcher.create(((DedicatedServer) requireHandle()).getCommandDispatcher());
    }

    @Override
    public NMSCommandDispatcher getVanillaCommandDispatcher() {
        return NMSCommandDispatcher.create(((DedicatedServer) requireHandle()).vanillaCommandDispatcher);
    }

    @Override
    public NMSDataPackResources getDataPackResources() {
        return NMSDataPackResources.create(getDataPackResourcesHandle());
    }

    @Override
    public Object getDataPackResourcesHandle() {
        return ((DedicatedServer) requireHandle()).dataPackResources;
    }

    @Override
    public void recompileFunctions(NMSCommandDispatcher dispatcher) {
        DedicatedServer server = (DedicatedServer) requireHandle();
        CustomFunctionManager library = server.dataPackResources.a();
        setFunctionDispatcher(library, CustomFunctionManager.class, dispatcher);
        // Mohist remaps IReloadListener but not its inner barrier interface, so a class implementing it fails to link
        // there; the barrier is a proxy of the reload method's parameter type instead.
        Method reload = null;
        for (Method method : CustomFunctionManager.class.getMethods()) {
            if (method.getParameterCount() == 6 && method.getReturnType() == CompletableFuture.class) {
                reload = method;
            }
        }
        if (reload == null) {
            throw new IllegalStateException("CustomFunctionManager has no reload method");
        }
        Class<?> barrierType = reload.getParameterTypes()[0];
        Object barrier = Proxy.newProxyInstance(barrierType.getClassLoader(),
                                                new Class<?>[]{barrierType},
                                                (proxy, method, args) -> {
                                                    switch (method.getName()) {
                                                        case "equals":
                                                            return proxy == args[0];
                                                        case "hashCode":
                                                            return System.identityHashCode(proxy);
                                                        case "toString":
                                                            return "CommandLib function recompilation";
                                                        default:
                                                            return CompletableFuture.completedFuture(args[0]);
                                                    }
                                                });
        Executor direct = Runnable::run;
        try {
            ((CompletableFuture<?>) reload.invoke(library,
                                                  barrier,
                                                  server.dataPackResources.h(),
                                                  GameProfilerDisabled.a,
                                                  GameProfilerDisabled.a,
                                                  direct,
                                                  direct)).join();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        keepPendingLoad(server.getFunctionData(), () -> server.getFunctionData().a(library));
    }

    @Override
    public boolean isRunning() {
        return ((DedicatedServer) requireHandle()).isRunning();
    }
''', [f"{NMS}.DedicatedServer",
      f"{NMS}.CustomFunctionManager",
      f"{NMS}.GameProfilerDisabled",
      "java.lang.reflect.Method",
      "java.lang.reflect.Proxy",
      "java.util.concurrent.CompletableFuture",
      "java.util.concurrent.Executor"])

w("NMSCraftBlockData", '''    public NMSCraftBlockData_SUFFIX() {
        super(null, CraftBlockData.class);
    }

    @Override
    public BlockData createData(NMSIBlockData nms) {
        return CraftBlockData.fromData((IBlockData) nms.requireHandle());
    }
''', [f"{NMS}.IBlockData", f"{CB}.block.data.CraftBlockData", "org.bukkit.block.data.BlockData"])

w("NMSCraftEnchantment", '''    public NMSCraftEnchantment_SUFFIX() {
        super(null, CraftEnchantment.class);
    }

    @Override
    public Enchantment createInstance(NMSEnchantment nms) {
        return new CraftEnchantment((net.minecraft.server.v1_16_R3.Enchantment) nms.requireHandle());
    }
''', [f"{CB}.enchantments.CraftEnchantment", "org.bukkit.enchantments.Enchantment"])

w("NMSCraftItemStack", '''    public NMSCraftItemStack_SUFFIX() {
        super(null, CraftItemStack.class);
    }

    @Override
    public org.bukkit.inventory.ItemStack asCraftMirror(NMSItemStack nms) {
        return CraftItemStack.asCraftMirror((ItemStack) nms.requireHandle());
    }
''', [f"{NMS}.ItemStack", f"{CB}.inventory.CraftItemStack"])

w("NMSCraftPotionEffectType", '''    public NMSCraftPotionEffectType_SUFFIX() {
        super(null, CraftPotionEffectType.class);
    }

    public NMSCraftPotionEffectType_SUFFIX(Object handle) {
        super(handle, CraftPotionEffectType.class);
    }

    @Override
    public PotionEffectType createInstance(NMSMobEffectList nms) {
        return new CraftPotionEffectType((MobEffectList) nms.requireHandle());
    }
''', [f"{NMS}.MobEffectList", f"{CB}.potion.CraftPotionEffectType", "org.bukkit.potion.PotionEffectType"])

w("NMSEntity", '''    public NMSEntity_SUFFIX(Object handle) {
        super(handle, Entity.class);
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity() {
        return ((Entity) requireHandle()).getBukkitEntity();
    }
''', [f"{NMS}.Entity"])

w("NMSScoreboardTeam", '''    public NMSScoreboardTeam_SUFFIX(Object handle) {
        super(handle, ScoreboardTeam.class);
    }

    @Override
    public String getName() {
        return ((ScoreboardTeam) requireHandle()).getName();
    }
''', [f"{NMS}.ScoreboardTeam"])

w("NMSVec3D", '''    public NMSVec3D_SUFFIX(Object handle) {
        super(handle, Vec3D.class);
    }

    @Override
    public double x() {
        return ((Vec3D) requireHandle()).x;
    }

    @Override
    public double y() {
        return ((Vec3D) requireHandle()).y;
    }

    @Override
    public double z() {
        return ((Vec3D) requireHandle()).z;
    }
''', [f"{NMS}.Vec3D"])

# Vec2F keeps the obfuscated field names i (x) and j (y) in the Spigot-mapped jar.
w("NMSVec2D", '''    public NMSVec2D_SUFFIX(Object handle) {
        super(handle, Vec2F.class);
    }

    @Override
    public float x() {
        return ((Vec2F) requireHandle()).i;
    }

    @Override
    public float y() {
        return ((Vec2F) requireHandle()).j;
    }
''', [f"{NMS}.Vec2F"])



def handle(wrapper, nms_class):
    """A wrapper that only holds a handle of the given class."""
    w(wrapper, f'''    public {wrapper}_SUFFIX(Object handle) {{
        super(handle, {nms_class}.class);
    }}
''', [f"{NMS}.{nms_class}"])


handle("NMSEnchantment", "Enchantment")
handle("NMSIBlockData", "IBlockData")
handle("NMSItemStack", "ItemStack")
handle("NMSMobEffectList", "MobEffectList")
handle("NMSParticle", "Particle")

# ArgumentRegistry keeps its map of argument serializers in a private field, which the wrapper finds by content.
w("NMSArgumentTypeRegistrar", '''    public NMSArgumentTypeRegistrar_SUFFIX() {
        super(ArgumentRegistry.class);
    }

    @Override
    public <T extends ArgumentType<?>> void registerAsGreedyString(Class<T> argumentTypeClass, Supplier<T> factory) {
        registerInArgumentRegistry(argumentTypeClass,
                                   factory,
                                   ArgumentSerializer.class,
                                   new MinecraftKey("brigadier:string"));
    }
''', [f"{NMS}.ArgumentRegistry", f"{NMS}.ArgumentSerializer", f"{NMS}.MinecraftKey"])

w("NMSDataPackResources", '''    public NMSDataPackResources_SUFFIX(Object handle) {
        super(handle, DataPackResources.class);
    }

    @Override
    public NMSCommandBuildContext getCommandBuildContext() {
        throw new UnsupportedOperationException();
    }
''', [f"{NMS}.DataPackResources"])

write_class_list(M, classes)
print(f"{M}: {len(classes)} classes")
