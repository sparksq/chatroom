package com.chat.im.im_chatserver.manager;

import com.chat.im.im_common.entity.entity.Invocation;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.util.AttributeKey;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
/**
 * @author: chovychan in 2022/4/27
 */
@Component
@Log4j2
public class NettyChannelManager {

    /**
     * {@link Channel#attr(AttributeKey)} 属性中，表示 Channel 对应的用户
     */
    private static final AttributeKey<String> CHANNEL_ATTR_KEY_USER = AttributeKey.newInstance("user");

    /**
     * Channel 映射
     */
    private final ConcurrentMap<ChannelId, Channel> channels = new ConcurrentHashMap<>();
    /**
     * 用户与 Channel 的映射。
     * 通过它, 可以获取用户对应的 Channel, 可以向指定用户发送消息。
     */
    private final ConcurrentMap<String, Channel> userChannels = new ConcurrentHashMap<>();

    /**
     * 添加 Channel 到 {@link #channels} 中
     *
     * @param channel Channel
     */
    public void add(Channel channel) {
        channels.put(channel.id(), channel);
        log.info("[add][一个连接({})加入]", channel.id());
    }

    /**
     * 添加指定用户到 {@link #userChannels} 中
     *
     * @param channel Channel
     * @param user 用户
     */
    public void addUser(Channel channel, String user) {
        Channel existChannel = channels.get(channel.id());
        if (existChannel == null) {
            log.error("[addUser][连接({}) 不存在]", channel.id());
            return;
        }
        // 设置属性
        channel.attr(CHANNEL_ATTR_KEY_USER).set(user);
        // 添加到 userChannels
        userChannels.put(user, channel);
    }

    public void remove(Channel channel) {
        // 移除 channels
        channels.remove(channel.id());
        // 移除 userChannels
        if (channel.hasAttr(CHANNEL_ATTR_KEY_USER)) {
            userChannels.remove(channel.attr(CHANNEL_ATTR_KEY_USER).get());
        }
        log.info("[remove][一个连接({})离开]", channel.id());
    }

    /**
     * 向指定用户发送消息
     *
     * @param user 用户
     * @param invocation 消息体
     */
    public void send(String user, Invocation invocation) {
        // 获得用户对应的 Channel
        Channel channel = userChannels.get(user);
        if (channel == null) {
            log.error("[send][连接不存在]");
            return;
        }
        if (!channel.isActive()) {
            log.error("[send][连接({})未激活]", channel.id());
            return;
        }
        // 发送消息
        channel.writeAndFlush(invocation);
    }

    /**
     * 向所有用户发送消息
     *
     * @param invocation 消息体
     */
    public void sendAll(Invocation invocation) {
        for (Channel channel : channels.values()) {
            if (!channel.isActive()) {
                log.error("[send][连接({})未激活]", channel.id());
                return;
            }
            // 发送消息
            channel.writeAndFlush(invocation);
        }
    }
}