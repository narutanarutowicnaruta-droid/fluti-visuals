package com.fluti.aihelper.client;

import com.fluti.aihelper.FlutiAiMod;
import com.fluti.aihelper.entity.HelperBotEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.util.Identifier;

public class HelperBotRenderer extends MobEntityRenderer<HelperBotEntity, BipedEntityRenderState, HelperBotModel> {

    private static final Identifier TEXTURE = Identifier.of(FlutiAiMod.MOD_ID, "textures/entity/helper_bot.png");

    public HelperBotRenderer(EntityRendererFactory.Context context) {
        super(context, new HelperBotModel(context.getPart(FlutiAiClient.HELPER_BOT_LAYER)), 0.5f);
    }

    @Override
    public BipedEntityRenderState createRenderState() {
        return new BipedEntityRenderState();
    }

    @Override
    public Identifier getTexture(BipedEntityRenderState state) {
        return TEXTURE;
    }
}
