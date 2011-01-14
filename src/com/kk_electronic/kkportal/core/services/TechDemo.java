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
package com.kk_electronic.kkportal.core.services;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.kk_electronic.kkportal.core.rpc.RemoteService;

/**
 * The {@link RemoteService} interface is a marker interface that
 * enables the framework to generate the implementation of this
 * interface, which links it to the rest of the system. 
 */
public interface TechDemo extends RemoteService {
	/**
	 * The interface must use {@link AsyncCallback} instead of
	 * a return value, since javascript is singlethreaded and
	 * we cannot make blocking calls. 
	 */
	void getCpuHistory(AsyncCallback<List<Double>> callback);

	/*
	 * The functions below are not relevant for the presentation
	 */
	void reload(AsyncCallback<?> callback);
	void inotify(String path,AsyncCallback<?> callback);
	void getWall(AsyncCallback<List<String>> callback);
	void postToWall(String message,AsyncCallback<?> callback);
}