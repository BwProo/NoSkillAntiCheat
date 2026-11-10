package ac.grim.grimac.checks.impl.ai;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatModelTest {

    private static String model(String features, int window, String extraStd) {
        String zeros = String.join(",", Collections.nCopies(15, "0.0"));
        String ones = String.join(",", Collections.nCopies(15, "1.0"));
        return "{\"format\":1,\"type\":\"logistic_regression\",\"features\":" + features
                + ",\"window_ticks\":" + window + ",\"min_combat_ticks\":50"
                + ",\"mean\":[" + zeros + "],\"std\":[" + (extraStd == null ? ones : extraStd) + "]"
                + ",\"weights\":[" + "2.0," + String.join(",", Collections.nCopies(14, "0.0")) + "]"
                + ",\"bias\":-1.0,\"threshold\":0.9}";
    }

    private static String names() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < CombatFeatures.NAMES.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(CombatFeatures.NAMES.get(i)).append('"');
        }
        return sb.append(']').toString();
    }

    @Test
    void scoresWithLogisticFunction() {
        CombatModel m = CombatModel.parse(model(names(), 100, null));
        double[] x = new double[15];
        x[0] = 0.5; // z = 2*0.5 - 1 = 0
        assertEquals(0.5, m.score(x), 1e-12);
        x[0] = 10; // z = 19
        assertTrue(m.score(x) > m.threshold());
        assertEquals(0.9, m.threshold(), 1e-12);
    }

    @Test
    void rejectsModelForOtherFeatures() {
        String wrong = names().replace("ping_mean", "ping_max");
        assertThrows(IllegalArgumentException.class, () -> CombatModel.parse(model(wrong, 100, null)));
    }

    @Test
    void rejectsOtherWindowAndZeroStd() {
        assertThrows(IllegalArgumentException.class, () -> CombatModel.parse(model(names(), 200, null)));
        String zeroStd = "0.0," + String.join(",", Collections.nCopies(14, "1.0"));
        assertThrows(IllegalArgumentException.class, () -> CombatModel.parse(model(names(), 100, zeroStd)));
    }
}
