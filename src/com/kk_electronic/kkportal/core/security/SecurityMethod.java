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
package com.kk_electronic.kkportal.core.security;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.kk_electronic.kkportal.core.inject.ConstructFromLiteral;
import com.kk_electronic.kkportal.core.rpc.Request;

@ConstructFromLiteral
public interface SecurityMethod {
	/**
	 * 
	 * @param request the request to be signed
	 * @param asyncCallback Should be called with the updated Request on success or onFailure if the method could not be signed
	 */
	void sign(Request request, AsyncCallback<Request> asyncCallback);

}
