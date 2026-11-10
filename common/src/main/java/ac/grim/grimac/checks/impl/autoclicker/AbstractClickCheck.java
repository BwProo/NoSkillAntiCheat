package ac.grim.grimac.checks.impl.autoclicker;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.BlockBreakListener;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.jetbrains.annotations.NotNull;

/**
 * NoSkillAntiCheat: shared click collection for the AutoClicker checks.
 *
 * <p>Collects the delay (in client ticks) between attack-style swings. Ticks in which the player
 * dug, placed, used an item or right-clicked an entity are discarded, because those also send
 * swings. Pauses longer than {@link ClickStats#MAX_DELAY_TICKS} end the current streak without
 * adding a sample. When {@code sample-size} delays are collected, {@link #evaluate} runs once and
 * the window starts over. Memory: one small int array per check and player.
 */
public abstract class AbstractClickCheck extends Check implements PreViaPacketReceiveListener, BlockBreakListener {
    // No field initializer: Check's constructor calls onReload() before subclass initializers run,
    // and an initializer here would replace the configured array afterwards.
    private int[] delays;
    private int count;
    private int ticksSinceSwing = Integer.MAX_VALUE;
    private int swingsThisTick;
    private boolean tickExcluded;
    private boolean digging;
    protected double minCps;

    protected AbstractClickCheck(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        int size = Math.max(20, Math.min(500, config.getIntElse(getConfigName() + ".sample-size", 50)));
        if (delays == null || delays.length != size) {
            delays = new int[size];
            count = 0;
        }
        minCps = config.getDoubleElse(getConfigName() + ".min-cps", 9.0);
        onClickConfig(config);
    }

    /** Hook for subclass thresholds. */
    protected void onClickConfig(@NotNull ConfigManager config) {}

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.START_DIGGING) {
            digging = true;
        } else if (blockBreak.action == DiggingAction.FINISHED_DIGGING || blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            digging = false;
        }
        tickExcluded = true;
    }

    @Override
    public void onPreViaPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (isAnimation(type)) {
            swingsThisTick++;
        } else if (type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                || type == PacketType.Play.Client.USE_ITEM
                || type == PacketType.Play.Client.PLAYER_DIGGING) {
            tickExcluded = true;
        } else if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                tickExcluded = true;
            }
        }

        if (isTickPacket(type)) {
            onTick();
        }
    }

    private void onTick() {
        if (swingsThisTick > 0) {
            if (tickExcluded || digging) {
                ticksSinceSwing = Integer.MAX_VALUE; // a non-attack swing breaks the streak
            } else {
                if (ticksSinceSwing <= ClickStats.MAX_DELAY_TICKS) add(ticksSinceSwing);
                for (int i = 1; i < swingsThisTick; i++) add(0); // extra clicks in the same tick
                ticksSinceSwing = 0;
            }
        }
        if (ticksSinceSwing != Integer.MAX_VALUE) ticksSinceSwing++;
        swingsThisTick = 0;
        tickExcluded = false;
    }

    private void add(int delay) {
        delays[count++] = delay;
        if (count == delays.length) {
            ClickStats stats = ClickStats.of(delays, count);
            count = 0;
            if (stats.cps() >= minCps) {
                evaluate(stats);
            }
        }
    }

    /** Called once per full window with at least {@code min-cps}. Flag or reward here. */
    protected abstract void evaluate(@NotNull ClickStats stats);

    protected static String describe(ClickStats s) {
        return String.format(java.util.Locale.ROOT, "cps=%.1f std=%.2f distinct=%d skew=%.2f kurt=%.2f n=%d",
                s.cps(), s.stdDevTicks(), s.distinctDelays(), s.skewness(), s.kurtosis(), s.samples());
    }
}
