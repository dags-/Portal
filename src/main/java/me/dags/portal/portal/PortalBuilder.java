package me.dags.portal.portal;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

/**
 * @Author <dags@dags.me>
 */
public class PortalBuilder {

    public String world = "";
    public Vector3i pos1 = null;
    public Vector3i pos2 = null;
    public boolean restrict = false;
    public final ItemStack tool = ItemStack.builder()
            .itemType(ItemTypes.STICK)
            .add(Keys.DISPLAY_NAME, Text.of("Portal Tool"))
            .build();

    public Portal build(String name) {
        return new Portal(name.toLowerCase(), this);
    }
}
