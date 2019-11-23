package me.ardacraft.portal;

import com.flowpowered.math.vector.Vector3i;
import me.dags.pitaya.config.Config;
import me.dags.pitaya.config.Node;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.world.World;

import java.util.*;

/**
 * @Author <dags@dags.me>
 */
public class LinkManager {

    private static final Map<Long, List<Link>> emptyMap = Collections.emptyMap();
    private static final List<Link> emptyList = Collections.emptyList();

    private final Node section;
    private final Config storage;
    private final Map<String, Map<Long, List<Link>>> links = new HashMap<>();

    public LinkManager(Config storage) {
        this.storage = storage;
        this.section = storage.node("links");
        reload(false);
    }

    public List<Link> getLinks(Transform<World> transform) {
        return getLinks(transform.getExtent().getName(), transform.getPosition().toInt());
    }

    public List<Link> getLinks(String world, Vector3i position) {
        int chunkX = position.getX() >> 4;
        int chunkZ = position.getZ() >> 4;
        return getLinks(world, chunkX, chunkZ);
    }

    public List<Link> getLinks(String world, int chunkX, int chunkZ) {
        long id = toId(chunkX, chunkZ);
        Map<Long, List<Link>> worldLinks = links.getOrDefault(world, emptyMap);
        return worldLinks.getOrDefault(id, LinkManager.emptyList);
    }

    public void register(Link link) {
        register(link.getPortal1(), link);
        register(link.getPortal2(), link);
        section.set(link.getPortal1().getName(), link.getPortal2().getName());
        storage.save();
    }

    public void reload(boolean deleteMissing) {
        List<String> remove = new LinkedList<>();
        section.iterate((key, value) -> {
            Optional<Portal> portal1 = Sponge.getRegistry().getType(Portal.class, key.toString());
            Optional<Portal> portal2 = Sponge.getRegistry().getType(Portal.class, value.get(""));
            if (portal1.isPresent() && portal2.isPresent()) {
                Link link = new Link(portal1.get(), portal2.get());
                register(link.getPortal1(), link);
                register(link.getPortal2(), link);
            } else {
                remove.add(key.toString());
            }
        });

        if (deleteMissing && !remove.isEmpty()) {
            for (String key : remove) {
                section.clear(key);
            }
            storage.save();
        }
    }

    private void register(Portal portal, Link link) {
        Map<Long, List<Link>> worldLinks = links.computeIfAbsent(portal.getWorldName(), s -> new HashMap<>());
        int minX = portal.getMin().getFloorX() >> 4;
        int minZ = portal.getMin().getFloorZ() >> 4;
        int maxX = portal.getMax().getFloorX() >> 4;
        int maxZ = portal.getMax().getFloorZ() >> 4;
        for (int cz = minZ; cz <= maxZ; cz++) {
            for (int cx = minX; cx <= maxX; cx++) {
                long id = toId(cx, cz);
                worldLinks.computeIfAbsent(id, i -> new LinkedList<>()).add(link);
            }
        }
    }

    private static long toId(int x, int z) {
        return (long) x & 4294967295L | ((long) z & 4294967295L) << 32;
    }
}
