package com.simplyti.cloud.ovn.client.ovsdb.coders;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.simplyti.cloud.ovn.client.OVNNbClient;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBOperationResult;
import com.simplyti.cloud.ovn.client.jsonrpc.domain.JsonRpcResponse;

import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;

@Sharable
public class OVSDBResponseDecoder extends SimpleChannelInboundHandler<JsonRpcResponse>{

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, JsonRpcResponse msg) throws Exception {
		List<OVSDBOperationResult> results = msg.getResult().stream()
			.map(this::toObject)
			.collect(Collectors.toList());
		ctx.channel().attr(OVNNbClient.CONSUMERS).get().remove(msg.getId()).accept(results);
	}
	
	private OVSDBOperationResult toObject(Object ovsObj){
		if (ovsObj instanceof Map){
			Map<?,?> map = (Map<?, ?>) ovsObj;
			if(map.containsKey("error")){
				return OVSDBOperationResult.error((String) map.get("error"));
			}else if(map.containsKey("rows")){
				List<?> elements = (List<?>) map.get("rows");
				@SuppressWarnings("unchecked")
				List<Map<String,Object>> rows = elements.stream()
					.map(element->(Map<String,Object>)element)
					.map(row->row.entrySet().stream()
						.collect(Collectors.toMap(entry->(String)entry.getKey(), entry->transform(entry.getValue()))))
					.collect(Collectors.toList());
				return OVSDBOperationResult.result(rows);
			}else{
				return OVSDBOperationResult.result(Collections.singletonList(map.entrySet().stream()
						.collect(Collectors.toMap(entry->(String)entry.getKey(), entry->transform(entry.getValue())))));
			}
		}else{
			return OVSDBOperationResult.ignored(null);
		}
	}
	
	private Object transform(Object value) {
		if (value instanceof List){
			List<?> list = (List<?>) value;
			if(list.get(0).equals("uuid")){
				return UUID.fromString((String) list.get(1));
			}else if(list.get(0).equals("map")){
				return toMap((List<?>) list.get(1));
			}else if(list.get(0).equals("set")){
				return toList((List<?>) list.get(1));
			}else{
				return value;
			}
		}else{
			return value;
		}
	}
	
	private List<Object> toList(List<?> list) {
		return list.stream().map(this::transform).collect(Collectors.toList());
	}

	private Map<String,Object> toMap(List<?> entries) {
		return entries.stream().map(List.class::cast)
			.collect(Collectors.toMap(l->(String)l.get(0), l->transform(l.get(1))));
	}
	
}
