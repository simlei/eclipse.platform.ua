/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.help.internal.webapp.servlet;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.webapp.HelpWebappPlugin;
import org.eclipse.help.webapp.IFilter;

public class ExtraFilters implements IFilter {

	public static final String CONTENTFILTER_XP_NAME = "org.eclipse.help.webapp.contentFilter"; //$NON-NLS-1$

	private static List<PrioritizedFilter> filters = null;

	public ExtraFilters() {
		if (filters == null) {
			readFilters();
		}
	}

	private void readFilters() {
		filters = new ArrayList<>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry
				.getConfigurationElementsFor(CONTENTFILTER_XP_NAME);
		for (IConfigurationElement element : elements) {

			Object obj = null;
			try {
				obj = element.createExecutableExtension("class"); //$NON-NLS-1$
			} catch (CoreException e) {
				HelpWebappPlugin.logError("Create extension failed:[" //$NON-NLS-1$
						+ CONTENTFILTER_XP_NAME + "].", e); //$NON-NLS-1$
			}
			if (obj instanceof IFilter) {

				int priority = 0;
				String priStr = element.getAttribute("priority"); //$NON-NLS-1$
				if (priStr != null && !"".equals(priStr)) { //$NON-NLS-1$
					try {
						priority = Integer.parseInt(priStr);
					} catch (NumberFormatException e) {
					}
				}
				PrioritizedFilter filter = new PrioritizedFilter((IFilter)obj, priority);
				filters.add(filter);
			}
		}
		sortFilters();
	}

	private static void sortFilters() {
		Collections.sort(filters);
		Collections.reverse(filters);
	}

	/*
	 * For JUnit testing
	 */
	public static void setFilters(PrioritizedFilter[] newFilters) {
		filters = new ArrayList<>();
		Collections.addAll(filters, newFilters);
		sortFilters();
	}

	@Override
	public OutputStream filter(HttpServletRequest req, OutputStream out) {
		PrioritizedFilter filter;
		for(int index = 0; index < filters.size(); index++) {
			filter = filters.get(index);
			out = filter.filter(req, out);
		}
		return out;
	}

}
