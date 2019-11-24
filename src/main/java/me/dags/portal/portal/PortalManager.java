package me.dags.portal.portal;

import me.dags.pitaya.config.Config;
import me.dags.pitaya.config.Node;
import org.spongepowered.api.registry.CatalogRegistryModule;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @Author <dags@dags.me>
 */
public class PortalManager implements CatalogRegistryModule<Portal> {

    private final Config storage;
    private final Node section;
    private final Map<String, Portal> registry = new HashMap<>();

    public PortalManager(Config storage) {
        this.storage = storage;
        this.section = storage.node("portals");
    }

    @Override
    public Optional<Portal> getById(String id) {
        return Optional.ofNullable(registry.get(id));
    }

    @Override
    public Collection<Portal> getAll() {
        return registry.values();
    }

    @Override
    public void registerDefaults() {
        registry.clear();
        section.iterate((key, value) -> {
            Portal portal = Portal.deserialize(key.toString(), value);
            registry.put(portal.getName(), portal);
        });
    }

    public boolean delete(String name) {
        if (registry.remove(name) != null) {
            section.clear(name);
            storage.save();
            return true;
        }
        return false;
    }

    public void register(Portal portal) {
        registry.put(portal.getName(), portal);
        Portal.serialize(portal, section.node(portal.getName()));
        storage.save();
    }
}
