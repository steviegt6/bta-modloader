package dev.tomat.modloader.mappings;

import net.minecraft.core.item.Item;

public class MinecraftBeta1_8_1_MappingProvider extends AbstractMappingProvider {
    public MinecraftBeta1_8_1_MappingProvider() {
        super(getMappings());
    }

    private static MappedClass[] getMappings() {
        return new MappedClass[]{
                new MappedClass("sv", "net/minecraft/core/item/Item", new MappedField[]{}, new MappedMethod[]{
                        new MappedMethod("a", "setKey", "(java.lang.String;)Lnet/minecraft/core/item/Item;")
                })
        };
    }
}
