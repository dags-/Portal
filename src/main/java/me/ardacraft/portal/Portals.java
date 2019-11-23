package me.ardacraft.portal;

import com.google.inject.Inject;
import me.dags.pitaya.command.CommandBus;
import me.dags.pitaya.command.annotation.Command;
import me.dags.pitaya.command.annotation.Permission;
import me.dags.pitaya.command.annotation.Src;
import me.dags.pitaya.command.fmt.Fmt;
import me.dags.pitaya.config.Config;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.MoveEntityEvent;
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
    public void pos1(@Src Player player) {
        PortalBuilder builder = activeBuilders.computeIfAbsent(player.getUniqueId(), u -> new PortalBuilder());
        builder.world = player.getWorld().getName();
        builder.pos1 = player.getPosition();
        Fmt.info("Set pos1 ").stress(builder.pos1).tell(player);
    }

    @Command("portal pos2")
    @Permission("portal.command.create")
    public void pos2(@Src Player player) {
        PortalBuilder builder = activeBuilders.computeIfAbsent(player.getUniqueId(), u -> new PortalBuilder());
        builder.pos2 = player.getPosition();
        Fmt.info("Set pos2 ").stress(builder.pos2).tell(player);
    }

    @Command("portal create <name>")
    @Permission("portal.command.delete")
    public void delete(@Src Player player, String name) {
        if (portals.delete(name)) {
            links.reload(true);
            Fmt.info("Removed portal ").stress(name).tell(player);
        } else {
            Fmt.error("A portal with that name does not exist").tell(player);
        }
    }

    @Command("portal create <name>")
    @Permission("portal.command.create")
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
        Fmt.info("Created portal ").stress(portal.getName()).tell(player);
    }

    @Command("portal link <portal1> <portal2>")
    @Permission("portal.command.link")
    public void link(@Src Player player, Portal portal1, Portal portal2) {
        Link link = new Link(portal1, portal2);
        links.register(link);
        Fmt.info("Created new portal link ").stress(portal1.getName()).info(" - ").stress(portal2.getName()).tell(player);
    }

    @Listener
    public void onPreInit(GamePreInitializationEvent event) {
        CommandBus.create().register(this);
        Sponge.getRegistry().registerModule(Portal.class, portals);
    }

    @Listener
    public void onMove(MoveEntityEvent.Position event) {
        Entity entity = event.getTargetEntity();
        Transform<World> transform = entity.getTransform();
        Link currentLink = activeLinks.get(entity.getUniqueId());

        if (currentLink != null) {
            // player still inside a link portal after teleporting
            if (currentLink.getPortal(transform).isPresent()) {
                return;
            }
            // player no longer inside a link portal
            activeLinks.remove(entity.getUniqueId());
        }

        for (Link link : links.getLinks(transform)) {
            Transform<World> destination = link.getTransform(transform);
            if (destination != transform) {
                activeLinks.put(entity.getUniqueId(), link);
                entity.setTransform(destination);
                return;
            }
        }
    }

    @Listener
    public void onTeleport(MoveEntityEvent.Teleport event) {
        event.setKeepsVelocity(true);
    }

    @Listener
    public void onQuit(ClientConnectionEvent.Disconnect event) {
        activeBuilders.remove(event.getTargetEntity().getUniqueId());
        activeLinks.remove(event.getTargetEntity().getUniqueId());
    }
}
