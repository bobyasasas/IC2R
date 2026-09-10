package ic2.neoforge.client;

import ic2.neoforge.menu.NukeMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The nuke payload screen (legacy guidef/nuke.xml: 176x219). */
public final class NukeScreen extends ContainerScreenBase<NukeMenu> {
    public NukeScreen(NukeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 219);
    }
}
