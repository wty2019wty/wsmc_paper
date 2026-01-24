package wsmc.bukkit;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;

import java.util.function.Consumer;

public class HttpServerHandler extends ChannelInboundHandlerAdapter {
    public final static String wsmcEndpoint = System.getProperty("wsmc.wsmcEndpoint", null);
    public final static String maxFramePayloadLength = System.getProperty("wsmc.maxFramePayloadLength", "65536");
    
    // 用于在 Channel 中存储真实 IP 的 AttributeKey
    public static final AttributeKey<String> REAL_IP_KEY = AttributeKey.valueOf("wsmc_real_ip");

    private final Consumer<HttpRequest> onWsmcHandshake;

    public HttpServerHandler(Consumer<HttpRequest> onWsmcHandshake) {
        this.onWsmcHandshake = onWsmcHandshake;
    }

    private boolean isWsmcEndpoint(String endpoint) {
        if (HttpServerHandler.wsmcEndpoint == null)
            return true;
        return HttpServerHandler.wsmcEndpoint.equals(endpoint);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) msg;
            String endpoint = httpRequest.uri();

            WSMC.debug("Http Request Received: " + httpRequest.uri());

            HttpHeaders headers = httpRequest.headers();

            if ("Upgrade".equalsIgnoreCase(headers.get(HttpHeaderNames.CONNECTION))
                    && "WebSocket".equalsIgnoreCase(headers.get(HttpHeaderNames.UPGRADE))
                    && isWsmcEndpoint(endpoint)) {
                
                String host = httpRequest.headers().get("Host");
                if (host == null) {
                    host = "localhost";
                }
                String url = "ws://" + host + httpRequest.uri();
                WSMC.debug("Upgrade to: " + headers.get("Upgrade") + " for: " + url);

                if (this.onWsmcHandshake != null) {
                    this.onWsmcHandshake.accept(httpRequest);
                }
                
                // 尝试提取真实 IP 并存储到 Channel 属性中
                String realIp = headers.get("X-Forwarded-For");
                if (realIp == null) {
                    realIp = headers.get("CF-Connecting-IP");
                }
                if (realIp != null) {
                     // 如果有多个 IP，取第一个
                    if (realIp.contains(",")) {
                        realIp = realIp.split(",")[0].trim();
                    }
                    ctx.channel().attr(REAL_IP_KEY).set(realIp);
                    WSMC.debug("Resolved Real IP: " + realIp);
                }

                // Adding new handler to the existing pipeline to handle WebSocket Messages
                ctx.pipeline().replace(this, "WsmcWebSocketServerHandler", new WebSocketHandler.WebSocketServerHandler());
                
                // 如果获取到了真实 IP，注入 HandshakeInjector
                if (realIp != null) {
                     // HandshakeInjector 必须在 WebSocketHandler 之后，以便拦截解包后的 Minecraft Handshake 包
                     ctx.pipeline().addAfter("WsmcWebSocketServerHandler", "WsmcHandshakeInjector", new HandshakeInjector(realIp));
                     WSMC.debug("Injected HandshakeInjector for IP: " + realIp);
                }

                WSMC.debug("Opened Channel: " + ctx.channel());

                int maxFramePayloadLengthInt = 65536;

                try {
                    maxFramePayloadLengthInt = Integer.parseInt(HttpServerHandler.maxFramePayloadLength);
                } catch (Exception e){
                    WSMC.debug("Unable to parse maxFramePayloadLength, value: " + HttpServerHandler.maxFramePayloadLength);
                }

                // Do the Handshake
                WebSocketServerHandshakerFactory wsFactory =
                        new WebSocketServerHandshakerFactory(url, null, true, maxFramePayloadLengthInt);
                WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(httpRequest);

                if (handshaker == null) {
                    WSMC.info("Unsupported WebSocket version");
                    WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                } else {
                    WSMC.debug("Handshaking starts...");
                    handshaker.handshake(ctx.channel(), httpRequest)
                            .addListener((future) -> WSMC.debug("Handshake is done"));
                }
            } else {
                // Not a WebSocket upgrade request, send a default HTTP response
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK, Unpooled.copiedBuffer("WSMC HTTP Response", CharsetUtil.UTF_8));
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        } else if (msg == LastHttpContent.EMPTY_LAST_CONTENT) {
            WSMC.debug("EMPTY_LAST_CONTENT");
        } else {
            ctx.fireChannelRead(msg);
        }
    }
}
