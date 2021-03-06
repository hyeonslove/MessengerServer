package Server.Netty;

import Connector.Client.ConnectClient;
import Connector.Opcode.RecvOpcodePacket;
import Connector.Server.ChattingJoinList;
import DataBase.DAO;
import Handling.MessengerHandler;
import Packet.MessengerReadPacket;
import Packet.RecvPacketManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.ArrayList;

@Sharable  //중요!
public class ServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    // 접속
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        System.out.println(ctx.channel().remoteAddress().toString() + " 님이 접속함.");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf slea) throws Exception {
        MessengerReadPacket packet = RecvPacketManager.getPacket(ctx, slea);
        if (packet != null) {
            final int header = packet.readUShort();
            for (final RecvOpcodePacket recv : RecvOpcodePacket.values()) {
                if (recv.getValue() == header) {
                    try {
                        MessengerHandler.PacketHandler(recv, ctx, packet);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    /* 모든 채널에서 패킷 읽기를 완료했을 때 */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    // 종료
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        // 서버 채팅 목록 정보에서 삭제하기.
        Integer account_uid = ConnectClient.getUid(ctx);
        if (account_uid != null) {
            ArrayList<Long> temp = DAO.getChattingList(account_uid);
            if (temp != null) {
                for (var item : temp) {
                    ChattingJoinList.exitChatting(item, ctx);
                }
            }
        }

        ConnectClient.logout(ctx); // 서버 접속정보에서 삭제하기
        System.out.println(ctx.channel().remoteAddress().toString() + "님이 종료하셨습니다.");
    }
    // 종료 이벤트
//    @Override
//    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
//        super.channelUnregistered(ctx);
//    }

}