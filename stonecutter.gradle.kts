plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.1" /* [SC] DO NOT EDIT */

stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String

    // Minecraft 1.21.11+ (and all of 26.x) renamed ResourceLocation to Identifier as part of shipping
    // unobfuscated - every usage is a real type reference (imports, parameter types, static factory calls),
    // never a comment or unrelated string, so a blanket literal replace is safe here.
    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
    }
}
