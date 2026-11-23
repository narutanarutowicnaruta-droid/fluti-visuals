package com.fluti.aihelper.command;

import com.fluti.aihelper.entity.HelperBotEntity;
import com.fluti.aihelper.util.BlockDictionary;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Locale;

public class ChatCommandHandler {

    public static boolean handleOutgoingMessage(String message) {
        if (message == null || !message.startsWith(".")) {
            return true;
        }

        String trimmed = message.substring(1).trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        String[] parts = trimmed.split("\s+", 2);
        String verb = parts[0].toLowerCase(Locale.ROOT);
        BlockDictionary.Language lang = BlockDictionary.detectLanguage(trimmed);

        boolean isMineVerb = verb.equals("добыть") || verb.equals("копать")
                || verb.equals("добути") || verb.equals("копати")
                || verb.equals("mine") || verb.equals("dig");

        boolean isStopVerb = verb.equals("стоп") || verb.equals("stop") || verb.equals("стой") || verb.equals("стій");

        MinecraftClient client = MinecraftClient.getInstance();

        if (isStopVerb) {
            HelperBotEntity.clearActiveTarget();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("[Fluti AI] Stopped."), false);
            }
            return false;
        }

        if (isMineVerb) {
            if (parts.length < 2 || parts[1].isBlank()) {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("[Fluti AI] Specify block: wood/дерево, stone/камінь, iron/залізо"), false);
                }
                return false;
            }

            String blockQuery = parts[1].trim();
            BlockDictionary.TargetSpec spec = BlockDictionary.resolve(blockQuery);

            if (spec != null) {
                HelperBotEntity.setActiveTarget(spec);
                if (client.player != null) {
                    client.player.sendMessage(Text.literal(BlockDictionary.formatAccepted(lang, spec.canonicalName())), false);
                }
            } else if (client.player != null) {
                client.player.sendMessage(Text.literal(BlockDictionary.formatUnknownBlock(lang, blockQuery)), false);
            }
            return false;
        }

        return true;
    }
}
