package com.fantasyraces;

import java.util.List;
import java.util.Set;

public final class Defs {
    private Defs() {}

    public record RaceDef(String id, String name, String lore, String pros, String cons, Fx fx, Set<Trait> traits) {
        public boolean has(Trait t) { return traits.contains(t); }
    }

    public record Node(String id, String name, int cost, List<String> parents, Fx fx, int slot,
                       double col, int row, String desc) {}

    public record ClassDef(String id, String race, String name, String desc, String kit,
                           String[] acts, List<Node> nodes) {}
}
