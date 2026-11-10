package ac.grim.grimac.manager.punish;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.platform.api.scheduler.TaskHandle;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.particle.data.ParticleDustData;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.protocol.sound.Sound;
import com.github.retrooper.packetevents.protocol.sound.SoundCategory;
import com.github.retrooper.packetevents.protocol.sound.Sounds;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleSubtitle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleText;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleTimes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * NoSkillAntiCheat: the ban animation. A punishment command written as
 * {@code [animation] ban %player% reason} first plays a three-second show around the cheater
 * (purple double helix, lightning, sounds, a title for the cheater) and then runs the command.
 * Everything is sent as packets from an async task: no world change, no entity, Folia-safe.
 */
public final class BanAnimation {
    public static final String PREFIX = "[animation]";
    static final int HELIX_FRAMES = 44;
    static final int STRIKE_FRAME = 46;
    static final int END_FRAME = 62;
    private static final double HEIGHT = 2.4;
    private static final double RADIUS = 1.1;

    private BanAnimation() {
    }

    /** Strips the prefix; returns null when the command does not start with it. */
    public static String stripPrefix(@NotNull String command) {
        if (!command.startsWith(PREFIX)) return null;
        return command.substring(PREFIX.length()).strip();
    }

    /**
     * Point of one helix strand at a frame, relative to the player's feet. Pure, unit-tested:
     * the strands are opposite each other, climb to {@value #HEIGHT} blocks and narrow as they rise.
     */
    public static double[] helixPoint(int frame, int strand) {
        double progress = Math.min(1.0, frame / (double) HELIX_FRAMES);
        double angle = frame * 0.45 + strand * Math.PI;
        double radius = RADIUS * (1.0 - 0.55 * progress);
        return new double[]{Math.cos(angle) * radius, progress * HEIGHT, Math.sin(angle) * radius};
    }

    /** Plays the animation, then runs {@code afterwards}. Falls back to running it at once. */
    public static void play(@NotNull GrimPlayer target, @NotNull Runnable afterwards) {
        ConfigManager config = GrimAPI.INSTANCE.getConfigManager().getConfig();
        if (!config.getBooleanElse("ban-animation.enabled", true)) {
            afterwards.run();
            return;
        }
        double radius = config.getDoubleElse("ban-animation.view-distance", 48);
        String title = config.getStringElse("ban-animation.title", "&5&lNSAC");
        String subtitle = config.getStringElse("ban-animation.subtitle", "&7Cheat detected");
        Vector3d origin = new Vector3d(target.x, target.y, target.z);
        List<GrimPlayer> viewers = viewers(target, origin, radius);
        int lightningId = Integer.MAX_VALUE - ThreadLocalRandom.current().nextInt(1_000_000);

        send(List.of(target), new WrapperPlayServerSetTitleTimes(5, 50, 15));
        send(List.of(target), new WrapperPlayServerSetTitleText(MessageUtil.miniMessage(title)));
        send(List.of(target), new WrapperPlayServerSetTitleSubtitle(MessageUtil.miniMessage(subtitle)));
        sound(List.of(target), Sounds.ENTITY_ELDER_GUARDIAN_CURSE, origin, 1.0f, 1.0f);
        sound(viewers, Sounds.BLOCK_BEACON_ACTIVATE, origin, 1.0f, 0.6f);

        int[] frame = {0};
        TaskHandle[] handle = new TaskHandle[1];
        boolean[] finished = {false};
        handle[0] = GrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(GrimAPI.INSTANCE.getGrimPlugin(), () -> {
            if (finished[0]) return;
            int f = frame[0]++;
            try {
                frame(viewers, origin, f, lightningId);
            } catch (Throwable ignored) {
                // never let a visual effect block the punishment
            }
            if (f >= END_FRAME) {
                finished[0] = true;
                if (handle[0] != null) handle[0].cancel();
                afterwards.run();
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    private static void frame(List<GrimPlayer> viewers, Vector3d o, int f, int lightningId) {
        if (f <= HELIX_FRAMES) {
            for (int strand = 0; strand < 2; strand++) {
                double[] p = helixPoint(f, strand);
                // dark violet and light violet strands
                ParticleDustData dust = strand == 0 ? new ParticleDustData(1.3f, 0.43f, 0.16f, 0.85f)
                        : new ParticleDustData(1.1f, 0.75f, 0.58f, 1.0f);
                particle(viewers, new Particle<>(ParticleTypes.DUST, dust), o.add(p[0], p[1], p[2]), 0, 0f, 2);
            }
            if (f % 6 == 0) {
                particle(viewers, new Particle<>(ParticleTypes.WITCH), o.add(0, 0.1, 0), 0.6f, 0f, 12);
                sound(viewers, Sounds.BLOCK_BEACON_POWER_SELECT, o, 0.4f, 0.8f + f / (float) HELIX_FRAMES);
            }
        }
        if (f == STRIKE_FRAME) {
            send(viewers, new WrapperPlayServerSpawnEntity(lightningId, Optional.of(UUID.randomUUID()), EntityTypes.LIGHTNING_BOLT,
                    o, 0f, 0f, 0f, 0, Optional.empty()));
            sound(viewers, Sounds.ENTITY_LIGHTNING_BOLT_THUNDER, o, 1.0f, 1.0f);
            particle(viewers, new Particle<>(ParticleTypes.EXPLOSION), o.add(0, 1, 0), 0.3f, 0f, 3);
            particle(viewers, new Particle<>(ParticleTypes.END_ROD), o.add(0, 1, 0), 0.2f, 0.35f, 60);
            particle(viewers, new Particle<>(ParticleTypes.REVERSE_PORTAL), o.add(0, 1, 0), 0.5f, 0.6f, 80);
        }
        if (f == STRIKE_FRAME + 12) send(viewers, new WrapperPlayServerDestroyEntities(lightningId));
    }

    private static List<GrimPlayer> viewers(GrimPlayer target, Vector3d o, double radius) {
        List<GrimPlayer> list = new ArrayList<>();
        list.add(target);
        String world = target.getWorldName();
        for (GrimPlayer other : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            if (other == target || world == null || !Objects.equals(world, other.getWorldName())) continue;
            double dx = other.x - o.getX(), dy = other.y - o.getY(), dz = other.z - o.getZ();
            if (dx * dx + dy * dy + dz * dz <= radius * radius) list.add(other);
        }
        return list;
    }

    private static void particle(List<GrimPlayer> viewers, Particle<?> particle, Vector3d at, float spread, float speed, int count) {
        send(viewers, new WrapperPlayServerParticle(particle, true, at, new Vector3f(spread, spread, spread), speed, count));
    }

    private static void sound(List<GrimPlayer> viewers, Sound sound, Vector3d at, float volume, float pitch) {
        Vector3i fixed = new Vector3i((int) (at.getX() * 8), (int) (at.getY() * 8), (int) (at.getZ() * 8));
        send(viewers, new WrapperPlayServerSoundEffect(sound, SoundCategory.PLAYER, fixed, volume, pitch));
    }

    private static void send(List<GrimPlayer> viewers, PacketWrapper<?> packet) {
        for (GrimPlayer viewer : viewers) {
            try {
                viewer.user.sendPacketSilently(packet);
            } catch (Throwable ignored) {
                // player left mid-animation
            }
        }
    }
}
