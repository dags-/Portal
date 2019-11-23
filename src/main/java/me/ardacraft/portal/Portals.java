package me.ardacraft.portal;

import com.flowpowered.math.vector.Vector3d;
import com.google.inject.Inject;
import me.dags.pitaya.command.CommandBus;
import me.dags.pitaya.command.annotation.*;
import me.dags.pitaya.command.command.Flags;
import me.dags.pitaya.command.fmt.Fmt;
import me.dags.pitaya.config.Config;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
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
    private final Map<UUID, Portal> activePortals = new HashMap<>();
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

    @Flag("r")
    @Command("portal link <from> <to>")
    @Permission("portal.command.link")
    @Description("Link from one portal to another (-r to link in the reverse direction as well)")
    public void link(@Src Player player, Portal from, Portal to, Flags flags) {
        if (from == to) {
            Fmt.error("Cannot link a portal to itself").tell(player);
        } else {
            Link forwards = new Link(from, to);
            links.register(forwards);
            Fmt.info("Created new portal link ").stress(forwards).tell(player);
            if (flags.has("r")) {
                Link backwards = new Link(to, from);
                links.register(backwards);
                Fmt.info("Created reverse link ").stress(backwards).tell(player);
            }
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
        Portal currentPortal = activePortals.get(player.getUniqueId());

        if (currentPortal != null) {
            // player still inside the portal they last teleported to
            if (currentPortal.contains(transform)) {
                return;
            }
            // player no longer inside a portal
            activePortals.remove(player.getUniqueId());
        }

        for (Link link : links.getLinks(transform)) {
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

    @Listener
    public void onQuit(ClientConnectionEvent.Disconnect event) {
        activeBuilders.remove(event.getTargetEntity().getUniqueId());
        activePortals.remove(event.getTargetEntity().getUniqueId());
    }
}
