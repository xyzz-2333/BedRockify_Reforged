import dev.bedrockify.forge.client.gui.GroupedList;
import java.util.List;
import java.util.Set;

/** Standalone Java 17 regression for list semantics shared by both inventory views. */
public final class GroupedListTest {
    private record Value(String id, String family, boolean craftable) {}
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static List<String> describe(List<GroupedList.Entry<Value>> rows) {
        return rows.stream().map(row -> (row.header() ? row.group() : "-") + ":" + row.value().id() + ":" + row.count()).toList();
    }
    public static void main(String[] args) {
        var source = List.of(new Value("a1", "alpha", false), new Value("loose", null, false),
                new Value("b1", "beta", false), new Value("a2", "alpha", true),
                new Value("single", "singleton", false), new Value("b2", "beta", true));
        java.util.function.BiFunction<String, List<Value>, Value> representative = (group, members) ->
                members.stream().filter(Value::craftable).findFirst().orElse(members.get(0));
        var collapsed = GroupedList.layout(source, Value::family, Set.of(), true, List.of(), representative);
        check(describe(collapsed).equals(List.of("alpha:a2:2", "-:loose:1", "beta:b2:2", "-:single:1")),
                "Interleaved families collapse once, choose the craftable representative and retain unrelated values");
        var expanded = GroupedList.layout(source, Value::family, Set.of("alpha", "stale"), true, List.of(), representative);
        check(describe(expanded).equals(List.of("alpha:a2:2", "-:a1:1", "-:a2:1", "-:loose:1", "beta:b2:2", "-:single:1")),
                "Expansion retains member order and both the representative header and its original member");
        var priority = GroupedList.layout(source, Value::family, Set.of(), true,
                List.of("missing", "singleton", "beta", "beta", "alpha"), representative);
        check(describe(priority).equals(List.of("beta:b2:2", "alpha:a2:2", "-:loose:1", "-:single:1")),
                "Priority affects only real groups; duplicate priorities do not duplicate headers");
        var flat = GroupedList.layout(source, value -> { throw new AssertionError("Flat lists need no classification"); },
                Set.of("alpha"), false, List.of("beta"), representative);
        check(flat.stream().map(GroupedList.Entry::value).toList().equals(source)
                && flat.stream().noneMatch(GroupedList.Entry::header), "Disabling grouping preserves the complete source order");
        check(source.size() == 6 && source.get(0).id().equals("a1"), "Grouping never changes the source list");
        check(GroupedList.<Value>layout(List.of(), Value::family, Set.of(), true, List.of("alpha"), representative).isEmpty(),
                "Stale group state cannot create rows in an empty list");
        System.out.println("GroupedList regression PASS");
    }
}
