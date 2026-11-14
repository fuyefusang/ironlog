import com.ironlog.train.data.Catalog;
import com.ironlog.train.data.Exercise;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Sanity checks for the built-in movement table: no duplicate ids, no missing muscles. */
public class CatalogTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        List<Exercise> all = Catalog.builtIn();

        check("library is big enough", all.size() >= 90, "size=" + all.size());

        Set<String> ids = new HashSet<>();
        Set<String> muscles = new HashSet<>();
        for (String m : Catalog.MUSCLES) {
            muscles.add(m);
        }

        Map<String, Integer> familyCount = new HashMap<>();
        for (Exercise e : all) {
            check("id present: " + e.name, e.id != null && !e.id.isEmpty(), "null id");
            check("id unique: " + e.id, ids.add(e.id), "duplicate id " + e.id);
            check("name present: " + e.id, e.name != null && !e.name.isEmpty(), "empty name");
            check("muscle valid: " + e.name, muscles.contains(e.muscle), "bad muscle " + e.muscle);
            check("equipment present: " + e.name, e.equipment != null && !e.equipment.isEmpty(),
                    "empty equipment");
            check("family present: " + e.name, e.family != null && !e.family.isEmpty(),
                    "empty family");
            check("primary muscle present: " + e.name,
                    e.primary != null && !e.primary.isEmpty(), "empty primary");
            Integer c = familyCount.get(e.family);
            familyCount.put(e.family, c == null ? 1 : c + 1);
        }

        // A family may legitimately span two muscle filters (硬拉 is listed under both
        // back and legs, 耸肩 under back and shoulders). The picker groups inside the
        // filtered list, so a family crossing two filters never mixes them in one view.
        Map<String, Set<String>> familyMuscles = new HashMap<>();
        for (Exercise e : all) {
            Set<String> ms = familyMuscles.get(e.family);
            if (ms == null) {
                ms = new HashSet<>();
                familyMuscles.put(e.family, ms);
            }
            ms.add(e.muscle);
        }
        for (Map.Entry<String, Set<String>> en : familyMuscles.entrySet()) {
            check("family spans at most two muscle filters: " + en.getKey(),
                    en.getValue().size() <= 2, "spans " + en.getValue());
        }

        int grouped = 0;
        for (int c : familyCount.values()) {
            if (c > 1) {
                grouped++;
            }
        }
        check("has equipment variants grouped", grouped >= 8, "grouped families=" + grouped);

        System.out.println();
        System.out.println("families=" + familyCount.size() + " grouped=" + grouped);
        System.out.println("passed=" + passed + " failed=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
        System.out.println("ALL CATALOG TESTS PASSED");
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + name + " -> " + detail);
        }
    }
}
