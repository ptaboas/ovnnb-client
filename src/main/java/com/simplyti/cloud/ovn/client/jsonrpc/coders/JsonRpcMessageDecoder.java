package com.simplyti.cloud.ovn.client.jsonrpc.coders;

import java.util.List;
import java.util.Map;

import com.jsoniter.JsonIterator;
import com.jsoniter.spi.TypeLiteral;
import com.simplyti.cloud.ovn.client.jsonrpc.domain.JsonRpcRequest;
import com.simplyti.cloud.ovn.client.jsonrpc.domain.JsonRpcResponse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToMessageDecoder;

@Sharable
public class JsonRpcMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

	private static final TypeLiteral<Map<String,Object>> MAP = new TypeLiteral<Map<String,Object>>(){};

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
		byte[] data = toBytes(msg);
		Map<String,Object> message = JsonIterator.deserialize(data, MAP);
		if(message.containsKey("method")){
			out.add(JsonIterator.deserialize(data, JsonRpcRequest.class));
		}else{
			out.add(JsonIterator.deserialize(data, JsonRpcResponse.class));
		}
	}

	private byte[] toBytes(ByteBuf msg) {
		byte[] data = new byte[msg.readableBytes()];
		msg.readBytes(data);
		return data;
	}
	
}
