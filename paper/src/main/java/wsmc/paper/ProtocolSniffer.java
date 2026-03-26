package wsmc.paper;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
// 1. 替换旧类导入为新类
import wsmc.HttpServerHandlerpaper;
import wsmc.WSMCpaper;
import wsmc.HttpGetSniffer;

import java.util.List;

public class ProtocolSniffer extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 4) {
            return;
        }

        int magic1 = in.getUnsignedByte(in.readerIndex());
        int magic2 = in.getUnsignedByte(in.readerIndex() + 1);
        int magic3 = in.getUnsignedByte(in.readerIndex() + 2);
        int magic4 = in.getUnsignedByte(in.readerIndex() + 3);

        if (isHttp(magic1, magic2, magic3, magic4)) {
            // 2. 替换 WSMC → WSMCpaper
            WSMCpaper.debug("Detected HTTP/WebSocket connection from " + ctx.channel().remoteAddress());
            ctx.pipeline().addAfter(ctx.name(), "wsmc_http_codec", new HttpServerCodec());
            ctx.pipeline().addAfter("wsmc_http_codec", "wsmc_http_aggregator", new HttpObjectAggregator(65536));
            // 3. 替换 HttpServerHandler → HttpServerHandlerpaper
            ctx.pipeline().addAfter("wsmc_http_aggregator", "wsmc_http_handler", new HttpServerHandlerpaper(null));
            ctx.pipeline().remove(this);
        } else {
            if (HttpGetSniffer.disableVanillaTCP) {
                // 4. 替换 WSMC → WSMCpaper
                WSMCpaper.info(ctx.channel().remoteAddress().toString() +
                        " attemps to establish a Vanilla TCP connection which has been disabled by WSMC.");
                ctx.close();
                return;
            }

            // 5. 替换 WSMC → WSMCpaper
            WSMCpaper.debug("Detected Minecraft connection from " + ctx.channel().remoteAddress());
            ctx.pipeline().remove(this);
            out.add(in.readBytes(in.readableBytes()));
        }
    }

    private boolean isHttp(int m1, int m2, int m3, int m4) {
        return m1 == 'G' && m2 == 'E' && m3 == 'T' && m4 == ' ' || // GET
               m1 == 'P' && m2 == 'O' && m3 == 'S' && m4 == 'T' || // POST
               m1 == 'P' && m2 == 'U' && m3 = 'T' && m4 == ' ' || // PUT
               m1 == 'H' && m2 == 'E' && m3 == 'A' && m4 == 'D' || // HEAD
               m1 == 'O' && m2 == 'P' && m3 == 'T' && m4 == 'I' || // OPTIONS
               m1 == 'D' && m2 == 'E' && m3 == 'L' && m4 == 'E' || // DELETE
               m1 == 'T' && m2 == 'R' && m3 == 'A' && m4 == 'C' || // TRACE
               m1 == 'C' && m2 == 'O' && m3 == 'N' && m4 == 'N';   // CONNECT
    }
}