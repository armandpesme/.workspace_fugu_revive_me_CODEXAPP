package com.fuguteams.fugureviveme.registry;

import com.fuguteams.fugureviveme.FuguReviveMe;
import com.fuguteams.fugureviveme.item.ReturnPendantItem;
import com.fuguteams.fugureviveme.item.SoulAnchorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FuguReviveMe.MOD_ID);

    public static final RegistryObject<Item> SOUL_ANCHOR =
            ITEMS.register("soul_anchor", SoulAnchorItem::new);
    public static final RegistryObject<Item> RETURN_PENDANT =
            ITEMS.register("return_pendant", ReturnPendantItem::new);

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
