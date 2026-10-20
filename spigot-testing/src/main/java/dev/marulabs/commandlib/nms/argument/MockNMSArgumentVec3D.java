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
        return argument(true);
    }

    @Override
    public ArgumentType<?> argument(boolean centerIntegers) {
        return new Vec3DArgumentType(centerIntegers);
    }

    @Override
    protected NMSVec3D parseImpl(CommandContext<?> ctx, String name) {
        double[] coords = (double[]) ctx.getArgument(name, Object.class);
        return new MockNMSVec3D(coords[0], coords[1], coords[2]);
    }

    /**
     * Reads three space-separated numbers from the input, e.g. {@code 1.0 2.0 3.0}. Like vanilla, with
     * {@code centerIntegers} an x or z written without a decimal point moves to the center of its block: {@code 1 64 1}
     * is {@code 1.5 64.0 1.5}. Relative and local coordinates are not supported.
     */
    private static final class Vec3DArgumentType implements ArgumentType<double[]> {
        private final boolean centerIntegers;

        private Vec3DArgumentType(boolean centerIntegers) {
            this.centerIntegers = centerIntegers;
        }

        @Override
        public double[] parse(StringReader reader) throws CommandSyntaxException {
            double x = coordinate(reader, centerIntegers);
            reader.expect(' ');
            double y = coordinate(reader, false);
            reader.expect(' ');
            double z = coordinate(reader, centerIntegers);
            return new double[]{x, y, z};
        }

        private static double coordinate(StringReader reader, boolean center) throws CommandSyntaxException {
            int start = reader.getCursor();
            double value = reader.readDouble();
            boolean integer = reader.getString()
                                    .substring(start, reader.getCursor())
                                    .indexOf('.') < 0;
            return center && integer ? value + 0.5 : value;
        }
    }
}
