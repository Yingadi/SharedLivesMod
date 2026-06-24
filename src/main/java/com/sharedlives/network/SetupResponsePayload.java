package com.sharedlives.network;

import com.sharedlives.SharedLivesMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetupResponsePayload(boolean enabled, int lives) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetupResponsePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SharedLivesMod.MOD_ID, "setup_response"));

    public static final StreamCodec<ByteBuf, SetupResponsePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,    SetupResponsePayload::enabled,
                    ByteBufCodecs.VAR_INT, SetupResponsePayload::lives,
                    SetupResponsePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
