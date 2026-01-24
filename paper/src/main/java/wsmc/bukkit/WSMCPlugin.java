package wsmc.bukkit;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class WSMCPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        WSMC.init(getLogger());
        
        saveDefaultConfig();
        
        if (getConfig().contains("debug")) {
            System.setProperty("wsmc.debug", String.valueOf(getConfig().getBoolean("debug")));
        }
        if (getConfig().contains("wsmcEndpoint")) {
            System.setProperty("wsmc.wsmcEndpoint", getConfig().getString("wsmcEndpoint"));
        }
        
        WSMC.init(getLogger());

        WSMC.info("Injecting Netty handlers...");
        inject();
    }

    @Override
    public void onDisable() {
        // Cleaning up handlers is complex and usually not fully supported for reloads
        // We rely on server restart for clean slate
    }

    @SuppressWarnings("unchecked")
    private void inject() {
        try {
            Object server = ReflectionUtils.getMinecraftServer();
            if (server == null) {
                WSMC.info("Failed to get MinecraftServer instance.");
                return;
            }

            // Find ServerConnection
            Object serverConnection = ReflectionUtils.getServerConnection(server);
            if (serverConnection == null) {
                // Try finding potential connections via fields if standard method fails
                List<Object> potentialConnections = new ArrayList<>();
                ReflectionUtils.findPotentialConnections(server, potentialConnections);
                if (!potentialConnections.isEmpty()) {
                    serverConnection = potentialConnections.get(0);
                    WSMC.debug("Using potential ServerConnection: " + serverConnection.getClass().getName());
                }
            }

            if (serverConnection == null) {
                WSMC.info("ERROR: Could not find ServerConnection instance. WSMC will not work.");
                return;
            }

            WSMC.debug("Found ServerConnection: " + serverConnection.getClass().getName());

            // Find List<ChannelFuture> channels
            List<ChannelFuture> channels = null;
            
            // 1. Try fields with List type and ChannelFuture content
            Class<?> clazz = serverConnection.getClass();
            while (clazz != null && clazz != Object.class) {
                for (Field f : clazz.getDeclaredFields()) {
                    if (f.getType() == List.class) {
                        f.setAccessible(true);
                        try {
                            List<?> list = (List<?>) f.get(serverConnection);
                            if (list != null && !list.isEmpty()) {
                                if (list.get(0) instanceof ChannelFuture) {
                                    channels = (List<ChannelFuture>) list;
                                    WSMC.debug("Found channels list via content check: " + f.getName());
                                    break;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (channels != null) break;
                clazz = clazz.getSuperclass();
            }

            // 2. If empty or not found, try by name
            if (channels == null) {
                String[] possibleNames = {"channels", "f", "g", "activeChannels", "boundChannels", "listeningChannels"};
                clazz = serverConnection.getClass();
                while (clazz != null && clazz != Object.class) {
                    for (String name : possibleNames) {
                        try {
                            Field field = clazz.getDeclaredField(name);
                            if (field.getType() == List.class) {
                                field.setAccessible(true);
                                channels = (List<ChannelFuture>) field.get(serverConnection);
                                WSMC.debug("Found channels list via name: " + name);
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                    if (channels != null) break;
                    clazz = clazz.getSuperclass();
                }
            }

            if (channels == null) {
                WSMC.info("ERROR: Could not find channels list in ServerConnection. WSMC will not work.");
                return;
            }

            WSMC.debug("Found " + channels.size() + " listening channels");

            // Inject into each channel
            for (ChannelFuture future : channels) {
                injectServerChannel(future);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void injectServerChannel(ChannelFuture future) {
        try {
            Channel channel = future.channel();
            if (channel.pipeline().get("wsmc_server_handler") == null) {
                channel.pipeline().addFirst("wsmc_server_handler", new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        if (msg instanceof Channel) {
                            Channel child = (Channel) msg;
                            // Use ChannelInitializer to ensure the detector is added immediately upon registration
                            // This matches the logic in the working PaperEntry implementation
                            child.pipeline().addFirst(new ChannelInitializer<Channel>() {
                                @Override
                                protected void initChannel(Channel ch) throws Exception {
                                    ch.pipeline().addFirst("wsmc_detector", new ProtocolDetector());
                                    WSMC.debug("Injected ProtocolDetector into child channel " + ch);
                                }
                            });
                        }
                        ctx.fireChannelRead(msg);
                    }
                });
                WSMC.debug("Injected ServerChannelHandler into listening channel " + channel);
            }
        } catch (Exception e) {
            WSMC.info("Failed to inject server channel: " + e.getMessage());
        }
    }

    private static class ReflectionUtils {
        public static Object getMinecraftServer() {
            try {
                return Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            } catch (Exception e) {
                return null;
            }
        }

        public static Object getServerConnection(Object server) {
            if (server == null) return null;
            Class<?> clazz = server.getClass();
            while (clazz != Object.class && clazz != null) {
                for (Field f : clazz.getDeclaredFields()) {
                    if (f.getType().getSimpleName().equals("ServerConnection") || 
                        f.getType().getName().endsWith(".ServerConnection")) {
                        f.setAccessible(true);
                        try {
                            return f.get(server);
                        } catch (IllegalAccessException e) {}
                    }
                }
                for (Method m : clazz.getDeclaredMethods()) {
                     if ((m.getReturnType().getSimpleName().equals("ServerConnection") || 
                          m.getReturnType().getName().endsWith(".ServerConnection")) 
                          && m.getParameterCount() == 0) {
                        m.setAccessible(true);
                        try {
                            return m.invoke(server);
                        } catch (Exception e) {}
                    }
                }
                clazz = clazz.getSuperclass();
            }
            return null;
        }
        
        public static void findPotentialConnections(Object server, List<Object> results) {
            if (server == null) return;
            Class<?> clazz = server.getClass();
            while (clazz != Object.class && clazz != null) {
                for (Field f : clazz.getDeclaredFields()) {
                    // 查找字段名或类型名看起来像 Connection 的对象
                    if (f.getType().getName().contains("Connection") || 
                        f.getType().getName().contains("Network")) {
                        f.setAccessible(true);
                        try {
                            Object val = f.get(server);
                            if (val != null && !results.contains(val)) {
                                results.add(val);
                            }
                        } catch (Exception e) {}
                    }
                }
                clazz = clazz.getSuperclass();
            }
        }
    }
}
