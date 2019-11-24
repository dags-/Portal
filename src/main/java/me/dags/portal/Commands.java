package me.dags.portal;

import me.dags.pitaya.command.annotation.*;
import me.dags.pitaya.command.command.Flags;
import me.dags.pitaya.command.fmt.Fmt;
import me.dags.pitaya.util.cache.IdCache;
import me.dags.portal.link.Link;
import me.dags.portal.portal.Portal;
import me.dags.portal.portal.PortalBuilder;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;

import java.util.concurrent.TimeUnit;

/**
 * @Author <dags@dags.me>
 */
public class Commands {

    private final Portals plugin;
    private final IdCache<PortalBuilder> builders = new IdCache<>(5, TimeUnit.MINUTES);

    Commands(Portals plugin) {
        this.plugin = plugin;
        Sponge.getEventManager().registerListeners(plugin, this);
    }

    @Command("portal pos1")
    @Permission("portal.command.create")
    @Description("Set the first position of the portal")
    public void pos1(@Src Player player) {
        PortalBuilder builder = builders.compute(player, u -> new PortalBuilder());
        builder.world = player.getWorld().getName();
        builder.pos1 = player.getPosition().toInt();
        Fmt.info("Set pos1 ").stress(builder.pos1).tell(player);
    }

    @Command("portal pos2")
    @Permission("portal.command.create")
    @Description("Set the second position of the portal")
    public void pos2(@Src Player player) {
        PortalBuilder builder = builders.compute(player, u -> new PortalBuilder());
        builder.world = player.getWorld().getName();
        builder.pos2 = player.getPosition().toInt();
        Fmt.info("Set pos2 ").stress(builder.pos2).tell(player);
    }

    @Command("portal create <name>")
    @Permission("portal.command.create")
    @Description("Create a portal with the given name")
    public void create(@Src Player player, String name) {
        PortalBuilder builder = builders.compute(player, u -> new PortalBuilder());
        if (builder.pos1 == null) {
            Fmt.error("Please select pos1 first").tell(player);
            return;
        }

        if (builder.pos2 == null) {
            Fmt.error("Please select pos2 first").tell(player);
            return;
        }

        Portal portal = new Portal(name.toLowerCase(), builder.world, builder.pos1, builder.pos2);
        plugin.getPortalManager().register(portal);
        builders.remove(player);
        Fmt.info("Created portal ").stress(portal).tell(player);
    }

    @Command("portal delete <name>")
    @Permission("portal.command.delete")
    @Description("Delete a portal with the given name")
    public void delete(@Src Player player, Portal portal) {
        if (plugin.getPortalManager().delete(portal.getName())) {
            plugin.getLinkManager().reload();
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
            plugin.getLinkManager().register(forwards);
            Fmt.info("Created new portal link ").stress(forwards).tell(player);
            if (flags.has("r")) {
                Link backwards = new Link(to, from);
                plugin.getLinkManager().register(backwards);
                Fmt.info("Created reverse link ").stress(backwards).tell(player);
            }
        }
    }

    @Command("portal unlink <portal>")
    @Permission("portal.command.unlink")
    @Description("Unlink any portals connected to the given portal")
    public void unlink(@Src Player player, Portal portal) {
        int unlinked = plugin.getLinkManager().unlink(portal);
        Fmt.info("Unlinked ").stress(unlinked).info(" portal connected to ").stress(portal).tell(player);
    }
}
