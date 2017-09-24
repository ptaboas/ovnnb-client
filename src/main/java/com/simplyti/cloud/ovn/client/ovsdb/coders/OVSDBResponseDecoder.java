package com.simplyti.cloud.ovn.client.ovsdb.coders;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedConstructor;
import com.fasterxml.classmate.members.ResolvedField;
import com.google.common.collect.Iterables;
import com.simplyti.cloud.ovn.client.InternalClient;
import com.simplyti.cloud.ovn.client.ResourceConsumer;
import com.simplyti.cloud.ovn.client.domain.annotations.Column;
import com.simplyti.cloud.ovn.client.domain.annotations.MapField;
import com.simplyti.cloud.ovn.client.jsonrpc.domain.JsonRpcResponse;
import com.simplyti.cloud.ovn.client.ovsdb.exceptions.OVSDBException;

import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;

@Sharable
public class OVSDBResponseDecoder extends SimpleChannelInboundHandler<JsonRpcResponse>{
	
	private final TypeResolver typeResolver = new TypeResolver();

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, JsonRpcResponse msg) throws Exception {
		ResourceConsumer<?> consumer = ctx.channel().attr(InternalClient.CONSUMERS).get().remove(msg.getId());
		if(checkError(consumer,msg)){
			return;
		}
		
		ResolvedType resolved = typeResolver.resolve(consumer.getResourceClass().getType());
		if(consumer.getResourceClass().getType().equals(Void.class)){
			@SuppressWarnings("unchecked")
			ResourceConsumer<Void> voidConsumer = (ResourceConsumer<Void>) consumer;
			voidConsumer.accept(null);
		}else if(consumer.getResourceClass().getType().equals(UUID.class)){
			Map<?,?> result = msg.getResult().stream().map(Map.class::cast)
				.filter(map->!map.containsKey("count")).findFirst().get();
			@SuppressWarnings("unchecked")
			ResourceConsumer<UUID> uuidConsumer = (ResourceConsumer<UUID>) consumer;
			uuidConsumer.accept(toUUID((List<?>)result.get("uuid")));
		}else if(resolved.isInstanceOf(Collection.class)){
			@SuppressWarnings("unchecked")
			ResourceConsumer<Collection<Object>> objConsumer = (ResourceConsumer<Collection<Object>>) consumer;
			if(msg.getResult().stream().filter(item-> item instanceof Map).findFirst().isPresent()){
				Map<?,?> resultMap = (Map<?, ?>) Iterables.get(msg.getResult(), 0);
				Collection<?> rows = (Collection<?>) resultMap.get("rows");
				ResolvedType itemClass = resolved.typeParametersFor(Collection.class).get(0);
				List<Object> resultList = rows.stream().map(Map.class::cast)
					.map(item->toObject(item,itemClass))
					.collect(Collectors.toList());
				
				objConsumer.accept(resultList);
			}else{
				objConsumer.accept(msg.getResult());
			}
		}else{
			Map<?,?> resultMap = (Map<?, ?>) Iterables.get(msg.getResult(), 0);
			Collection<?> rows = (Collection<?>) resultMap.get("rows");
			if(rows.isEmpty()){
				consumer.accept(null);
			}else{
				@SuppressWarnings("unchecked")
				ResourceConsumer<Object>objConsumer = (ResourceConsumer<Object>) consumer;
				objConsumer.accept(toObject((Map<?, ?>) Iterables.get(rows, 0),typeResolver.resolve(consumer.getResourceClass().getType())));
			}
			
		}
	}
	
	private boolean checkError(ResourceConsumer<?> consumer, JsonRpcResponse msg) {
		@SuppressWarnings("rawtypes")
		Optional<Map> errorData = msg.getResult().stream()
			.filter(item->item instanceof Map)
			.map(Map.class::cast)
			.filter(item->item.containsKey("error"))
			.findFirst();
		if(errorData.isPresent()){
			Map<?,?> errorDetails = errorData.get();
			String error = (String) errorDetails.get("error");
			String details = (String) errorDetails.get("details");
			consumer.setFailure(new OVSDBException(error+": "+details));
			return true;
		}else{
			return false;
		}
		
	}

	@SuppressWarnings("unchecked")
	private Object toObject(Map<?,?> rowMap, ResolvedType resourceClass) {
		MemberResolver memberResolver = new MemberResolver(typeResolver);
		ResolvedTypeWithMembers resolvedType = memberResolver.resolve(resourceClass,null,null);
		
		ResolvedConstructor constructor = resolvedType.getConstructors()[0];
		Parameter[] parameters = constructor.getRawMember().getParameters();
		Object[] arguments = new Object[parameters.length];
		
		Stream.of(resolvedType.getMemberFields()).forEach(field->{
			final Object value;
			if(field.getType().isInstanceOf(UUID.class)){
				value = transform(field.getType(),rowMap.get("_uuid"));
			}else if(field.getRawMember().isAnnotationPresent(MapField.class)){
				String key = field.getRawMember().getAnnotation(MapField.class).value();
				Object ovsValue = rowMap.get(key);
				if(ovsValue!=null){
					List<?> entries = (List<?>) Iterables.get(((Iterable<?>)ovsValue), 1);
					value = entries.stream().map(Iterable.class::cast)
						.filter(item->Iterables.get(item, 0).equals(field.getName()))
						.findFirst().map(entry->transform(field.getType(),Iterables.get((Iterable<?>)entry, 1)))
						.orElse(null);
				}else{
					value = null;
				}
			}else if(field.getRawMember().isAnnotationPresent(Column.class)){
				String key = field.getRawMember().getAnnotation(Column.class).value();
				Object ovsValue = rowMap.get(key);
				if(ovsValue!=null){
					value = transform(field.getType(),ovsValue);
				}else{
					value = null;
				}
			}else{
				value = transform(field.getType(),rowMap.get(field.getName()));
			}
			argumentIndexFor(field,parameters,arguments,value);
		});
		
		try {
			return constructor.getRawMember().newInstance(arguments);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object transform(ResolvedType type, Object value) {
		if(type.isInstanceOf(String.class)){
			return value.toString();
		}else if(type.isInstanceOf(UUID.class)){
			List<?> list = (List<?>) value;
			return UUID.fromString((String) list.get(1));
		}else if(type.isInstanceOf(Enum.class)){
			return Enum.valueOf((Class<? extends Enum>) type.getErasedType(), ((String)value).toUpperCase());
		}else if(type.isInstanceOf(Map.class)){
			List<?> entries = (List<?>) Iterables.get(((Iterable<?>)value), 1);
			ResolvedType keyType = type.typeParametersFor(Map.class).get(0);
			ResolvedType valueType = type.typeParametersFor(Map.class).get(1);
			return entries.stream().map(obj->(Iterable<Object>)obj)
				.collect(Collectors.toMap(
						item->transform(keyType,Iterables.get(item, 0)), 
						item->transform(valueType,Iterables.get(item, 1))));
		}else if(type.isInstanceOf(Collection.class)){
			if(String.class.isAssignableFrom(value.getClass())){
				ResolvedType itemType = type.typeParametersFor(Collection.class).get(0);
				return Stream.of(((String) value).split(","))
					.map(item->transform(itemType,item))
					.collect(Collectors.toList());
			}else{
				ResolvedType itemType = type.typeParametersFor(Collection.class).get(0);
				Collection<?> ovsValue = (Collection<?>) value;
				if(Iterables.get(ovsValue, 0).equals("set")){
					Collection<?> items = (Collection<?>) Iterables.get(ovsValue, 1);
					return items.stream().map(item->transform(itemType,item)).collect(Collectors.toList());
				}else{
					return Collections.singleton(transform(itemType, value));
				}
			}
		}else{
			return type.getStaticMethods().stream().filter(method->method.getName().equals("valueOf")).findFirst()
				.map(method->invoke(method.getRawMember(),value))
				.orElse(null);
		}
	}



	private Object invoke(Method method,Object obj) {
		try {
			return method.invoke(null, obj);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			return null;
		}
	}



	private void argumentIndexFor(ResolvedField field, Parameter[] parameters,Object[] args,Object value) {
		for(int index=0;index<parameters.length;index++){
			if(parameters[index].getType().equals(field.getRawMember().getType())){
				args[index]=value;
			}
		}
	}
	
	private UUID toUUID(List<?> list) {
		return UUID.fromString((String) list.get(1));
	}

}
