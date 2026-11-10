package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.GrimProcessor;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.nbt.NBTByte;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor;

import java.util.List;

/**
 * NoSkillAntiCheat: sign translation probe (detects hacked clients such as Meteor even when they
 * hide their brand). A few seconds after joining, the client gets a client-side-only sign at the
 * bottom of the world below the player, with mod translation keys as text, and is asked to edit it.
 * The sign is out of reach, so the client closes the editor at once and sends the text back,
 * translated with its own language files. A translated key means the mod is installed.
 * The fake block is restored afterwards and the reply is never processed by the server.
 *
 * <p>Limits: the editor may flash for a frame; client mods can block this at any time.
 * Config: mod-probe.enabled, mod-probe.delay-seconds, mod-probe.keys ("Label=translation.key").
 */
public final class SignProbe extends GrimProcessor implements PacketReceiveListener {
    private static final int TIMEOUT_TICKS = 200;
    private int ticks;
    private boolean sent, done;
    private Vector3i position;
    private WrappedBlockState originalState;
    private int ticksSinceSent;
    private List<TranslationProbe.Key> keys;

    public SignProbe(GrimPlayer player) {
        super(player);
    }

    private static ConfigManager config() {
        return GrimAPI.INSTANCE.getConfigManager().getConfig();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (done) return;
        if (sent && event.getPacketType() == PacketType.Play.Client.UPDATE_SIGN) {
            WrapperPlayClientUpdateSign packet = new WrapperPlayClientUpdateSign(event);
            if (!packet.getBlockPosition().equals(position)) return;
            event.setCancelled(true); // the server must never handle our fake sign
            finish();
            String mod = TranslationProbe.detect(keys, packet.getTextLines());
            if (mod != null) {
                BlacklistedClient check = player.checkManager.get(BlacklistedClient.class);
                if (check != null) check.flagDetected("sign-translation=" + mod);
            }
            return;
        }
        if (!isTickPacket(event.getPacketType())) return;
        if (sent) {
            ++ticksSinceSent;
            // Safety net: if the client kept the editor open, closing it also sends the (translated) text
            if (ticksSinceSent == 2) player.user.sendPacket(new WrapperPlayServerCloseWindow(0));
            if (ticksSinceSent > TIMEOUT_TICKS) finish(); // no answer: restore and give up
            return;
        }
        ConfigManager config = config();
        int delayTicks = (int) (config.getDoubleElse("mod-probe.delay-seconds", 3.0) * 20);
        if (++ticks < delayTicks) return;
        if (!config.getBooleanElse("mod-probe.enabled", true)
                || player.getClientVersion().isOlderThan(ClientVersion.V_1_20)) {
            done = true;
            return;
        }
        keys = TranslationProbe.parse(config.getStringListElse("mod-probe.keys", TranslationProbe.DEFAULT_KEYS));
        if (keys.isEmpty()) {
            done = true;
            return;
        }
        send();
    }

    private void send() {
        int minY = player.compensatedWorld.getMinHeight();
        position = new Vector3i((int) Math.floor(player.x), minY, (int) Math.floor(player.z));
        originalState = player.compensatedWorld.getBlock(position);
        try {
            WrappedBlockState sign = WrappedBlockState.getDefaultState(player.getClientVersion(), StateTypes.OAK_SIGN);
            player.user.sendPacket(new WrapperPlayServerBlockChange(position, sign.getGlobalId()));
            player.user.sendPacket(new WrapperPlayServerBlockEntityData(position, BlockEntityTypes.SIGN, signNbt()));
            player.user.sendPacket(new WrapperPlayServerOpenSignEditor(position, true));
            sent = true;
        } catch (RuntimeException e) {
            done = true;
            LogUtil.warn("Sign probe could not be sent to " + player.getName() + ": " + e.getMessage());
        }
    }

    private NBTCompound signNbt() {
        boolean componentNbt = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_5);
        NBTCompound front = new NBTCompound();
        front.setTag("messages", messages(componentNbt, true));
        front.setTag("color", new NBTString("black"));
        front.setTag("has_glowing_text", new NBTByte(false));
        NBTCompound back = new NBTCompound();
        back.setTag("messages", messages(componentNbt, false));
        back.setTag("color", new NBTString("black"));
        back.setTag("has_glowing_text", new NBTByte(false));
        NBTCompound nbt = new NBTCompound();
        nbt.setTag("front_text", front);
        nbt.setTag("back_text", back);
        nbt.setTag("is_waxed", new NBTByte(false));
        return nbt;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private NBTList messages(boolean componentNbt, boolean withKeys) {
        if (componentNbt) {
            NBTList<NBTCompound> list = NBTList.createCompoundList();
            for (int i = 0; i < 4; i++) {
                NBTCompound line = new NBTCompound();
                if (withKeys && i < keys.size()) {
                    line.setTag("translate", new NBTString(keys.get(i).key()));
                } else {
                    line.setTag("text", new NBTString(""));
                }
                list.addTag(line);
            }
            return list;
        }
        NBTList<NBTString> list = NBTList.createStringList();
        for (int i = 0; i < 4; i++) {
            String json = withKeys && i < keys.size()
                    ? "{\"translate\":\"" + keys.get(i).key().replace("\\", "\\\\").replace("\"", "\\\"") + "\"}"
                    : "{\"text\":\"\"}";
            list.addTag(new NBTString(json));
        }
        return list;
    }

    private void finish() {
        done = true;
        if (position != null && originalState != null) {
            player.user.sendPacket(new WrapperPlayServerBlockChange(position, originalState.getGlobalId()));
        }
    }
}
