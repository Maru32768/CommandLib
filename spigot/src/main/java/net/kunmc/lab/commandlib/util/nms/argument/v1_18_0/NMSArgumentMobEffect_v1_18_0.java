package net.kunmc.lab.commandlib.util.nms.argument.v1_18_0;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.commandlib.util.nms.argument.NMSArgumentMobEffect;
import net.kunmc.lab.commandlib.util.nms.world.NMSMobEffectList;

public class NMSArgumentMobEffect_v1_18_0 extends NMSArgumentMobEffect {
    public NMSArgumentMobEffect_v1_18_0() {
        super(null, "commands.arguments.ArgumentMobEffect");
    }

    @Override
    public ArgumentType<?> argument() {
        return ((ArgumentType<?>) newInstance(new Class[]{}, new Object[]{}));
    }

    @Override
    protected NMSMobEffectList parseImpl(CommandContext<?> ctx, String name) {
        return NMSMobEffectList.create(invokeStaticMethod("a", ctx, name));
    }
}
