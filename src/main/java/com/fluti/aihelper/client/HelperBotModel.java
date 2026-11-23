package com.fluti.aihelper.client;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;

public class HelperBotModel extends BipedEntityModel<BipedEntityRenderState> {

    public HelperBotModel(ModelPart root) {
        super(root);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = BipedEntityModel.getModelData(Dilation.NONE, 0.0f);
        return TexturedModelData.of(modelData, 64, 64);
    }
}
