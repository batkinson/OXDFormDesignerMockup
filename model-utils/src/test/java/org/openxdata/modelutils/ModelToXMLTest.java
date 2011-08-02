package org.openxdata.modelutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.fcitmuk.epihandy.DynamicOptionDef;
import org.fcitmuk.epihandy.FormDef;
import org.fcitmuk.epihandy.PageDef;
import org.fcitmuk.epihandy.QuestionDef;
import org.fcitmuk.epihandy.xform.EpihandyXform;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

@SuppressWarnings("unchecked")
public class ModelToXMLTest {

	private static Logger log = LoggerFactory.getLogger(ModelToXMLTest.class);

	static FormDef sampleDef;
	static String sampleXml;
	static String convertedXml;
	static FormDef reparsedDef;
	static InputStream convertedStream;
	static InputSource convertedSource;
	static XPath xpath;
	static Map<Short, QuestionDef> dynOptDepMap;

	/**
	 * While not strictly necessary, nothing here needs to be setup more than
	 * once. By only running it once, it should be a bit faster, but more
	 * importantly, the logs will contain the information from a single xml
	 * conversion.
	 */
	@BeforeClass
	public static void setUpOnce() {

		InputStream sampleStream = ModelToXMLTest.class
				.getResourceAsStream("/sample.xml");

		log.debug("loading sample xform");
		try {
			sampleXml = IOUtils.toString(sampleStream);
			log.debug("loaded xml:\n" + sampleXml);
		} catch (IOException e) {
			String msg = "Failed to read xform";
			log.error(msg, e);
			throw new RuntimeException(msg);
		}

		log.debug("converting sample xform to model");
		sampleDef = EpihandyXform
				.fromXform2FormDef(new StringReader(sampleXml));

		log.debug("constructing dynamic option dependency map");
		dynOptDepMap = OptionUtils.getDynOptDepMap(sampleDef);

		log.debug("converting model back to xform");
		convertedXml = ModelToXML.convert(sampleDef);

		log.debug("reconstructing model by parsing converted xform");
		reparsedDef = EpihandyXform.fromXform2FormDef(new StringReader(
				convertedXml));

		log.debug("constructing resettable streams containing converted xml");
		convertedStream = new ByteArrayInputStream(convertedXml.getBytes());
		convertedSource = new InputSource(convertedStream);

		log.debug("constructing xpath engine");
		XPathFactory xpathFactory = javax.xml.xpath.XPathFactory.newInstance();
		xpath = xpathFactory.newXPath();
		xpath.setNamespaceContext(new NamespaceContext());
	}

	@Test
	public void testNullConversion() {
		try {
			ModelToXML.convert(null);
			fail("Should throw exception with null document.");
		} catch (Exception e) {
		}
	}

	@Test
	public void testFormConversion() {
		assertEquals(sampleDef.getVariableName(), reparsedDef.getVariableName());
		assertEquals(sampleDef.getDescriptionTemplate(),
				reparsedDef.getDescriptionTemplate());
		assertEquals(sampleDef.getId(), reparsedDef.getId());
		assertEquals(sampleDef.getName(), reparsedDef.getName());
	}

	@Test
	public void testPageConversion() {
		assertEquals("page counts didn't match", sampleDef.getPageCount(),
				reparsedDef.getPageCount());
		for (int i = 0; i < reparsedDef.getPageCount(); i++) {
			PageDef samplePage = sampleDef.getPageAt(i);
			PageDef reparsedPage = reparsedDef.getPageAt(i);
			assertNotNull("original page should not be null", samplePage);
			assertNotNull("reparsed page should not be null", reparsedPage);
			assertEquals("page numbers should mach", samplePage.getPageNo(),
					reparsedPage.getPageNo());
			assertEquals("page names should match", samplePage.getName(),
					reparsedPage.getName());
		}
	}

	@Test
	public void testBindingConversion() throws Exception {
		String[] exprs = {
				"count(//xf:bind[@id='patientid' and @nodeset='/patientreg/patientid' and @type='xsd:string'])",
				"count(//xf:bind[@id='lastname' and @nodeset='/patientreg/lastname' and @required='true()' and @type='xsd:string'])",
				"count(//xf:bind[@id='pregnant' and @nodeset='/patientreg/pregnant' and @readonly='true()' and @type='xsd:boolean' and @action='enable' and @required='false()'])",
				"count(//xf:bind[@id='picture' and @nodeset='/patientreg/picture' and @type='xsd:base64Binary' and @format='image'])",
				"count(//xf:bind[@id='coughsound' and @nodeset='/patientreg/coughsound' and @format='audio' and @type='xsd:base64Binary'])",
				"count(//xf:bind[@id='recordvideo' and @nodeset='/patientreg/recordvideo' and @format='video' and @type='xsd:base64Binary'])",
				"count(//xf:bind[@id='location' and @nodeset='/patientreg/location' and @format='gps' and @type='xsd:string'])",
				"count(//xf:bind[@id='phone' and @nodeset='/patientreg/phone' and @format='phonenumber' and @type='xsd:string'])",
				"count(//xf:bind[@id='weight' and @nodeset='/patientreg/weight' and @type='xsd:decimal'])",
				"count(//xf:bind[@id='height' and @nodeset='/patientreg/height' and @type='xsd:int'])",
				"count(//xf:bind[@id='birthdate' and @nodeset='/patientreg/birthdate' and @type='xsd:date'])",
				"count(//xf:bind[@id='starttime' and @nodeset='/patientreg/starttime' and @type='xsd:time'])",
				"count(//xf:bind[@id='visitdate' and @nodeset='/patientreg/visitdate' and @type='xsd:dateTime'])",
				"count(//xf:bind[@id='kid' and @nodeset='/patientreg/kids/kid' and @readonly='true()' and @action='enable' and @required='false()'])" };
		for (String expr : exprs) {
			convertedStream.reset(); // Restore stream state
			XPathExpression compiledExpr = xpath.compile(expr);
			Double matchCount = (Double) compiledExpr.evaluate(convertedSource,
					XPathConstants.NUMBER);
			assertEquals(expr + " unique binding not present ", 1,
					matchCount.intValue());
		}
	}

	@Test
	public void testBoundInputConversion() throws Exception {

		String matchPattern = "count(//xf:input[@bind=''{0}''])";
		String[] matchQuestions = { "patientid", "location", "phone", "weight",
				"height", "birthdate", "starttime", "visitdate" };

		for (String matchQuestion : matchQuestions) {
			convertedStream.reset(); // Restore stream state
			String expr = MessageFormat.format(matchPattern, matchQuestion);
			XPathExpression compiledExpr = xpath.compile(expr);
			Double matchCount = (Double) compiledExpr.evaluate(convertedSource,
					XPathConstants.NUMBER);
			assertEquals(expr + " input not present ", 1, matchCount.intValue());
		}

		String[] noMatchQuestions = { "kid", "kidsex", "kidage", "title",
				"sex", "arvs", "picture", "coughsound", "recordvideo",
				"continent", "country", "district", "village" };

		for (String noMatchQuestion : noMatchQuestions) {
			convertedStream.reset(); // Restore stream state
			String expr = MessageFormat.format(matchPattern, noMatchQuestion);
			XPathExpression compiledExpr = xpath.compile(expr);
			Double matchCount = (Double) compiledExpr.evaluate(convertedSource,
					XPathConstants.NUMBER);
			assertEquals(expr + " input should not be present ", 0,
					matchCount.intValue());
		}
	}

	@Test
	public void testBoundUploadConversion() throws Exception {

		String matchPattern = "count(//xf:upload[@bind=''{0}'' and @mediatype=''{1}''])";
		String[][] matchParams = { { "picture", "image/*" },
				{ "coughsound", "audio/*" }, { "recordvideo", "video/*" } };

		for (String[] matchQuestion : matchParams) {
			convertedStream.reset(); // Restore stream state
			String expr = MessageFormat.format(matchPattern,
					(Object[]) matchQuestion);
			XPathExpression compiledExpr = xpath.compile(expr);
			Double matchCount = (Double) compiledExpr.evaluate(convertedSource,
					XPathConstants.NUMBER);
			assertEquals(expr + " upload not present ", 1,
					matchCount.intValue());
		}
	}

	@Test
	public void testExclusiveListConversion() throws Exception {

		String matchPattern = "count(//xf:select1[@bind=''{0}'' and count(xf:item) = ''{1}''])";
		String[] matchParams = { "title", "sex", "continent" };

		for (String matchQuestion : matchParams) {
			QuestionDef qDef = sampleDef.getQuestion("/patientreg/"
					+ matchQuestion);
			convertedStream.reset(); // Restore stream state
			String expr = MessageFormat.format(matchPattern, matchQuestion,
					Integer.toString(qDef.getOptions().size()));
			XPathExpression compiledExpr = xpath.compile(expr);
			Double matchCount = (Double) compiledExpr.evaluate(convertedSource,
					XPathConstants.NUMBER);
			assertEquals(expr + " select1 not present ", 1,
					matchCount.intValue());
		}
	}

	@Test
	public void testMultiSelectListConversion() throws Exception {

		String matchPattern = "count(//xf:select[@bind=''{0}'' and count(xf:item) = ''{1}''])";
		String[] matchParams = { "arvs" };

		for (String matchQuestion : matchParams) {
			QuestionDef qDef = sampleDef.getQuestion("/patientreg/"
					+ matchQuestion);
			convertedStream.reset(); // Restore stream state
			String expr = MessageFormat.format(matchPattern, matchQuestion,
					Integer.toString(qDef.getOptions().size()));
			XPathExpression compiledExpr = xpath.compile(expr);
			Double matchCount = (Double) compiledExpr.evaluate(convertedSource,
					XPathConstants.NUMBER);
			assertEquals(expr + " select not present ", 1,
					matchCount.intValue());
		}
	}

	@Test
	public void testDynamicListConversion() throws Exception {

		String matchPattern = "count(//xf:select1[@bind=''{1}'']/xf:itemset[@nodeset=\"instance(''{1}'')/item[@parent=instance(''{0}'')/{2}]\"])";
		String[][] matchParams = { { "country", "continent" },
				{ "district", "country" }, { "village", "district" } };

		for (String[] matchQuestion : matchParams) {
			convertedStream.reset(); // Restore stream state
			String expr = MessageFormat.format(matchPattern, "patientreg",
					matchQuestion[0], matchQuestion[1]);
			XPathExpression compiledExpr = xpath.compile(expr);
			Double matchCount = (Double) compiledExpr.evaluate(convertedSource,
					XPathConstants.NUMBER);
			assertEquals(expr + " select1 not present ", 1,
					matchCount.intValue());
		}
	}

	@Test
	public void testDynamicListItemConversion() throws Exception {

		String matchPattern = "count(//xf:instance[@id=''{0}'']/dynamiclist[count(item) = ''{1}''])";
		String[] matchParams = { "country", "district", "village" };

		for (String matchQuestion : matchParams) {
			QuestionDef qDef = sampleDef.getQuestion("/patientreg/"
					+ matchQuestion);
			QuestionDef parentQuestionDef = dynOptDepMap.get(qDef.getId());
			int optCount = 0;
			DynamicOptionDef dynOptDef = sampleDef
					.getDynamicOptions(parentQuestionDef.getId());
			for (Short key : (Set<Short>) dynOptDef.getParentToChildOptions()
					.keySet())
				optCount += dynOptDef.getOptionList(key).size();
			convertedStream.reset(); // Restore stream state
			String expr = MessageFormat.format(matchPattern, matchQuestion,
					optCount);
			XPathExpression compiledExpr = xpath.compile(expr);
			Double matchCount = (Double) compiledExpr.evaluate(convertedSource,
					XPathConstants.NUMBER);
			assertEquals(expr + " instance not present ", 1,
					matchCount.intValue());
		}
	}

	@Test
	public void testDefaultsConversion() throws Exception {

		String matchPattern = "count(//xf:instance[@id=''{0}'']/{0}[{1} = \"{2}\"])";
		String[][] matchParams = { { "patientreg", "nokids", "0" },
				{ "patientreg", "starttime", "'today()'" } };

		for (String[] matchParam : matchParams) {
			convertedStream.reset(); // Restore stream state
			String expr = MessageFormat.format(matchPattern,
					(Object[]) matchParam);
			XPathExpression compiledExpr = xpath.compile(expr);
			Double matchCount = (Double) compiledExpr.evaluate(convertedSource,
					XPathConstants.NUMBER);
			assertEquals(expr + " default value not present ", 1,
					matchCount.intValue());
		}
	}

	@Test
	public void testValidationConversion() throws Exception {
		String matchPattern = "count(//xf:bind[@id=''{0}'' and @constraint=''{1}'' and @message=''{2}''])";
		String[][] matchParams = {
				{ "birthdate", ". <= today()", "Cannot be greater than today" },
				{ "weight", ". >= 1.1 and . <= 99.9",
						"Should be between 0 and 200 inclusive" },
				{ "height", ". >= 1 and . >= 20",
						"Should be between 1 and 20 inclusive" },
				{ "nokids", ". >= 0 and . < 100", "Should be between 0 and 100" },
				{ "kid", "length(.) = patientreg/nokids",
						"Kid rows should be equal to the number of kids" }, };

		for (String[] matchParam : matchParams) {
			convertedStream.reset(); // Restore stream state
			String expr = MessageFormat.format(matchPattern,
					(Object[]) matchParam);
			XPathExpression compiledExpr = xpath.compile(expr);
			Double matchCount = (Double) compiledExpr.evaluate(convertedSource,
					XPathConstants.NUMBER);
			assertEquals(expr + " validation rule not present ", 1,
					matchCount.intValue());
		}
	}

	@Test
	public void testSkipLogicConversion() throws Exception {
		String[] matchPatterns = {
				"count(//xf:bind[@id='pregnant' and @relevant=\"/patientreg/sex = 'female'\" and @action='enable'])",
				"count(//xf:bind[@id='yearsmarried' and @relevant=\"/patientreg/title contains('r')\" and @action='show'])",
				"count(//xf:bind[@id='evermarried' and @relevant=\"/patientreg/title not(contains('r'))\"])",
				"count(//xf:bind[@id='seenme' and @relevant=\"/patientreg/country starts-with('us')\"])",
				"count(//xf:bind[@id='willseeme' and @relevant=\"/patientreg/country not(starts-with('us'))\"])", };

		for (String matchPattern : matchPatterns) {
			convertedStream.reset(); // Restore stream state
			String expr = matchPattern;
			XPathExpression compiledExpr = xpath.compile(matchPattern);
			Double matchCount = (Double) compiledExpr.evaluate(convertedSource,
					XPathConstants.NUMBER);
			assertEquals(expr + " skip rule not present ", 1,
					matchCount.intValue());
		}
	}

	@Test
	public void testRepeatInstanceConversion() throws Exception {
		String matchPattern = "count(//xf:instance[@id=''{0}'']/{0}/kids/kid/{1})";
		String[] matchParams = { "kidname", "kidsex", "kidage" };

		String instanceId = "patientreg";

		for (String matchParam : matchParams) {
			convertedStream.reset(); // Restore stream state
			String expr = MessageFormat.format(matchPattern, new Object[] {
					instanceId, matchParam });
			XPathExpression compiledExpr = xpath.compile(expr);
			Double matchCount = (Double) compiledExpr.evaluate(convertedSource,
					XPathConstants.NUMBER);
			assertEquals(expr + " repeat instance element not present ", 1,
					matchCount.intValue());
		}
	}

	@Test
	public void testRepeatControlConversion() throws Exception {
		String matchPattern = "count(//xf:group[@id=''{0}'' and xf:label = ''{1}'' and xf:repeat[@bind=''{2}'']])";
		String[][] matchParams = { { "kids/kid", "Kids", "kid" } };

		for (String[] matchParam : matchParams) {
			convertedStream.reset(); // Restore stream state
			String expr = MessageFormat.format(matchPattern,
					(Object[]) matchParam);
			XPathExpression compiledExpr = xpath.compile(expr);
			Double matchCount = (Double) compiledExpr.evaluate(convertedSource,
					XPathConstants.NUMBER);
			assertEquals(expr + " repeat control not present ", 1,
					matchCount.intValue());
		}
	}

	@Test
	public void testRepeatBindConversion() throws Exception {
		String[] matchPatterns = { "count(//xf:bind[@id='kid' and @nodeset='/patientreg/kids/kid'])" };

		for (String matchPattern : matchPatterns) {
			convertedStream.reset(); // Restore stream state
			String expr = matchPattern;
			XPathExpression compiledExpr = xpath.compile(expr);
			Double matchCount = (Double) compiledExpr.evaluate(convertedSource,
					XPathConstants.NUMBER);
			assertEquals(expr + " repeat bind not present ", 1,
					matchCount.intValue());
		}
	}

	@Test
	public void testNestedControlConversion() throws Exception {
		String matchPattern = "count(//xf:group[@id=''kids/kid'']/xf:repeat/xf:{0}[@ref=''{1}'' and @type=''{2}''])";
		String[][] matchParams = { { "input", "kidname", "xsd:string" },
				{ "select1", "kidsex", "xsd:string" },
				{ "input", "kidage", "xsd:int" } };

		for (String[] matchParam : matchParams) {
			convertedStream.reset(); // Restore stream state
			String expr = MessageFormat.format(matchPattern,
					(Object[]) matchParam);
			XPathExpression compiledExpr = xpath.compile(expr);
			Double matchCount = (Double) compiledExpr.evaluate(convertedSource,
					XPathConstants.NUMBER);
			assertEquals(expr + " nested control not present ", 1,
					matchCount.intValue());
		}
	}

	@Test
	public void testNoNSConverstion() {
		log.debug("testing conversion to xml with no namespace");
		String convertedXml = ModelToXML.convert(sampleDef, false);
		log.debug("converted xml without namespace:\n" + convertedXml);
		assertFalse("exported xml should not contain namespace prefixes",
				convertedXml.contains("xf:"));
	}
}
