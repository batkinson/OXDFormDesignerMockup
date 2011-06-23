package org.openxdata.modelutils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import junit.framework.TestCase;

import org.fcitmuk.epihandy.FormDef;
import org.fcitmuk.epihandy.PageDef;
import org.fcitmuk.epihandy.xform.EpihandyXform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelToXMLTest extends TestCase {

	private static Logger log = LoggerFactory.getLogger(ModelToXMLTest.class);

	FormDef sampleDef;
	String sampleXml;

	{
		InputStream sampleStream = ModelToXMLTest.class
				.getResourceAsStream("/sample.xml");

		BufferedReader sampleReader = new BufferedReader(new InputStreamReader(
				sampleStream));
		StringBuilder buf = new StringBuilder();
		String line = null;
		try {
			while ((line = sampleReader.readLine()) != null)
				buf.append(line);
			buf.append('\n');
			sampleXml = buf.toString();
		} catch (IOException e) {
			String msg = "Failed to read in sample.xml";
			log.error(msg, e);
			throw new RuntimeException(msg);
		}

		sampleDef = EpihandyXform
				.fromXform2FormDef(new StringReader(sampleXml));
	}

	public void testNullConversion() {
		try {
			ModelToXML.convert(null);
			fail("Should throw exception with null document.");
		} catch (Exception e) {
		}
	}

	public void testFormConversion() {
		String result = ModelToXML.convert(sampleDef);
		FormDef formDef = EpihandyXform.fromXform2FormDef(new StringReader(
				result));
		assertEquals(sampleDef.getVariableName(), formDef.getVariableName());
		assertEquals(sampleDef.getDescriptionTemplate(),
				formDef.getDescriptionTemplate());
		assertEquals(sampleDef.getId(), formDef.getId());
		assertEquals(sampleDef.getName(), formDef.getName());
	}

	public void testPageConversion() {
		String result = ModelToXML.convert(sampleDef);
		FormDef formDef = EpihandyXform.fromXform2FormDef(new StringReader(
				result));
		assertEquals("page counts didn't match", sampleDef.getPageCount(),
				formDef.getPageCount());
		for (int i = 0; i < formDef.getPageCount(); i++) {
			PageDef samplePage = sampleDef.getPageAt(i);
			PageDef reparsedPage = formDef.getPageAt(i);
			assertNotNull("original page should not be null", samplePage);
			assertNotNull("reparsed page should not be null", reparsedPage);
			assertEquals("page numbers should mach", samplePage.getPageNo(),
					reparsedPage.getPageNo());
			assertEquals("page names should match", samplePage.getName(),
					reparsedPage.getName());
		}
	}

}
