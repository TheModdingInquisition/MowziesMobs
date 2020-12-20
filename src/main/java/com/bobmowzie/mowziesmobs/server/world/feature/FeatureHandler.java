package com.bobmowzie.mowziesmobs.server.world.feature;

import com.bobmowzie.mowziesmobs.MowziesMobs;
import com.bobmowzie.mowziesmobs.server.spawn.SpawnHandler;
import com.bobmowzie.mowziesmobs.server.world.feature.structure.BarakoaVillagePieces;
import com.bobmowzie.mowziesmobs.server.world.feature.structure.BarakoaVillageStructure;
import com.bobmowzie.mowziesmobs.server.world.feature.structure.WroughtnautChamberPieces;
import com.bobmowzie.mowziesmobs.server.world.feature.structure.WroughtnautChamberStructure;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.structure.IStructurePieceType;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.placement.IPlacementConfig;
import net.minecraft.world.gen.placement.Placement;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;
import java.util.Set;

@Mod.EventBusSubscriber(modid = MowziesMobs.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FeatureHandler {
    public static Structure<NoFeatureConfig> WROUGHTNAUT_CHAMBER = (Structure<NoFeatureConfig>) new WroughtnautChamberStructure(NoFeatureConfig::deserialize).setRegistryName(MowziesMobs.MODID, "wroughtnaut_chamber");
    public static IStructurePieceType WROUGHTNAUT_CHAMBER_PIECE = WroughtnautChamberPieces.Piece::new;

    public static Structure<NoFeatureConfig> BARAKOA_VILLAGE = (Structure<NoFeatureConfig>) new BarakoaVillageStructure(NoFeatureConfig::deserialize).setRegistryName(MowziesMobs.MODID, "barakoa_village");
    public static IStructurePieceType BARAKOA_VILLAGE_PIECE = BarakoaVillagePieces.Piece::new;

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Feature<?>> event) {
        event.getRegistry().registerAll(
                WROUGHTNAUT_CHAMBER,
                BARAKOA_VILLAGE
        );
        registerPiece(WROUGHTNAUT_CHAMBER_PIECE, "WROUGHTNAUT_CHAMBER_PIECE");
        registerPiece(BARAKOA_VILLAGE_PIECE, "BARAKOA_VILLAGE_PIECE");
    }

    static IStructurePieceType registerPiece(IStructurePieceType structurePiece, String key)
    {
        return Registry.register(Registry.STRUCTURE_PIECE, key.toLowerCase(Locale.ROOT), structurePiece);
    }

    public static void addStructureGeneration() {
        for (Biome biome : ForgeRegistries.BIOMES) {

            if (SpawnHandler.FERROUS_WROUGHTNAUT_BIOMES.contains(biome)) {
                biome.addStructure(WROUGHTNAUT_CHAMBER, IFeatureConfig.NO_FEATURE_CONFIG);
                ConfiguredFeature configuredFeature = Biome.createDecoratedFeature(WROUGHTNAUT_CHAMBER, IFeatureConfig.NO_FEATURE_CONFIG, Placement.NOPE, IPlacementConfig.NO_PLACEMENT_CONFIG);
                biome.addFeature(GenerationStage.Decoration.UNDERGROUND_STRUCTURES, configuredFeature);
            }

            if (SpawnHandler.BARAKO_BIOMES.contains(biome)) {
                biome.addStructure(BARAKOA_VILLAGE, IFeatureConfig.NO_FEATURE_CONFIG);
                ConfiguredFeature configuredFeature = Biome.createDecoratedFeature(BARAKOA_VILLAGE, IFeatureConfig.NO_FEATURE_CONFIG, Placement.NOPE, IPlacementConfig.NO_PLACEMENT_CONFIG);
                biome.addFeature(GenerationStage.Decoration.SURFACE_STRUCTURES, configuredFeature);
            }

        }
    }
}