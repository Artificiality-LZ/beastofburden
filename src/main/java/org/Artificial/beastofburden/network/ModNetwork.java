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
        CHANNEL.registerMessage(nextId(), ToggleAutonomousPlanningMessage.class, ToggleAutonomousPlanningMessage::encode, ToggleAutonomousPlanningMessage::decode, ToggleAutonomousPlanningMessage::handle);
        CHANNEL.registerMessage(nextId(), CyclePlanningModeMessage.class, CyclePlanningModeMessage::encode, CyclePlanningModeMessage::decode, CyclePlanningModeMessage::handle);
        CHANNEL.registerMessage(nextId(), SaveColonyPlanMessage.class, SaveColonyPlanMessage::encode, SaveColonyPlanMessage::decode, SaveColonyPlanMessage::handle);
    }

    private static int nextId()
    {
        return packetId++;
    }
}
