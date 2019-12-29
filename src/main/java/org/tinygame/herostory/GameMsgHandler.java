package org.tinygame.herostory;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.tinygame.herostory.msg.GameMsgProtocol;

import java.util.HashMap;
import java.util.Map;


public class GameMsgHandler extends SimpleChannelInboundHandler<Object> {
    /**
     * 客户端通信组，一定使用static,否则无法实现群发
     */
    private static final ChannelGroup _channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private static final Map<Integer, User> _userMap = new HashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        _channelGroup.add(ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
        _channelGroup.remove(ctx.channel());

        Integer userId = (Integer) ctx.channel().attr(AttributeKey.valueOf("userId")).get();

        if (userId == null) {
            return;
        }
        _userMap.remove(userId);
        GameMsgProtocol.UserQuitResult.Builder resultBuilder = GameMsgProtocol.UserQuitResult.newBuilder();
        resultBuilder.setQuitUserId(userId);
        GameMsgProtocol.UserQuitResult newResult = resultBuilder.build();
        _channelGroup.writeAndFlush(newResult);
    }


    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("收到客户端消息,msgClazz =" + msg.getClass().getName() + ",msg=" + msg);

        if (msg instanceof GameMsgProtocol.UserEntryCmd) {
            GameMsgProtocol.UserEntryCmd cmd = (GameMsgProtocol.UserEntryCmd) msg;
            int userId = cmd.getUserId();
            String heroAvatar = cmd.getHeroAvatar();
            GameMsgProtocol.UserEntryResult.Builder resultBuilder = GameMsgProtocol.UserEntryResult.newBuilder();
            resultBuilder.setUserId(userId);
            resultBuilder.setHeroAvatar(heroAvatar);

            User newUser = new User();
            newUser.userId = userId;
            newUser.heroAvatar = heroAvatar;
            _userMap.put(newUser.userId, newUser);

            ctx.channel().attr(AttributeKey.valueOf("userId")).set(userId);


            GameMsgProtocol.UserEntryResult newResult = resultBuilder.build();
            _channelGroup.writeAndFlush(newResult);
        } else if (msg instanceof GameMsgProtocol.WhoElseIsHereCmd) {
            GameMsgProtocol.WhoElseIsHereResult.Builder resuleBuilder = GameMsgProtocol.WhoElseIsHereResult.newBuilder();
            for (User currUser : _userMap.values()) {
                if (currUser == null) {
                    continue;
                }
                GameMsgProtocol.WhoElseIsHereResult.UserInfo.Builder userInfoBuilder = GameMsgProtocol.WhoElseIsHereResult.UserInfo.newBuilder();
                userInfoBuilder.setUserId(currUser.userId);
                userInfoBuilder.setHeroAvatar(currUser.heroAvatar);
                resuleBuilder.addUserInfo(userInfoBuilder);
            }

            GameMsgProtocol.WhoElseIsHereResult newResult = resuleBuilder.build();
            ctx.writeAndFlush(newResult);
        } else if (msg instanceof GameMsgProtocol.UserMoveToCmd) {
            Integer userId = (Integer) ctx.channel().attr(AttributeKey.valueOf("userId")).get();
            if (userId == null) {
                return;
            }
            GameMsgProtocol.UserMoveToCmd cmd = (GameMsgProtocol.UserMoveToCmd) msg;
            GameMsgProtocol.UserMoveToResult.Builder resultBuilder = GameMsgProtocol.UserMoveToResult.newBuilder();
            resultBuilder.setMoveUserId(userId);
            resultBuilder.setMoveToPosX(cmd.getMoveToPosX());
            resultBuilder.setMoveToPosY(cmd.getMoveToPosY());

            GameMsgProtocol.UserMoveToResult newResult = resultBuilder.build();
            _channelGroup.writeAndFlush(newResult);
        }
    }
}
