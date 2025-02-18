/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
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
 *     Alexander Kurtakov - Bug 460787
 *******************************************************************************/

package org.eclipse.ua.tests.help.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.search.AnalyzerDescriptor;
import org.eclipse.help.internal.search.PluginIndex;
import org.eclipse.help.internal.search.QueryBuilder;
import org.eclipse.help.internal.search.SearchIndexWithIndexingProgress;
import org.eclipse.ua.tests.plugin.UserAssistanceTestPlugin;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Verify that older versions of the index can be read by this
 * version of Eclipse.
 *
 * How to maintain this test - if when upgrading to a new version
 * of Lucene one of the IndexReadable tests fails you need to
 * make the following changes:
 * 1. Change the corresponding Compatible() test to expect a result of false
 * 2. Comment out the failing test
 * 3. Change the help system to recognize that version of Lucene as being incompatible
 */

public class PrebuiltIndexCompatibility {

	/**
	 * Test index built with Lucene 1.9.1
	 */
	@Test(expected = IndexFormatTooOldException.class)
	public void test1_9_1_IndexUnReadable() throws Exception {
		checkReadable("data/help/searchindex/index191");
	}

	/**
	 * Test index built with Lucene 2.9.1
	 */
	@Test(expected = IndexFormatTooOldException.class)
	public void test2_9_1_IndexUnReadable() throws Exception {
		checkReadable("data/help/searchindex/index291");
	}

	/**
	 * Test index built with Lucene 3.5.0
	 */
	@Test(expected = IndexFormatTooOldException.class)
	public void test3_5_0_IndexUnReadable() throws Exception {
		checkReadable("data/help/searchindex/index350");
	}

	/**
	 * Test index built with Lucene 6.1.0
	 */
	@Test(expected = IndexFormatTooOldException.class)
	public void test6_1_0_IndexUnReadable() throws Exception {
		checkReadable("data/help/searchindex/index610");
	}

	/**
	 * Test index built with Lucene 7.0.0
	 */
	@Test(expected = IndexFormatTooOldException.class)
	public void test7_0_0_IndexUnReadable() throws Exception {
		checkReadable("data/help/searchindex/index700");
	}

	/**
	 * Test index built with Lucene 8.0.0
	 */
	@Test
	public void test8_0_0_IndexReadable() throws Exception {
		checkReadable("data/help/searchindex/index800");
	}

	/**
	 ** Test compatibility of Lucene 1.9.1 index with current Lucene
	 */
	@Test
	public void test1_9_1Compatible()
	{
		checkCompatible("data/help/searchindex/index191", false);
	}

	/**
	 ** Test compatibility of Lucene 2.9.1 index with current Lucene
	 */
	@Test
	public void test2_9_1Compatible()
	{
		checkCompatible("data/help/searchindex/index291", false);
	}

	/**
	 ** Test compatibility of Lucene 3.5.0 index with current Lucene
	 */
	@Test
	public void test3_5_0Compatible() {
		checkCompatible("data/help/searchindex/index350", false);
	}

	/**
	 ** Test compatibility of Lucene 6.1.0 index with current Lucene
	 */
	@Test
	public void test6_1_0Compatible() {
		checkCompatible("data/help/searchindex/index610", false);
	}

	/**
	 ** Test compatibility of Lucene 7.0.0 index with current Lucene
	 */
	@Test
	public void test7_0_0Compatible() {
		checkCompatible("data/help/searchindex/index700", false);
	}

	@Test
	public void test1_9_1LuceneCompatible()
	{
		checkLuceneCompatible("1.9.1", false);
	}

	@Test
	public void test1_4_103NotLuceneCompatible()
	{
		checkLuceneCompatible("1.4.103", false);
	}

	@Test
	public void test2_9_1LuceneCompatible()
	{
		checkLuceneCompatible("2.9.1", false);
	}

	@Test
	public void test3_5_0LuceneCompatible() {
		checkLuceneCompatible("3.5.0", false);
	}

	@Test
	public void test6_1_0LuceneCompatible() {
		checkLuceneCompatible("6.1.0", false);
	}

	@Test
	public void test7_0_0LuceneCompatible() {
		checkLuceneCompatible("7.0.0", false);
	}

	@Test
	public void test8_0_0LuceneCompatible() {
		checkLuceneCompatible("8.0.0", true);
	}

	@Test
	public void testPluginIndexEqualToItself() {
		PluginIndex index = createPluginIndex("data/help/searchindex/index610");
		assertTrue(index.equals(index));
	}

	/**
	 * Verify that if the paths and plugins are the same two PluginIndex objects are equal
	 */
	@Test
	public void testPluginIndexEquality() {
		PluginIndex index1a = createPluginIndex("data/help/searchindex/index610");
		PluginIndex index1b = createPluginIndex("data/help/searchindex/index610");
		assertTrue(index1a.equals(index1b));
	}

	/**
	 * Verify that if the paths and plugins are the same two PluginIndex objects are equal
	 */
	@Test
	public void testPluginIndexHash() {
		PluginIndex index1a = createPluginIndex("data/help/searchindex/index610");
		PluginIndex index1b = createPluginIndex("data/help/searchindex/index610");
		assertEquals(index1a.hashCode(), index1b.hashCode());
	}

	/**
	 * Verify that if the paths are different two PluginIndex objects are not equal
	 */
	@Test
	public void testPluginIndexInequality() {
		PluginIndex index1 = createPluginIndex("data/help/searchindex/index610");
		PluginIndex index2 = createPluginIndex("data/help/searchindex/index350");
		assertFalse(index1.equals(index2));
	}

	/*
	 * Verifies that a prebuilt index can be searched
	 */
	private void checkReadable(String indexPath) throws IOException,
			CorruptIndexException {
		Path path = new Path(indexPath);
		Bundle bundle = UserAssistanceTestPlugin.getDefault().getBundle();
		URL url = FileLocator.find(bundle, path, null);
		URL resolved = FileLocator.resolve(url);
		if ("file".equals(resolved.getProtocol())) { //$NON-NLS-1$
			String filePath = resolved.getFile();
			QueryBuilder queryBuilder = new QueryBuilder("eclipse", new AnalyzerDescriptor("en-us"));
			Query luceneQuery = queryBuilder.getLuceneQuery(new ArrayList<String>() , false);
			IndexSearcher searcher;
			try (Directory luceneDirectory = new NIOFSDirectory(new File(filePath).toPath())) {
				searcher = new IndexSearcher(DirectoryReader.open(luceneDirectory));
				TopDocs hits = searcher.search(luceneQuery, 500);
				assertTrue(hits.totalHits.value >= 1);
			}
		} else {
			fail("Cannot resolve to file protocol");
		}
	}

	/*
	 * Tests the isCompatible method in PluginIndex
	 */
	private void checkCompatible(String versionDirectory, boolean expected) {
		PluginIndex pluginIndex = createPluginIndex(versionDirectory);
		Path path = new Path(versionDirectory);
		assertEquals(expected, pluginIndex.isCompatible(UserAssistanceTestPlugin.getDefault().getBundle(), path));
	}

	public PluginIndex createPluginIndex(String versionDirectory) {
		PluginIndex pluginIndex;
		SearchIndexWithIndexingProgress index = BaseHelpSystem.getLocalSearchManager().getIndex("en_us".toString());
		BaseHelpSystem.getLocalSearchManager().ensureIndexUpdated(
				new NullProgressMonitor(),
				index);
		pluginIndex = new PluginIndex("org.eclipse.ua.tests", "data/help/searchindex/" + versionDirectory, index);
		return pluginIndex;
	}

	/*
	 * Tests the isLuceneCompatible method in SearchIndex
	 */
	private void checkLuceneCompatible(String version, boolean expected) {
		SearchIndexWithIndexingProgress index = BaseHelpSystem.getLocalSearchManager().getIndex("en_us".toString());
		BaseHelpSystem.getLocalSearchManager().ensureIndexUpdated(
				new NullProgressMonitor(),
				index);
		assertEquals(expected, index.isLuceneCompatible(version));
	}
}
