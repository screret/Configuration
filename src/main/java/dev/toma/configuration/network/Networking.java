package dev.toma.configuration.network;

import dev.toma.configuration.Configuration;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public final class Networking {

    public static final Marker MARKER = MarkerManager.getMarker("Network");

    public static void sendClientPacket(ServerPlayer target, CustomPacketPayload packet) {
        target.connection.send(packet);
    }

    public static void registerPayloads(RegisterPayloadHandlerEvent event) {
        event.registrar(Configuration.MODID)
                .common(S2C_SendConfigData.ID, S2C_SendConfigData::decode, builder -> builder.client(S2C_SendConfigData::handle));
    }
}
