package ac.grim.grimac.checks.impl.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Java and Python (tools/ai/features.py) must compute identical features. */
class CombatFeaturesParityTest {

    @Test
    @SuppressWarnings("deprecation")
    void javaMatchesPythonOnFixture() throws IOException {
        Path dir = Path.of("src/test/resources/ai");
        List<String> lines = Files.readAllLines(dir.resolve("parity-window.csv"), StandardCharsets.UTF_8);
        int n = lines.size() - 1;
        assertEquals(CombatFeatures.WINDOW_TICKS, n);
        double[] yaw = new double[n], pitch = new double[n], dyaw = new double[n], dpitch = new double[n];
        double[] tdx = new double[n], tdy = new double[n], tdz = new double[n];
        int[] swings = new int[n], ping = new int[n];
        boolean[] attack = new boolean[n], hasTarget = new boolean[n];
        for (int i = 0; i < n; i++) {
            String[] f = lines.get(i + 1).split(",", -1);
            yaw[i] = Double.parseDouble(f[1]);
            pitch[i] = Double.parseDouble(f[2]);
            dyaw[i] = Double.parseDouble(f[3]);
            dpitch[i] = Double.parseDouble(f[4]);
            swings[i] = Integer.parseInt(f[9]);
            attack[i] = f[10].equals("1");
            hasTarget[i] = !f[11].isEmpty();
            if (hasTarget[i]) {
                tdx[i] = Double.parseDouble(f[12]);
                tdy[i] = Double.parseDouble(f[13]);
                tdz[i] = Double.parseDouble(f[14]);
            }
            ping[i] = Integer.parseInt(f[15]);
        }
        double[] actual = CombatFeatures.compute(yaw, pitch, dyaw, dpitch, swings, attack, hasTarget, tdx, tdy, tdz, ping, n);
        JsonObject expected = new JsonParser().parse(Files.readString(dir.resolve("parity-expected.json"))).getAsJsonObject();
        assertEquals(CombatFeatures.NAMES.size(), expected.size());
        for (int i = 0; i < CombatFeatures.NAMES.size(); i++) {
            String name = CombatFeatures.NAMES.get(i);
            double want = expected.get(name).getAsDouble();
            assertEquals(want, actual[i], 1e-9 * Math.max(1, Math.abs(want)), name);
        }
    }
}
