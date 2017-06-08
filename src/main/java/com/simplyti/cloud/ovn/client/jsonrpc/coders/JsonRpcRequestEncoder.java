package com.simplyti.cloud.ovn.client.jsonrpc.coders;

import java.io.OutputStream;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplyti.cloud.ovn.client.jsonrpc.domain.JsonRpcMessage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToByteEncoder;

@Sharable
public class JsonRpcRequestEncoder extends MessageToByteEncoder<JsonRpcMessage>{

	private final ObjectMapper mapper;
	
	public JsonRpcRequestEncoder(){
		this.mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, JsonRpcMessage msg, ByteBuf out) throws Exception {
		mapper.writeValue((OutputStream)new ByteBufOutputStream(out), msg);
	}

}
