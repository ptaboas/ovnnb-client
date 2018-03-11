package com.simplyti.cloud.ovn.client.jsonrpc.coders;

import com.jsoniter.output.JsonStream;
import com.simplyti.cloud.ovn.client.jsonrpc.domain.JsonRpcMessage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToByteEncoder;

@Sharable
public class JsonRpcRequestEncoder extends MessageToByteEncoder<JsonRpcMessage>{

	@Override
	protected void encode(ChannelHandlerContext ctx, JsonRpcMessage msg, ByteBuf out) throws Exception {
		JsonStream.serialize(msg, new ByteBufOutputStream(out));
	}

}
