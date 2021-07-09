package Server.Netty;

import Packet.ReadPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@Sharable  //중요!
public class ServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    // 접속
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        System.out.println(ctx.channel().remoteAddress().toString().split(":")[0] + " (이)가 접속함.");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf slea) throws Exception {
        byte[] bs = new byte[slea.writerIndex()];
        slea.getBytes(0, bs);
        ReadPacket packet = new ReadPacket(bs);
        System.out.println(packet.readShort());
        System.out.println(packet.readString());
        System.out.println(packet.readString());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception{
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    // 종료
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        System.out.println("누군가 종료함");
    }
    // 종료 이벤트
//    @Override
//    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
//        super.channelUnregistered(ctx);
//        System.out.println("누군가 종료함");
//    }

}