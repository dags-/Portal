package me.ardacraft.portal;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import me.dags.pitaya.config.Node;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.World;

import java.util.Optional;

/**
 * @Author <dags@dags.me>
 */
public class Portal implements CatalogType {

    private final String name;
    private final String world;
    private final Vector3d min;
    private final Vector3d max;

    public Portal(String name, String world, Vector3d p1, Vector3d p2) {
        this.min = p1.min(p2).toDouble();
        this.max = p1.max(p2).toDouble();
        this.name = name;
        this.world = world;
    }

    @Override
    public String getId() {
        return getName();
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return world;
    }

    public Optional<World> getWorld() {
        return Sponge.getServer().getWorld(getWorldName());
    }

    public Vector3d getMin() {
        return min;
    }

    public Vector3d getMax() {
        return max;
    }

    public boolean contains(String world, Vector3d position) {
        return getWorldName().equals(world) && contains(position);
    }

    public boolean contains(Vector3i vec) {
        return contains(vec.getX(), vec.getY(), vec.getZ());
    }

    public boolean contains(Vector3d vec) {
        return contains(vec.getX(), vec.getY(), vec.getZ());
    }

    public boolean contains(double x, double y, double z) {
        return x >= min.getX() && x <= max.getX() && y >= min.getY() && y <= max.getY() && z >= min.getZ() && z <= max.getZ();
    }

    public static void serialize(Portal portal, Node node) {
        node.set("world", portal.getWorldName());
        node.set("x1", portal.getMin().getX());
        node.set("y1", portal.getMin().getY());
        node.set("z1", portal.getMin().getZ());
        node.set("x2", portal.getMax().getX());
        node.set("y2", portal.getMax().getY());
        node.set("z2", portal.getMax().getZ());
    }

    public static Portal deserialize(String name, Node node) {
        String world = node.get("world");
        double x1 = node.get("x1", 0D);
        double y1 = node.get("y1", 0D);
        double z1 = node.get("z1", 0D);
        double x2 = node.get("x2", 0D);
        double y2 = node.get("y2", 0D);
        double z2 = node.get("z2", 0D);
        Vector3d pos1 = new Vector3d(x1, y1, z1);
        Vector3d pos2 = new Vector3d(x2, y2, z2);
        return new Portal(name, world, pos1, pos2);
    }
}
