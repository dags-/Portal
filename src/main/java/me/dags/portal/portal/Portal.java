package me.dags.portal.portal;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import me.dags.pitaya.command.fmt.Fmt;
import me.dags.pitaya.config.Node;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextRepresentable;
import org.spongepowered.api.world.World;

import javax.sound.sampled.Port;
import java.util.Optional;

/**
 * @Author <dags@dags.me>
 */
public class Portal implements CatalogType, TextRepresentable {

    private final String name;
    private final String world;
    private final Vector3i pos1;
    private final Vector3i pos2;
    private final boolean restricted;

    private transient final Vector3d min;
    private transient final Vector3d max;
    private transient final String permission;

    public Portal(String name, PortalBuilder builder) {
        this.name = name;
        this.world = builder.world;
        this.pos1 = builder.pos1.min(builder.pos2);
        this.pos2 = builder.pos1.max(builder.pos2);
        this.restricted = builder.restrict;

        this.min = pos1.toDouble();
        this.max = pos2.add(Vector3i.ONE).toDouble();
        this.permission = "portal.use." + name;
    }

    private Portal(String name, String world, Vector3i p1, Vector3i p2, boolean restricted) {
        this.name = name;
        this.world = world;
        this.pos1 = p1.min(p2);
        this.pos2 = p1.max(p2);
        this.restricted = restricted;
        this.min = pos1.toDouble();
        this.max = pos2.add(Vector3i.ONE).toDouble();
        this.permission = "portal.use." + name;
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

    public boolean canUse(Subject subject) {
        return !restricted || subject.hasPermission(permission);
    }

    public boolean contains(Transform<World> transform) {
        return contains(transform.getExtent().getName(), transform.getPosition());
    }

    public boolean contains(String world, Vector3d position) {
        return getWorldName().equals(world) && contains(position);
    }

    public boolean contains(Vector3d vec) {
        return contains(vec.getX(), vec.getY(), vec.getZ());
    }

    public boolean contains(double x, double y, double z) {
        return x >= min.getX() && y >= min.getY() && z >= min.getZ() && x < max.getX() && y < max.getY() && z < max.getZ();
    }

    public static void serialize(Portal portal, Node node) {
        node.set("world", portal.getWorldName());

        node.set("x1", portal.pos1.getX());
        node.set("y1", portal.pos1.getY());
        node.set("z1", portal.pos1.getZ());

        node.set("x2", portal.pos2.getX());
        node.set("y2", portal.pos2.getY());
        node.set("z2", portal.pos2.getZ());

        node.set("restricted", portal.restricted);
    }

    public static Portal deserialize(String name, Node node) {
        String world = node.get("world", "");

        int x1 = node.get("x1", 0);
        int y1 = node.get("y1", 0);
        int z1 = node.get("z1", 0);

        int x2 = node.get("x2", 0);
        int y2 = node.get("y2", 0);
        int z2 = node.get("z2", 0);

        boolean restricted = node.get("restricted", false);

        Vector3i pos1 = new Vector3i(x1, y1, z1);
        Vector3i pos2 = new Vector3i(x2, y2, z2);
        return new Portal(name, world, pos1, pos2, restricted);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public Text toText() {
        return Fmt.stress(getName()).toText();
    }
}
