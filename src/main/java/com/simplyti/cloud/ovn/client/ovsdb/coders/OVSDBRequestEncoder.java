package com.simplyti.cloud.ovn.client.ovsdb.coders;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBDeleteRequest;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBInsertRequest;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBMutateRequest;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBOperationRequest;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBSelectRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSRequest;
import com.simplyti.cloud.ovn.client.jsonrpc.domain.JsonRpcRequest;
import com.simplyti.cloud.ovn.client.mutation.Mutation;
import com.google.common.collect.ImmutableSet;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToMessageEncoder;

@Sharable
public class OVSDBRequestEncoder extends MessageToMessageEncoder<OVSRequest>{
	
	private final ObjectMapper mapper;
	
	public OVSDBRequestEncoder(){
		this.mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, OVSRequest msg, List<Object> out) throws Exception {
		JsonRpcRequest jsonRpcRequest = new JsonRpcRequest(msg.getId(), msg.getMethod().getName(), toParams(msg));
		out.add(jsonRpcRequest);
	}

	private Set<Object> toParams(OVSRequest msg) {
		return ImmutableSet.builder()
			.add(msg.getDbName())
			.addAll(toParams(msg.getOperations())).build();
	}

	private Set<Object> toParams(Collection<OVSDBOperationRequest> operations) {
		return operations.stream().map(operation->{
			if(operation instanceof OVSDBInsertRequest){
				OVSDBInsertRequest insert = (OVSDBInsertRequest) operation;
				return builder(insert)
					.put("row",buildObject(insert.getRow())).build();
			}else if(operation instanceof OVSDBSelectRequest){
				OVSDBSelectRequest select = (OVSDBSelectRequest) operation;
				return builder(select)
					.put("where",select.getCriteria().stream()
							.map(criteria->ImmutableSet.of(criteria.getColumn(),criteria.getCondition().getOperand(),toOVSObject(criteria.getValue())))
							.collect(Collectors.toSet()))
					.build();
			}else if(operation instanceof OVSDBMutateRequest){
				OVSDBMutateRequest mutate = (OVSDBMutateRequest) operation;
				return builder(mutate)
						.put("where",where(mutate.getCriteria()))
						.put("mutations",mutations(mutate.getMutations()))
						.build();
			}else if(operation instanceof OVSDBDeleteRequest){
				OVSDBDeleteRequest delete = (OVSDBDeleteRequest) operation;
				return builder(delete)
					.put("where",where(delete.getCriteria()))
					.build();
			}else{
				throw new IllegalStateException("Unknown operation type "+operation.getClass());
			}
		}).collect(Collectors.toSet());
	}

	private Object mutations(Collection<Mutation> mutations) {
		return mutations.stream()
				.map(mutation->ImmutableSet.of(mutation.getField(),mutation.getMutator().getOperand(),toOVSObject(mutation.getValue())))
				.collect(Collectors.toSet());
	}

	private Set<Set<?>> where(Collection<Criteria> criterias) {
		return criterias.stream()
				.map(criteria->ImmutableSet.of(criteria.getColumn(),criteria.getCondition().getOperand(),toOVSObject(criteria.getValue())))
				.collect(Collectors.toSet());
	}

	private Builder<String, Object> builder(OVSDBOperationRequest operation) {
		return ImmutableMap.<String, Object>builder()
				.put("op",operation.getOperation().getName())
				.put("table",operation.getTable());
	}
	
	private Object buildObject(Object row) {
		Map<String,?> mapRow = mapper.convertValue(row, new TypeReference<Map<String,?> >() {});
		Builder<String, Object> builder = ImmutableMap.<String,Object>builder();
		mapRow.entrySet().forEach(column->{
			builder.put(column.getKey(),toOVSObject(column.getValue()));
		});
		return builder.build();
	}

	private Object toOVSObject(Object value) {
		if(value instanceof Map){
			Map<?,?> map = (Map<?, ?>) value;
			return ImmutableSet.of("map",
					map.entrySet().stream()
							.map(entry->Arrays.asList(entry.getKey().toString(),toOVSObject(entry.getValue())))
							.collect(Collectors.toSet()));
		} else if(value instanceof Iterable){
			Iterable<?> iterable = (Iterable<?>) value;
			return ImmutableSet.of("set",StreamSupport.stream(iterable.spliterator(),false)
					.map(this::toOVSObject)
					.collect(Collectors.toSet()));
		}else if(value instanceof UUID){
			UUID uuid = (UUID) value;
			return ImmutableSet.of("uuid",uuid.toString());
		}else{
			return value;
		}
	}

}
