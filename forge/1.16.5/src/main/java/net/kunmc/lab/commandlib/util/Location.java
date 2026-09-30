package net.kunmc.lab.commandlib.util;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

public class Location implements Cloneable {
    private Reference<World> world;
    private double x;
    private double y;
    private double z;
    private float pitch;
    private float yaw;

    public Location(World world, Vector3d vec) {
        this(world, vec.x, vec.y, vec.z);
    }

    public Location(World world, double x, double y, double z) {
        this(world, x, y, z, 0.0F, 0.0F);
    }

    public Location(World world, double x, double y, double z, float yaw, float pitch) {
        if (world != null) {
            this.world = new WeakReference<>(world);
        }

        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void setWorld(World world) {
        if (world == null) {
            this.world = null;
            return;
        }

        this.world = new WeakReference<>(world);
    }

    public World getWorld() {
        if (world == null) {
            return null;
        }

        return world.get();
    }

    public Chunk getChunk() {
        return getWorld().getChunk(getBlockX(), getBlockZ());
    }

    public BlockState getBlockState() {
        return getWorld().getBlockState(toBlockPos());
    }

    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }

    public Location setX(double x) {
        this.x = x;
        return this;
    }

    public double getX() {
        return x;
    }

    public int getBlockX() {
        return MathHelper.floor(getX());
    }

    public Location setY(double y) {
        this.y = y;
        return this;
    }

    public double getY() {
        return y;
    }

    public int getBlockY() {
        return MathHelper.floor(getY());
    }

    public Location setZ(double z) {
        this.z = z;
        return this;
    }

    public double getZ() {
        return z;
    }

    public int getBlockZ() {
        return MathHelper.floor(getZ());
    }

    public Location setYaw(float yaw) {
        this.yaw = yaw;
        return this;
    }

    public float getYaw() {
        return yaw;
    }

    public Location setPitch(float pitch) {
        this.pitch = pitch;
        return this;
    }

    public float getPitch() {
        return pitch;
    }

    public Location add(Location another) {
        this.x += another.x;
        this.y += another.y;
        this.z += another.z;

        return this;
    }

    public Location add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;

        return this;
    }

    public Location subtract(Location another) {
        this.x -= another.x;
        this.y -= another.y;
        this.z -= another.z;

        return this;
    }

    public Location subtract(double x, double y, double z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;

        return this;
    }

    public double distance(Location another) {
        double x2 = Math.pow(x - another.x, 2);
        double y2 = Math.pow(y - another.y, 2);
        double z2 = Math.pow(z - another.z, 2);

        return Math.sqrt(x2 + y2 + z2);
    }

    public Location multiply(double m) {
        this.x *= m;
        this.y *= m;
        this.z *= m;

        return this;
    }

    @Override
    public Location clone() {
        return new Location(world.get(), x, y, z, yaw, pitch);
    }
}
