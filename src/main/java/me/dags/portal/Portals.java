package me.dags.portal;

import com.google.inject.Inject;
import me.dags.pitaya.command.CommandBus;
import me.dags.pitaya.config.Config;
import me.dags.portal.link.LinkManager;
import me.dags.portal.portal.Portal;
import me.dags.portal.portal.PortalManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.Plugin;

import java.nio.file.Path;

/**
 * @Author <dags@dags.me>
 */
@Plugin(id = "portal")
public class Portals {

    private final LinkManager links;
    private final PortalManager portals;

    @Inject
    public Portals(@DefaultConfig(sharedRoot = false) Path path) {
        Config storage = Config.must(path);
        portals = new PortalManager(storage);
        links = new LinkManager(storage);
    }

    @Listener
    public void onPreInit(GamePreInitializationEvent event) {
        Sponge.getRegistry().registerModule(Portal.class, portals);
        CommandBus.create().register(new Commands(this)).submit();
        Sponge.getEventManager().registerListeners(this, new Events(this));
    }

    @Listener
    public void onPostInit(GamePostInitializationEvent event) {
        links.reload();
    }

    @Listener
    public void onReload(GameReloadEvent event) {
        getPortalManager().registerDefaults();
        links.reload();
    }

    public LinkManager getLinkManager() {
        return links;
    }

    public PortalManager getPortalManager() {
        return portals;
    }
}
