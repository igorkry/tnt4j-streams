package com.jkoolcloud.tnt4j.streams.preparsers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldFormatType;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Pre-parser to convert RAW binary activity data to valid XML parseable by actual activity XML parser. It skip's any of
 * unlike XML characters and tries to construct XML DOM structure for actual parser to continue. It uses SAX handler
 * capabilities to parse input stream. The class extends ErrorHandler interface to catch parsing error, on such event
 * the parser skip's a character and continues to parse element.
 * <p>
 * This preparer is also capable to make valid XML document from RAW activity data having truncated XML structures.
 *
 * @version $Revision: 1 $
 */
public class XMLFromBinDataPreParser extends DefaultHandler
		implements ActivityDataPreParser<Document>, ContentHandler, LexicalHandler, ErrorHandler {

	private static final String XML_PREFIX = "<?xml version='1.0' encoding='UTF-8'?>\n"; // NON-NLS
	private static final String ROOT_ELEMENT = "root"; // NON-NLS

	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(XMLFromBinDataPreParser.class);

	private ActivityFieldFormatType format;

	private Position errorPosition = new Position();
	private Position lastGoodPosition = new Position();

	private Locator locator;

	private Node root = null;
	private Document document = null;
	private Stack<Node> _nodeStk = new Stack<>();

	private SAXParser parser;

	private int bPos;

	/**
	 * Constructs a new XMLFromBinDataPreParser.
	 *
	 * @throws ParserConfigurationException
	 *             if initialization of SAX parser fails
	 */
	public XMLFromBinDataPreParser() throws ParserConfigurationException {
		this(ActivityFieldFormatType.bytes);
	}

	/**
	 * Constructs a new XMLFromBinDataPreParser.
	 *
	 * @param type
	 *            RAW activity data format name from {@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldFormatType}
	 *            enumeration
	 *
	 * @throws ParserConfigurationException
	 *             if initialization of SAX parser fails
	 */
	public XMLFromBinDataPreParser(String type) throws ParserConfigurationException {
		this(ActivityFieldFormatType.valueOf(type));
	}

	/**
	 * Constructs a new XMLFromBinDataPreParser.
	 *
	 * @param format
	 *            RAW activity data format
	 *
	 * @throws ParserConfigurationException
	 *             if initialization of SAX parser fails
	 */
	public XMLFromBinDataPreParser(ActivityFieldFormatType format) throws ParserConfigurationException {
		this.format = format;

		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			parser = parserFactory.newSAXParser();
			parser.getXMLReader().setErrorHandler(this);
		} catch (Exception e) {
			throw new ParserConfigurationException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"XMLFromBinDataPreParser.init.failure"));
		}
	}

	private boolean initNewDocument() {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			document = factory.newDocumentBuilder().newDocument();
			root = document.createElement(ROOT_ELEMENT);
			bPos = 0;
			return true;
		} catch (ParserConfigurationException e1) {
			return false;
			// can't initialize
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Parsing input stream to prepare XML {@link Document} The method checks for acceptable input source and rethrows
	 * {@link ParseException} if something fails.
	 */
	@Override
	public Document preParse(Object data) throws ParseException {
		if (!initNewDocument()) {
			throw new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"XMLFromBinDataPreParser.doc.init.failure"), 0);
		}
		InputStream is;
		boolean closeWhenDone = false;
		if (data instanceof String) {
			switch (format) {
			case base64Binary:
				is = new ByteArrayInputStream(Utils.base64Decode((String) data));
				break;
			case hexBinary:
				is = new ByteArrayInputStream(Utils.decodeHex((String) data));
				break;
			case bytes:
			case string:
			default:
				is = new ByteArrayInputStream(((String) data).getBytes());
				break;
			}
			closeWhenDone = true;
		} else if (data instanceof byte[]) {
			is = new ByteArrayInputStream((byte[]) data);
			closeWhenDone = true;
		} else if (data instanceof InputStream) {
			is = (InputStream) data;

			if (!is.markSupported()) {
				try {
					byte[] copy = IOUtils.toByteArray(is); // Read all and make a copy, because stream does not support
					// rewind.
					is = new ByteArrayInputStream(copy);
					closeWhenDone = true;
				} catch (IOException e) {
					throw new ParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"XMLFromBinDataPreParser.data.read.failed", e.getLocalizedMessage()), 0);
				}
			}
		} else {
			throw new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"XMLFromBinDataPreParser.input.unsupported"), 0);
		}

		try {
			parseBinInput(is);
		} catch (IOException e) {
			throw new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"XMLFromBinDataPreParser.bin.data.parse.failure"), 0);
		} finally {
			if (closeWhenDone) {
				Utils.close(is);
			}
		}
		return getDOM();
	}

	/**
	 * Actual RAW data parsing method. Field {@code bPos} represents actual position in source data. On the SAX
	 * exception {@code bPos} adjusted to error position and the parser continues from there until the end of stream.
	 *
	 * @param is
	 *            input stream to read
	 * @throws IOException
	 *             if I/O exception occurs while reading input stream
	 */
	private void parseBinInput(InputStream is) throws IOException {
		InputStream prefixIS = new ByteArrayInputStream(XML_PREFIX.getBytes());
		SequenceInputStream sIS = new SequenceInputStream(Collections.enumeration(Arrays.asList(prefixIS, is)));
		InputSource parserInputSource = new InputSource(sIS);
		parserInputSource.setEncoding(Utils.UTF8);

		while (bPos <= is.available()) {
			try {
				is.skip(bPos);
			} catch (IOException e) {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"XMLFromBinDataPreParser.skip.failed"), e);
				return;
			}
			try {
				parser.parse(parserInputSource, this);
			} catch (Exception e) {
				bPos += errorPosition.column;

				prefixIS.reset();
				is.reset();

				if (bPos >= is.available()) {
					handleTrailingText(is);
				}

				if (!_nodeStk.isEmpty()) {
					_nodeStk.pop();
				}

			}
		}

		Utils.close(prefixIS);
	}

	/**
	 * In case of incomplete XML data, preserves last chunk of characters as last element text data.
	 * 
	 * @param is
	 *            input stream to read
	 * @throws IOException
	 *             if I/O exception occurs while reading input stream
	 */
	protected void handleTrailingText(InputStream is) throws IOException {
		int textLength = errorPosition.column - lastGoodPosition.column;

		if (textLength > 0) {
			byte[] buffer = new byte[textLength];
			try {
				is.skip(bPos - 1 - buffer.length);
				is.read(buffer);
				String text = new String(buffer, Utils.UTF8);
				characters(text.toCharArray(), 0, text.length());
			} finally {
				is.reset();
			}
		}
	}

	private Document getDOM() {
		document.appendChild(root);

		return document;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This pre-parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link java.lang.String}</li>
	 * <li>{@link java.io.InputStream}</li>
	 * <li>{@code byte[]}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return String.class.isInstance(data) || InputStream.class.isInstance(data) || byte[].class.isInstance(data);
	}

	@Override
	public String dataTypeReturned() {
		return "XML"; // NON-NLS
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		Node last = _nodeStk.peek();

		// No text nodes can be children of root (DOM006 exception)
		if (last != document) {
			String text = new String(ch, start, length);
			last.appendChild(document.createTextNode(text));
		}
	}

	@Override
	public void startDocument() {
		_nodeStk.push(root);
	}

	@Override
	public void endDocument() {
		_nodeStk.pop();
	}

	@Override
	public void startElement(String namespace, String localName, String qName, Attributes attrs) {
		Element tmp = document.createElementNS(namespace, qName);

		// Add attributes to element
		int attrsCount = attrs.getLength();
		for (int i = 0; i < attrsCount; i++) {
			if (attrs.getLocalName(i) == null) {
				tmp.setAttribute(attrs.getQName(i), attrs.getValue(i));
			} else {
				tmp.setAttributeNS(attrs.getURI(i), attrs.getQName(i), attrs.getValue(i));
			}
		}

		// Append this new node onto current stack node
		Node last = _nodeStk.peek();
		last.appendChild(tmp);

		markLastGoodPosition();

		// Push this node onto stack
		LOGGER.log(OpLevel.TRACE, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
				"XMLFromBinDataPreParser.found.element"), qName);
		_nodeStk.push(tmp);
	}

	@Override
	public void endElement(String namespace, String localName, String qName) {
		_nodeStk.pop();

		markLastGoodPosition();
	}

	private void markLastGoodPosition() {
		lastGoodPosition.line = locator.getLineNumber();
		lastGoodPosition.column = locator.getColumnNumber();
	}

	/**
	 * Adds processing instruction node to DOM.
	 */
	@Override
	public void processingInstruction(String target, String data) {
		Node last = _nodeStk.peek();
		ProcessingInstruction pi = document.createProcessingInstruction(target, data);
		if (pi != null) {
			last.appendChild(pi);
		}
	}

	/**
	 * This class is only used internally so this method should never be called.
	 */
	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) {
	}

	/**
	 * This class is only used internally so this method should never be called.
	 */
	@Override
	public void setDocumentLocator(Locator locator) {
		this.locator = locator;
	}

	/**
	 * This class is only used internally so this method should never be called.
	 */
	@Override
	public void skippedEntity(String name) {
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Lexical Handler method to create comment node in DOM tree.
	 */
	@Override
	public void comment(char[] ch, int start, int length) {
		Node last = _nodeStk.peek();
		Comment comment = document.createComment(new String(ch, start, length));
		if (comment != null) {
			last.appendChild(comment);
		}
	}

	@Override
	public void warning(SAXParseException e) throws SAXException {
		error(e);
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		errorPosition.line = e.getLineNumber();
		errorPosition.column = e.getColumnNumber();
		LOGGER.log(OpLevel.TRACE,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "XMLFromBinDataPreParser.skipping"),
				bPos);
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		error(e);
	}

	// Lexical Handler methods - not implemented
	@Override
	public void startCDATA() {
	}

	@Override
	public void endCDATA() {
	}

	@Override
	public void startEntity(java.lang.String name) {
	}

	@Override
	public void endEntity(String name) {
	}

	@Override
	public void startDTD(String name, String publicId, String systemId) throws SAXException {
	}

	@Override
	public void endDTD() {
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
	}

	@Override
	public void endPrefixMapping(String prefix) {
	}

	private static class Position {
		int line;
		int column;
	}
}