package me.dags.portal.link;

import com.flowpowered.math.vector.Vector3i;
import me.dags.pitaya.config.Config;
import me.dags.pitaya.config.Node;
import me.dags.portal.portal.Portal;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.world.World;

import java.util.*;

/**
 * @Author <dags@dags.me>
 */
public class LinkManager {

    private static final List<Link> emptyList = Collections.emptyList();
    private static final Map<Long, List<Link>> emptyMap = Collections.emptyMap();

    private final Node section;
    private final Config storage;
    private final Map<String, Map<Long, List<Link>>> links = new HashMap<>();

    public LinkManager(Config storage) {
        this.storage = storage;
        this.section = storage.node("links");
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
        Map<Long, List<Link>> worldLinks = links.getOrDefault(world, emptyMap);
        if (worldLinks == emptyMap) {
            return emptyList;
        }
        return worldLinks.getOrDefault(getId(chunkX, chunkZ), LinkManager.emptyList);
    }

    public void register(Link link) {
        addLink(link);
        section.set(link.getFrom().getName(), link.getTo().getName());
        storage.save();
    }

    public void reload() {
        links.clear();
        section.iterate((key, value) -> {
            Optional<Portal> portal1 = Sponge.getRegistry().getType(Portal.class, key.toString());
            Optional<Portal> portal2 = Sponge.getRegistry().getType(Portal.class, value.get(""));
            if (portal1.isPresent() && portal2.isPresent()) {
                Link link = new Link(portal1.get(), portal2.get());
                addLink(link);
            }
        });
    }

    public int unlink(Portal portal) {
        List<String> remove = new LinkedList<>();

        section.iterate((key, value) -> {
            if (key.toString().equals(portal.getName()) || value.get("").equals(portal.getName())) {
                remove.add(key.toString());
            }
        });

        if (remove.size() > 0) {
            remove.forEach(section::clear);
            storage.save();
            reload();
        }

        return remove.size();
    }

    private void addLink(Link link) {
        Map<Long, List<Link>> chunks = links.computeIfAbsent(link.getFrom().getWorldName(), s -> new HashMap<>());
        int minX = link.getFrom().getMin().getFloorX() >> 4;
        int minZ = link.getFrom().getMin().getFloorZ() >> 4;
        int maxX = link.getFrom().getMax().getFloorX() >> 4;
        int maxZ = link.getFrom().getMax().getFloorZ() >> 4;
        for (int cz = minZ; cz <= maxZ; cz++) {
            for (int cx = minX; cx <= maxX; cx++) {
                chunks.computeIfAbsent(getId(cx, cz), i -> new LinkedList<>()).add(link);
            }
        }
    }

    private static long getId(int x, int z) {
        return (long) x & 4294967295L | ((long) z & 4294967295L) << 32;
    }
}
