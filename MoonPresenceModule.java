package com.moonclient.modules;

import com.moonclient.MoonClient;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shows "Moon Client" on your Discord profile.
 *
 * Discord decides the name and the picture from a Discord *application*, so you need your own (free):
 * create one at discord.com/developers named "Moon Client", upload the moon picture under
 * Rich Presence > Art Assets with the key "moon", then paste the Application ID into this module.
 *
 * Talks to the Discord desktop app directly (named pipe on Windows, unix socket on Linux/Mac),
 * so it needs no extra libraries.
 */
public class MoonPresenceModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> applicationId = sgGeneral.add(new StringSetting.Builder()
        .name("application-id")
        .description("Application ID of your Moon Client app from discord.com/developers.")
        .defaultValue("")
        .build());

    private final Setting<String> largeImage = sgGeneral.add(new StringSetting.Builder()
        .name("large-image-key")
        .description("Name of the moon picture you uploaded under Rich Presence > Art Assets.")
        .defaultValue("moon")
        .build());

    private final Setting<String> details = sgGeneral.add(new StringSetting.Builder()
        .name("details")
        .description("First line of your status.")
        .defaultValue("Moon Client")
        .build());

    private final Setting<String> state = sgGeneral.add(new StringSetting.Builder()
        .name("state")
        .description("Second line of your status.")
        .defaultValue("Playing Minecraft")
        .build());

    private Rpc rpc;

    public MoonPresenceModule() {
        super(MoonClient.CATEGORY, "moon-presence", "Shows Moon Client on your Discord profile.");
    }

    @Override
    public void onActivate() {
        String id = applicationId.get().trim();
        if (!id.matches("\\d{15,25}")) {
            error("Set application-id first (create an app at discord.com/developers).");
            toggle();
            return;
        }
        rpc = new Rpc(id, details.get(), state.get(), largeImage.get().trim());
        rpc.start();
    }

    @Override
    public void onDeactivate() {
        if (rpc != null) {
            rpc.stop();
            rpc = null;
        }
    }

    // ---------------------------------------------------------------- Discord IPC

    private interface Transport extends Closeable {
        void write(byte[] data) throws IOException;
        void readFully(byte[] buf) throws IOException;
    }

    private static final class PipeTransport implements Transport {
        private final RandomAccessFile file;
        PipeTransport(RandomAccessFile file) { this.file = file; }
        @Override public void write(byte[] data) throws IOException { file.write(data); }
        @Override public void readFully(byte[] buf) throws IOException { file.readFully(buf); }
        @Override public void close() throws IOException { file.close(); }
    }

    private static final class SocketTransport implements Transport {
        private final SocketChannel channel;
        private final InputStream in;
        private final OutputStream out;
        SocketTransport(SocketChannel channel) {
            this.channel = channel;
            this.in = Channels.newInputStream(channel);
            this.out = Channels.newOutputStream(channel);
        }
        @Override public void write(byte[] data) throws IOException { out.write(data); out.flush(); }
        @Override public void readFully(byte[] buf) throws IOException {
            byte[] r = in.readNBytes(buf.length);
            if (r.length != buf.length) throw new EOFException();
            System.arraycopy(r, 0, buf, 0, buf.length);
        }
        @Override public void close() throws IOException { channel.close(); }
    }

    private static final class Rpc {
        private final String clientId, details, state, largeImage;
        private volatile boolean running;
        private volatile Transport transport;
        private Thread thread;

        Rpc(String clientId, String details, String state, String largeImage) {
            this.clientId = clientId;
            this.details = details;
            this.state = state;
            this.largeImage = largeImage;
        }

        void start() {
            running = true;
            thread = new Thread(this::run, "moon-discord-rpc");
            thread.setDaemon(true);
            thread.start();
        }

        void stop() {
            running = false;
            closeTransport();
            if (thread != null) thread.interrupt();
        }

        private void run() {
            long startSeconds = System.currentTimeMillis() / 1000L;
            while (running) {
                try {
                    if (transport == null) {
                        Transport t = connect();
                        if (t == null) { Thread.sleep(10_000); continue; }
                        transport = t;
                        send(0, "{\"v\":1,\"client_id\":\"" + esc(clientId) + "\"}");
                        readFrame();
                    }
                    send(1, activityJson(startSeconds));
                    readFrame();
                    Thread.sleep(15_000);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    closeTransport();
                    try { Thread.sleep(10_000); } catch (InterruptedException ie) { break; }
                }
            }
            closeTransport();
        }

        private String activityJson(long startSeconds) {
            StringBuilder activity = new StringBuilder("{");
            activity.append("\"details\":\"").append(esc(details)).append("\",");
            activity.append("\"state\":\"").append(esc(state)).append("\",");
            activity.append("\"timestamps\":{\"start\":").append(startSeconds).append("}");
            if (!largeImage.isEmpty()) {
                activity.append(",\"assets\":{\"large_image\":\"").append(esc(largeImage))
                    .append("\",\"large_text\":\"Moon Client\"}");
            }
            activity.append("}");

            return "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + ProcessHandle.current().pid()
                + ",\"activity\":" + activity + "},\"nonce\":\"" + UUID.randomUUID() + "\"}";
        }

        private void send(int opcode, String json) throws IOException {
            byte[] payload = json.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buf = ByteBuffer.allocate(8 + payload.length).order(ByteOrder.LITTLE_ENDIAN);
            buf.putInt(opcode).putInt(payload.length).put(payload);
            transport.write(buf.array());
        }

        private void readFrame() throws IOException {
            byte[] header = new byte[8];
            transport.readFully(header);
            int length = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
            if (length < 0 || length > 1_000_000) throw new IOException("bad frame");
            transport.readFully(new byte[length]);
        }

        private synchronized void closeTransport() {
            Transport t = transport;
            transport = null;
            if (t != null) {
                try { t.close(); } catch (IOException ignored) {}
            }
        }

        private static Transport connect() {
            boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");

            List<Path> dirs = new ArrayList<>();
            if (!windows) {
                for (String env : new String[]{"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"}) {
                    String v = System.getenv(env);
                    if (v != null && !v.isEmpty()) dirs.add(Path.of(v));
                }
                dirs.add(Path.of("/tmp"));
            }

            for (int i = 0; i < 10; i++) {
                if (windows) {
                    try {
                        return new PipeTransport(new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw"));
                    } catch (IOException ignored) {}
                } else {
                    for (Path dir : dirs) {
                        for (String sub : new String[]{"", "app/com.discordapp.Discord", "snap.discord"}) {
                            Path p = dir.resolve(sub).resolve("discord-ipc-" + i);
                            if (!Files.exists(p)) continue;
                            try {
                                SocketChannel ch = SocketChannel.open(StandardProtocolFamily.UNIX);
                                ch.connect(UnixDomainSocketAddress.of(p));
                                return new SocketTransport(ch);
                            } catch (IOException ignored) {}
                        }
                    }
                }
            }
            return null;
        }

        private static String esc(String s) {
            StringBuilder out = new StringBuilder();
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    case '\n' -> out.append("\\n");
                    case '\r' -> out.append("\\r");
                    case '\t' -> out.append("\\t");
                    default -> { if (c >= 0x20) out.append(c); }
                }
            }
            return out.toString();
        }
    }
}
