package wsmc.paper;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
// 1. 修正导入：WSMC → WSMCpaper
import wsmc.WSMCpaper;

import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.util.Map;

public class IpInjection {

    private static Field addressField;

    public static void inject(Channel channel, SocketAddress newAddress) {
        try {
            ChannelHandler connectionHandler = findConnectionHandler(channel);
            
            if (connectionHandler == null) {
                // Handler might not be added yet, let's wait a bit
                // 2. 替换 WSMC → WSMCpaper
                WSMCpaper.debug("packet_handler not found immediately. Scheduling retry...");
                channel.eventLoop().execute(() -> {
                    try {
                        ChannelHandler handler = findConnectionHandler(channel);
                        if (handler != null) {
                            setAddress(handler, newAddress);
                        } else {
                            // 3. 替换 WSMC → WSMCpaper
                            WSMCpaper.debug("Could not find packet_handler even after waiting. Pipeline: " + channel.pipeline().names());
                        }
                    } catch (Exception e) {
                        // 4. 替换 WSMC → WSMCpaper
                        WSMCpaper.debug("Error while injecting IP after delay: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                return;
            }
            setAddress(connectionHandler, newAddress);
        } catch (Exception e) {
            // 5. 替换 WSMC → WSMCpaper
            WSMCpaper.debug("Error while injecting IP: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static ChannelHandler findConnectionHandler(Channel channel) {
        ChannelHandler connectionHandler = channel.pipeline().get("packet_handler");
        if (connectionHandler == null) {
            connectionHandler = channel.pipeline().get("nethandler"); // Forge/Some versions
        }
        
        if (connectionHandler == null) {
            // Try to find the handler by type or name pattern
            for (Map.Entry<String, ChannelHandler> entry : channel.pipeline()) {
                String name = entry.getKey();
                ChannelHandler handler = entry.getValue();
                String className = handler.getClass().getName();
                
                if (name.contains("packet") || name.contains("connection") || 
                    className.contains("Connection") || className.contains("NetworkManager")) {
                    return handler;
                }
            }
        }
        return connectionHandler;
    }

    private static void setAddress(Object connection, SocketAddress newAddress) throws Exception {
        if (addressField == null) {
            addressField = findAddressField(connection.getClass());
        }
        
        try {
            addressField.set(connection, newAddress);
            // 6. 替换 WSMC → WSMCpaper
            WSMCpaper.debug("Successfully injected IP: " + newAddress);
        } catch (IllegalAccessException e) {
            // 7. 替换 WSMC → WSMCpaper
            WSMCpaper.debug("Failed to set address field. You might need JVM argument: --add-opens java.base/java.net=ALL-UNNAMED or similar.");
            throw e;
        }
    }

    private static Field findAddressField(Class<?> connectionClass) throws NoSuchFieldException {
        // Try common names first
        String[] fieldNames = {"address", "socketAddress", "remoteAddress", "k", "l", "m", "n"}; 
        
        for (String name : fieldNames) {
            try {
                Field field = getField(connectionClass, name);
                if (field != null && SocketAddress.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    // 8. 替换 WSMC → WSMCpaper
                    WSMCpaper.debug("Found address field by name: " + name);
                    return field;
                }
            } catch (Exception ignored) {}
        }

        // Fallback: search for any SocketAddress field
        Class<?> current = connectionClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (SocketAddress.class.isAssignableFrom(field.getType()) && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    // 9. 替换 WSMC → WSMCpaper
                    WSMCpaper.debug("Found address field by type: " + field.getName() + " in " + current.getName());
                    return field;
                }
            }
            current = current.getSuperclass();
        }

        throw new NoSuchFieldException("Could not find SocketAddress field in " + connectionClass.getName());
    }
    
    private static Field getField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            Class<?> parent = clazz.getSuperclass();
            if (parent != null && parent != Object.class) {
                return getField(parent, name);
            }
        }
        return null;
    }
}