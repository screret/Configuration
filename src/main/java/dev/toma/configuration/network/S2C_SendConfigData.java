package dev.toma.configuration.network;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.ConfigHolder;
import dev.toma.configuration.config.adapter.TypeAdapter;
import dev.toma.configuration.config.value.ConfigValue;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;

public class S2C_SendConfigData implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(Configuration.MODID, "send_config_data");

    private final String config;

    public S2C_SendConfigData(String config) {
        this.config = config;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.config);
        ConfigHolder.getConfig(this.config).ifPresent(data -> {
            Map<String, ConfigValue<?>> serialized = data.getNetworkSerializedFields();
            buffer.writeInt(serialized.size());
            for (Map.Entry<String, ConfigValue<?>> entry : serialized.entrySet()) {
                String id = entry.getKey();
                ConfigValue<?> value = entry.getValue();
                TypeAdapter adapter = value.getAdapter();
                buffer.writeUtf(id);
                adapter.encodeToBuffer(value, buffer);
            }
        });
    }

    public static S2C_SendConfigData decode(FriendlyByteBuf buffer) {
        String config = buffer.readUtf();
        S2C_SendConfigData packet = new S2C_SendConfigData(config);
        int i = buffer.readInt();
        ConfigHolder.getConfig(config).ifPresent(data -> {
            Map<String, ConfigValue<?>> serialized = data.getNetworkSerializedFields();
            for (int j = 0; j < i; j++) {
                String fieldId = buffer.readUtf();
                ConfigValue<?> value = serialized.get(fieldId);
                if (value == null) {
                    Configuration.LOGGER.fatal(Networking.MARKER, "Received unknown config value " + fieldId);
                    throw new RuntimeException("Unknown config field: " + fieldId);
                }
                packet.setValue(value, buffer);
            }
        });
        return packet;
    }

    public static void handle(S2C_SendConfigData packet, IPayloadContext context) {

    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @SuppressWarnings("unchecked")
    private <V> void setValue(ConfigValue<V> value, FriendlyByteBuf buffer) {
        TypeAdapter adapter = value.getAdapter();
        V v = (V) adapter.decodeFromBuffer(value, buffer);
        value.set(v);
    }
}
