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
package com.kk_electronic.kkportal.core.rpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kk_electronic.kkportal.core.event.FrameReceivedEvent;
import com.kk_electronic.kkportal.core.event.FrameSentEvent;
import com.kk_electronic.kkportal.core.event.INotificationEvent;
import com.kk_electronic.kkportal.core.event.ServerConnectEvent;
import com.kk_electronic.kkportal.core.event.ServerDisconnectEvent;
import com.kk_electronic.kkportal.core.inject.FlexInjector;
import com.kk_electronic.kkportal.core.reflection.FeatureMap;
import com.kk_electronic.kkportal.core.reflection.SecurityMap;
import com.kk_electronic.kkportal.core.security.SecurityMethod;
import com.kk_electronic.kkportal.examples.modules.NewWallMessageEvent;

/**
 * RpcDispatcher is a flexible {@link Dispatcher} for use with direct server
 * side communication using bundling.
 * 
 * It assumes that the server can handle the connection type by the class
 * implementing {@link WebSocket} and the it uses the correct
 * {@link FrameEncoder} class to transmit the rpc calls, and that the rpc calls
 * and {@link RemoteServer} is supported by the backend without any security
 * wrapper.
 * 
 * After connecting is uses a call the
 * {@link RemoteServer#getSecurityMap(AsyncCallback)} to get a list of the
 * available method and the security requirements.
 * 
 * It then transmits the calls as fast as possible using bundling techniques for
 * incoming and outgoing frames. The actual bundling and data format is defined
 * by the {@link FrameEncoder}.
 * 
 * TODO: split feature and security handling into separate classes.
 * 
 * @author Jes Andersen
 * 
 */
@Singleton
public class RpcDispatcher implements FrameSentEvent.Handler, Dispatcher,
		ServerConnectEvent.Handler, ServerDisconnectEvent.Handler,
		FrameReceivedEvent.Handler {

	private final IdCreator<Integer> idCreator;

	private Map<Class<? extends RemoteService>, SecurityMethod> authenticationMethods = new HashMap<Class<? extends RemoteService>, SecurityMethod>();

	private Set<String> serverFeatures;

	private final WebSocket socket;

	private final FrameEncoder frameEncoder;

	private final FeatureMap clientFeatureMap;

	private final SecurityMap clientSecurityMap;

	private final FlexInjector injector;

	private final EventBus eventbus;

	/**
	 * A holder to keep metainformation about the calls made. should generally
	 * have no use outside this class.
	 * 
	 * @author Jes Andersen
	 * 
	 * @param <T>
	 *            used to provide type safety for the callback
	 */
	public final static class PendingCall<T> implements AsyncCallback<Response> {
		private final AsyncCallback<T> callback;
		private Request request;
		private PendingCallStatus status;
		private final FrameEncoder encoder;
		private final Class<?>[] returnValueType;

		public PendingCall(AsyncCallback<T> callback, Request request,
				FrameEncoder encoder, Class<?>[] returnValueType) {
			this.callback = callback;
			this.request = request;
			this.encoder = encoder;
			this.returnValueType = returnValueType;
			status = PendingCallStatus.NEW;
		}

		public void setStatus(PendingCallStatus status) {
			this.status = status;
		}

		public PendingCallStatus getStatus() {
			return status;
		}

		public void setRequest(Request request) {
			this.request = request;
		}

		@Override
		public void onFailure(Throwable caught) {
			if (callback != null) {
				callback.onFailure(caught);
			}
		}

		@Override
		public void onSuccess(Response response) {
			T result = encoder.<T> decodeResult(returnValueType, response
					.getResult());
			if (callback != null) {
				callback.onSuccess(result);
			}
		}
	}

	@Inject
	public RpcDispatcher(IdCreator<Integer> idCreator, WebSocket socket,
			FrameEncoder encoder, SecurityMap clientSecurityMap,
			FeatureMap clientFeatureMap, FlexInjector injector,
			EventBus eventbus) {
		this.idCreator = idCreator;
		this.socket = socket;
		this.frameEncoder = encoder;
		this.clientSecurityMap = clientSecurityMap;
		this.clientFeatureMap = clientFeatureMap;
		this.injector = injector;
		this.eventbus = eventbus;
		authenticationMethods.put(RemoteServer.class, null);
		socket.addServerConnectHandler(this);
		socket.addServerDisconnectHandler(this);
		socket.addFrameReceivedHandler(this);
		socket.addFrameSentHandler(this);
	}

	public <T> void execute(AsyncCallback<T> callback,
			Class<?>[] returnValueType,
			Class<? extends RemoteService> serverinterface, String method,
			Object... params) {
		if (serverFeatures != null && serverFeatures.contains(serverinterface)) {
			callback
					.onFailure(new Exception("Feature not supported on server"));
		}
		String featureName = clientFeatureMap.getKeyFromClass(serverinterface);
		if (featureName == null) {
			GWT.log("RPC-could not map class to feature: "
					+ serverinterface.getName());
		}
		Request request = new Request(featureName, method, params);
		// TODO: Delay Creation of id
		request.setId(idCreator.getNextId());
		final PendingCall<T> pendingCall = new PendingCall<T>(callback,
				request, frameEncoder, returnValueType);
		pending.put(request.getId(), pendingCall);

		if (authenticationMethods.containsKey(serverinterface)) {
			SecurityMethod securityMethod = authenticationMethods
					.get(serverinterface);
			if (securityMethod == null) {
				transmit(pendingCall);
			} else {
				signAndTransmit(pendingCall, securityMethod);
			}
		} else {
			updateServerSecurityMap();
			GWT.log("RPC-Delaying call due to unresolved security: "
					+ request.getFeatureName() + "." + request.getMethod());
		}
	}

	protected void signAndTransmit(final PendingCall<?> pendingCall,
			SecurityMethod securityMethod) {
		pendingCall.status = PendingCallStatus.WAITING_FOR_SECURITY;
		securityMethod.sign(pendingCall.request, new AsyncCallback<Request>() {

			@Override
			public void onFailure(Throwable caught) {
				GWT.log("RPC-Security could not sign request - aborting call",
						caught);
			}

			@Override
			public void onSuccess(Request result) {
				pendingCall.setRequest(result);
				transmit(pendingCall);
			}
		});
	}

	protected void transmit(PendingCall<?> pendingCall) {
		pendingCall.setStatus(PendingCallStatus.WAITING_FOR_TRANSMIT);
		txQueue.add(pendingCall.request);
		sendQueue();
	}

	protected void sendQueue() {
		if (txQueue.isEmpty())
			return;

		if (socket.isConnected()) {
			if (!socket.isTxBusy()) {
				socket.send(frameEncoder.encode(txQueue));
				for (Request request : txQueue) {
					pending.get(request.getId()).setStatus(
							PendingCallStatus.WAITING_FOR_RESPONSE);
				}
				txQueue.clear();
			}
		} else {
			socket.connect(GWT.getHostPageBaseURL() + "websocket", "kk-entity");
		}
	}

	List<Request> txQueue = new ArrayList<Request>();

	Map<Integer, PendingCall<?>> pending = new HashMap<Integer, PendingCall<?>>();

	private void cancelCallsWithUnsupportedFeatures() {
		for (PendingCall<?> call : pending.values()) {
			if (!serverFeatures.contains(call.request.getFeatureName())) {
				call
						.onFailure(new Exception(
								"Feature not supported on server"));
			}
		}
	}

	private void addFeature(final String featureName, final String securityName) {
		final Class<? extends RemoteService> feature = clientFeatureMap
				.getClassFromKey(featureName);
		if (feature == null) {
			GWT.log("RPC-PortalServer has unknown feature " + featureName);
			return;
		}
		if (securityName == null) {
			GWT.log("RPC-PortalServer Feature " + featureName
					+ " with no security enabled");
			authenticationMethods.put(feature, null);
		} else {
			Class<? extends SecurityMethod> security = clientSecurityMap
					.getClassFromKey(securityName);
			if (security == null) {
				GWT.log("RPC-PortalServer Feature " + featureName
						+ " has unknown security " + securityName);
				return;
			}
			loadAuthenticationMethod(feature, security);
		}
	}

	private void loadAuthenticationMethod(
			final Class<? extends RemoteService> feature,
			final Class<? extends SecurityMethod> security) {
		injector.create(security, new AsyncCallback<SecurityMethod>() {
			@Override
			public void onFailure(Throwable caught) {
				GWT.log("RPC-PortalServer Feature "
						+ clientFeatureMap.getKeyFromClass(feature)
						+ " with security "
						+ clientSecurityMap.getKeyFromClass(security)
						+ " disabled since code cannot be loaded", caught);
				cancelCallsWithFeature(feature);
			}

			@Override
			public void onSuccess(SecurityMethod result) {
				GWT.log("RPC-PortalServer Feature "
						+ clientFeatureMap.getKeyFromClass(feature)
						+ " with security "
						+ clientSecurityMap.getKeyFromClass(security)
						+ " enabled");
				addAuthenticationMethod(feature, result);
			}
		});
	}

	protected void cancelCallsWithFeature(Class<? extends RemoteService> feature) {
		Iterator<PendingCall<?>> i = pending.values().iterator();
		String featureName = clientFeatureMap.getKeyFromClass(feature);
		for (PendingCall<?> call = i.next(); i.hasNext(); call = i.next()) {
			if (call.getStatus() == PendingCallStatus.NEW
					&& call.request.getFeatureName().equals(featureName)) {
				call.onFailure(new Exception(
						"RPC-Could not load security method"));
				i.remove();
			}
		}
	}

	protected void addAuthenticationMethod(
			Class<? extends RemoteService> feature,
			SecurityMethod securityMethod) {
		authenticationMethods.put(feature, securityMethod);
		String featureName = clientFeatureMap.getKeyFromClass(feature);
		for (PendingCall<?> call : pending.values()) {
			if (call.getStatus() == PendingCallStatus.NEW
					&& call.request.getFeatureName().equals(featureName)) {
				signAndTransmit(call, securityMethod);
			}
		}
	}

	private boolean isUpdating = false;

	/**
	 * this updates the the feature and security map of the server.
	 */
	public void updateServerSecurityMap() {
		if (isUpdating)
			return;
		isUpdating = true;
		AsyncCallback<Map<String, String>> x = new AsyncCallback<Map<String, String>>() {

			@Override
			public void onSuccess(Map<String, String> result) {
				serverFeatures = new HashSet<String>();
				for (Entry<String, String> entry : result.entrySet()) {
					serverFeatures.add(entry.getKey());
					addFeature(entry.getKey(), entry.getValue());
				}
				cancelCallsWithUnsupportedFeatures();
			}

			@Override
			public void onFailure(Throwable caught) {
				GWT.log("RPC-The server does not fully support the kkProtocol",
						caught);
				socket.close();
			}
		};
		/*
		 * To remove a circular dependency we make the call directly here
		 */
		execute(x, new Class<?>[] { Map.class, String.class, String.class },
				com.kk_electronic.kkportal.core.rpc.RemoteServer.class,
				"getSecurityMap");
	}

	@Override
	public void onServerConnect(ServerConnectEvent event) {
		sendQueue();
		updateServerSecurityMap();
	}

	@Override
	public void onServerDisconnect(ServerDisconnectEvent event) {

	}

	@Override
	public void onFrameReceived(FrameReceivedEvent event) {
		List<Response> responses = frameEncoder.decode(event.getData());
		for (Response response : responses) {
			if (response == null)
				continue;
			if (response.isNotification()) {
				//TODO Make generic
				GWT.log("SERVEREVENT-" + response.getMethod());
				List<Object> params = frameEncoder.decodeResult(
						new Class<?>[] { List.class, Object.class },
						response.getParams());
				if ("INotification".equals(response.getMethod())) {
					String s1 = frameEncoder.decodeResult(
							new Class<?>[] { String.class }, params.get(0));
					String s2 = frameEncoder.decodeResult(
							new Class<?>[] { String.class }, params.get(1));
					eventbus.fireEvent(new INotificationEvent(s1, s2));
				} else if ("NewWallMessageEvent".equals(response.getMethod())){
					String s1 = frameEncoder.decodeResult(
							new Class<?>[] { String.class }, params.get(0));
					eventbus.fireEvent(new NewWallMessageEvent(s1));
				}
				continue;
			}
			PendingCall<?> pendingCall = pending.remove(response.getId());
			if (response.hasError()) {
				pendingCall.setStatus(PendingCallStatus.DONE);
				pending.remove(response.getId());
				pendingCall.onFailure(new Exception(response.getError()
						.getMessage()));
			} else {
				pendingCall.onSuccess(response);
			}
		}
	}

	@Override
	public void onFrameSent(FrameSentEvent frameSentEvent) {
		DeferredCommand.addCommand(new Command() {

			@Override
			public void execute() {
				sendQueue();
			}
		});
	}
}
