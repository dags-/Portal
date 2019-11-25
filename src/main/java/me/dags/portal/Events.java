package me.dags.portal;

import com.flowpowered.math.vector.Vector3d;
import me.dags.portal.link.Link;
import me.dags.portal.portal.Portal;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @Author <dags@dags.me>
 */
public class Events {

    private final Portals portals;
    private final Map<UUID, Portal> activePortals = new HashMap<>();

    Events(Portals portals) {
        this.portals = portals;
    }

    @Listener(order = Order.POST)
    public void onQuit(ClientConnectionEvent.Disconnect event) {
        activePortals.remove(event.getTargetEntity().getUniqueId());
    }

    @Listener
    public void onMove(MoveEntityEvent.Position event, @Root Player player) {
        Transform<World> transform = event.getToTransform();
        Portal currentPortal = activePortals.get(player.getUniqueId());

        if (currentPortal != null) {
            // player still inside the portal they last teleported to
            if (currentPortal.contains(transform)) {
                return;
            }
            // player no longer inside a portal
            activePortals.remove(player.getUniqueId());
        }

        for (Link link : portals.getLinkManager().getLinks(transform)) {
            Transform<World> destination = link.getTransform(transform);
            if (destination != transform) {
                Vector3d velocity = player.getVelocity();
                activePortals.put(player.getUniqueId(), link.getTo());
                player.setTransform(destination);
                // sponge destroys the player object when transporting between worlds?
                Sponge.getServer().getPlayer(player.getUniqueId()).ifPresent(p -> p.setVelocity(velocity));
                return;
            }
        }
    }
}
