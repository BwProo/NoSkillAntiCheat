package ac.grim.grimac.checks.impl.world;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockBreakListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;

/**
 * NoSkillAntiCheat: Xray detection from mining behaviour. Every mined ore is exposed at the moment
 * it is broken, so instead this looks at HOW a rare-ore vein was found: if the only air next to its
 * first ore is the player's own tunnel (no cave), it was found by tunnelling. Legit players find
 * such veins only now and then per block of stone mined; xrayers dig straight to hidden ores and
 * find far too many. Counts veins, not blocks, and only natural underground blocks as "mined".
 * Works together with Paper's anti-xray (which hides ores) and feeds Improbable. Experimental.
 */
@CheckData(name = "XrayMining", stableKey = "nsac.world.xray_mining", description = "Found too many hidden rare-ore veins for the amount of stone mined", experimental = true, decay = 0.05)
public class XrayMining extends Check implements BlockBreakListener {
    private static final Set<StateType> RARE = Set.of(
            StateTypes.DIAMOND_ORE, StateTypes.DEEPSLATE_DIAMOND_ORE, StateTypes.EMERALD_ORE,
            StateTypes.DEEPSLATE_EMERALD_ORE, StateTypes.ANCIENT_DEBRIS);
    private static final Set<StateType> NATURAL = Set.of(
            StateTypes.STONE, StateTypes.DEEPSLATE, StateTypes.TUFF, StateTypes.GRANITE, StateTypes.DIORITE,
            StateTypes.ANDESITE, StateTypes.NETHERRACK, StateTypes.BASALT, StateTypes.BLACKSTONE, StateTypes.GRAVEL,
            StateTypes.CALCITE, StateTypes.SMOOTH_BASALT, StateTypes.SOUL_SAND, StateTypes.SOUL_SOIL);
    private static final int RECENT = 512;
    private static final int[][] SIDES = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private final long[] recentBroken = new long[RECENT];
    private int recentIndex;
    private XrayStats stats;
    private int minVeins;
    private double minBlocksPerVein;
    private Vector3i lastOre;
    private long lastOreTime;

    public XrayMining(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        long window = (long) (config.getDoubleElse(getConfigName() + ".window-minutes", 30.0) * 60_000);
        if (stats == null) stats = new XrayStats(window);
        minVeins = config.getIntElse(getConfigName() + ".min-veins", 4);
        minBlocksPerVein = config.getDoubleElse(getConfigName() + ".min-blocks-per-vein", 100.0);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action != DiggingAction.FINISHED_DIGGING) return;
        if (player.gamemode != GameMode.SURVIVAL) return;
        StateType type = blockBreak.block.getType();
        Vector3i pos = blockBreak.position;
        long now = System.currentTimeMillis();
        if (RARE.contains(type)) {
            boolean sameVein = lastOre != null && now - lastOreTime < 60_000 && distanceSq(lastOre, pos) <= 9;
            lastOre = pos;
            lastOreTime = now;
            if (!sameVein && foundByTunnel(pos)) {
                stats.tunnelVein(now);
                if (stats.suspicious(minVeins, minBlocksPerVein)) {
                    flag(String.format(Locale.ROOT, "%d hidden veins in %d blocks (%.0f blocks/vein)",
                            stats.tunnelVeins(), stats.mined(), stats.blocksPerVein()));
                    stats.reset();
                }
            }
        } else if (NATURAL.contains(type)) {
            stats.mined(now);
        }
        remember(pos);
    }

    /** True if every air block next to the ore was mined by the player (no cave exposure). */
    private boolean foundByTunnel(Vector3i ore) {
        for (int[] s : SIDES) {
            int x = ore.getX() + s[0], y = ore.getY() + s[1], z = ore.getZ() + s[2];
            StateType neighbour = player.compensatedWorld.getBlock(x, y, z).getType();
            boolean open = neighbour.isAir() || neighbour == StateTypes.WATER || neighbour == StateTypes.LAVA;
            if (open && !wasBrokenByPlayer(x, y, z)) return false; // cave, ravine or water: visible ore
        }
        return true;
    }

    private void remember(Vector3i pos) {
        recentBroken[recentIndex] = key(pos.getX(), pos.getY(), pos.getZ());
        recentIndex = (recentIndex + 1) % RECENT;
    }

    private boolean wasBrokenByPlayer(int x, int y, int z) {
        long k = key(x, y, z);
        for (long v : recentBroken) if (v == k) return true;
        return false;
    }

    private static long key(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | ((long) y & 0xFFF);
    }

    private static int distanceSq(Vector3i a, Vector3i b) {
        int dx = a.getX() - b.getX(), dy = a.getY() - b.getY(), dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
