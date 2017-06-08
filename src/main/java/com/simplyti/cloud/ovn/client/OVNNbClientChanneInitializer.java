package com.simplyti.cloud.ovn.client;

import com.simplyti.cloud.ovn.client.jsonrpc.coders.JsonRpcMessageDecoder;
import com.simplyti.cloud.ovn.client.jsonrpc.coders.JsonRpcRequestEncoder;
import com.simplyti.cloud.ovn.client.ovsdb.coders.OVSDBRequestDecoder;
import com.simplyti.cloud.ovn.client.ovsdb.coders.OVSDBRequestEncoder;
import com.simplyti.cloud.ovn.client.ovsdb.coders.OVSDBResponseDecoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class OVNNbClientChanneInitializer extends ChannelInitializer<SocketChannel> {
	
	private final JsonRpcRequestEncoder jsonRpcRequestEncoder;
	
	private final JsonRpcMessageDecoder jsonRpcRequestDecoder;
	
	private final OVSDBRequestEncoder ovsDBRequestEncoder;
	
	private final OVSDBResponseDecoder ovsDBResponseDecoder;
	
	private final OVSDBRequestDecoder ovsDBRequestDecoder;

	private final LoggingHandler logging;

	private boolean vervose;
	
	public OVNNbClientChanneInitializer(boolean vervose){
		this.vervose=vervose;
		logging = new LoggingHandler(LogLevel.INFO);
		jsonRpcRequestEncoder = new JsonRpcRequestEncoder();
		jsonRpcRequestDecoder = new JsonRpcMessageDecoder();
		ovsDBRequestEncoder = new OVSDBRequestEncoder();
		ovsDBResponseDecoder = new OVSDBResponseDecoder();
		ovsDBRequestDecoder = new OVSDBRequestDecoder();
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		if(vervose){
			ch.pipeline().addLast(logging);
		}
		
		ch.pipeline().addLast(new JsonObjectDecoder());
		ch.pipeline().addLast(jsonRpcRequestEncoder);
		ch.pipeline().addLast(jsonRpcRequestDecoder);
		
		ch.pipeline().addLast(ovsDBRequestEncoder);
		ch.pipeline().addLast(ovsDBRequestDecoder);
		ch.pipeline().addLast(ovsDBResponseDecoder);
	} 

}
