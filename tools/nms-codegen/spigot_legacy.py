"""
Typed classes for Spigot 1.16.5, which compiles against the Spigot-mapped server jar. Run as
`generate.py spigot-1.16.5`.
"""
from codegen import register, write

M = "spigot-1.16.5"
NMS = "net.minecraft.server.v1_16_R3"
CB = "org.bukkit.craftbukkit.v1_16_R3"
wrappers = []


def w(wrapper, body, imports=()):
    write(M, wrapper, body, imports)
    wrappers.append(wrapper)


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
    body = f'''    public {wrapper}_spigot_1_16_5() {{
        super(null, "{class_name}");
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

w("NMSArgumentProfile", '''    public NMSArgumentProfile_spigot_1_16_5() {
        super(null, "ArgumentProfile");
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

w("NMSArgumentPredicateItemStack", '''    public NMSArgumentPredicateItemStack_spigot_1_16_5(Object handle) {
        super(handle, "ArgumentPredicateItemStack");
    }

    @Override
    public NMSItemStack createItemStack(int amount, boolean checkOverStack) {
        try {
            return NMSItemStack.create(((ArgumentPredicateItemStack) getHandle()).a(amount, checkOverStack));
        } catch (CommandSyntaxException e) {
            throw new UncheckedCommandSyntaxException(e);
        }
    }
''', [f"{NMS}.ArgumentPredicateItemStack"])

w("NMSArgumentTileLocation", '''    public NMSArgumentTileLocation_spigot_1_16_5(Object handle) {
        super(handle, "ArgumentTileLocation");
    }

    @Override
    public NMSIBlockData getBlockData() {
        return NMSIBlockData.create(((ArgumentTileLocation) getHandle()).a());
    }
''', [f"{NMS}.ArgumentTileLocation"])

w("NMSChatMessage", '''    public NMSChatMessage_spigot_1_16_5(Message handle) {
        super(handle, "ChatMessage");
    }

    @Override
    public String getKey() {
        return ((ChatMessage) getHandle()).getKey();
    }

    @Override
    public Object[] getArgs() {
        return ((ChatMessage) getHandle()).getArgs();
    }
''', [f"{NMS}.ChatMessage", "com.mojang.brigadier.Message"])

w("NMSCommandDispatcher", '''    public NMSCommandDispatcher_spigot_1_16_5(Object handle) {
        super(handle, "CommandDispatcher");
    }

    @Override
    public com.mojang.brigadier.CommandDispatcher<?> getBrigadier() {
        return ((CommandDispatcher) getHandle()).a();
    }
''', [f"{NMS}.CommandDispatcher"])

w("NMSCommandListenerWrapper", '''    public NMSCommandListenerWrapper_spigot_1_16_5(Object handle) {
        super(handle, "CommandListenerWrapper");
    }

    @Override
    public CommandSender getBukkitSender() {
        return wrapper().getBukkitSender();
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity() {
        Entity entity = wrapper().getEntity();
        return entity != null ? entity.getBukkitEntity() : null;
    }

    @Override
    public World getBukkitWorld() {
        WorldServer world = wrapper().getWorld();
        return world != null ? world.getWorld() : null;
    }

    @Override
    public Location getBukkitLocation() {
        Vec3D pos = wrapper().getPosition();
        World world = getBukkitWorld();
        return world != null && pos != null ? new Location(world, pos.x, pos.y, pos.z) : null;
    }

    private CommandListenerWrapper wrapper() {
        return (CommandListenerWrapper) getHandle();
    }
''', [f"{NMS}.CommandListenerWrapper", f"{NMS}.Entity", f"{NMS}.Vec3D", f"{NMS}.WorldServer", "org.bukkit.Location",
      "org.bukkit.World", "org.bukkit.command.CommandSender"])

w("NMSVanillaCommandWrapper", '''    public NMSVanillaCommandWrapper_spigot_1_16_5() {
        super(null, "command.VanillaCommandWrapper");
    }

    @Override
    @SuppressWarnings("unchecked")
    public BukkitCommand createInstance(NMSCommandDispatcher dispatcher, CommandNode<?> command) {
        return new VanillaCommandWrapper((CommandDispatcher) dispatcher.getHandle(),
                                         (CommandNode<CommandListenerWrapper>) command);
    }
''', [f"{NMS}.CommandDispatcher", f"{NMS}.CommandListenerWrapper", f"{CB}.command.VanillaCommandWrapper",
      "com.mojang.brigadier.tree.CommandNode", "org.bukkit.command.defaults.BukkitCommand"])

w("NMSParticleParam", '''    public NMSParticleParam_spigot_1_16_5(Object handle) {
        super(handle, "ParticleParam");
    }

    @Override
    public NMSParticle getParticle() {
        return NMSParticle.create(((ParticleParam) getHandle()).getParticle());
    }
''', [f"{NMS}.ParticleParam"])

w("NMSCraftParticle", '''    public NMSCraftParticle_spigot_1_16_5() {
        super(null, "CraftParticle");
    }

    @Override
    public Particle toBukkit(NMSParticleParam nms) {
        return CraftParticle.toBukkit((ParticleParam) nms.getHandle());
    }
''', [f"{NMS}.ParticleParam", f"{CB}.CraftParticle", "org.bukkit.Particle"])

w("NMSCraftServer", '''    public NMSCraftServer_spigot_1_16_5(Server handle) {
        super(handle, "CraftServer");
    }

    @Override
    public NMSDedicatedServer getServer() {
        return NMSDedicatedServer.create(((CraftServer) getHandle()).getServer());
    }
''', [f"{CB}.CraftServer", "org.bukkit.Server"])

w("NMSDedicatedServer", '''    public NMSDedicatedServer_spigot_1_16_5(Object handle) {
        super(handle, "DedicatedServer");
    }

    @Override
    public NMSCommandDispatcher getCommandDispatcher() {
        return NMSCommandDispatcher.create(((DedicatedServer) getHandle()).getCommandDispatcher());
    }

    @Override
    public NMSDataPackResources getDataPackResources() {
        return NMSDataPackResources.create(((DedicatedServer) getHandle()).dataPackResources);
    }
''', [f"{NMS}.DedicatedServer"])

w("NMSCraftBlockData", '''    public NMSCraftBlockData_spigot_1_16_5() {
        super(null, "block.data.CraftBlockData");
    }

    @Override
    public BlockData createData(NMSIBlockData nms) {
        return CraftBlockData.fromData((IBlockData) nms.getHandle());
    }
''', [f"{NMS}.IBlockData", f"{CB}.block.data.CraftBlockData", "org.bukkit.block.data.BlockData"])

w("NMSCraftEnchantment", '''    public NMSCraftEnchantment_spigot_1_16_5() {
        super(null, "enchantments.CraftEnchantment");
    }

    @Override
    public Enchantment createInstance(NMSEnchantment nms) {
        return new CraftEnchantment((net.minecraft.server.v1_16_R3.Enchantment) nms.getHandle());
    }
''', [f"{CB}.enchantments.CraftEnchantment", "org.bukkit.enchantments.Enchantment"])

w("NMSCraftItemStack", '''    public NMSCraftItemStack_spigot_1_16_5() {
        super(null, "inventory.CraftItemStack");
    }

    @Override
    public org.bukkit.inventory.ItemStack asCraftMirror(NMSItemStack nms) {
        return CraftItemStack.asCraftMirror((ItemStack) nms.getHandle());
    }
''', [f"{NMS}.ItemStack", f"{CB}.inventory.CraftItemStack"])

w("NMSCraftPotionEffectType", '''    public NMSCraftPotionEffectType_spigot_1_16_5() {
        this(null);
    }

    public NMSCraftPotionEffectType_spigot_1_16_5(Object handle) {
        super(handle, "potion.CraftPotionEffectType");
    }

    @Override
    public PotionEffectType createInstance(NMSMobEffectList nms) {
        return new CraftPotionEffectType((MobEffectList) nms.getHandle());
    }
''', [f"{NMS}.MobEffectList", f"{CB}.potion.CraftPotionEffectType", "org.bukkit.potion.PotionEffectType"])

w("NMSEntity", '''    public NMSEntity_spigot_1_16_5(Object handle) {
        super(handle, "Entity");
    }

    @Override
    public org.bukkit.entity.Entity getBukkitEntity() {
        return ((Entity) getHandle()).getBukkitEntity();
    }
''', [f"{NMS}.Entity"])

w("NMSScoreboardTeam", '''    public NMSScoreboardTeam_spigot_1_16_5(Object handle) {
        super(handle, "ScoreboardTeam");
    }

    @Override
    public String getName() {
        return ((ScoreboardTeam) getHandle()).getName();
    }
''', [f"{NMS}.ScoreboardTeam"])

w("NMSVec3D", '''    public NMSVec3D_spigot_1_16_5(Object handle) {
        super(handle, "Vec3D");
    }

    @Override
    public double x() {
        return ((Vec3D) getHandle()).x;
    }

    @Override
    public double y() {
        return ((Vec3D) getHandle()).y;
    }

    @Override
    public double z() {
        return ((Vec3D) getHandle()).z;
    }
''', [f"{NMS}.Vec3D"])

register(wrappers)
print(f"{M}: {len(wrappers)} classes")
