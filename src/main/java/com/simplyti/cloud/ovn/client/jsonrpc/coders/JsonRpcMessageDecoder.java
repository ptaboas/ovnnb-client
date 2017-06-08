package com.simplyti.cloud.ovn.client.jsonrpc.coders;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplyti.cloud.ovn.client.jsonrpc.domain.JsonRpcRequest;
import com.simplyti.cloud.ovn.client.jsonrpc.domain.JsonRpcResponse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToMessageDecoder;

@Sharable
public class JsonRpcMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

	private final ObjectMapper mapper = new ObjectMapper();

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
		Map<String,?> message = mapper.readValue((InputStream)new ByteBufInputStream(msg), new TypeReference<Map<String,?>>() {});
		if(message.containsKey("method")){
			out.add(mapper.convertValue(message, JsonRpcRequest.class));
		}else{
			out.add(mapper.convertValue(message, JsonRpcResponse.class));
		}
	}
	
}
