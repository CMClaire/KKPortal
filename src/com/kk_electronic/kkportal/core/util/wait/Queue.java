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
package com.kk_electronic.kkportal.core.util.wait;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Queue<T> {
	HashMap<Object,List<T>> waiting = new HashMap<Object, List<T>>();
	

	public void enque(List<T> elements,Object waitsource){
		if(waiting.containsKey(waitsource)){
			waiting.get(waitsource).addAll(elements);
		} else {
			waiting.put(waitsource, elements);
		}
	}

	public void enque(T element,Object waitsource){
		if(waiting.containsKey(waitsource)){
			waiting.get(waitsource).add(element);
		} else {
			List<T> elements = new ArrayList<T>();
			elements.add(element);
			waiting.put(waitsource,elements);
		}
	}
	
	public List<T> deque(Object waitsource){
		if(waiting.containsKey(waitsource)){
			List<T> retval = waiting.remove(waitsource);
			return retval;
		}
		return Collections.emptyList();
	}
}
