package Handling;

import Connector.Client.ConnectClient;
import Connector.Opcode.RecvOpcodePacket;
import Connector.Opcode.SendOpcodePacket;
import Connector.Server.ChattingJoinList;
import DataBase.DAO;
import Packet.MessengerReadPacket;
import Packet.MessengerSendPacket;
import UserException.ResultHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;


public class MessengerHandler {
    public static void PacketHandler(final RecvOpcodePacket opcode, ChannelHandlerContext ctx, MessengerReadPacket slea) throws Exception {
        switch (opcode) {
            // 로그인
            case LOGIN_MESSENGER: {
                String email = slea.readString();
                String password = slea.readString();
                MessengerSendPacket sp = new MessengerSendPacket();
                sp.writeShort(SendOpcodePacket.LOGIN_MESSENGER.getValue());
                if (DAO.canLogin(email, password) == ResultHandler.Success) {
                    final Integer uid = DAO.getAccountUid(email);
                    final int port = Integer.parseInt(ctx.channel().remoteAddress().toString().split(":")[1]);
                    if (!ConnectClient.connect_client_uid.containsKey(uid)) {  // 서버에 접속중인 uid가 아니면
                        ArrayList<Long> temp = DAO.getChattingList(email);
                        if (temp != null) {
                            for (var item : temp)
                                ChattingJoinList.enterChatting(item, ctx);

                        }
                        ConnectClient.connect_client_uid.put(uid, ctx);
                        ConnectClient.connect_client_port.put(port, uid);
                        System.out.println(ctx.channel().remoteAddress().toString() + "님이 " + email + " 로 로그인하였습니다."); // TODO: 콘솔알림
                        sp.writeShort(1);  // 접속 성공
                    } else {
                        sp.writeShort(-1);  // 이미 접속중인 계정
                    }
                } else {
                    sp.writeShort(0);
                }
                ctx.writeAndFlush(sp.getByteBuf());
                break;
            }

            // 로그아웃
            case LOGOUT_MESSENGER: {
                MessengerSendPacket sp = new MessengerSendPacket();
                sp.writeShort(SendOpcodePacket.LOGOUT_MESSENGER.getValue());
                Integer account_uid = ConnectClient.getUid(ctx);
                if (account_uid != null) {
                    ArrayList<Long> temp = DAO.getChattingList(account_uid);
                    if (temp != null) {
                        for (var item : temp) {
                            ChattingJoinList.exitChatting(item, ctx);
                        }
                    }
                }

                if (ConnectClient.logout(ctx) == ResultHandler.Success) {
                    System.out.println(ctx.channel().remoteAddress().toString() + "님이 로그아웃하셨습니다.");
                    sp.writeShort(1);
                } else {
                    sp.writeShort(-1);
                }
                ctx.writeAndFlush(sp.getByteBuf());
                break;
            }

            // 친구추가
            case INSERT_FIREND: {
                int my_uid = slea.readInt();
                int friend_uid = slea.readInt();

                MessengerSendPacket sp = new MessengerSendPacket();
                sp.writeShort(SendOpcodePacket.INSERT_FIREND.getValue());
                if (DAO.insertFriend(my_uid, friend_uid) == ResultHandler.Success) {
                    sp.writeShort(1);
                } else {
                    sp.writeShort(0);
                }
            }

            // 채팅방 생성
            case INSERT_CHATTING: {
                int my_uid = slea.readInt();
                String title = slea.readString();
                MessengerSendPacket sp = new MessengerSendPacket();
                sp.writeShort(SendOpcodePacket.INSERT_CHATTING.getValue());
                if (DAO.insertChatting(my_uid, title) == ResultHandler.Success) {
                    Long chatting_uid = DAO.getChattingUid(my_uid, title);
                    // TODO: if Create, ServerInfo Update
                    if (chatting_uid != null) {
                        if (ChattingJoinList.createChatting(chatting_uid) == ResultHandler.Success) {
                            sp.writeShort(1);
                            sp.writeLong(chatting_uid);
                        } else {
                            sp.writeShort(0);
                        }
                    } else {
                        sp.writeShort(0);
                    }
                } else {
                    sp.writeShort(0);
                }
                ctx.writeAndFlush(sp.getByteBuf());
            }

            // 채팅방 삭제
            case DELETE_CHATTING: {
                long chatting_uid = slea.readInt();
                MessengerSendPacket sp = new MessengerSendPacket();
                sp.writeShort(SendOpcodePacket.DELETE_CHATTING.getValue());
                if (DAO.deleteChatting(chatting_uid) == ResultHandler.Success) {
                    // TODO: if Delete, ServerInfo Update
                    if (ChattingJoinList.deleteChatting(chatting_uid) == ResultHandler.Success) {
                        sp.writeShort(1);
                    } else {
                        sp.writeShort(0);
                    }
                } else {
                    sp.writeShort(0);
                }

                ctx.writeAndFlush(sp.getByteBuf());
            }

            // 채팅방 입장
            case INSERT_CHATTING_JOIN: {
                long chatting_uid = slea.readLong();
                int account_uid = slea.readInt();
                MessengerSendPacket sp = new MessengerSendPacket();
                sp.writeShort(SendOpcodePacket.INSERT_CHATTING_JOIN.getValue());
                // TODO: user enter시 각방에 notice해야함.
                if (DAO.insertChattingJoin(chatting_uid, account_uid) == ResultHandler.Success) {
                    // TODO: if Join, ServerInfo Update
                    if (ChattingJoinList.enterChatting(chatting_uid, ctx) == ResultHandler.Success) {
                        sp.writeShort(1);
                    } else {
                        sp.writeShort(0);
                    }
                } else {
                    sp.writeShort(0);
                }
                ctx.writeAndFlush(sp.getByteBuf());
            }

            // 채팅방 나가기
            case DELETE_CHATTING_JOIN: {
                long chatting_uid = slea.readLong();
                int account_uid = slea.readInt();
                MessengerSendPacket sp = new MessengerSendPacket();
                sp.writeShort(SendOpcodePacket.DELETE_CHATTING_JOIN.getValue());
                // TODO: user exit시 각방에 notice해야함.
                if (DAO.deleteChattingJoin(chatting_uid, account_uid) == ResultHandler.Success) {
                    // TODO: if Exit, ServerInfo Update

                    if (ChattingJoinList.exitChatting(chatting_uid, ctx) == ResultHandler.Success) {
                        sp.writeShort(1);
                    } else {
                        sp.writeShort(0);
                    }
                } else {
                    sp.writeShort(0);
                }
                ctx.writeAndFlush(sp.getByteBuf());
            }

            // 채팅방에 글쓰기
            case INSERT_CHATTING_LOG: {
                long chatting_uid = slea.readLong();
                int account_uid = slea.readInt();
                String body = slea.readString();
                MessengerSendPacket sp = new MessengerSendPacket();
                sp.writeShort(SendOpcodePacket.INSERT_CHATTING_LOG.getValue());

                if (DAO.insertChattingLog(chatting_uid, account_uid, body) == ResultHandler.Success) {
                    sp.writeShort(1);
                    // 밑에는 채팅방 전원에게 메세지를 보내는 부분
                    var users = ChattingJoinList.getCtx(chatting_uid);
                    if (users != null) {
                        MessengerSendPacket sp1 = new MessengerSendPacket();
                        sp1.writeShort(SendOpcodePacket.SEND_MESSAGE_GERNAL_CHAT.getValue());
                        sp1.writeLong(chatting_uid);
                        sp1.writeString(body);
                        for (var user : users) {
                            try {
                                user.writeAndFlush(sp1.getByteBuf());
                            } catch (Exception ignored) {
                            }
                        }
                    }
                } else {
                    sp.writeShort(0);
                }

                ctx.writeAndFlush(sp.getByteBuf());
            }

            // 이메일로 휘원탈퇴하기
            case DELETE_ACCOUNT_EMAIL: {
                String email = slea.readString();
                String password = slea.readString();
                int bd_year = slea.readShort();
                int bd_month = slea.readByte();
                int bd_day = slea.readByte();
                String phone = slea.readString();
                MessengerSendPacket sp = new MessengerSendPacket();
                sp.writeShort(SendOpcodePacket.DELETE_ACCOUNT_EMAIL.getValue());

                if (DAO.canAccountInfo(email, password, bd_year, bd_month, bd_day, phone) == ResultHandler.Success) {
                    // 회원정보가 맞으면
                    if (DAO.deleteAccount(email) == ResultHandler.Success) {
                        // 회원정보를 삭제하였으면
                        sp.writeShort(1);
                    } else {
                        // 삭제에 실패했으면
                        sp.writeShort(0);
                    }
                } else {
                    // 회원정보가 아니면
                    sp.writeShort(0);
                }
                ctx.writeAndFlush(sp.getByteBuf());
            }

            // 친구삭제
            case DELETE_FRIEND: {
                int my_uid = slea.readInt();
                int friend_uid = slea.readInt();

                MessengerSendPacket sp = new MessengerSendPacket();
                sp.writeShort(SendOpcodePacket.DELETE_FRIEND.getValue());

                if (DAO.deleteFriend(my_uid, friend_uid) == ResultHandler.Success) {
                    sp.writeShort(1);
                } else {
                    sp.writeShort(0);
                }
                ctx.writeAndFlush(sp.getByteBuf());
            }


            default: {
                System.out.println("Unknown Opcode : " + opcode);
                break;
            }
        }

    }
}
