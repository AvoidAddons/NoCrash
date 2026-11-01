# Avoid Mod Template
The template for creating mods using Avoid Framework.<br>
Learn more [here.](https://github.com/AvoidLib/Avoid/wiki)

## Funcutter multiversioning
Funcutter is a very fast, and quite lightweight alternative to Stonecutter.<br/>
It allows you to modify your gradle.properties, java files and resources for each version.

Since Avoid removes the pain with compatibility, *(you don't have to implement anything to achieve it)*<br/>
this is only useful if:

- you use NMC/NMS (`net.minecraft.client` / `net.minecraft.server`);
- you want to just test your mod on multiple versions, to make sure Avoid isn't broken;
- you depend on non-Avoid mods/plugins.

To understand what a Funcutter file does, run the following in your command prompt:
```shell
funcutter !dump
```

To run your Minecraft client for each Funcutter version, run the following in your command prompt:
```shell
funcutter runClient --build-cache --no-rebuild
```

*(the `--` arguments are optional, but recommended)*
