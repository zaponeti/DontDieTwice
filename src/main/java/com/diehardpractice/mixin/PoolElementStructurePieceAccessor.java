package com.diehardpractice.mixin;

import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the jigsaw piece template a generated structure piece was built from, to identify structure variants. */
@Mixin(PoolElementStructurePiece.class)
public interface PoolElementStructurePieceAccessor {
	@Accessor("element")
	StructurePoolElement diehardpractice$getElement();
}
