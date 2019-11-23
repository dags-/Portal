package me.ardacraft.portal;

import com.flowpowered.math.vector.Vector3d;
import me.dags.pitaya.command.fmt.Fmt;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextRepresentable;
import org.spongepowered.api.world.World;

import java.util.Optional;

/**
 * @Author <dags@dags.me>
 */
public class Link implements TextRepresentable {

    private final Portal portal1;
    private final Portal portal2;

    public Link(Portal portal1, Portal portal2) {
        this.portal1 = portal1;
        this.portal2 = portal2;
    }

    public Portal getPortal1() {
        return portal1;
    }

    public Portal getPortal2() {
        return portal2;
    }

    public Optional<Portal> getPortal(Transform<World> transform) {
        if (getPortal1().contains(transform.getExtent().getName(), transform.getPosition())) {
            return Optional.of(getPortal1());
        }
        if (getPortal2().contains(transform.getExtent().getName(), transform.getPosition())) {
            return Optional.of(getPortal2());
        }
        return Optional.empty();
    }

    public Transform<World> getTransform(Transform<World> transform) {
        return getPortal(transform).map(portal -> getTransform(transform, portal)).orElse(transform);
    }

    public Transform<World> getTransform(Transform<World> transform, Portal portal) {
        if (portal != getPortal1() && portal != getPortal2()) {
            return transform;
        }

        Portal target = portal == getPortal1() ? getPortal2() : getPortal1();
        Optional<World> world = target.getWorld();
        if (!world.isPresent()) {
            return transform;
        }

        Vector3d position = transform.getPosition();
        Vector3d rotation = transform.getRotation();
        Vector3d offset = position.sub(portal.getOrigin());
        Vector3d destination = target.getOrigin().add(offset);

        return new Transform<>(world.get(), destination, rotation);
    }

    @Override
    public String toString() {
        return portal1 + " <-> " + portal2;
    }

    @Override
    public Text toText() {
        return Fmt.stress(portal1).info(" <-> ").stress(portal2).toText();
    }
}
