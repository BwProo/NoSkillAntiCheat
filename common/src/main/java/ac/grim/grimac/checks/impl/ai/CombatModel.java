package ac.grim.grimac.checks.impl.ai;

import ac.grim.grimac.utils.anticheat.LogUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * NoSkillAntiCheat: logistic-regression model written by tools/ai/train.py. Scoring is 15
 * multiply-adds and one exp(), so it runs inline on the player's thread (a thread hand-off would
 * cost more than the math). Shared by all players and reloaded when the file changes.
 */
public final class CombatModel {
    private static volatile @Nullable CombatModel current;
    private static volatile long loadedModified = -1;
    private static volatile String loadedPath = "";

    private final double[] mean, std, weights;
    private final double bias, threshold;

    CombatModel(double[] mean, double[] std, double[] weights, double bias, double threshold) {
        this.mean = mean;
        this.std = std;
        this.weights = weights;
        this.bias = bias;
        this.threshold = threshold;
    }

    public static @Nullable CombatModel current() {
        return current;
    }

    /** (Re)loads the model if the file appeared, changed or vanished. Cheap when unchanged. */
    public static synchronized void reloadIfChanged(@NotNull File file) {
        long modified = file.isFile() ? file.lastModified() : 0;
        if (modified == loadedModified && file.getPath().equals(loadedPath)) return;
        loadedModified = modified;
        loadedPath = file.getPath();
        if (modified == 0) {
            current = null;
            return;
        }
        try {
            current = parse(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            LogUtil.info("Loaded AI combat model from " + file.getPath());
        } catch (Exception e) {
            current = null;
            LogUtil.warn("Could not load AI combat model " + file.getPath() + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    public static @NotNull CombatModel parse(@NotNull String json) {
        JsonObject o = new JsonParser().parse(json).getAsJsonObject();
        if (!"logistic_regression".equals(o.get("type").getAsString())) {
            throw new IllegalArgumentException("unsupported model type " + o.get("type"));
        }
        List<String> names = new ArrayList<>();
        for (var e : o.getAsJsonArray("features")) names.add(e.getAsString());
        if (!names.equals(CombatFeatures.NAMES)) {
            throw new IllegalArgumentException("feature list does not match this plugin version: " + names);
        }
        if (o.get("window_ticks").getAsInt() != CombatFeatures.WINDOW_TICKS
                || o.get("min_combat_ticks").getAsInt() != CombatFeatures.MIN_COMBAT_TICKS) {
            throw new IllegalArgumentException("window settings do not match this plugin version");
        }
        double[] mean = array(o.getAsJsonArray("mean")), std = array(o.getAsJsonArray("std")), w = array(o.getAsJsonArray("weights"));
        if (mean.length != names.size() || std.length != names.size() || w.length != names.size()) {
            throw new IllegalArgumentException("array sizes do not match the feature count");
        }
        for (double s : std) if (!(s > 0)) throw new IllegalArgumentException("std must be > 0");
        return new CombatModel(mean, std, w, o.get("bias").getAsDouble(), o.get("threshold").getAsDouble());
    }

    private static double[] array(JsonArray a) {
        double[] out = new double[a.size()];
        for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsDouble();
        return out;
    }

    /** Probability that the window is cheating, 0..1. */
    public double score(double @NotNull [] features) {
        double z = bias;
        for (int i = 0; i < weights.length; i++) z += weights[i] * (features[i] - mean[i]) / std[i];
        if (z < -40) return 0;
        if (z > 40) return 1;
        return 1.0 / (1.0 + Math.exp(-z));
    }

    public double threshold() {
        return threshold;
    }
}
