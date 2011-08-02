package org.openxdata.modelutils;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.fcitmuk.epihandy.FormDef;
import org.fcitmuk.epihandy.PageDef;
import org.fcitmuk.epihandy.xform.EpihandyXform;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuestionTreeTest {

	private static Logger log = LoggerFactory.getLogger(QuestionTreeTest.class);

	static String sampleXml;
	static InputStream sampleStream;

	FormDef sampleForm;

	@BeforeClass
	public static void oneTimeSetup() throws IOException {

		log.debug("reading sample xform");
		InputStream sampleStream = FormUtilsTest.class
				.getResourceAsStream("/sample.xml");
		try {
			sampleXml = IOUtils.toString(sampleStream);
			log.debug("read sample xform:\n" + sampleXml);
		} catch (IOException e) {
			String msg = "Failed to read xform";
			log.error(msg, e);
			throw e;
		}

		// So we can reset
		FormUtilsTest.sampleStream = new ByteArrayInputStream(
				sampleXml.getBytes());
	}

	@Before
	public void setUp() {
		sampleForm = EpihandyXform
				.fromXform2FormDef(new StringReader(sampleXml));
	}

	@After
	public void tearDown() throws IOException {
		FormUtilsTest.sampleStream.reset();
	}

	@Test
	public void testConstructWithNoPages() {
		QuestionTree tree = QuestionTree
				.constructTreeFromFormDef(new FormDef());
		assertTrue("tree should have a single root that is a leaf-node",
				tree.isRoot() && tree.isLeaf());
	}

	@Test
	public void testConstructWithNoQuestions() {

		FormDef f = new FormDef();
		Vector<PageDef> pages = new Vector<PageDef>();
		PageDef p = new PageDef();
		pages.add(p);
		f.setPages(pages);

		QuestionTree tree = QuestionTree
				.constructTreeFromFormDef(new FormDef());
		assertTrue("tree should have a single root that is a leaf-node",
				tree.isRoot() && tree.isLeaf());
	}

	@Test
	public void testConstructWithSampleForm() {
		QuestionTree tree = QuestionTree.constructTreeFromFormDef(sampleForm);
		assertTrue("tree should have a root that is not a leaf-node",
				tree.isRoot() && !tree.isLeaf());
	}
}
