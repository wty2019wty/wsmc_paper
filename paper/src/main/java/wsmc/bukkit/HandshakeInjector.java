package wsmc.bukkit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;

public class HandshakeInjector extends ChannelInboundHandlerAdapter {
    private final String realIp;
    private boolean injected = false;

    public HandshakeInjector(String realIp) {
        this.realIp = realIp;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!injected && msg instanceof ByteBuf) {
            ByteBuf in = (ByteBuf) msg;
            in.markReaderIndex();
            
            try {
                // 读取 Length (VarInt)
                if (!canReadVarInt(in)) {
                    in.resetReaderIndex();
                    ctx.fireChannelRead(msg);
                    return;
                }
                int length = readVarInt(in);
                
                if (in.readableBytes() < 1) { // 至少要有 Packet ID
                     in.resetReaderIndex();
                     ctx.fireChannelRead(msg);
                     return;
                }
                
                int id = readVarInt(in);
                
                if (id == 0x00) { // Handshake Packet ID
                    int protocolVersion = readVarInt(in);
                    String host = readString(in);
                    int port = in.readUnsignedShort();
                    int state = readVarInt(in);
                    
                    // 构建新的 Host (BungeeCord 格式)
                    // 格式: host\00ip\00uuid\00properties
                    // 使用一个随机 UUID 或者基于 IP 的 UUID
                    String uuid = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + realIp).getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
                    String newHost = host + "\00" + realIp + "\00" + uuid;
                    
                    WSMC.debug("Injecting IP into Handshake. Original: " + host + ", New: " + newHost);
                    
                    ByteBuf newPacket = Unpooled.buffer();
                    writeVarInt(newPacket, 0x00); // Packet ID
                    writeVarInt(newPacket, protocolVersion);
                    writeString(newPacket, newHost);
                    newPacket.writeShort(port);
                    writeVarInt(newPacket, state);
                    
                    ByteBuf out = Unpooled.buffer();
                    writeVarInt(out, newPacket.readableBytes()); // Length
                    out.writeBytes(newPacket);
                    newPacket.release();
                    
                    // 释放原始包，传递新包
                    in.release();
                    ctx.fireChannelRead(out);
                    
                    // 任务完成，移除自己
                    ctx.pipeline().remove(this);
                    injected = true;
                    return;
                } 
            } catch (Exception e) {
                WSMC.debug("Error parsing handshake: " + e.getMessage());
            }
            
            // 如果不是 Handshake 或解析失败，重置并透传
            in.resetReaderIndex();
        }
        
        ctx.fireChannelRead(msg);
    }
    
    // 检查是否包含完整的 VarInt
    private boolean canReadVarInt(ByteBuf buf) {
        int remaining = buf.readableBytes();
        int index = buf.readerIndex();
        for (int i = 0; i < 5; i++) {
            if (i >= remaining) return false;
            if ((buf.getByte(index + i) & 0x80) == 0) return true;
        }
        return false; // VarInt 太长
    }

    public static int readVarInt(ByteBuf buf) {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = buf.readByte();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));
            numRead++;
            if (numRead > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((read & 0b10000000) != 0);
        return result;
    }

    public static String readString(ByteBuf buf) {
        int len = readVarInt(buf);
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeVarInt(ByteBuf buf, int value) {
        do {
            byte temp = (byte)(value & 0b01111111);
            value >>>= 7;
            if (value != 0) {
                temp |= 0b10000000;
            }
            buf.writeByte(temp);
        } while (value != 0);
    }

    public static void writeString(ByteBuf buf, String string) {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }
}
