package org.openxdata.modelutils;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;
import org.fcitmuk.epihandy.FormDef;
import org.fcitmuk.epihandy.xform.EpihandyXform;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuleUtilsTest {

	private static Logger log = LoggerFactory.getLogger(RuleUtilsTest.class);

	static String skipruleXml;
	static InputStream skipruleStream;

	FormDef skipruleForm;

	@BeforeClass
	public static void oneTimeSetup() throws IOException {

		log.debug("reading skiprulecons xform");
		InputStream skipruleStream = FormUtilsTest.class
				.getResourceAsStream("/skiprulecons.xml");
		try {
			skipruleXml = IOUtils.toString(skipruleStream);
			log.debug("read sample xform:\n" + skipruleXml);
		} catch (IOException e) {
			String msg = "Failed to read xform";
			log.error(msg, e);
			throw e;
		}

		// So we can reset
		FormUtilsTest.sampleStream = new ByteArrayInputStream(
				skipruleXml.getBytes());
	}

	@Before
	public void setUp() {
		skipruleForm = EpihandyXform.fromXform2FormDef(new StringReader(
				skipruleXml));
	}

	@Test
	public void testConsolidateSkipRules() {
		RuleUtils.consolidateSkipRules(skipruleForm);
		assertEquals(3, skipruleForm.getSkipRules().size());
	}
}
