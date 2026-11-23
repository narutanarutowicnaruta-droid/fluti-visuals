package com.fluti.aihelper;

import com.fluti.aihelper.entity.HelperBotEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlutiAiMod implements ModInitializer {
    public static final String MOD_ID = "fluti_ai";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final RegistryKey<EntityType<?>> HELPER_BOT_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(MOD_ID, "helper_bot")
    );

    public static final EntityType<HelperBotEntity> HELPER_BOT = Registry.register(
            Registries.ENTITY_TYPE,
            HELPER_BOT_KEY,
            EntityType.Builder.create(HelperBotEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 1.8f)
                    .build(HELPER_BOT_KEY)
    );

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(HELPER_BOT, HelperBotEntity.createBotAttributes());
        LOGGER.info("Fluti AI Helper initialized for 1.21.11");
    }
}
