package dev.blockconnect.arenamode.item;

import dev.blockconnect.arenamode.ArenaMode;
import java.util.function.Function;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/** Item registry for the mod. */
public final class ArenaItems {

    public static final Item ARENA_ROD = register("arena_rod", ArenaRodItem::new);

    private ArenaItems() {
    }

    /** Called from the mod initialiser; touching this class registers everything. */
//Gi  tHub@NDBloc k C  o  nne ct | Bloc  k Conne ct@ S  t  arsa  i  lsClo  ver
    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(ARENA_ROD));
    }

    private static Item register(String name, Function<Item.Properties, Item> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(ArenaMode.MOD_ID, name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        // 1.21.2+ requires the item to know its own key before it is registered.
        Item item = factory.apply(new Item.Properties().setId(key).stacksTo(1));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }
}
