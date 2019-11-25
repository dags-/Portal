package me.dags.portal.link;

import com.flowpowered.math.vector.Vector3d;
import me.dags.pitaya.command.fmt.Fmt;
import me.dags.portal.portal.Portal;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextRepresentable;
import org.spongepowered.api.world.World;

import java.util.Optional;

/**
 * @Author <dags@dags.me>
 */
public class Link implements TextRepresentable {

    private final Portal from;
    private final Portal to;

    public Link(Portal from, Portal to) {
        this.from = from;
        this.to = to;
    }

    public Portal getFrom() {
        return from;
    }

    public Portal getTo() {
        return to;
    }

    public Transform<World> getTransform(Transform<World> transform) {
        if (!getFrom().contains(transform)) {
            return transform;
        }

        Optional<World> world = getTo().getWorld();
        if (!world.isPresent()) {
            return transform;
        }

        Vector3d position = transform.getPosition();
        Vector3d rotation = transform.getRotation();
        Vector3d offset = position.sub(getFrom().getMin());
        Vector3d destination = getTo().getMin().add(offset);
        return new Transform<>(world.get(), destination, rotation);
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }

    @Override
    public Text toText() {
        return Fmt.stress(from).info(" -> ").stress(to).toText();
    }
}
