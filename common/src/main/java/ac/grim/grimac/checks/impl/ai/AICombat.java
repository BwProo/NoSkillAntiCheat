package ac.grim.grimac.checks.impl.ai;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.util.Vector3d;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Locale;

/**
 * NoSkillAntiCheat: AI combat check. Collects the same per-tick values as the recorder into fixed
 * arrays; every {@link CombatFeatures#WINDOW_TICKS} ticks with enough combat it computes the
 * features and scores them with the model from plugins/NoSkillAntiCheat/ai/model.json (trained with
 * tools/ai/train.py). Without a model file it does nothing. Experimental: shadow flags only.
 */
@CheckData(name = "AICombat", stableKey = "nsac.ai.combat", description = "AI model rated the combat behaviour as cheating", experimental = true, decay = 0.2)
public class AICombat extends Check implements PacketReceiveListener {
    private static final int N = CombatFeatures.WINDOW_TICKS;
    private static final int TARGET_TICKS = 100;
    private final double[] yaw = new double[N], pitch = new double[N], dyaw = new double[N], dpitch = new double[N];
    private final double[] tdx = new double[N], tdy = new double[N], tdz = new double[N];
    private final int[] swings = new int[N], ping = new int[N];
    private final boolean[] attack = new boolean[N], hasTarget = new boolean[N];
    private int index, combatTicks;
    private float lastYaw, lastPitch;
    private int swingsThisTick;
    private boolean attackThisTick;
    private int target = Integer.MIN_VALUE;
    private int ticksSinceTarget = Integer.MAX_VALUE;

    public AICombat(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        CombatModel.reloadIfChanged(new File(new File(GrimAPI.INSTANCE.getGrimPlugin().getDataFolder(), "ai"), "model.json"));
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (CombatModel.current() == null) return; // no model: zero cost beyond this check
        int attacked = ac.grim.grimac.utils.anticheat.AttackPackets.attackedPlayer(player, event);
        if (attacked != ac.grim.grimac.utils.anticheat.AttackPackets.NONE) {
            attackThisTick = true;
            target = attacked;
            ticksSinceTarget = 0;
        } else if (isAnimation(event.getPacketType())) {
            swingsThisTick++;
        } else if (isTickPacket(event.getPacketType())) {
            onTick();
        }
    }

    private void onTick() {
        int i = index;
        yaw[i] = player.yaw;
        pitch[i] = player.pitch;
        dyaw[i] = player.yaw - lastYaw;
        dpitch[i] = player.pitch - lastPitch;
        lastYaw = player.yaw;
        lastPitch = player.pitch;
        swings[i] = swingsThisTick;
        attack[i] = attackThisTick;
        ping[i] = player.getTransactionPing();
        hasTarget[i] = false;
        if (ticksSinceTarget <= TARGET_TICKS) {
            PacketEntity entity = player.compensatedEntities.getEntity(target);
            if (entity != null) {
                Vector3d pos = entity.trackedServerPosition.getPos();
                tdx[i] = pos.getX() - player.x;
                tdy[i] = pos.getY() - player.y;
                tdz[i] = pos.getZ() - player.z;
                hasTarget[i] = true;
                combatTicks++;
            }
        }
        swingsThisTick = 0;
        attackThisTick = false;
        if (ticksSinceTarget != Integer.MAX_VALUE) ticksSinceTarget++;

        if (++index < N) return;
        index = 0;
        boolean enoughCombat = combatTicks >= CombatFeatures.MIN_COMBAT_TICKS;
        combatTicks = 0;
        CombatModel model = CombatModel.current();
        if (!enoughCombat || model == null) return;

        double[] features = CombatFeatures.compute(yaw, pitch, dyaw, dpitch, swings, attack, hasTarget, tdx, tdy, tdz, ping, N);
        double probability = model.score(features);
        if (probability >= model.threshold()) {
            flag(String.format(Locale.ROOT, "p=%.3f threshold=%.3f", probability, model.threshold()));
        } else {
            reward();
        }
    }
}
