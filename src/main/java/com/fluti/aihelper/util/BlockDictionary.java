package com.fluti.aihelper.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BlockDictionary {

    public enum Language {
        RU, UA, EN
    }

    public record TargetSpec(String canonicalName, List<Block> blocks, TagKey<Block> tag) {
        public boolean matches(Block block) {
            if (tag != null && block.getDefaultState().isIn(tag)) {
                return true;
            }
            return blocks != null && blocks.contains(block);
        }
    }

    private static final Map<String, TargetSpec> ALIAS_MAP = new HashMap<>();

    static {
        TargetSpec woodSpec = new TargetSpec(
                "wood",
                List.of(
                        Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG,
                        Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.MANGROVE_LOG, Blocks.CHERRY_LOG,
                        Blocks.PALE_OAK_LOG, Blocks.CRIMSON_STEM, Blocks.WARPED_STEM
                ),
                BlockTags.LOGS
        );
        registerAliases(woodSpec, "wood", "log", "logs", "tree", "дерево", "бревно", "колода", "деревина");

        TargetSpec stoneSpec = new TargetSpec(
                "stone",
                List.of(Blocks.STONE, Blocks.COBBLESTONE, Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE),
                null
        );
        registerAliases(stoneSpec, "stone", "cobble", "cobblestone", "камень", "булыжник", "камінь", "бруківка");

        TargetSpec dirtSpec = new TargetSpec(
                "dirt",
                List.of(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT),
                null
        );
        registerAliases(dirtSpec, "dirt", "soil", "земля", "грунт", "ґрунт", "дерн");

        TargetSpec sandSpec = new TargetSpec(
                "sand",
                List.of(Blocks.SAND, Blocks.RED_SAND),
                null
        );
        registerAliases(sandSpec, "sand", "песок", "пісок");

        TargetSpec coalSpec = new TargetSpec(
                "coal",
                List.of(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE),
                null
        );
        registerAliases(coalSpec, "coal", "уголь", "вугілля");

        TargetSpec ironSpec = new TargetSpec(
                "iron",
                List.of(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE),
                null
        );
        registerAliases(ironSpec, "iron", "железо", "залізо");

        TargetSpec goldSpec = new TargetSpec(
                "gold",
                List.of(Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE),
                null
        );
        registerAliases(goldSpec, "gold", "золото");

        TargetSpec diamondSpec = new TargetSpec(
                "diamond",
                List.of(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE),
                null
        );
        registerAliases(diamondSpec, "diamond", "diamonds", "алмаз", "алмазы", "діамант", "діаманти");
    }

    private static void registerAliases(TargetSpec spec, String... aliases) {
        for (String alias : aliases) {
            ALIAS_MAP.put(alias.toLowerCase(Locale.ROOT).trim(), spec);
        }
    }

    public static TargetSpec resolve(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return null;
        }
        return ALIAS_MAP.get(rawInput.toLowerCase(Locale.ROOT).trim());
    }

    public static Language detectLanguage(String verbOrMessage) {
        String lower = verbOrMessage.toLowerCase(Locale.ROOT);
        if (lower.contains("добути") || lower.contains("копати") || lower.contains("є") || lower.contains("і")) {
            return Language.UA;
        }
        if (lower.contains("добыть") || lower.contains("копать")) {
            return Language.RU;
        }
        return Language.EN;
    }

    public static String formatAccepted(Language lang, String canonicalName) {
        return switch (lang) {
            case UA -> "[Fluti AI] Прийнято! Починаю добувати: " + canonicalName;
            case RU -> "[Fluti AI] Принято! Начинаю добывать: " + canonicalName;
            case EN -> "[Fluti AI] Accepted! Starting to mine: " + canonicalName;
        };
    }

    public static String formatUnknownBlock(Language lang, String query) {
        return switch (lang) {
            case UA -> "[Fluti AI] Невідомий блок: " + query + " (спробуй дерево/wood, камінь/stone, залізо/iron)";
            case RU -> "[Fluti AI] Неизвестный блок: " + query + " (попробуй дерево/wood, камень/stone, железо/iron)";
            case EN -> "[Fluti AI] Unknown block: " + query + " (try wood, stone, iron, coal, diamond)";
        };
    }
}
