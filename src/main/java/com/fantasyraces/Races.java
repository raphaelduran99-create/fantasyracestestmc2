package com.fantasyraces;

import com.fantasyraces.Defs.*;

import java.util.*;

public final class Races {
    public static final List<RaceDef> RACES = new ArrayList<>();
    public static final List<ClassDef> CLASSES = new ArrayList<>();
    private static final Map<String, RaceDef> RM = new HashMap<>();
    private static final Map<String, ClassDef> CM = new HashMap<>();

    public static RaceDef race(String id) { return RM.get(id); }
    public static ClassDef cls(String id) { return CM.get(id); }
    public static List<ClassDef> classesOf(String race) {
        return CLASSES.stream().filter(c -> c.race().equals(race)).toList();
    }

    private static void r(String id, String name, String lore, String pros, String cons, String fx, Trait... t) {
        Set<Trait> set = t.length == 0 ? EnumSet.noneOf(Trait.class) : EnumSet.copyOf(Arrays.asList(t));
        RaceDef d = new RaceDef(id, name, lore, pros, cons, Fx.parse(fx), set);
        RACES.add(d); RM.put(id, d);
    }

    private static String act(String id) {
        if (!Actives.ALL.containsKey(id)) { FantasyRaces.LOGGER.warn("Compétence inconnue {}", id); return "dash"; }
        return id;
    }

    private static void c(String id, String race, String name, String desc, String kit,
                          String a, String b, String cap, String x1, String x2) {
        x1 = act(x1); x2 = act(x2);
        String[] A = a.split("\\|"), B = b.split("\\|"), C = cap.split("\\|");
        Fx fa = Fx.parse(A[1]).scaled(0.5), fb = Fx.parse(B[1]).scaled(0.5), fc = Fx.parse(C[1]);
        List<Node> n = List.of(
                new Node(id + ".a1", A[0] + " I", 1, List.of(), fa, -1, 0, 0, "Passif"),
                new Node(id + ".b1", B[0] + " I", 1, List.of(), fb, -1, 3, 0, "Passif"),
                new Node(id + ".a2", A[0] + " II", 2, List.of(id + ".a1"), fa, -1, 0, 1, "Passif"),
                new Node(id + ".act1", "★ " + Actives.ALL.get(x1).name(), 2, List.of(id + ".a1"), new Fx(), 0, 1, 1, Actives.ALL.get(x1).desc()),
                new Node(id + ".act2", "★ " + Actives.ALL.get(x2).name(), 2, List.of(id + ".b1"), new Fx(), 1, 2, 1, Actives.ALL.get(x2).desc()),
                new Node(id + ".b2", B[0] + " II", 2, List.of(id + ".b1"), fb, -1, 3, 1, "Passif"),
                new Node(id + ".cap", C[0], 3, List.of(id + ".a2", id + ".b2"), fc, -1, 1.5, 2, "Maîtrise"));
        ClassDef d = new ClassDef(id, race, name, desc, kit, new String[]{x1, x2}, n);
        CLASSES.add(d); CM.put(id, d);
    }

    static {
        // ===== RACES =====
        r("human", "Humain", "Adaptable et ambitieux.", "Apprend vite (+25% de points de compétence), chanceux.", "Aucun don exotique.",
                "luck=1;learn=0.25");
        r("elf", "Elfe", "Gracieux enfant des forêts.", "Rapide, saute haut, tir à distance plus précis.",
                "Fragile (-1 cœur) et strictement végétarien : aucune viande ni poisson.",
                "speed=0.15;jump=0.25;rvel=0.1;health=-2", Trait.VEGETARIAN);
        r("dwarf", "Nain", "Maître des montagnes, 1 bloc de haut.", "Mine vite, robuste, voit dans les profondeurs.",
                "Aveuglé par le soleil en surface (un casque protège).",
                "scale=-0.45;mine=0.3;armor=2;kb=0.2;health=2", Trait.SUN_BLIND, Trait.UNDERGROUND_SIGHT);
        r("fairy", "Fée", "Minuscule et lumineuse.", "Petite, rapide, chute lentement, magie renforcée.",
                "Très fragile (-3 cœurs), le fer lui fait +50% de dégâts.",
                "scale=-0.5;health=-6;speed=0.1;power=0.15", Trait.SLOW_FALL, Trait.IRON_WEAK);
        r("dragonkin", "Demi-dragon", "Du sang de dragon coule en lui.", "Immunisé au feu, nage dans la lave, écailles (+armure).",
                "L'eau le brûle (1 dégât / 3 s).",
                "armor=3;atk=1.5;health=4;kb=0.2", Trait.LAVA_SWIM, Trait.WATER_HURT);
        r("orc", "Orc", "Brute des plaines.", "Force, vie et résistance au recul.",
                "Plus lent (-5%), compétences rechargées 15% plus lentement.",
                "atk=2;health=4;kb=0.3;speed=-0.05;cdr=-0.15");
        r("goblin", "Gobelin", "Petit, vif et chanceux.", "Petit, rapide, très chanceux, voit dans le noir.",
                "Fragile (-2 cœurs), frappe moins fort.",
                "scale=-0.3;speed=0.12;luck=2;health=-4;atk=-0.5", Trait.DARK_SIGHT);
        r("halfling", "Halfelin", "Petit gourmand discret.", "Petit, chanceux, chutes amorties.",
                "Mange beaucoup : la faim monte plus vite.",
                "scale=-0.3;luck=1;safefall=3;speed=0.05", Trait.HUNGER_DRAIN);
        r("tiefling", "Tieffelin", "Héritage infernal.", "Résiste au feu (-50%), voit dans le noir, magie +20%.",
                "Moins de vie (-1 cœur) et +25% de dégâts magiques subis.",
                "health=-2;atk=1;power=0.2", Trait.FIRE_HALF, Trait.DARK_SIGHT, Trait.MAGIC_WEAK);
        r("gnome", "Gnome", "Minuscule inventeur.", "Très petit, recharges -10%, magie +15%, chanceux.",
                "Allonge réduite (-0.5) et peu de vie (-1 cœur).",
                "scale=-0.4;luck=1;power=0.15;cdr=0.1;reach=-0.5;health=-2");
        r("triton", "Triton", "Peuple des océans.", "Respire sous l'eau, nage très vite, mine bien sous l'eau.",
                "Lent sur terre (-8%).",
                "swim=1;oxygen=3;subm=0.5;speed=-0.08;health=2", Trait.WATER_BREATH);
        r("undead", "Mort-vivant", "Revenu d'entre les morts.", "Les morts-vivants ne t'attaquent pas, immunisé au poison, -10% dégâts.",
                "Brûle au soleil sans casque.",
                "dmgred=0.1;tough=1", Trait.UNDEAD_PEACE, Trait.NO_POISON, Trait.SUN_BURN);
        r("lycan", "Loup-garou", "Chasseur de la pleine lune.", "Rapide, griffes, force et vitesse la nuit.",
                "Carnivore : ne mange que de la viande. L'or lui fait +50% de dégâts.",
                "speed=0.1;atk=1;jump=0.15", Trait.CARNIVORE, Trait.NIGHT_POWER, Trait.GOLD_WEAK);
        r("dryad", "Dryade", "Esprit de la forêt.", "Beaucoup de vie, se nourrit au soleil, magie +15%.",
                "Lente (-8%), le feu lui fait +50% de dégâts.",
                "health=6;speed=-0.08;power=0.15", Trait.SUN_FEED, Trait.FIRE_WEAK);
        r("goliath", "Géant", "Colosse des cimes.", "Grand, très fort, longue allonge, beaucoup de vie.",
                "Lent (-5%) et affamé en permanence.",
                "scale=0.2;health=8;atk=2;reach=1;step=0.4;kb=0.3;speed=-0.05", Trait.HUNGER_DRAIN);

        // ===== CLASSES (id, race, nom, desc, kit, passifA, passifB, maîtrise, skill1, skill2) =====
        c("human_knight", "human", "Chevalier", "Combattant en armure, protège ses alliés.",
                "iron_sword:1,shield:1,iron_chestplate:1,bread:8",
                "Armure de plates|armor=2;tough=1", "Bravoure|atk=1;kb=0.2", "Paladin|dmgred=0.08;health=2", "shield", "roar");
        c("human_ranger", "human", "Ranger", "Archer polyvalent et pisteur.",
                "bow:1,arrow:48,stone_sword:1,leather_boots:1,cooked_beef:8",
                "Tireur|rdmg=0.12;rspeed=0.3", "Pisteur|speed=0.06;luck=1", "Œil de faucon|rvel=0.2;rdmg=0.1", "volley", "reveal");
        c("human_cleric", "human", "Clerc", "Soigneur béni.",
                "stone_sword:1,golden_apple:2,bread:8,shield:1",
                "Foi|power=0.2;health=2", "Dévotion|dmgred=0.06;luck=1", "Bénédiction|power=0.15;cdr=0.15", "heal", "cleanse");

        c("elf_archer", "elf", "Archer sylvestre", "Bande vite, tire loin et fort.",
                "bow:1,arrow:64,leather_boots:1,apple:8,bread:8",
                "Corde tendue|rspeed=0.4;rdmg=0.1", "Flèche lointaine|rvel=0.25;rdmg=0.12", "Tir parfait|rdmg=0.15;rspeed=0.2;rvel=0.2", "volley", "sprite");
        c("elf_druid", "elf", "Druide", "Guérisseur de la nature.",
                "stone_hoe:1,sweet_berries:16,golden_carrot:6,bone_meal:16,bread:8",
                "Régénération|health=4;power=0.15", "Sève vitale|dmgred=0.06;luck=1", "Gardien de la forêt|power=0.2;cdr=0.1", "heal", "entangle");
        c("elf_bladedancer", "elf", "Danseur de lames", "Duelliste aérien et gracieux.",
                "iron_sword:1,leather_boots:1,golden_carrot:8,bread:8",
                "Grâce|speed=0.1;atkspeed=0.2", "Esquive|jump=0.15;dmgred=0.06", "Tempête de lames|atk=1;atkspeed=0.2", "dash", "frenzy");

        c("dwarf_warrior", "dwarf", "Guerrier de la montagne", "Mur de fer à la hache.",
                "iron_axe:1,shield:1,iron_helmet:1,iron_chestplate:1,cooked_beef:8",
                "Peau de granit|armor=2;tough=1", "Hache lourde|atk=1;kb=0.2", "Inébranlable|dmgred=0.1;health=4", "stoneskin", "stomp");
        c("dwarf_miner", "dwarf", "Mineur", "Roi des galeries.",
                "iron_pickaxe:1,iron_shovel:1,torch:32,bread:8,lantern:1",
                "Veine|mine=0.3", "Œil du mineur|luck=1;speed=0.06", "Maître mineur|mine=0.3;luck=1", "reveal", "dash");
        c("dwarf_runesmith", "dwarf", "Forgerune", "Grave des runes de pouvoir.",
                "stone_sword:1,lapis_lazuli:16,iron_ingot:8,experience_bottle:6,bread:8",
                "Runes de force|power=0.25;atk=1", "Trempe|armor=1;tough=1", "Grand Forgeron|power=0.2;cdr=0.15", "bolt", "shield");

        c("fairy_mage", "fairy", "Enchanteresse", "Magie arcanique et téléportation.",
                "amethyst_shard:16,honey_bottle:4,sweet_berries:8,stone_sword:1",
                "Poussière de fée|power=0.3", "Ailes vives|speed=0.1;cdr=0.1", "Archimage|power=0.25;cdr=0.1", "bolt", "blink");
        c("fairy_healer", "fairy", "Fée guérisseuse", "Soutien lumineux.",
                "honey_bottle:6,golden_apple:1,glow_berries:16,golden_carrot:8",
                "Lueur apaisante|power=0.3;health=2", "Aura|dmgred=0.06;luck=1", "Source de vie|power=0.2;health=2;cdr=0.1", "heal", "cleanse");
        c("fairy_trickster", "fairy", "Farfadet", "Illusions et coups bas.",
                "stone_sword:1,ender_pearl:2,bread:8,golden_carrot:4",
                "Malice|atk=1;luck=2", "Vif-argent|speed=0.1;jump=0.2", "Maître illusionniste|atk=0.5;speed=0.1", "smoke", "blink");

        c("dragon_dragoon", "dragonkin", "Chevalier-dragon", "Guerrier aux écailles et souffle de feu.",
                "iron_sword:1,shield:1,iron_helmet:1,cooked_beef:8",
                "Écailles|armor=2;tough=1", "Griffes|atk=1;atkspeed=0.1", "Sang de dragon|dmgred=0.1;atk=1", "firebreath", "stomp");
        c("dragon_pyro", "dragonkin", "Pyromancien", "Maître des flammes (sans incendie).",
                "stone_sword:1,blaze_powder:8,coal:16,cooked_porkchop:8",
                "Flamme intérieure|power=0.3", "Chaleur|speed=0.05;health=2", "Cœur de brasier|power=0.25;cdr=0.15", "firebreath", "bolt");
        c("dragon_scout", "dragonkin", "Éclaireur ailé", "Archer qui bondit et plane.",
                "bow:1,arrow:32,leather_boots:1,cooked_chicken:8",
                "Ailes atrophiées|jump=0.3;safefall=4", "Tir de feu|rdmg=0.15;rvel=0.15", "Vol plané|safefall=6;speed=0.1", "leap", "volley");

        c("orc_berserker", "orc", "Berserker", "Fureur au corps-à-corps.",
                "iron_axe:1,cooked_beef:12,leather_chestplate:1",
                "Rage|atk=1.5;atkspeed=0.1", "Peau épaisse|health=4;kb=0.2", "Fureur sanglante|lifesteal=0.08;atk=1", "frenzy", "dash");
        c("orc_warchief", "orc", "Chef de guerre", "Commande et encaisse.",
                "iron_sword:1,shield:1,iron_chestplate:1,cooked_porkchop:12",
                "Commandement|power=0.1;dmgred=0.06", "Poigne|atk=1;armor=1", "Seigneur de guerre|dmgred=0.08;atk=1", "roar", "shield");
        c("orc_shaman", "orc", "Chaman", "Esprits ancestraux.",
                "stone_sword:1,bone:8,cooked_mutton:12,golden_apple:1",
                "Esprits|power=0.3;health=2", "Totem|dmgred=0.06;lifesteal=0.04", "Voix des ancêtres|power=0.2;cdr=0.15", "drain", "heal");

        c("goblin_tinker", "goblin", "Bricoleur", "Arbalète bricolée.",
                "crossbow:1,arrow:32,redstone:16,cooked_chicken:8,stone_sword:1",
                "Arbalète bricolée|rdmg=0.2;rspeed=0.25", "Bidouille|cdr=0.1;luck=1", "Ingénieur fou|rdmg=0.2;cdr=0.1", "volley", "dash");
        c("goblin_thief", "goblin", "Voleur", "Lame sournoise, pieds légers.",
                "stone_sword:1,leather_boots:1,ender_pearl:1,cooked_porkchop:8",
                "Lame sournoise|atk=1.5;atkspeed=0.1", "Pieds légers|speed=0.1;jump=0.15", "Ombre|atk=1;speed=0.1", "smoke", "dash");
        c("goblin_hexer", "goblin", "Sorcier gobelin", "Malédictions et ponction de vie.",
                "stone_sword:1,spider_eye:8,rotten_flesh:8,cooked_chicken:8",
                "Malédiction|power=0.3", "Chance maudite|luck=2;speed=0.05", "Hexe|power=0.2;lifesteal=0.05", "drain", "entangle");

        c("halfling_slinger", "halfling", "Frondeur", "Tireur d'élite discret.",
                "bow:1,arrow:40,leather_boots:1,bread:16,cooked_chicken:8",
                "Œil vif|rdmg=0.18;rspeed=0.2", "Pieds de hobbit|speed=0.1;safefall=3", "Tireur d'élite|rdmg=0.2;rvel=0.2", "volley", "smoke");
        c("halfling_burglar", "halfling", "Cambrioleur", "Invisible et insaisissable.",
                "stone_sword:1,leather_boots:1,pumpkin_pie:4,bread:12,ender_pearl:1",
                "Discrétion|speed=0.1;luck=1", "Doigts agiles|atk=1;atkspeed=0.1", "Roi des ombres|atk=1;luck=1", "smoke", "dash");
        c("halfling_herbalist", "halfling", "Herboriste", "Cuisine et soigne.",
                "stone_sword:1,bread:16,pumpkin_pie:8,golden_carrot:8,honey_bottle:4",
                "Cuisine revigorante|health=4;power=0.2", "Bon appétit|dmgred=0.06;luck=1", "Festin|power=0.2;health=2", "heal", "cleanse");

        c("tiefling_warlock", "tiefling", "Occultiste", "Pacte et magie sombre.",
                "stone_sword:1,amethyst_shard:16,cooked_beef:8,experience_bottle:6",
                "Pacte|power=0.35", "Sang maudit|lifesteal=0.06;atk=1", "Seigneur du pacte|power=0.25;lifesteal=0.05", "drain", "bolt");
        c("tiefling_infernal", "tiefling", "Lame infernale", "Duelliste enflammé.",
                "iron_sword:1,chainmail_chestplate:1,cooked_beef:8",
                "Peau braisée|armor=1;tough=1;kb=0.1", "Fer rouge|atk=1;atkspeed=0.1", "Maître d'armes|atk=1;dmgred=0.08", "dash", "frenzy");
        c("tiefling_bard", "tiefling", "Bateleur diabolique", "Charme, illusions, esquives.",
                "stone_sword:1,ender_pearl:2,cooked_porkchop:8,golden_carrot:4",
                "Charisme|luck=2;power=0.15", "Pirouette|speed=0.08;jump=0.1", "Illusionniste|power=0.15;cdr=0.15", "blink", "smoke");

        c("gnome_artificer", "gnome", "Artificier", "Mécanismes et arbalète (sans explosif).",
                "crossbow:1,arrow:32,redstone:16,iron_ingot:8,bread:8",
                "Mécanismes|rdmg=0.2;cdr=0.1", "Ingéniosité|luck=1;power=0.2", "Génie|cdr=0.15;rdmg=0.15", "volley", "bolt");
        c("gnome_illusionist", "gnome", "Illusionniste", "Mirages et téléportation.",
                "stone_sword:1,amethyst_shard:16,ender_pearl:2,bread:8",
                "Mirages|power=0.3", "Esquive|speed=0.08;dmgred=0.06", "Maître des mirages|power=0.2;cdr=0.15", "smoke", "blink");
        c("gnome_tunneler", "gnome", "Foreur", "Creuse et repère.",
                "iron_pickaxe:1,iron_shovel:1,torch:32,bread:8,stone_sword:1",
                "Foreuse|mine=0.4", "Œil de taupe|luck=1;breach=1", "Maître foreur|mine=0.3;luck=1", "reveal", "dash");

        c("triton_lancer", "triton", "Lancier des vagues", "Trident et nage rapide.",
                "trident:1,turtle_helmet:1,cooked_cod:12,water_bucket:1",
                "Lance marine|atk=1;swim=0.3", "Écailles|armor=1;oxygen=2", "Roi des mers|atk=1;subm=0.5", "surge", "dash");
        c("triton_tidemage", "triton", "Mage des marées", "Magie de l'eau et soins.",
                "stone_sword:1,prismarine_shard:16,cooked_salmon:12,water_bucket:1",
                "Marée|power=0.3", "Source|health=2;oxygen=2", "Onde vitale|power=0.2;cdr=0.15", "heal", "surge");
        c("triton_hunter", "triton", "Chasseur d'abysses", "Arbalète et repérage sous-marin.",
                "crossbow:1,arrow:32,fishing_rod:1,cooked_cod:8,turtle_helmet:1",
                "Harpon|rdmg=0.2;rvel=0.15", "Nageur|swim=0.5;speed=0.04", "Terreur abyssale|rdmg=0.2;subm=0.5", "volley", "reveal");

        c("undead_necro", "undead", "Nécromancien", "Siphonne la vie.",
                "stone_sword:1,bone:16,rotten_flesh:8,experience_bottle:6",
                "Magie noire|power=0.3", "Siphon d'âme|lifesteal=0.07", "Seigneur des morts|power=0.25;lifesteal=0.04", "drain", "roar");
        c("undead_deathknight", "undead", "Chevalier de la mort", "Armure maudite, lame glacée.",
                "iron_sword:1,iron_chestplate:1,shield:1,rotten_flesh:8,cooked_beef:6",
                "Armure maudite|armor=2;tough=1", "Lame glacée|atk=1;lifesteal=0.04", "Fléau|atk=1;dmgred=0.08", "stomp", "shield");
        c("undead_bonearcher", "undead", "Archer squelette", "Mort silencieuse à distance.",
                "bow:1,arrow:64,bone:16,rotten_flesh:8",
                "Mort silencieuse|rdmg=0.2;rspeed=0.25", "Os creux|speed=0.06;jump=0.1", "Flèche maudite|rdmg=0.2;rvel=0.2", "volley", "smoke");

        c("lycan_stalker", "lycan", "Traqueur nocturne", "Chasseur à l'arc, repère les proies.",
                "stone_sword:1,bow:1,arrow:32,cooked_beef:16",
                "Traque|speed=0.06;rdmg=0.12;rvel=0.12", "Flair|luck=1;atk=1", "Alpha traqueur|rdmg=0.15;atk=1", "reveal", "leap");
        c("lycan_fury", "lycan", "Lycan furieux", "Déchaîné au corps-à-corps.",
                "iron_axe:1,cooked_mutton:16,leather_chestplate:1",
                "Crocs|atk=1.5;atkspeed=0.1", "Pelage|health=4;kb=0.2", "Bête alpha|lifesteal=0.07;atk=1", "frenzy", "leap");
        c("lycan_packleader", "lycan", "Chef de meute", "Soutient et rugit.",
                "iron_sword:1,shield:1,bone:16,cooked_beef:12",
                "Hurlement|power=0.2;dmgred=0.06", "Esprit de meute|health=4;luck=1", "Seigneur de meute|dmgred=0.08;power=0.15", "roar", "heal");

        c("dryad_warden", "dryad", "Gardien des bois", "Écorce épaisse, racines entravantes.",
                "stone_axe:1,shield:1,leather_chestplate:1,sweet_berries:16,bread:8",
                "Écorce|armor=2;tough=1", "Racines|kb=0.2;health=4", "Ent|dmgred=0.1;health=4", "stoneskin", "entangle");
        c("dryad_healer", "dryad", "Soigneuse de sève", "Soins de la nature.",
                "stone_sword:1,bone_meal:32,golden_carrot:8,honey_bottle:6",
                "Sève|power=0.3;health=2", "Photosynthèse|dmgred=0.06;luck=1", "Arbre-vie|power=0.2;cdr=0.15", "heal", "cleanse");
        c("dryad_thorn", "dryad", "Archère d'épines", "Pluie d'épines.",
                "bow:1,arrow:48,sweet_berries:16,bread:8",
                "Épines|rdmg=0.18;rspeed=0.25", "Camouflage|speed=0.06;luck=1", "Pluie d'épines|rdmg=0.2;rvel=0.2", "volley", "entangle");

        c("goliath_titan", "goliath", "Titan", "Force brute et séismes.",
                "iron_axe:1,iron_chestplate:1,shield:1,cooked_beef:16",
                "Muscles|atk=2;kb=0.2", "Carrure|health=6;armor=1", "Colosse|atk=1;dmgred=0.1", "stomp", "roar");
        c("goliath_thrower", "goliath", "Lanceur de rocs", "Projectiles puissants.",
                "crossbow:1,arrow:32,leather_chestplate:1,cooked_porkchop:16",
                "Bras puissant|rdmg=0.25;rvel=0.25", "Poids|health=4;kb=0.1", "Catapulte|rdmg=0.25;rvel=0.2", "volley", "leap");
        c("goliath_sage", "goliath", "Sage des géants", "Sagesse ancestrale.",
                "stone_sword:1,bone:8,golden_apple:1,cooked_mutton:12",
                "Sagesse|power=0.3;health=2", "Résistance|dmgred=0.06;armor=1", "Ancien|power=0.2;cdr=0.15", "heal", "shield");
    }

    private Races() {}
}
