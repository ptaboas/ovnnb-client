package com.simplyti.cloud.ovn.client.ovsdb.coders;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.domain.annotations.MapField;
import com.simplyti.cloud.ovn.client.domain.db.NamedUUID;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBDeleteRequest;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBInsertRequest;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBMutateRequest;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBOperationRequest;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBSelectRequest;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBUpdateRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSRequest;
import com.simplyti.cloud.ovn.client.jsonrpc.domain.JsonRpcRequest;
import com.simplyti.cloud.ovn.client.mutation.Mutation;
import com.simplyti.cloud.ovn.client.domain.annotations.Column;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToMessageEncoder;

@Sharable
public class OVSDBRequestEncoder extends MessageToMessageEncoder<OVSRequest>{
	
	private final TypeResolver typeResolver;
	
	public OVSDBRequestEncoder(){
		this.typeResolver = new TypeResolver();
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, OVSRequest msg, List<Object> out) throws Exception {
		JsonRpcRequest jsonRpcRequest = new JsonRpcRequest(msg.getId(), msg.getMethod().getName(), toParams(msg));
		out.add(jsonRpcRequest);
	}

	private List<Object> toParams(OVSRequest msg) {
		return msg.getParams().stream().map(this::toOVSObject).collect(Collectors.toList());
	}

	private Object toParams(OVSDBOperationRequest operation) {
			if(operation instanceof OVSDBInsertRequest){
				OVSDBInsertRequest insert = (OVSDBInsertRequest) operation;
				return addNamedUUID(builder(insert)
					.put("row",buildObject(insert.getRow())),insert).build();
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
			}else if(operation instanceof OVSDBUpdateRequest){
				OVSDBUpdateRequest update = (OVSDBUpdateRequest) operation;
				return builder(update)
					.put("where",where(update.getCriteria()))
					.put("row",buildObject(update.getRow()))
					.build();
			}else{
				throw new IllegalStateException("Unknown operation type "+operation.getClass());
			}
	}
	
	private Builder<String, Object> addNamedUUID(Builder<String, Object> builder, OVSDBInsertRequest insert){
		if(insert.getUuidName()!=null){
			return builder.put("uuid-name",insert.getUuidName());
		}
		return builder;
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
	
	
	private Object buildObject(Object rowObj) {
		MemberResolver memberResolver = new MemberResolver(typeResolver);
		ResolvedTypeWithMembers resolvedType = memberResolver.resolve(typeResolver.resolve(rowObj.getClass()),null,null);
		Map<String, Object> row = Maps.newHashMap();
		Stream.of(resolvedType.getMemberFields()).forEach(field->{
			if(field.getRawMember().isAnnotationPresent(MapField.class)){
				String column = field.getRawMember().getAnnotation(MapField.class).value();
				String key = field.getName();
				Object value = getValue(resolvedType,key,rowObj);
				if(value!=null){
					addMapField(row,column,key,value.toString());
				}
			}else{
				final String key;
				final Object value;
				if(field.getRawMember().isAnnotationPresent(Column.class)){
					key = field.getRawMember().getAnnotation(Column.class).value();
					value = getValue(resolvedType,field.getName(),rowObj);
				}else{
					key = field.getName();
					value = getValue(resolvedType,key,rowObj);
				}
				if(value!=null){
					row.put(key, toOVSObject(value));
				}
			}
		});
		return row;
	}

	@SuppressWarnings("unchecked")
	private void addMapField(Map<String, Object> row, String column, String key, Object value) {
		final List<Object> mapColumn;
		if(row.containsKey(column)){
			List<Object> mapColumnWrap = (List<Object>) row.get(column);
			mapColumn = (List<Object>) mapColumnWrap.get(1);
		}else{
			List<Object> mapColumnWrap = new ArrayList<>();
			mapColumnWrap.add("map");
			mapColumn = new ArrayList<>();
			mapColumnWrap.add(mapColumn);
			row.put(column, mapColumnWrap);
		}
		mapColumn.add(ImmutableSet.of(key, value));
	}

	private Object getValue(ResolvedTypeWithMembers resolvedType, String key, Object obj) {
		try {
			return Stream.of(resolvedType.getMemberMethods()).filter(method->method.getName().equals("get"+capitalize(key))).findFirst()
				.get().getRawMember().invoke(obj);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private String capitalize(String str) {
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

	private Object toOVSObject(Object value) {
		if(value instanceof OVSDBOperationRequest){
			return toParams((OVSDBOperationRequest) value);
		}else if(value instanceof Map){
			Map<?,?> map = (Map<?, ?>) value;
			return ImmutableSet.of("map",
					map.entrySet().stream()
							.map(entry->Arrays.asList(entry.getKey().toString(),mapValue(entry.getValue())))
							.collect(Collectors.toSet()));
		} else if(value instanceof Iterable){
			Iterable<?> iterable = (Iterable<?>) value;
			return ImmutableSet.of("set",StreamSupport.stream(iterable.spliterator(),false)
					.map(this::toOVSObject)
					.collect(Collectors.toSet()));
		}else if(value instanceof UUID){
			UUID uuid = (UUID) value;
			return ImmutableSet.of("uuid",uuid.toString());
		}else if(value instanceof NamedUUID){
			NamedUUID namedUUID = (NamedUUID) value;
			return ImmutableSet.of("named-uuid", namedUUID.getName());
		}else{
			return value.toString();
		}
	}

	private String mapValue(Object value) {
		if(Iterable.class.isAssignableFrom(value.getClass())){
			return Joiner.on(',').join((Iterable<?>) value);
		}else if(value instanceof String){
			return (String) value;
		}else{
			return null;
		}
	}

}
