package dev.imabad.theatrical.blockentities;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.imabad.theatrical.TheatricalExpectPlatform;
import dev.imabad.theatrical.TheatricalRegistry;
import dev.imabad.theatrical.blockentities.interfaces.ArtNetInterfaceBlockEntity;
import dev.imabad.theatrical.blockentities.light.MovingLightBlockEntity;
import dev.imabad.theatrical.blocks.Blocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = TheatricalRegistry.get(Registries.BLOCK_ENTITY_TYPE);
    public static final RegistrySupplier<BlockEntityType<MovingLightBlockEntity>> MOVING_LIGHT = BLOCK_ENTITIES.register("moving_light", () -> BlockEntityType.Builder.of(MovingLightBlockEntity::new, Blocks.MOVING_LIGHT_BLOCK.get()).build(null));
    public static final RegistrySupplier<BlockEntityType<ArtNetInterfaceBlockEntity>> ART_NET_INTERFACE = BLOCK_ENTITIES.register("artnet_interface", () -> BlockEntityType.Builder.of(ArtNetInterfaceBlockEntity::new, Blocks.ART_NET_INTERFACE.get()).build(null));
}
