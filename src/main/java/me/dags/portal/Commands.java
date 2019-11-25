package me.dags.portal;

import com.flowpowered.math.vector.Vector3i;
import me.dags.pitaya.command.annotation.*;
import me.dags.pitaya.command.command.Flags;
import me.dags.pitaya.command.fmt.Fmt;
import me.dags.pitaya.util.cache.IdCache;
import me.dags.portal.link.Link;
import me.dags.portal.portal.Portal;
import me.dags.portal.portal.PortalBuilder;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @Author <dags@dags.me>
 */
public class Commands {

    private final Portals plugin;
    private final IdCache<PortalBuilder> builders = new IdCache<>(5, TimeUnit.MINUTES, (id, builder) -> {
        Optional<Player> player = Sponge.getServer().getPlayer(id);
        player.ifPresent(Fmt.subdued("Your portal builder has expired")::tell);
    });

    Commands(Portals plugin) {
        this.plugin = plugin;
        Sponge.getEventManager().registerListeners(plugin, this);
    }

    @Command("portal wand")
    @Permission("portal.command.wand")
    @Description("Give yourself a tool for creating portals")
    public void wand(@Src Player player) {
        PortalBuilder builder = builders.compute(player, u -> new PortalBuilder());
        player.getInventory().offer(builder.tool);
        Fmt.info("You were given a portal wand").tell(player);
    }

    @Command("portal pos1")
    @Permission("portal.command.pos")
    @Description("Set the first position of the portal")
    public void pos1(@Src Player player) {
        PortalBuilder builder = builders.compute(player, u -> new PortalBuilder());
        builder.world = player.getWorld().getName();
        builder.pos1 = player.getPosition().toInt();
        Fmt.info("Set pos1 ").stress(builder.pos1).tell(player);
    }

    @Command("portal pos2")
    @Permission("portal.command.pos")
    @Description("Set the second position of the portal")
    public void pos2(@Src Player player) {
        PortalBuilder builder = builders.compute(player, u -> new PortalBuilder());
        builder.world = player.getWorld().getName();
        builder.pos2 = player.getPosition().toInt();
        Fmt.info("Set pos2 ").stress(builder.pos2).tell(player);
    }

    @Command("portal restrict <bool>")
    @Permission("portal.command.restrict")
    @Description("Set whether permission should be required to use the portal")
    public void restrict(@Src Player player, boolean restrict) {
        PortalBuilder builder = builders.compute(player, u -> new PortalBuilder());
        builder.restrict = restrict;
        Fmt.info("Set restricted ").stress(restrict).tell(player);
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

        Portal portal = builder.build(name);
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

    @Listener
    public void onPrimary(InteractBlockEvent.Primary event, @Root Player player) {
        boolean cancel = onWand(player, event.getTargetBlock().getPosition(), 1);
        event.setCancelled(cancel);
    }

    @Listener
    public void onSecondary(InteractBlockEvent.Primary event, @Root Player player) {
        boolean cancel = onWand(player, event.getTargetBlock().getPosition(), 2);
        event.setCancelled(cancel);
    }

    private boolean onWand(Player player, Vector3i position, int type) {
        Optional<ItemStack> stack = player.getItemInHand(HandTypes.MAIN_HAND);
        if (!stack.isPresent()) {
            return false;
        }

        Optional<PortalBuilder> builder = builders.get(player);
        if (!builder.isPresent()) {
            return false;
        }

        if (!stack.get().equalTo(builder.get().tool)) {
            return false;
        }

        if (type == 1) {
            builder.get().pos1 = position;
        } else {
            builder.get().pos2 = position;
        }

        builder.get().world = player.getWorld().getName();
        Fmt.info("Set pos%s ", type).stress(position).tell(player);
        return true;
    }
}
