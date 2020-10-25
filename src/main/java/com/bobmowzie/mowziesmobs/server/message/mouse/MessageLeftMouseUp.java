package com.bobmowzie.mowziesmobs.server.message.mouse;

import io.netty.buffer.ByteBuf;
import com.ilexiconn.llibrary.server.entity.EntityPropertiesHandler;
import com.ilexiconn.llibrary.server.network.AbstractMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Created by Josh on 5/25/2017.
 */
public class MessageLeftMouseUp extends AbstractMessage<MessageLeftMouseUp> {
    public MessageLeftMouseUp() {

    }

    @Override
    public void toBytes(ByteBuf buf) {

    }

    @Override
    public void fromBytes(ByteBuf buf) {

    }

    @Override
    public void onClientReceived(Minecraft client, MessageLeftMouseUp message, PlayerEntity player, MessageContext messageContext) {

    }

    @Override
    public void onServerReceived(MinecraftServer server, MessageLeftMouseUp message, PlayerEntity player, MessageContext messageContext) {
        MowziePlayerProperties property = EntityPropertiesHandler.INSTANCE.getProperties(player, MowziePlayerProperties.class);
        property.mouseLeftDown = false;
        for (int i = 0; i < property.powers.length; i++) {
            property.powers[i].onLeftMouseUp(player);
        }
    }
}