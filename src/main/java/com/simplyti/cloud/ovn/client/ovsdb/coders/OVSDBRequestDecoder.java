package com.simplyti.cloud.ovn.client.ovsdb.coders;

import java.util.Collections;
import java.util.List;

import com.simplyti.cloud.ovn.client.jsonrpc.domain.JsonRpcRequest;
import com.simplyti.cloud.ovn.client.jsonrpc.domain.JsonRpcResponse;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToMessageDecoder;

@Sharable
public class OVSDBRequestDecoder extends MessageToMessageDecoder<JsonRpcRequest>{

	@Override
	protected void decode(ChannelHandlerContext ctx, JsonRpcRequest msg, List<Object> out) throws Exception {
		if(msg.getId().equals("echo")){
			ctx.channel().writeAndFlush(new JsonRpcResponse(msg.getId(),Collections.emptyList(),null));
		}
	}

}
