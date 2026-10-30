package dev.bedrockify.forge.client.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/** Ordered group headers and expansion, independent of rendering and Minecraft item APIs. */
public final class GroupedList {
    public record Entry<T>(T value, String group, int count) {
        public boolean header() { return group != null; }
    }
    private record Classified<T>(T value, String group) {}
    private GroupedList() {}

    /** Singletons stay in source order. priority only moves groups with at least two members. */
    public static <T> List<Entry<T>> layout(List<T> source, Function<? super T, String> family,
            Set<String> expanded, boolean collapse, List<String> priority,
            BiFunction<String, List<T>, T> representative) {
        if (!collapse) return source.stream().map(value -> new Entry<>(value, null, 1)).toList();
        List<Classified<T>> classified = new ArrayList<>(source.size());
        Map<String, List<T>> families = new LinkedHashMap<>();
        for (T value : source) {
            String group = family.apply(value);
            classified.add(new Classified<>(value, group));
            if (group != null) families.computeIfAbsent(group, ignored -> new ArrayList<>()).add(value);
        }
        List<Entry<T>> result = new ArrayList<>();
        Set<String> added = new HashSet<>();
        Consumer<String> append = group -> {
            List<T> members = families.get(group);
            if (members == null || members.size() < 2 || !added.add(group)) return;
            result.add(new Entry<>(representative.apply(group, List.copyOf(members)), group, members.size()));
            if (expanded.contains(group)) for (T member : members) result.add(new Entry<>(member, null, 1));
        };
        priority.forEach(append);
        for (Classified<T> member : classified) {
            if (member.group() == null || families.get(member.group()).size() < 2)
                result.add(new Entry<>(member.value(), null, 1));
            else append.accept(member.group());
        }
        return List.copyOf(result);
    }
}
