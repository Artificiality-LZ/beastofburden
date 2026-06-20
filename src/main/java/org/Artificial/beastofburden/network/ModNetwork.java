package org.Artificial.beastofburden.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.Artificial.beastofburden.Beastofburden;

public final class ModNetwork
{
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
      ResourceLocation.fromNamespaceAndPath(Beastofburden.MODID, "main"),
      () -> PROTOCOL,
      PROTOCOL::equals,
      PROTOCOL::equals
    );

    private static int packetId;

    private ModNetwork()
    {
    }

    public static void register()
    {
        CHANNEL.registerMessage(nextId(), SaveBeastConfigMessage.class, SaveBeastConfigMessage::encode, SaveBeastConfigMessage::decode, SaveBeastConfigMessage::handle);
    }

    private static int nextId()
    {
        return packetId++;
    }
}
