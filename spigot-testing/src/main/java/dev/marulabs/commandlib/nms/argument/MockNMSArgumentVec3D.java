package dev.marulabs.commandlib.nms.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marulabs.commandlib.nms.world.MockNMSVec3D;
import dev.marulabs.commandlib.util.nms.argument.NMSArgumentVec3D;
import dev.marulabs.commandlib.util.nms.world.NMSVec3D;

public class MockNMSArgumentVec3D extends NMSArgumentVec3D {
    public MockNMSArgumentVec3D() {
        super(null, Object.class);
    }

    @Override
    public ArgumentType<?> argument() {
        return new Vec3DArgumentType();
    }

    @Override
    protected NMSVec3D parseImpl(CommandContext<?> ctx, String name) {
        double[] coords = (double[]) ctx.getArgument(name, Object.class);
        return new MockNMSVec3D(coords[0], coords[1], coords[2]);
    }

    /**
     * Reads three space-separated doubles from the input, e.g. {@code 1.0 2.0 3.0}.
     * Use this format when writing location arguments in test commands.
     */
    private static final class Vec3DArgumentType implements ArgumentType<double[]> {
        @Override
        public double[] parse(StringReader reader) throws CommandSyntaxException {
            double x = reader.readDouble();
            reader.expect(' ');
            double y = reader.readDouble();
            reader.expect(' ');
            double z = reader.readDouble();
            return new double[]{x, y, z};
        }
    }
}
