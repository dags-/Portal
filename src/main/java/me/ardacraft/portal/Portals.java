package me.ardacraft.portal;

import com.flowpowered.math.vector.Vector3d;
import com.google.inject.Inject;
import me.dags.pitaya.command.CommandBus;
import me.dags.pitaya.command.annotation.Command;
import me.dags.pitaya.command.annotation.Description;
import me.dags.pitaya.command.annotation.Permission;
import me.dags.pitaya.command.annotation.Src;
import me.dags.pitaya.command.fmt.Fmt;
import me.dags.pitaya.config.Config;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.world.World;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @Author <dags@dags.me>
 */
@Plugin(id = "portal")
public class Portals {

    private final LinkManager links;
    private final PortalManager portals;
    private final Map<UUID, Link> activeLinks = new HashMap<>();
    private final Map<UUID, PortalBuilder> activeBuilders = new HashMap<>();

    @Inject
    public Portals(@DefaultConfig(sharedRoot = false) Path path) {
        Config storage = Config.must(path);
        portals = new PortalManager(storage);
        links = new LinkManager(storage);
    }

    @Command("portal pos1")
    @Permission("portal.command.create")
    @Description("Set the first position of the portal")
    public void pos1(@Src Player player) {
        PortalBuilder builder = activeBuilders.computeIfAbsent(player.getUniqueId(), u -> new PortalBuilder());
        builder.world = player.getWorld().getName();
        builder.pos1 = player.getPosition().toInt();
        Fmt.info("Set pos1 ").stress(builder.pos1).tell(player);
    }

    @Command("portal pos2")
    @Permission("portal.command.create")
    @Description("Set the second position of the portal")
    public void pos2(@Src Player player) {
        PortalBuilder builder = activeBuilders.computeIfAbsent(player.getUniqueId(), u -> new PortalBuilder());
        builder.world = player.getWorld().getName();
        builder.pos2 = player.getPosition().toInt();
        Fmt.info("Set pos2 ").stress(builder.pos2).tell(player);
    }

    @Command("portal create <name>")
    @Permission("portal.command.create")
    @Description("Create a portal with given name")
    public void create(@Src Player player, String name) {
        PortalBuilder builder = activeBuilders.computeIfAbsent(player.getUniqueId(), u -> new PortalBuilder());
        if (builder.pos1 == null) {
            Fmt.error("Please select pos1 first").tell(player);
            return;
        }

        if (builder.pos2 == null) {
            Fmt.error("Please select pos2 first").tell(player);
            return;
        }

        Portal portal = new Portal(name.toLowerCase(), builder.world, builder.pos1, builder.pos2);
        portals.register(portal);
        activeBuilders.remove(player.getUniqueId());
        Fmt.info("Created portal ").stress(portal).tell(player);
    }

    @Command("portal delete <name>")
    @Permission("portal.command.delete")
    @Description("Delete a portal with given name")
    public void delete(@Src Player player, Portal portal) {
        if (portals.delete(portal.getName())) {
            links.reload();
            Fmt.info("Removed portal ").stress(portal).tell(player);
        } else {
            Fmt.error("A portal with that name does not exist").tell(player);
        }
    }

    @Command("portal link <portal1> <portal2>")
    @Permission("portal.command.link")
    @Description("Link two different portals together")
    public void link(@Src Player player, Portal portal1, Portal portal2) {
        if (portal1 == portal2) {
            Fmt.error("Cannot link a portal to itself").tell(player);
        } else {
            Link link = new Link(portal1, portal2);
            links.register(link);
            Fmt.info("Created new portal link ").stress(link).tell(player);
        }
    }

    @Command("portal unlink <portal>")
    @Permission("portal.command.unlink")
    @Description("Unlink any portals connected to the given portal")
    public void unlink(@Src Player player, Portal portal) {
        int unlinked = links.unlink(portal);
        Fmt.info("Unlinked ").stress(unlinked).info(" portals connected to ").stress(portal).tell(player);
    }

    @Listener
    public void onPreInit(GamePreInitializationEvent event) {
        CommandBus.create().register(this).submit();
        Sponge.getRegistry().registerModule(Portal.class, portals);
    }

    @Listener
    public void onPostInit(GamePostInitializationEvent event) {
        links.reload();
    }

    @Listener
    public void onMove(MoveEntityEvent.Position event, @Root Player player) {
        Transform<World> transform = event.getToTransform();
        Link currentLink = activeLinks.get(player.getUniqueId());

        if (currentLink != null) {
            // player still inside a link portal after teleporting
            if (currentLink.getPortal(transform).isPresent()) {
                return;
            }
            // player no longer inside a link portal
            activeLinks.remove(player.getUniqueId());
        }

        for (Link link : links.getLinks(transform)) {
            Transform<World> destination = link.getTransform(transform);
            if (destination != transform) {
                Vector3d velocity = player.getVelocity();
                activeLinks.put(player.getUniqueId(), link);
                player.setTransform(destination);
                // sponge destroys the player object when transporting between worlds?
                Sponge.getServer().getPlayer(player.getUniqueId()).ifPresent(p -> p.setVelocity(velocity));
                return;
            }
        }
    }

    @Listener
    public void onQuit(ClientConnectionEvent.Disconnect event) {
        activeBuilders.remove(event.getTargetEntity().getUniqueId());
        activeLinks.remove(event.getTargetEntity().getUniqueId());
    }
}
