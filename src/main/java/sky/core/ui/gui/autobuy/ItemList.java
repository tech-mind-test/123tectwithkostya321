package sky.core.ui.gui.autobuy;

import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import sky.core.modules.api.constructors.impl.ItemSetting;
import sky.core.modules.impl.player.AutoBuy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class ItemList {
    private static final List<ItemSetting> SPOOKY_ITEMS = new ArrayList<>();
    private static final List<ItemSetting> HOLY_ITEMS = new ArrayList<>();

    static {
        // --- ГАВНО ТАЙМ ITEMS ---
        addSpooky(new ItemSetting(Items.TOTEM_OF_UNDYING.getDefaultInstance(), "Талисман Карателя", false).addNBTparametr("Несёт строгий приговор"));
        addSpooky(new ItemSetting(Items.TOTEM_OF_UNDYING.getDefaultInstance(), "Талисман Крушителя", false).addNBTparametr("Легендарный символ"));
        addSpooky(new ItemSetting(Items.TOTEM_OF_UNDYING.getDefaultInstance(), "Талисман Ярости", false).addNBTparametr("Чистая, дикая агрессия"));
        addSpooky(new ItemSetting(Items.TOTEM_OF_UNDYING.getDefaultInstance(), "Талисман Мрака", false).addNBTparametr("Мрак сгущается рядом"));
        addSpooky(new ItemSetting(Items.TOTEM_OF_UNDYING.getDefaultInstance(), "Талисман Демона", false).addNBTparametr("Печать разжигает ярость"));
        addSpooky(new ItemSetting(Items.TOTEM_OF_UNDYING.getDefaultInstance(), "Талисман Тирана", false).addNBTparametr("Тиран подавляет слабых"));
        addSpooky(new ItemSetting(Items.TOTEM_OF_UNDYING.getDefaultInstance(), "Талисман вихря", false).addNBTparametr("Вихарь не знает покоя"));

        ItemSetting erisSphere = new ItemSetting(Items.PLAYER_HEAD.getDefaultInstance(), "Сфера Эрида", false).addNBTparametr("Холод Эриды");
        setSkin(erisSphere, "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzg2MTE4NywKICAicHJvZmlsZUlkIiA6ICJlZGUyYzdhMGFjNjM0MTNiYjA5ZDNmMGJlZTllYzhlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ0aGVEZXZKYWRlIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzZlNGUyZjEwNDdmM2VjNmU5ZTQ1OTE4NDczOWUzM2I3YzFmYzYzYWQ4MjAyYmRhYjlmMDI0NTA4YWRkMjNlNWIiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==");
        addSpooky(erisSphere);

        String aphinaTex = "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzg2MTE4NywKICAicHJvZmlsZUlkIiA6ICJlZGUyYzdhMGFjNjM0MTNiYjA5ZDNmMGJlZTllYzhlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJTcGhlcmVBdGhlbmEiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTNmOWVlZGEzYmEyM2ZlMTQyM2M0MDM2ZTdkZDBhNzQ0NjFkZmY5NmJhZGM1YjJmMmI5ZmFhN2NjMTZmMzgyZiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9".replaceAll("\\s+", "");
        ItemSetting aphinaSphere = new ItemSetting(Items.PLAYER_HEAD.getDefaultInstance(), "Сфера Афины", false).addNBTparametr(aphinaTex);
        setSkin(aphinaSphere, aphinaTex);
        addSpooky(aphinaSphere);

        ItemSetting titanSphere = new ItemSetting(Items.PLAYER_HEAD.getDefaultInstance(), "Сфера Титана", false).addNBTparametr("Мощь Титанов крепка");
        setSkin(titanSphere, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODFlOTY5ODQ1OGI3ODQxYzk2YWU0ZjI0ZWM4NGFlMDE3MjQxMDA2NDFjNTY0ZTJhN2IxODVmNDA2ZThlZDIzIn19fQ==");
        addSpooky(titanSphere);

        ItemSetting aresSphere = new ItemSetting(Items.PLAYER_HEAD.getDefaultInstance(), "Сфера Ареса", false).addNBTparametr("Дух Ареса");
        setSkin(aresSphere, "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzc3NDI1NSwKICAicHJvZmlsZUlkIiA6ICJhYWMxYjA2OWNkMjE0NWE2ODNlNzQxNzE4MDcxMGU4MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJqdXNhbXUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE2YWRjNmJhZmNiNTdmZDcwN2RlZTdkZDZhNzM2ZmUxMjY3MTFkNTNhMWZkNmNlNzg5ZGE0MWIzYmUxM2YyYSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9");
        addSpooky(aresSphere);

        ItemSetting beastSphere = new ItemSetting(Items.PLAYER_HEAD.getDefaultInstance(), "Сфера Бестии", false).addNBTparametr("Звериная дикая");
        setSkin(beastSphere, "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0MzgzNDkzMCwKICAicHJvZmlsZUlkIiA6ICI1MzUzNWIxN2M0ZDY0NWQ0YWUwY2U2ZjM4Zjk0NTFjYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJVYml2aXMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTQxMWFjMTczODFiOWZjZTliYWIzYzcyYWZkYjdmMTk4NTcwZGFmNDczMmJkODExZDMxYzIyN2Q4MGZhMzliMSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9");
        addSpooky(beastSphere);

        ItemSetting hydraSphere = new ItemSetting(Items.PLAYER_HEAD.getDefaultInstance(), "Сфера Гидры", false).addNBTparametr("Живучесть тёмных глубин");
        setSkin(hydraSphere, "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODUzMjE4MywKICAicHJvZmlsZUlkIiA6ICI1OGZmZWI5NTMxNGQ0ODcwYTQwYjVjYjQyZDRlYTU5OCIsCiAgInByb2ZpbGVOYW1lIiA6ICJTa2luREJuZXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2UzYzExOGQ2OTZkOTEwZTU0ZGUwMmNhNGQ4MDc1NDNmOWIxOGMwMDhjOTgzOGQyZmY2OTM3NzYyMmZiMWQzMiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9");
        addSpooky(hydraSphere);

        ItemSetting chaosSphere = new ItemSetting(Items.PLAYER_HEAD.getDefaultInstance(), "Сфера Хаоса", false).addNBTparametr("Хаос искажает");
        setSkin(chaosSphere, "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODY0MTkwMCwKICAicHJvZmlsZUlkIiA6ICIxNzRjZmRiNGEzY2I0M2I1YmZjZGU0MjRjM2JiMmM2ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJtYXJhZWwxOCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lN2E3YWU3Y2RjZjYxNmU4YjdhNDIyMWE2MjFiMjQzNTc1M2M2MGVkNmEyNThlYTA2MGRhZTMwMDJmZmU5ZTI4IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=");
        addSpooky(chaosSphere);

        ItemSetting satyrSphere = new ItemSetting(Items.PLAYER_HEAD.getDefaultInstance(), "Сфера сатира", false).addNBTparametr("Шёпот Сатира");
        setSkin(satyrSphere, "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODYwODUyOCwKICAicHJvZmlsZUlkIiA6ICJkMTQ4NjFiM2UwZmM0Njk5OTFlMTcyNTllMzdiZjZhZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJyYXhpdG9jbCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83NzFhOWE0OThiNGZhNWVjNDkzNjJmOWJjODhlZGE0ZjUyYjA0ZGU0OWQ3NWFhM2NhMzMyYTFmZWExYWEwZTU3IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0");
        addSpooky(satyrSphere);
        addSpooky(new ItemSetting(Items.TRIPWIRE_HOOK.getDefaultInstance(), "Отмычка к Сферам", false).addNBTparametr("Этой отмычкой можно").addNBTparametr("Открыть хранилище").addNBTparametr("С Сферами"));
        addSpooky(new ItemSetting(Items.NETHERITE_SCRAP.getDefaultInstance(), "Трапка", false).addNBTparametr("Нерушимая клетка").addNBTparametr("15 секунд"));
        addSpooky(new ItemSetting(Items.SUGAR.getDefaultInstance(), "Явная пыль", false).addNBTparametr("Световая вспышка").addNBTparametr("Радиус: 10 блоков"));
        addSpooky(new ItemSetting(Items.ENDER_EYE.getDefaultInstance(), "Дезориентация", false).addNBTparametr("Звуковая волна").addNBTparametr("Радиус: 10 блоков"));
        ItemSetting krabs = new ItemSetting(Items.PLAYER_HEAD.getDefaultInstance(), "Крабсбургер", false).addNBTparametr("Обменяй крабсбургер").addNBTparametr("/warp SpongeBob");
        setSkin(krabs, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWFlNWQ2NTZjNjM1Mjc4MjJjMjE3ZjQyNjdkYTBiNzUyNmU2NTQyNTRiNDFlNDA3N2VhNjc3YmM3Nzg2M2M1YiJ9fX0=");
        addSpooky(krabs);
        addSpooky(new ItemSetting(Items.DRAGON_HEAD.getDefaultInstance(), "Голова Дракона", false));
        addSpooky(new ItemSetting(Items.BEACON.getDefaultInstance(), "Маяк", false));
        addSpooky(new ItemSetting(Items.SPAWNER.getDefaultInstance(), "Спавнер", false));
        addSpooky(new ItemSetting(Items.WITHER_SKELETON_SKULL.getDefaultInstance(), "Череп визер скелета", false));
        ItemSetting villagerEgg = new ItemSetting(Items.VILLAGER_SPAWN_EGG.getDefaultInstance(), "Яйцо жителя", false);
        villagerEgg.setSearchQuery("яйцо призыва крестьянина");addSpooky(villagerEgg);
        ItemSetting zombieVillagerEgg = new ItemSetting(Items.ZOMBIE_VILLAGER_SPAWN_EGG.getDefaultInstance(), "Яйцо зомби-жителя", false);
        zombieVillagerEgg.setSearchQuery("яйцо призыва зомби-крестьянина");
        addSpooky(zombieVillagerEgg);

        addSpooky(new ItemSetting(Items.NETHERITE_SWORD.getDefaultInstance(), "Меч", false));
        addSpooky(new ItemSetting(Items.NETHERITE_PICKAXE.getDefaultInstance(), "Молот Тора", false).addNBTparametr("Молот Тора"));
        addSpooky(new ItemSetting(Items.GOLDEN_PICKAXE.getDefaultInstance(), "Божье Касание", false).addNBTparametr("Божье Касание").addNBTparametr("Может добыть спавнер"));
        addSpooky(new ItemSetting(Items.NETHERITE_AXE.getDefaultInstance(), "Топор", false));
        addSpooky(new ItemSetting(Items.CROSSBOW.getDefaultInstance(), "Арбалет Крушителя", false).addNBTparametr("Арбалет Крушителя"));
        addSpooky(new ItemSetting(Items.TRIDENT.getDefaultInstance(), "Трезубец крушителя", false).addNBTparametr("Трезубец крушителя"));
        addSpooky(new ItemSetting(Items.NETHERITE_SWORD.getDefaultInstance(), "Полуночный меч", false).addNBTparametr("Полуночный меч"));

        addSpooky(new ItemSetting(Items.NETHERITE_HELMET.getDefaultInstance(), "Шлем Крушителя", false).addNBTparametr("Оригинальный предмет").addEnchantment("protection", 5));
        addSpooky(new ItemSetting(Items.NETHERITE_CHESTPLATE.getDefaultInstance(), "Нагрудник Крушителя", false).addNBTparametr("Оригинальный предмет").addEnchantment("protection", 5));
        addSpooky(new ItemSetting(Items.NETHERITE_LEGGINGS.getDefaultInstance(), "Поножи Крушителя", false).addNBTparametr("Оригинальный предмет").addEnchantment("protection", 5));
        addSpooky(new ItemSetting(Items.NETHERITE_BOOTS.getDefaultInstance(), "Ботинки Крушителя", false).addNBTparametr("Оригинальный предмет").addEnchantment("protection", 5));

        addSpooky(new ItemSetting(Items.POTION.getDefaultInstance(), "Зелье", false));
        addSpooky(new ItemSetting(Items.SPLASH_POTION.getDefaultInstance(), "Святая Вода", false).addNBTparametr("Святая Вода"));
        addSpooky(new ItemSetting(Items.SPLASH_POTION.getDefaultInstance(), "Зелье Палладина", false).addNBTparametr("Прилив здоровья III").addNBTparametr("Невидимость III"));
        addSpooky(new ItemSetting(Items.SPLASH_POTION.getDefaultInstance(), "Зелье Радиации", false).addNBTparametr("Иссушение").addNBTparametr("Голод V"));
        addSpooky(new ItemSetting(Items.SPLASH_POTION.getDefaultInstance(), "Зелье Ассасина", false).addNBTparametr("Сила IV").addNBTparametr("Спешка"));
        addSpooky(new ItemSetting(Items.SPLASH_POTION.getDefaultInstance(), "Зелье Гнева", false).addNBTparametr("Сила V").addNBTparametr("Замедление IV"));
        addSpooky(new ItemSetting(Items.SPLASH_POTION.getDefaultInstance(), "Снотворное", false).addNBTparametr("Слепота").addNBTparametr("Иссушение III"));

        addSpooky(new ItemSetting(Items.SNOWBALL.getDefaultInstance(), "Хлопушка", false).addNBTparametr("Хлопушка"));
        addSpooky(new ItemSetting(Items.ARROW.getDefaultInstance(), "Мучительная стрела", false).addNBTparametr("Мучительная стрела"));
        addSpooky(new ItemSetting(Items.ARROW.getDefaultInstance(), "Кровавая стрела", false).addNBTparametr("Кровавая стрела"));
        addSpooky(new ItemSetting(Items.EMERALD_ORE.getDefaultInstance(), "Изумрудная руда", false));
        addSpooky(new ItemSetting(Items.GUNPOWDER.getDefaultInstance(), "Порох", false));
        addSpooky(new ItemSetting(Items.CREEPER_SPAWN_EGG.getDefaultInstance(), "Загадочное яйцо (Крипер)", false).addNBTparametr("Брутальный пиглин"));
        addSpooky(new ItemSetting(Items.WITCH_SPAWN_EGG.getDefaultInstance(), "Загадочное яйцо (Ведьма)", false).addNBTparametr("Брутальный пиглин"));
        addSpooky(new ItemSetting(Items.SPAWNER.getDefaultInstance(), "Загадочный спавнер", false).addNBTparametr("Брутальный пиглин"));

        addSpooky(new ItemSetting(Items.ELYTRA.getDefaultInstance(), "Элитры", false));
        addSpooky(new ItemSetting(Items.TOTEM_OF_UNDYING.getDefaultInstance(), "Тотем бессмертия", false));
        addSpooky(new ItemSetting(Items.GOLDEN_APPLE.getDefaultInstance(), "Золотое яблоко", false));
        addSpooky(new ItemSetting(Items.ENCHANTED_GOLDEN_APPLE.getDefaultInstance(), "Зачарованное золотое яблоко", false));
        addSpooky(new ItemSetting(Items.GOLDEN_CARROT.getDefaultInstance(), "Золотая морковь", false));
        addSpooky(new ItemSetting(Items.DIAMOND.getDefaultInstance(), "Алмаз", false));
        addSpooky(new ItemSetting(Items.IRON_INGOT.getDefaultInstance(), "Железный слиток", false));
        addSpooky(new ItemSetting(Items.GOLD_INGOT.getDefaultInstance(), "Золотой слиток", false));
        addSpooky(new ItemSetting(Items.EMERALD.getDefaultInstance(), "Изумруд", false));
        addSpooky(new ItemSetting(Items.NETHERITE_INGOT.getDefaultInstance(), "Незеритовый слиток", false));
        addSpooky(new ItemSetting(Items.NETHERITE_SCRAP.getDefaultInstance(), "Незеритовый лом", false));
        addSpooky(new ItemSetting(Items.IRON_BLOCK.getDefaultInstance(), "Железный блок", false));
        addSpooky(new ItemSetting(Items.GOLD_BLOCK.getDefaultInstance(), "Золотой блок", false));
        addSpooky(new ItemSetting(Items.DIAMOND_BLOCK.getDefaultInstance(), "Алмазный блок", false));
        addSpooky(new ItemSetting(Items.EMERALD_BLOCK.getDefaultInstance(), "Изумрудный блок", false));
        addSpooky(new ItemSetting(Items.ANCIENT_DEBRIS.getDefaultInstance(), "Древние обломки", false));
        addSpooky(new ItemSetting(Items.BEACON.getDefaultInstance(), "Маяк (Обычный)", false));
        addSpooky(new ItemSetting(Items.ENDER_PEARL.getDefaultInstance(), "Эндер-жемчуг", false));
        addSpooky(new ItemSetting(Items.FIREWORK_ROCKET.getDefaultInstance(), "Фейерверк", false));
        addSpooky(new ItemSetting(Items.CHORUS_FRUIT.getDefaultInstance(), "Хорус", false));
        addSpooky(new ItemSetting(Items.STRUCTURE_BLOCK.getDefaultInstance(), "Прогрузчик чанков [1x1]", false)
                .setSearchQuery("Прогрузчик чанков 1x1"));

        addSpooky(new ItemSetting(Items.STRUCTURE_BLOCK.getDefaultInstance(), "Прогрузчик чанков [3x3]", false)
                .setSearchQuery("Прогрузчик чанков 3x3"));

        addSpooky(new ItemSetting(Items.STRUCTURE_BLOCK.getDefaultInstance(), "Прогрузчик чанков [5x5]", false)
                .setSearchQuery("Прогрузчик чанков 5x5"));
        addSpooky(new ItemSetting(Items.JIGSAW.getDefaultInstance(), "Блок дамагер", false).addNBTparametr("● Каст: Нанесение урона").addNBTparametr("● Радиус: 1,5 блока"));
        addSpooky(new ItemSetting(Items.SOUL_LANTERN.getDefaultInstance(), "Проклятая душа", false).addNBTparametr("Обменяй души на ценные").addNBTparametr("ресурсы у Собирателя душ").addNBTparametr("/warp soulcollector"));
        addSpooky(new ItemSetting(Items.PAPER.getDefaultInstance(), "Драконий скин", false).addNBTparametr("Используя этот предмет").addNBTparametr("Вы его расходуете").addNBTparametr("и получаете Драконий скин взамен").addNBTparametr("[ПКМ] чтобы использовать x1 скин").addNBTparametr("[SHIFT+ПКМ] чтобы использовать все скины").addNBTparametr("Предмет нужно держать в руке"));

        addSpooky(new ItemSetting(Items.SHULKER_BOX.getDefaultInstance(), "Шалкеровый ящик", false));
        addSpooky(new ItemSetting(Items.DRAGON_EGG.getDefaultInstance(), "Яйцо дракона", false));
        //холик
        addHoly(new ItemSetting(Items.EMERALD_ORE.getDefaultInstance(), "Изумрудная руда", false));
        addHoly(new ItemSetting(Items.GOLDEN_APPLE.getDefaultInstance(), "Золотое яблоко", false));
        addHoly(new ItemSetting(Items.PLAYER_HEAD.getDefaultInstance(), "Сфера Цербера", false).addNBTparametr("Сфера Цербера").addNBTparametr("Спешка I").addNBTparametr("Урон V"));
        addHoly(new ItemSetting(Items.NETHERITE_SWORD.getDefaultInstance(), "меч eternity", false).addEnchantment("sharpness", 7)  .addNBTparametr("ᴇᴛᴇʀɴɪᴛʏ") .addNBTparametr("Богач"));
        addHoly(new ItemSetting(Items.NETHERITE_PICKAXE.getDefaultInstance(), "Кирка eternity", false).addNBTparametr("Кирка ᴇᴛᴇʀɴɪᴛʏ").addEnchantment("efficiency", 10).addNBTparametr("Бур II"));
        addHoly(new ItemSetting(Items.GOLDEN_HELMET.getDefaultInstance(), "Шлем солнца", false).addNBTparametr("Шлем солнца").addNBTparametr("Непробиваемый II").addEnchantment("protection", 5).addEnchantment("blast_protection", 5).addEnchantment("projectile_protection", 5));

        addHoly(new ItemSetting(Items.NETHERITE_CHESTPLATE.getDefaultInstance(), "Нагрудник eternity", false)
                .addNBTparametr("Непробиваемый I")
                .addEnchantment("protection", 5)
                .addNBTparametr("ᴇᴛᴇʀɴɪᴛʏ")
                .addEnchantment("unbreaking", 4));

        addHoly(new ItemSetting(Items.NETHERITE_LEGGINGS.getDefaultInstance(), "Поножи eternity", false)
                .addNBTparametr("Непробиваемый I")
                .addEnchantment("protection", 5)
                .addNBTparametr("ᴇᴛᴇʀɴɪᴛʏ")
                .addEnchantment("unbreaking", 4));

        addHoly(new ItemSetting(Items.NETHERITE_BOOTS.getDefaultInstance(), "Ботинки eternity", false)
                .addNBTparametr("Непробиваемый I")
                .addEnchantment("protection", 5)
                .addNBTparametr("ᴇᴛᴇʀɴɪᴛʏ")
                .addEnchantment("unbreaking", 4));
        ItemSetting mythSphere = new ItemSetting(Items.PLAYER_HEAD.getDefaultInstance(), "сфера на урон 3", false)
                .addNBTparametr("Урон III")
                .addNBTparametr("Броня II");
        mythSphere.setSearchQuery("Мифическая сфера");
        setSkin(mythSphere, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmFmZjJlYjQ5OGU1YzZhMDQ0ODRmMGM5Zjc4NWI0NDg0NzlhYjIxM2RmOTVlYzkxMTc2YTMwOGExMmFkZDcwIn19fQ==");
        addHoly(mythSphere);

        ItemSetting eternitysphere = new ItemSetting(Items.PLAYER_HEAD.getDefaultInstance(), "Урон III + Броня II", false)
                .addNBTparametr("ᴇᴛᴇʀɴɪᴛʏ").addNBTparametr("Скорость II").addNBTparametr("Броня II").addNBTparametr("Урон II");
        mythSphere.setSearchQuery("Сфера ᴇᴛᴇʀɴɪᴛʏ");
        setSkin(eternitysphere, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGM5MzY1NjQyYzZlZGRjZmVkZjViNWUxNGUyYmM3MTI1N2Q5ZTRhMzM2M2QxMjNjNmYzM2M1NWNhZmJmNmQifX19==");
        addHoly(eternitysphere);
        ItemSetting armortalitySphere = new ItemSetting(Items.PLAYER_HEAD.getDefaultInstance(), "Сфера ARMORTALITY", false).addNBTparametr("Броня II").addNBTparametr("Урон II").addNBTparametr("Макс. здоровье II");setSkin(armortalitySphere, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWE2MmI5ZGU2YTI2Yjg2ODY5Y2EyMmVhNDBmMWJkZTgwYTA0MzBhNTQ1NDdiZWNjZThmZGE4NzA3Nzc3MjU4ZiJ9fX0=");addHoly(armortalitySphere);
        ItemSetting immortalitySphere = new ItemSetting(Items.PLAYER_HEAD.getDefaultInstance(), "Сфера IMMORTALITY", false).addNBTparametr("Урон III").addNBTparametr("Скорость II");
        setSkin(immortalitySphere, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODNlZDRjZTIzOTMzZTY2ZTA0ZGYxNjA3MDY0NGY3NTk5ZWViNTUzMDdmN2VhZmU4ZDkyZjQwZmIzNTIwODYzYyJ9fX0=");addHoly(immortalitySphere);
        ItemSetting mythSphereSpeed = new ItemSetting(Items.PLAYER_HEAD.getDefaultInstance(), "Скор II + Броня III", false).addNBTparametr("Мифическая сфера").addNBTparametr("Скорость II").addNBTparametr("Броня III");
        setSkin(mythSphereSpeed, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmFmZjJlYjQ5OGU1YzZhMDQ0ODRmMGM5Zjc4NWI0NDg0NzlhYjIxM2RmOTVlYzkxMTc2YTMwOGExMmFkZDcwIn19fQ==");addHoly(mythSphereSpeed);
        ItemSetting mythSphereDmgArmor = new ItemSetting(Items.PLAYER_HEAD.getDefaultInstance(), "Урон II + Броня III", false).addNBTparametr("Мифическая сфера").addNBTparametr("Урон II").addNBTparametr("Броня III");
        setSkin(mythSphereDmgArmor, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmFmZjJlYjQ5OGU1YzZhMDQ0ODRmMGM5Zjc4NWI0NDg0NzlhYjIxM2RmOTVlYzkxMTc2YTMwOGExMmFkZDcwIn19fQ==");addHoly(mythSphereDmgArmor);
        addHoly(new ItemSetting(Items.TOTEM_OF_UNDYING.getDefaultInstance(), "Талисман eternity", false)  .addNBTparametr("ᴇᴛᴇʀɴɪᴛʏ").addNBTparametr("Скорость II").addNBTparametr("Броня II").addNBTparametr("Урон II"));
        addHoly(new ItemSetting(Items.TOTEM_OF_UNDYING.getDefaultInstance(), "Талисман STINGER", false).addNBTparametr("Талисман STINGER").addNBTparametr("Скорость I").addNBTparametr("Броня II").addNBTparametr("Урон II"));
        ItemSetting stingerSphere = new ItemSetting(Items.PLAYER_HEAD.getDefaultInstance(), "Сфера STINGER", false).addNBTparametr("ᴇᴛᴇʀɴɪᴛʏ").addNBTparametr("Скорость I").addNBTparametr("Урон II").addNBTparametr("Броня II");
        setSkin(stingerSphere, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGM5MzY1NjQyYzZlZGRjZmVkZjViNWUxNGUyYmM3MTI1N2Q5ZTRhMzM2M2QxMjNjNmYzM2M1NWNhZmJmNmQifX19");addHoly(stingerSphere);
        addHoly(new ItemSetting(Items.ORANGE_DYE.getDefaultInstance(), "Руна Бессмертие", false).addNBTparametr("Бессмертие").addNBTparametr("после активации тотема"));
    }

    private static void addSpooky(ItemSetting setting) {
        System.out.println("Добавлен предмет в Spooky тайм: " + setting.getName());
        SPOOKY_ITEMS.add(setting);
    }

    private static void addHoly(ItemSetting setting) {
        HOLY_ITEMS.add(setting);
    }
    public static String getSearchNameForItem(ItemSetting item) {
        if (item == null) return null;

        if (AutoBuy.instance != null && !AutoBuy.instance.mode.is("Холиворлд")) {
            String name = item.getName();

            if (name.contains("Прогрузчик чанков")) {
                if (name.contains("[1x1]")) return "Прогрузчик чанков 1x1";
                if (name.contains("[3x3]")) return "Прогрузчик чанков 3x3";
                if (name.contains("[5x5]")) return "Прогрузчик чанков 5x5";
            }
        }
        return item.getSearchQuery() != null ? item.getSearchQuery() : item.getName();
    }
    private static void setSkin(ItemSetting setting, String value) {
        CompoundNBT owner = new CompoundNBT();
        value = value.replaceAll("\\s+", "");

        if (value.length() > 20) {
            owner.putUniqueId("Id", UUID.randomUUID());
            CompoundNBT props = new CompoundNBT();
            ListNBT textures = new ListNBT();
            CompoundNBT tex = new CompoundNBT();
            tex.putString("Value", value);
            textures.add(tex);
            props.put("textures", textures);
            owner.put("Properties", props);
        } else {
            owner.putUniqueId("Id", UUID.randomUUID());
            owner.putString("Name", value);
        }
        setting.getItemStack().getOrCreateTag().put("SkullOwner", owner);
    }

    public static List<ItemSetting> getItems() {
        if (AutoBuy.instance != null && AutoBuy.instance.mode.is("Холиворлд")) {
            return Collections.unmodifiableList(HOLY_ITEMS);
        }
        return Collections.unmodifiableList(SPOOKY_ITEMS);
    }

    public static ItemSetting getByName(String name) {
        if (name == null) return null;
        for (ItemSetting item : getItems()) {
            if (item.getName().equalsIgnoreCase(name)) return item;
        }
        return null;
    }
}