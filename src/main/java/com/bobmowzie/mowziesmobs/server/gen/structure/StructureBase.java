package com.bobmowzie.mowziesmobs.server.gen.structure;

import net.minecraft.block.Block;
import net.minecraft.world.World;

public class StructureBase {
    public static void replaceBlocks(Block toReplace, Block replacement, int x, int y, int z, int length, int height, int width, World world) {
//        System.out.println("Replacing block beginning at" + x + ", " + y + ", " + z);
        int i = 0;
        for (int x_ = x; x_ <= x + length; x_++) {
            for (int z_ = z; z_ <= z + width; z_++) {
//                System.out.println("Block at " + x_ + ", " + z_);
                for (int y_ = y; y_ <= y + height; y_++) {
                    if (world.getBlock(x_, y_, z_) == toReplace) {
//                        System.out.println("Block at " + x_ + ", " + y_ + ", " + z_ + " is " + world.getBlock(x_, y_, z_).getLocalizedName());
                        world.setBlock(x_, y_, z_, replacement);
                        i++;
                    }
                }
            }
        }
//        System.out.println("Replacing " + i + " block");
    }

    public static void replaceBlocks(Block toReplace, int toReplaceMetaData, Block replacement, int replacementMetaData, int x, int y, int z, int length, int height, int width, World world) {
//        System.out.println("Replacing block beginning at" + x + ", " + y + ", " + z);
        int i = 0;
        for (int x_ = x; x_ <= x + length; x_++) {
            for (int z_ = z; z_ <= z + width; z_++) {
                for (int y_ = y; y_ <= y + height; y_++) {
//                    System.out.println("Block at " + x_ + ", " + y_ + ", " + z_);
                    if (world.getBlock(x_, y_, z_) == toReplace && world.getBlockMetadata(x_, y_, z_) == toReplaceMetaData) {
//                        System.out.println("Block at " + x_ + ", " + y_ + ", " + z_ + " is " + world.getBlock(x_, y_, z_).getLocalizedName());
                        world.setBlock(x_, y_, z_, replacement, replacementMetaData, 3);
                        i++;
                    }
                }
            }
        }
//        System.out.println("Replacing " + i + " block");
    }
}