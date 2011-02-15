/*
 * Copyright 2010 kk-electronic a/s. 
 * 
 * This file is part of KKPortal.
 *
 * KKPortal is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * KKPortal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with KKPortal.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kk_electronic.kkportal.core.rpc.jsonformat;

import java.util.List;

import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.kk_electronic.kkportal.core.rpc.FrameEncoder;
import com.kk_electronic.kkportal.core.rpc.JsonEncoder;

public class JsonString implements JsonValue<String> {

	public JsonString() {
	}
	@Override
	public void toJson(StringBuilder response, String object,
			FrameEncoder<JSONValue> encoder) throws UnableToSerialize {
		response.append(JsonUtils.escapeValue(object));
	}
	@Override
	public String fromJson(JSONValue jsonValue, List<Class<?>> subtypes,
			JsonEncoder simpleEncoder) throws UnableToDeserialize {
		if(jsonValue.isNull() != null) return null;
		JSONString jsonString = jsonValue.isString();
		if(jsonString == null) throw new UnableToDeserialize("Expected json String");
		return jsonString.stringValue();
	}
}
