package wsmc.paper;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import org.bukkit.plugin.java.JavaPlugin;
import wsmc.WSMC;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class PaperEntry extends JavaPlugin {

    @Override
    public void onEnable() {
        WSMC.setLogger(new wsmc.Logger() {
            private final java.util.logging.Logger paperLogger = getLogger();

            @Override
            public void info(String msg) {
                paperLogger.info(msg);
            }

            @Override
            public void debug(String msg) {
                paperLogger.fine(msg);
            }
        });

        WSMC.info("Initializing WSMC for Paper...");
        try {
            inject();
            WSMC.info("WSMC initialized successfully.");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize WSMC: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void inject() throws Exception {
        // Get the CraftServer instance
        Object craftServer = getServer();
        
        // Get the MinecraftServer instance from CraftServer
        Object minecraftServer = null;
        try {
            minecraftServer = craftServer.getClass().getMethod("getServer").invoke(craftServer);
        } catch (NoSuchMethodException e) {
            // Try alternative method names
            for (Method method : craftServer.getClass().getMethods()) {
                if (method.getReturnType().getSimpleName().contains("Server") && method.getParameterCount() == 0) {
                    minecraftServer = method.invoke(craftServer);
                    break;
                }
            }
        }
        
        if (minecraftServer == null) {
            throw new RuntimeException("Could not find MinecraftServer instance");
        }
        
        WSMC.debug("Found MinecraftServer: " + minecraftServer.getClass().getName());

        // Find ServerConnection in MinecraftServer - more comprehensive search
        Object serverConnection = null;
        
        // Log all available methods and fields for debugging
        WSMC.debug("Searching for ServerConnection in " + minecraftServer.getClass().getName());
        
        // First try to find it via methods with more flexible matching
        for (Method method : minecraftServer.getClass().getMethods()) {
            String methodName = method.getName().toLowerCase();
            String returnTypeName = method.getReturnType().getSimpleName().toLowerCase();
            
            // Look for methods that return connection/network related types
            if (methodName.contains("connection") || methodName.contains("network") || 
                returnTypeName.contains("connection") || returnTypeName.contains("network")) {
                try {
                    Object result = method.invoke(minecraftServer);
                    if (result != null) {
                        String resultType = result.getClass().getSimpleName().toLowerCase();
                        WSMC.debug("Method " + method.getName() + " returns " + resultType);
                        
                        if (resultType.contains("serverconnection") || resultType.contains("networkmanager")) {
                            serverConnection = result;
                            WSMC.debug("Found ServerConnection via method: " + method.getName() + " (type: " + result.getClass().getName() + ")");
                            break;
                        }
                    }
                } catch (Exception e) {
                    WSMC.debug("Failed to invoke method " + method.getName() + ": " + e.getMessage());
                }
            }
        }
        
        // If not found via methods, try fields with more comprehensive search
        if (serverConnection == null) {
            WSMC.debug("Searching fields for ServerConnection...");
            for (Field field : minecraftServer.getClass().getDeclaredFields()) {
                String fieldName = field.getName().toLowerCase();
                String fieldType = field.getType().getSimpleName().toLowerCase();
                
                // Look for fields with connection/network related names or types
                if (fieldName.contains("connection") || fieldName.contains("network") || 
                    fieldType.contains("connection") || fieldType.contains("network")) {
                    try {
                        field.setAccessible(true);
                        Object result = field.get(minecraftServer);
                        if (result != null) {
                            String resultType = result.getClass().getSimpleName().toLowerCase();
                            WSMC.debug("Field " + field.getName() + " (type: " + fieldType + ") contains " + resultType);
                            
                            if (resultType.contains("serverconnection") || resultType.contains("networkmanager")) {
                                serverConnection = result;
                                WSMC.debug("Found ServerConnection via field: " + field.getName() + " (type: " + result.getClass().getName() + ")");
                                break;
                            }
                        }
                    } catch (Exception e) {
                        WSMC.debug("Failed to access field " + field.getName() + ": " + e.getMessage());
                    }
                }
            }
        }

        if (serverConnection == null) {
            // Last resort: try to find any field that might contain the server connection
            WSMC.debug("Attempting last resort search for ServerConnection...");
            for (Field field : minecraftServer.getClass().getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object result = field.get(minecraftServer);
                    if (result != null) {
                        String resultType = result.getClass().getSimpleName().toLowerCase();
                        if (resultType.contains("server") && resultType.contains("connection")) {
                            serverConnection = result;
                            WSMC.debug("Found potential ServerConnection via last resort: " + field.getName() + " (type: " + result.getClass().getName() + ")");
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        if (serverConnection == null) {
            throw new RuntimeException("Could not find ServerConnection in " + minecraftServer.getClass().getName() + 
                                     ". Available methods: " + getMethodNames(minecraftServer.getClass()) + 
                                     ". Available fields: " + getFieldNames(minecraftServer.getClass()));
        }

        WSMC.debug("Found ServerConnection: " + serverConnection.getClass().getName());

        // Find channels list in ServerConnection
        List<ChannelFuture> channels = null;
        
        // Try to find List<ChannelFuture> fields
        for (Field field : serverConnection.getClass().getDeclaredFields()) {
            if (field.getType() == List.class) {
                try {
                    field.setAccessible(true);
                    Object obj = field.get(serverConnection);
                    if (obj instanceof List) {
                        List<?> list = (List<?>) obj;
                        if (!list.isEmpty() && list.get(0) instanceof ChannelFuture) {
                            channels = (List<ChannelFuture>) list;
                            WSMC.debug("Found channels via field: " + field.getName());
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        
        // If not found, try common field names
        if (channels == null) {
            String[] possibleNames = {"channels", "f", "g", "h", "activeChannels", "boundChannels", "listeningChannels"};
            for (String name : possibleNames) {
                try {
                    Field field = serverConnection.getClass().getDeclaredField(name);
                    if (field.getType() == List.class) {
                        field.setAccessible(true);
                        Object obj = field.get(serverConnection);
                        if (obj instanceof List) {
                            channels = (List<ChannelFuture>) obj;
                            WSMC.debug("Found channels via field name: " + name);
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        if (channels == null) {
            throw new RuntimeException("Could not find channels list");
        }

        WSMC.debug("Found " + channels.size() + " channels");

        // Inject our handler into each channel
        for (ChannelFuture future : channels) {
            try {
                Channel serverChannel = future.channel();
                serverChannel.pipeline().addFirst(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        if (msg instanceof Channel) {
                            Channel childChannel = (Channel) msg;
                            childChannel.pipeline().addFirst(new ChannelInitializer<Channel>() {
                                @Override
                                protected void initChannel(Channel ch) throws Exception {
                                    ch.pipeline().addFirst("wsmc_sniffer", new ProtocolSniffer());
                                    WSMC.debug("Injected WSMC sniffer into new connection");
                                }
                            });
                        }
                        ctx.fireChannelRead(msg);
                    }
                });
                WSMC.debug("Successfully injected into channel: " + serverChannel);
            } catch (Exception e) {
                WSMC.debug("Failed to inject into channel: " + e.getMessage());
            }
        }
    }

    // Helper methods for debugging
    private String getMethodNames(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();
        for (Method m : clazz.getMethods()) {
            sb.append(m.getName()).append(" ");
        }
        return sb.toString();
    }

    private String getFieldNames(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();
        for (Field f : clazz.getDeclaredFields()) {
            sb.append(f.getName()).append(" ");
        }
        return sb.toString();
    }
}