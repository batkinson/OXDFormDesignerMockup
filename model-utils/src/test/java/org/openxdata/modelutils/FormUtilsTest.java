package org.openxdata.modelutils;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;
import org.fcitmuk.epihandy.FormDef;
import org.fcitmuk.epihandy.xform.EpihandyXform;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormUtilsTest {

	private static Logger log = LoggerFactory.getLogger(FormUtilsTest.class);

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
	public void testUpdateFormVarNameNull() {
		log.debug("testing updates on default form def object");
		FormDef f = new FormDef();
		FormUtils.updateFormVarName(f, "doesnot", "matter");
	}

	@Test
	public void testUpdateFormVarName() {

		log.debug("testing variable name updates");

		String newName = "newname";
		String oldName = sampleForm.getVariableName();

		sampleForm.setVariableName(newName);
		FormUtils.updateFormVarName(sampleForm, oldName, newName);

		String result = ModelToXML.convert(sampleForm);

		log.debug("converted xml after varname update:\n" + result);

		assertTrue("form should not contain old variable name: " + oldName,
				result.indexOf(oldName) == -1);
	}
}
