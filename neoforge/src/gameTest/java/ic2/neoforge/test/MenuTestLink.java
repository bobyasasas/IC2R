package ic2.neoforge.test;

import ic2.neoforge.menu.MachineMenu;

import io.netty.buffer.Unpooled;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;

/** Preserve the native signed-short wire format while observing server menu updates in tests. */
final class MenuTestLink {
    static void connect(MachineMenu server, MachineMenu client) {
        server.addSlotListener(
                new ContainerListener() {
                    @Override
                    public void slotChanged(
                            AbstractContainerMenu menu, int slot, ItemStack stack) {}

                    @Override
                    public void dataChanged(AbstractContainerMenu menu, int id, int value) {
                        var buffer = new FriendlyByteBuf(Unpooled.buffer());
                        try {
                            var codec = ClientboundContainerSetDataPacket.STREAM_CODEC;
                            codec.encode(
                                    buffer,
                                    new ClientboundContainerSetDataPacket(
                                            server.containerId, id, value));
                            var decoded = codec.decode(buffer);
                            client.setData(decoded.getId(), decoded.getValue());
                        } finally {
                            buffer.release();
                        }
                    }
                });
    }

    private MenuTestLink() {}
}
