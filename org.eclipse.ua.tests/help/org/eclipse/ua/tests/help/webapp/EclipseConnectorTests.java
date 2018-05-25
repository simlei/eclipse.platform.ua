/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/

package org.eclipse.ua.tests.help.webapp;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.eclipse.core.runtime.URIUtil;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.server.WebappManager;
import org.eclipse.ua.tests.help.remote.RemotePreferenceStore;
import org.eclipse.ua.tests.help.remote.RemoteTestUtils;
import org.eclipse.ua.tests.help.remote.TestServerManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EclipseConnectorTests {
	private int mode;

	@Before
	public void setUp() throws Exception {
		BaseHelpSystem.ensureWebappRunning();
		mode = BaseHelpSystem.getMode();
		RemotePreferenceStore.savePreferences();
		RemotePreferenceStore.setMockRemoteServer();
		BaseHelpSystem.setMode(BaseHelpSystem.MODE_INFOCENTER);
		HelpPlugin.getTocManager().getTocs("en");
	}

	@After
	public void tearDown() throws Exception {
		RemotePreferenceStore.restorePreferences();
		BaseHelpSystem.setMode(mode);
	}

	@Test
	public void testEncodedAmpersand() throws Exception {
		final String path = "/data/help/index/" + URIUtil.fromString("topic&.html").toString();
		String remoteContent = getHelpContent("mock.toc", path, "en");
		int port = TestServerManager.getPort(0);
		String expectedContent = RemoteTestUtils.createMockContent("mock.toc", path, "en", port);
		assertEquals(expectedContent, remoteContent);
	}

	@Test
	public void testEncodedSpace() throws Exception {
		final String path = "/data/help/index/" + URIUtil.fromString("topic .html").toString();
		String remoteContent = getHelpContent("mock.toc", path, "en");
		int port = TestServerManager.getPort(0);
		String expectedContent = RemoteTestUtils.createMockContent("mock.toc", path, "en", port);
		assertEquals(expectedContent, remoteContent);
	}

	@Test
	public void testEncodedPercentSign() throws Exception {
		final String path = "/data/help/index/" + URIUtil.fromString("topic%.html").toString();
		String remoteContent = getHelpContent("mock.toc", path, "en");
		int port = TestServerManager.getPort(0);
		String expectedContent = RemoteTestUtils.createMockContent("mock.toc", path, "en", port);
		assertEquals(expectedContent, remoteContent);
	}

	private static String getHelpContent(String plugin, String path, String locale) throws Exception {
		int port = WebappManager.getPort();
		URL url = new URL("http", "localhost", port, "/help/nftopic/" + plugin + path + "?lang=" + locale);
		return RemoteTestUtils.readFromURL(url);
	}
}
