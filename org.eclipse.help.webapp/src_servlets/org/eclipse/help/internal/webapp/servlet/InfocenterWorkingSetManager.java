/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.help.internal.webapp.servlet;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.http.*;

import org.eclipse.core.runtime.*;
import org.eclipse.help.internal.*;
import org.eclipse.help.internal.workingset.*;

/**
 * The working  set manager stores help working sets. Working sets are persisted
 * whenever one is added or removed.
 * @since 3.0
 */
public class InfocenterWorkingSetManager implements IHelpWorkingSetManager {
	private static final int MAX_COOKIES=15;
	 private static final int MAX_COOKIE_PAYLOAD = 4096 - "wset01=".length()  - "81920<".length()- 1; // Http11 connector will not serve the request that contains line longer than 4096 (not even one full cookie)

	private HttpServletRequest request;
	private HttpServletResponse response;

	// Current working set , empty string means all documents
	private String currentWorkingSet = "";
	private SortedSet workingSets = new TreeSet(new WorkingSetComparator());
	private String locale;
	private AdaptableTocsArray root;

	/**
	 * Constructor
	 * @param locale
	 */
	public InfocenterWorkingSetManager(
		HttpServletRequest request,
		HttpServletResponse response,
		String locale) {
		this.request = request;
		this.response = response;
		this.locale = locale;
		restoreState();
	}

	public AdaptableTocsArray getRoot() {
		if (root == null)
			root =
				new AdaptableTocsArray(
					HelpSystem.getTocManager().getTocs(locale));
		return root;
	}

	/**
	 * Adds a new working set and saves it
	 */
	public void addWorkingSet(WorkingSet workingSet) {
		if (workingSet == null || workingSets.contains(workingSet))
			return;
		workingSets.add(workingSet);
		saveState();
	}

	/**
	 * Creates a new working set
	 */
	public WorkingSet createWorkingSet(
		String name,
		AdaptableHelpResource[] elements) {
		return new WorkingSet(name, elements);
	}

	/**
	 * Returns a working set by name
	 * 
	 */
	public WorkingSet getWorkingSet(String name) {
		if (name == null || workingSets == null)
			return null;

		Iterator iter = workingSets.iterator();
		while (iter.hasNext()) {
			WorkingSet workingSet = (WorkingSet) iter.next();
			if (name.equals(workingSet.getName()))
				return workingSet;
		}
		return null;
	}

	/**
	 * Implements IWorkingSetManager.
	 * 
	 * @see org.eclipse.ui.IWorkingSetManager#getWorkingSets()
	 */
	public WorkingSet[] getWorkingSets() {
		return (WorkingSet[]) workingSets.toArray(
			new WorkingSet[workingSets.size()]);
	}

	/**
	 * Removes specified working set
	 */
	public void removeWorkingSet(WorkingSet workingSet) {
		workingSets.remove(workingSet);
		saveState();
	}

	private void restoreState() {
		String data = restoreString();
		if (data == null) {
			return;
		}

		String[] values = data.split("\\|", -1);
		if (values.length < 1) {
			return;
		}
		try {
			currentWorkingSet = URLDecoder.decode(values[0], "UTF8");
		} catch (UnsupportedEncodingException uee) {
		}

		try {
			i : for (int i = 1; i < values.length; i++) {
				String[] nameAndHrefs = values[i].split("&", -1);

				String name = URLDecoder.decode(nameAndHrefs[0], "UTF8");

				AdaptableHelpResource[] elements =
					new AdaptableHelpResource[nameAndHrefs.length - 1];
				// for each href (working set resource)
				for (int e = 0; e < nameAndHrefs.length - 1; e++) {
					int h = e + 1;
					elements[e] =
						getAdaptableToc(
							URLDecoder.decode(nameAndHrefs[h], "UTF8"));
					if (elements[e] == null) {
						elements[e] =
							getAdaptableTopic(
								URLDecoder.decode(nameAndHrefs[h], "UTF8"));
					}
					if (elements[e] == null) {
						// working set cannot be restored
						System.out.println(
							"cannot restore ws=" + nameAndHrefs[h]);
						// TODO comment system.out
						continue i;
					}
				}
				WorkingSet ws = createWorkingSet(name, elements);
				workingSets.add(ws);
			}
		} catch (UnsupportedEncodingException uee) {
		}

	}
	/* * Persists all working sets. Should only be called by the webapp working
	 * set dialog.
	 * Saves the working sets in the persistence store
	 * format: curentWorkingSetName|name1&href11&href12|name2&href22
	 */
	private void saveState() {
		StringBuffer data = new StringBuffer();
		try {
			data.append(URLEncoder.encode(currentWorkingSet, "UTF8"));
		} catch (UnsupportedEncodingException uee) {
		}

		for (Iterator i = workingSets.iterator(); i.hasNext();) {
			data.append('|');
			WorkingSet ws = (WorkingSet) i.next();
			try {
				data.append(URLEncoder.encode(ws.getName(), "UTF8"));
			} catch (UnsupportedEncodingException uee) {
			}

			AdaptableHelpResource[] resources = ws.getElements();
			for (int j = 0; j < resources.length; j++) {
				data.append('&');

				IAdaptable parent = resources[j].getParent();
				if (parent == getRoot()) {
					// saving toc
					try {
						data.append(
							URLEncoder.encode(resources[j].getHref(), "UTF8"));
					} catch (UnsupportedEncodingException uee) {
					}
				} else {
					// saving topic as tochref_topic#_
					AdaptableToc toc = (AdaptableToc) parent;
					AdaptableHelpResource[] siblings = (toc).getChildren();
					for (int t = 0; t < siblings.length; t++) {
						if (siblings[t] == resources[j]) {
							try {
								data.append(
									URLEncoder.encode(toc.getHref(), "UTF8"));
							} catch (UnsupportedEncodingException uee) {
							}
							data.append('_');
							data.append(t);
							data.append('_');
							break;
						}
					}
				}
			}
		}

		saveString(data.toString());
	}

	/**
	 * * @param changedWorkingSet the working set that has changed
	*/
	public void workingSetChanged(WorkingSet changedWorkingSet) {
		saveState();
	}

	/**
	 * Synchronizes the working sets. Should only be called by the webapp
	 * working set manager dialog.
	 *
	 * @param changedWorkingSet the working set that has changed
	 */
	public void synchronizeWorkingSets() {
		//HelpSystem.getWorkingSetManager(locale).synchronizeWorkingSets();
	}

	public AdaptableToc getAdaptableToc(String href) {
		return getRoot().getAdaptableToc(href);
	}

	public AdaptableTopic getAdaptableTopic(String id) {

		if (id == null || id.length() == 0)
			return null;

		// toc id's are hrefs: /pluginId/path/to/toc.xml
		// topic id's are based on parent toc id and index of topic: /pluginId/path/to/toc.xml_index_
		int len = id.length();
		if (id.charAt(len - 1) == '_') {
			// This is a first level topic
			String indexStr =
				id.substring(id.lastIndexOf('_', len - 2) + 1, len - 1);
			int index = 0;
			try {
				index = Integer.parseInt(indexStr);
			} catch (Exception e) {
			}

			String tocStr = id.substring(0, id.lastIndexOf('_', len - 2));
			AdaptableToc toc = getAdaptableToc(tocStr);
			if (toc == null)
				return null;
			IAdaptable[] topics = toc.getChildren();
			if (index < 0 || index >= topics.length)
				return null;
			else
				return (AdaptableTopic) topics[index];
		}

		return null;
	}
	/**
	 * Saves string in browser cookies.  Cookies can store limited length string.
	 * This method will attemt to split string among multiple cookies (up to 15 out of 20 possible cookies).
	 * The following cookies will be set
	 * wset1=length<substing1
	 * wset2=substrging2
	 * ...
	 * wsetn=substringn
	 * @param data a string containing legal characters for cookie value
	 * @throws exception when data is too long.
	 */
	private void saveString(String data) {
		int len = data.length();
		int n = len / MAX_COOKIE_PAYLOAD;
		if (n >MAX_COOKIES) {
			// too much data, do not save (there is only 20 cookies per domain)
			return;
		}
		for (int i = 1; i <= n; i++) {
			Cookie cookie = null;
			if (i == 1) {
				cookie =
					new Cookie(
						"wset1",
						data.length() + "<" + data.substring(0, MAX_COOKIE_PAYLOAD));
			} else {
				cookie = new Cookie("wset"+i, data.substring(MAX_COOKIE_PAYLOAD*(i-1), MAX_COOKIE_PAYLOAD*i));
			}
			cookie.setMaxAge(5 * 365 * 24 * 60 * 60);
			// TODO comment system.out
			System.out.println("Saving data in cookie: " + cookie.getValue());
			response.addCookie(cookie);
		}
		if (len % MAX_COOKIE_PAYLOAD > 0) {
			Cookie cookie = null;
			if (n == 0) {
				cookie =
					new Cookie(
						"wset1",
						len+ "<" + data.substring(0, len));
			} else {
				cookie = new Cookie("wset"+(n+1), data.substring(MAX_COOKIE_PAYLOAD*n, len));
			}
			cookie.setMaxAge(5 * 365 * 24 * 60 * 60);
			// TODO comment system.out
			System.out.println("Saving data in cookie: " + cookie.getValue());
			response.addCookie(cookie);
			
		}
		// if using less cookies than before, delete not needed cookies
		for(int i=n+1; i<=MAX_COOKIES; i++){
			if(i==n+1 &&len % MAX_COOKIE_PAYLOAD>0){
				continue;
			}
			Cookie cookie=new Cookie("wset"+i,"");
			cookie.setMaxAge(0);
			response.addCookie(cookie);

		}
		
	}

	/**
	 * @return null or String
	 */
	private String restoreString() {
		String value1=getCookieValue("wset1");
		if(value1==null){
			return null;
		}
		String lengthAndSubstring1[]=value1.split("<");
		if(lengthAndSubstring1.length<2){
			return null;
		}
		int len=0;
		try{
			len=Integer.parseInt(lengthAndSubstring1[0]);
		}catch(NumberFormatException nfe){
			return null;
		}
		if(len<=0){
			return null;
		}
		StringBuffer data=new StringBuffer(len);
		data.append(lengthAndSubstring1[1]);
		int n = len / MAX_COOKIE_PAYLOAD;
		for (int i = 2; i <= n; i++) {
			String substring=getCookieValue("wset"+i);
			if(substring==null){
				return null;
			}
			data.append(substring);
		}
		if (len % MAX_COOKIE_PAYLOAD > 0 && n>0) {
			String substring=getCookieValue("wset"+(n+1));
			if(substring==null){
				return null;
			}
			data.append(substring);
		}
		// TODO comment system.out
		System.out.println("data.length()"+data.length());
		if(data.length()!=len){
			// TODO comment system.out
			System.out.println("Verification error data lenght is "+data.length()+", instead of "+len);
		}
		
		return data.toString();
	}

	/**
	 * @return null or String
	 */
	private String getCookieValue(String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (int i = 0; i < cookies.length; i++) {
			if (name.equals(cookies[i].getName())) {
				return cookies[i].getValue();
			}
		}
		return null;
	}

	public String getCurrentWorkingSet() {
		return currentWorkingSet;
	}

	public void setCurrentWorkingSet(String workingSet) {
		currentWorkingSet = workingSet;
		saveState();
	}

}
