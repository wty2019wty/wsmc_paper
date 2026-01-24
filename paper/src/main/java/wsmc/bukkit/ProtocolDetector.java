package wsmc.bukkit;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import java.util.List;

public class ProtocolDetector extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // Debug logging
        if (WSMC.debug()) {
            int len = in.readableBytes();
            StringBuilder hex = new StringBuilder();
            // Create a copy to read without modifying index
            ByteBuf copy = in.duplicate(); 
            for (int i = 0; i < Math.min(len, 10); i++) {
                hex.append(String.format("%02X ", copy.readUnsignedByte()));
            }
            WSMC.debug("ProtocolDetector: Received " + len + " bytes from " + ctx.channel().remoteAddress());
            WSMC.debug("Header: " + hex.toString());
            WSMC.debug("Pipeline: " + ctx.pipeline().names());
        }

        // 至少需要几个字节来判断
        if (in.readableBytes() < 3) {
            return;
        }
        
        // 标记当前位置，以便后面重置或读取
        in.markReaderIndex();
        
        int magic1 = in.readUnsignedByte();
        int magic2 = in.readUnsignedByte();
        int magic3 = in.readUnsignedByte();
        
        in.resetReaderIndex();
        
        boolean isHttp = 
            (magic1 == 'G' && magic2 == 'E' && magic3 == 'T') || // GET
            (magic1 == 'P' && magic2 == 'O' && magic3 == 'S') || // POST
            (magic1 == 'H' && magic2 == 'E' && magic3 == 'A') || // HEAD
            (magic1 == 'O' && magic2 == 'P' && magic3 == 'T') || // OPTIONS
            (magic1 == 'C' && magic2 == 'O' && magic3 == 'N');   // CONNECT
            
        if (isHttp) {
            WSMC.debug("Detected HTTP/WebSocket connection from " + ctx.channel().remoteAddress());
            
            // 动态调整 Pipeline
            ctx.pipeline().addAfter(ctx.name(), "HttpServerCodec", new HttpServerCodec());
            ctx.pipeline().addAfter("HttpServerCodec", "HttpObjectAggregator", new HttpObjectAggregator(65536));
            ctx.pipeline().addAfter("HttpObjectAggregator", "HttpServerHandler", new HttpServerHandler(null));
            
            // 移除探测器
            ctx.pipeline().remove(this);
            
            // 将当前累积的数据作为 ByteBuf 输出给下一个 Handler (HttpServerCodec)
            // retain() 是必须的，因为 readBytes 会增加引用计数，而 ByteToMessageDecoder 会在 decode 返回后尝试 release 输入 buf
            // 但在这里我们实际上是把输入 buf 的内容传递出去了。
            // 正确做法：ByteToMessageDecoder 会自动 release 'in'。如果我们要把它传给下一个，需要 out.add。
            // out.add(in.readBytes(in.readableBytes())); 
            // 这会创建一个新的 ByteBuf (slice or copy depending on implementation, usually sliced retained).
            
            out.add(in.readBytes(in.readableBytes()));
        } else {
            WSMC.debug("Detected standard TCP connection from " + ctx.channel().remoteAddress());
            ctx.pipeline().remove(this);
            // 对于非 HTTP，也需要把数据传下去
            out.add(in.readBytes(in.readableBytes()));
        }
    }
}
