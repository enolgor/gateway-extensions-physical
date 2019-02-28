/*
 * Copyright 2016-2018 Universitat Politècnica de València
 * Copyright 2016-2018 Università della Calabria
 * Copyright 2016-2018 Prodevelop, SL
 * Copyright 2016-2018 Technische Universiteit Eindhoven
 * Copyright 2016-2018 Fundación de la Comunidad Valenciana para la
 * Investigación, Promoción y Estudios Comerciales de Valenciaport
 * Copyright 2016-2018 Rinicom Ltd
 * Copyright 2016-2018 Association pour le développement de la formation
 * professionnelle dans le transport
 * Copyright 2016-2018 Noatum Ports Valenciana, S.A.U.
 * Copyright 2016-2018 XLAB razvoj programske opreme in svetovanje d.o.o.
 * Copyright 2016-2018 Systems Research Institute Polish Academy of Sciences
 * Copyright 2016-2018 Azienda Sanitaria Locale TO5
 * Copyright 2016-2018 Alessandro Bassi Consulting SARL
 * Copyright 2016-2018 Neways Technologies B.V.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.interiot.gateway.devicecontroller.panstamp._new;

import java.nio.ByteBuffer;

import eu.interiot.gateway.commons.api.device.Attribute;
import eu.interiot.gateway.commons.physical.api.dmanager.DeviceState;
import me.legrange.panstamp.Endpoint;
import me.legrange.panstamp.EndpointListener;
import me.legrange.panstamp.definition.Type;

public abstract class EndpointListenerImpl<T> implements EndpointListener<T>{

	private DeviceState deviceState;
	protected Attribute attribute;
	
	public EndpointListenerImpl(DeviceState deviceState, Attribute attribute) {
		this.deviceState = deviceState;
		this.attribute = attribute;
	}
	
	public abstract Object convertValue(T value);
	
	@Override
	public void valueReceived(Endpoint<T> ep, T value) {
		deviceState.updateValue(attribute, convertValue(value));
	}
	
	private static class NumberListener extends EndpointListenerImpl<Double>{

		public NumberListener(DeviceState deviceState, Attribute attribute) {
			super(deviceState, attribute);
		}

		@Override
		public Object convertValue(Double value) {
			switch(super.attribute.getType()) {
			case INTEGER: return value.intValue();
			case FLOAT: return value;
			case BOOLEAN: return value != 0d;
			case STRING: return value.toString();
			default: return null;
			}
		}

	}
	
	private static class StringListener extends EndpointListenerImpl<String>{

		public StringListener(DeviceState deviceState, Attribute attribute) {
			super(deviceState, attribute);
		}

		@Override
		public Object convertValue(String value) {
			switch(super.attribute.getType()) {
			case INTEGER: return Integer.parseInt(value);
			case FLOAT: return Double.parseDouble(value);
			case BOOLEAN: return (value.trim().toLowerCase().equals("true") || !value.trim().equals("0"));
			case STRING: return value.toString();
			default: return null;
			}
		}

	}
	
	private static class BSListener extends EndpointListenerImpl<byte[]>{ //not tested

		public BSListener(DeviceState deviceState, Attribute attribute) {
			super(deviceState, attribute);
		}

		@Override
		public Object convertValue(byte [] value) {
			ByteBuffer wrapped = ByteBuffer.wrap(value); // big-endian by default
			switch(super.attribute.getType()) {
			case INTEGER: return wrapped.getInt();
			case FLOAT: return wrapped.getDouble();
			case BOOLEAN: return wrapped.getInt() != 0;
			case STRING: return new String(value);
			default: return null;
			}
		}

	}
	
	private static class IntegerListener extends EndpointListenerImpl<Integer>{

		public IntegerListener(DeviceState deviceState, Attribute attribute) {
			super(deviceState, attribute);
		}

		@Override
		public Object convertValue(Integer value) {
			switch(super.attribute.getType()) {
			case INTEGER: return value;
			case FLOAT: return value.doubleValue();
			case BOOLEAN: return value != 0;
			case STRING: return value.toString();
			default: return null;
			}
		}

	}
	
	private static class BooleanListener extends EndpointListenerImpl<Boolean>{

		public BooleanListener(DeviceState deviceState, Attribute attribute) {
			super(deviceState, attribute);
		}

		@Override
		public Object convertValue(Boolean value) {
			switch(super.attribute.getType()) {
			case INTEGER: return value ? 1 : 0;
			case FLOAT: return value ? 1d : 0d;
			case BOOLEAN: return value;
			case STRING: return value.toString();
			default: return null;
			}
		}

	}
	
	public static EndpointListenerImpl<?> getEndpointListenerImpl(Type type, DeviceState deviceState, Attribute attribute){
		switch(type) {
		case BINARY:
			return new BooleanListener(deviceState, attribute);
		case INTEGER:
			return new IntegerListener(deviceState, attribute);
		case NUMBER:
			return new NumberListener(deviceState, attribute);
		case BSTRING:
			return new BSListener(deviceState, attribute);
		default:
			return new StringListener(deviceState, attribute);
		}
	}
	
}
