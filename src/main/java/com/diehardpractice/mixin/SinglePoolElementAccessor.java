package com.diehardpractice.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes which template resource location (or inline template) a jigsaw piece was built from. */
@Mixin(SinglePoolElement.class)
public interface SinglePoolElementAccessor {
	@Accessor("template")
	Either<ResourceLocation, StructureTemplate> diehardpractice$getTemplate();
}
