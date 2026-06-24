package com.sharedlives.network;

import com.sharedlives.SharedLivesMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetupPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetupPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(SharedLivesMod.MOD_ID, "open_setup"));

    public static final StreamCodec<ByteBuf, SetupPayload> STREAM_CODEC =
            StreamCodec.unit(new SetupPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
