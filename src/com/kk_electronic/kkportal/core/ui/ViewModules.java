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
package com.kk_electronic.kkportal.core.ui;


import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.kk_electronic.kkportal.core.Activity;
import com.kk_electronic.kkportal.core.security.User;
import com.kk_electronic.kkportal.core.services.ModuleService;
import com.kk_electronic.kkportal.core.services.PortalService;
import com.kk_electronic.kkportal.core.services.ModuleService.ModuleInfo;
import com.kk_electronic.kkportal.core.services.ModuleService.TabInfo;

/**
 * Note this is not a good example yet. Does not have View-Presenter seperation
 * 
 * @author Jes Andersen type filter text
 */
public class ViewModules implements Activity {
	private final ModuleService moduleService;
	private final ModuleDisplay display;
	protected List<List<ModuleInfo>> moduleList;
	private TabInfo tab;
	
	@Inject
	public ViewModules(PortalService service, ModuleService moduleService,
			User user,ModuleDisplay display) {
		this.moduleService = moduleService;
		this.display = display;
		display.setViewModules(this);
		
		moduleService.getTabs(user, new AsyncCallback<List<TabInfo>>() {
			
			@Override
			public void onSuccess(List<TabInfo> result) {
				viewTab(result.get(0));
			}
			
			@Override
			public void onFailure(Throwable caught) {
				GWT.log("Didn't get tabs", caught);
			}
		});
	}

	public void viewTab(final TabInfo tab) {
		this.tab = tab;
		if (tab == null)
			return;
		moduleService.getModules(tab.getId(), new AsyncCallback<List<List<ModuleInfo>>>() {

			@Override
			public void onFailure(Throwable caught) {
				GWT.log("failed to get modules for tab: "
						+ tab.toString(), caught);
			}

			@Override
			public void onSuccess(List<List<ModuleInfo>> result) {
				ViewModules.this.moduleList = result;
				display.setModuleList(result,true);
			}
		});
	}
	
	protected boolean removeWindow(ModuleWindow window){
		for(List<ModuleInfo> column : moduleList){
			if(column.remove(window)) {
				display.setModuleList(moduleList);
				return true;
			}
		}
		return false;
	}

	@Override
	public Widget asWidget() {	
		return display.asWidget();
	}
	
	public boolean remove(ModuleInfo module){
		for (List<ModuleInfo> moduleInfos : moduleList){
			if(moduleInfos.remove(module)){
				updateDisplays();
				return true;
			}
		}
		return false;
	}

	private void updateDisplays() {
		display.setModuleList(moduleList);
	}
	
	public void add(ModuleInfo module, double d, int j) {
		int columnIndex = (int) ((d / 100.0) * moduleList.size());
		List<ModuleInfo> column = moduleList.get(columnIndex);
		int moduleindex=0;
		while(moduleindex<column.size()){
			int height = 20 + column.get(moduleindex).getHeight();
			if(j < height / 2)
				break;
			j -= height;
			moduleindex++;
		}
		column.add(moduleindex,module);
		updateDisplays();
	}

	Timer delayedSave = new Timer() {
		
		@Override
		public void run() {
			moduleService.setModules(tab.getId(), moduleList, new AsyncCallback<Object>() {

				@Override
				public void onFailure(Throwable caught) {
					GWT.log("ViewModules-Failed to save modules:",caught);
				}

				@Override
				public void onSuccess(Object result) {
					GWT.log("ViewModules-Saved Modules:");
				}
			});
		}
	};
	
	public void save() {
		delayedSave.schedule(1300);
	}
}
