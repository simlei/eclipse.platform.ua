/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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
package org.eclipse.help.internal.base.remote;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.help.AbstractContentExtensionProvider;
import org.eclipse.help.IContentExtension;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.util.ProxyUtil;
import org.eclipse.help.internal.dynamic.DocumentReader;

public class RemoteExtensionProvider extends AbstractContentExtensionProvider {

	private static final String PATH_EXTENSIONS = "/extension"; //$NON-NLS-1$
	private static final String PROTOCOL_HTTP = "http"; //$NON-NLS-1$

	private DocumentReader reader;

	public RemoteExtensionProvider() {
		RemoteHelp.addPreferenceChangeListener(event -> contentChanged());
	}

	@Override
	public IContentExtension[] getContentExtensions(String locale) {
		if (RemoteHelp.isEnabled()) {
			List<IContentExtension> contributions = new ArrayList<>();
			PreferenceFileHandler handler = new PreferenceFileHandler();
			String isEnabled[] = handler.isEnabled();
			for (int ic = 0; ic < handler.getTotalRemoteInfocenters(); ic++) {
				if (isEnabled[ic].equalsIgnoreCase("true")) { //$NON-NLS-1$
					InputStream in = null;
					try {
						URL url = RemoteHelp.getURL(ic, PATH_EXTENSIONS);

						if(url.getProtocol().equalsIgnoreCase(PROTOCOL_HTTP))
						{
							in = ProxyUtil.getStream(url);
						}
						else
						{
							in = HttpsUtility.getHttpsStream(url);
						}

						if (reader == null) {
							reader = new DocumentReader();
						}
						UAElement element = reader.read(in);
						IContentExtension[] children = element.getChildren(IContentExtension.class);
						Collections.addAll(contributions, children);
					} catch (IOException e) {
						String msg = "I/O error while trying to contact the remote help server"; //$NON-NLS-1$
						HelpBasePlugin.logError(msg, e);
					} catch (Throwable t) {
						String msg = "Internal error while reading topic extensions from remote server"; //$NON-NLS-1$
						HelpBasePlugin.logError(msg, t);
					} finally {
						if (in != null) {
							try {
								in.close();
							} catch (IOException e) {
								// nothing more we can do
							}
						}
					}
				}
			}
			return contributions.toArray(new IContentExtension[contributions.size()]);
		}
		return new IContentExtension[0];
	}
}
