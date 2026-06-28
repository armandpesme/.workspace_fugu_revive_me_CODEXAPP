package com.fuguteams.fugureviveme.registry;

import com.fuguteams.fugureviveme.FuguReviveMe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FuguReviveMe.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN =
            CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fugu_revive_me.main"))
                    .icon(() -> new ItemStack(ModItems.SOUL_ANCHOR.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SOUL_ANCHOR.get());
                        output.accept(ModItems.RETURN_PENDANT.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_TABS.register(modEventBus);
    }
}
