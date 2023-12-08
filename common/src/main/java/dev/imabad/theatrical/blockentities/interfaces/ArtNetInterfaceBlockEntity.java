package dev.imabad.theatrical.blockentities.interfaces;

import com.mojang.util.UUIDTypeAdapter;
import com.mojang.util.UndashedUuid;
import dev.imabad.theatrical.TheatricalClient;
import dev.imabad.theatrical.blockentities.BlockEntities;
import dev.imabad.theatrical.blockentities.ClientSyncBlockEntity;
import dev.imabad.theatrical.config.TheatricalConfig;
import dev.imabad.theatrical.dmx.DMXNetworkData;
import dev.imabad.theatrical.net.SendArtNetData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class ArtNetInterfaceBlockEntity extends ClientSyncBlockEntity {
    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T be) {
        ArtNetInterfaceBlockEntity tile = (ArtNetInterfaceBlockEntity) be;
        if(level.isClientSide){
            if(tile.isOwnedByCurrentClient()){
                byte[] data = TheatricalClient.getArtNetManager().getClient(tile.ip).readDmxData(tile.subnet, tile.universe);
                new SendArtNetData(pos, data).sendToServer();
            }
        }
    }

    private int subnet, universe, tickTimer = 0;
    private String ip = "127.0.0.1";
    private UUID ownerUUID;

    public ArtNetInterfaceBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.ART_NET_INTERFACE.get(), blockPos, blockState);
    }

    @Override
    public void write(CompoundTag compoundTag) {
        compoundTag.putString("ip", ip);
        compoundTag.putInt("subnet", subnet);
        compoundTag.putInt("universe", universe);
        compoundTag.putUUID("ownerUUID", ownerUUID);
    }

    @Override
    public void read(CompoundTag compoundTag) {
        this.ip = compoundTag.getString("ip");
        this.subnet = compoundTag.getInt("subnet");
        this.universe = compoundTag.getInt("universe");
        this.ownerUUID = compoundTag.getUUID("ownerUUID");
    }

    public void update(byte[] data) {
        if(level != null && level.getServer() != null) {
            var dmxData = DMXNetworkData.getInstance();
            if(dmxData != null) {
                dmxData.getConsumersInRange(getBlockPos(), TheatricalConfig.INSTANCE.COMMON.wirelessDMXRadius).forEach(dmxConsumer -> dmxConsumer.consume(data));
            }
        }
    }

    public boolean isOwnedByCurrentClient(){
        if(!TheatricalConfig.INSTANCE.CLIENT.doOwnerCheck){
            return true;
        }
        if(level != null && level.isClientSide){
            return ownerUUID != null && ownerUUID.equals(Minecraft.getInstance().getUser().getProfileId());
        }
        return false;
    }

    public boolean hasReceivedPacket(){
        if(level != null && level.isClientSide){
            return TheatricalClient.getArtNetManager().getClient(this.ip).hasReceivedPacket();
        }
        return false;
    }

    public long getLastReceivedPacket(){
        if(level != null && level.isClientSide){
            return TheatricalClient.getArtNetManager().getClient(this.ip).getLastPacketMS();
        }
        return 0;
    }

    public int getUniverse() {
        return universe;
    }

    public String getIp() {
        return ip;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void updateConfig(String ipAddress, int dmxUniverse){
        this.ip = ipAddress;
        this.universe = dmxUniverse;
        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
}
