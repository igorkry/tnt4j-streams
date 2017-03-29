package com.jkoolcloud.tnt4j.streams.preparsers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.ParseException;
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
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * TODO PreParser for preparing streams of unvalidable XML to actual activity XML parser. It skip's any of unlike XML
 * characters and tries to construct XML DOM structure for actual parser to continue. It uses SAX handler capabilities
 * to parse input stream. The class extends ErrorHandler interface to catch parsing error, on such event the parser
 * skip's a character and continues to parse element
 *
 * @version $Revision: 1 $
 */
public class XMLFromBinDataPreParser extends DefaultHandler
		implements DataPreParser<Document>, ContentHandler, LexicalHandler, ErrorHandler {

	private static final String XML_PREFIX = "<?xml version='1.0' encoding='UTF-8'?>\n"; // NON-NLS
	private static final String ROOT_ELEMENT = "root"; // NON-NLS

	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(XMLFromBinDataPreParser.class);

	private int errorLine;
	private int errorColumn;

	private Node root = null;
	private Document document = null;
	private Stack<Node> _nodeStk = new Stack<>();

	private SAXParser parser;

	private int bPos;

	/**
	 * TODO
	 *
	 * @throws ParserConfigurationException
	 */
	public XMLFromBinDataPreParser() throws ParserConfigurationException {
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
	 * Parsing input stream to prepare XML {@link Document} The method checks for acceptable input source and rethrows
	 * {@link ParseException} if something fails.
	 */
	@Override
	public Document preParse(Object data) throws ParseException {
		if (!initNewDocument()) {
			throw new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"XMLFromBinDataPreParser.doc.init.failure"), 0);
		}
		InputStream is = null;
		if (data instanceof String) {
			is = new ByteArrayInputStream(((String) data).getBytes());
		} else if (data instanceof InputStream) {
			is = (InputStream) data;

			if (!is.markSupported()) {
				try {
					byte[] copy = IOUtils.toByteArray(is); // Read all and make a copy, because stream does not support
					// rewind.
					is = new ByteArrayInputStream(copy);
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
		}
		return getDOM();
	}

	/**
	 * Actual parsing method. Field {@code bPos} represents actual source's position. On the SAX exception {@code bPos}
	 * adjusted to error position and the parser continues from there until the end of stream.
	 *
	 * @param is
	 *            input stream to read
	 *
	 * @throws IOException
	 */
	private void parseBinInput(InputStream is) throws IOException { // TODO: optimize
		while (bPos <= is.available()) {
			byte[] bb = new byte[is.available()];
			try {
				is.read(bb, 0, bPos);
			} catch (IndexOutOfBoundsException e) {
				logger().log(OpLevel.CRITICAL, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"XMLFromBinDataPreParser.IOB"));
			}
			// is.mark(bPos);
			StringWriter writer = new StringWriter();
			writer.write(XML_PREFIX);
			writer.flush();
			IOUtils.copy(is, writer, Utils.UTF8);
			StringBuffer buffer = writer.getBuffer();
			InputStream inputStream = IOUtils.toInputStream(buffer, Utils.UTF8);
			try {
				parser.parse(inputStream, this);
			} catch (Exception e) {
				if (e.getMessage().equals("XML document structures must start and end within the same entity.")) { // NON-NLS
					_nodeStk.pop();
				}
				bPos += errorColumn;
				is.reset();
			}
		}
	}

	private Document getDOM() {
		document.appendChild(root);

		return document;
	}

	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public boolean isDataClassSupported(Object data) {
		return String.class.isInstance(data) || InputStream.class.isInstance(data);
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

		// Push this node onto stack
		logger().log(OpLevel.TRACE, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
				"XMLFromBinDataPreParser.found.element"), qName);
		_nodeStk.push(tmp);
	}

	@Override
	public void endElement(String namespace, String localName, String qName) {
		_nodeStk.pop();
	}

	@Override
	public void endPrefixMapping(String prefix) {
		// do nothing
	}

	/**
	 * This class is only used internally so this method should never be called.
	 */
	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) {
	}

	/**
	 * adds processing instruction node to DOM.
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
	public void setDocumentLocator(Locator locator) {
	}

	/**
	 * This class is only used internally so this method should never be called.
	 */
	@Override
	public void skippedEntity(String name) {
	}

	/**
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

	// Lexical Handler methods- not implemented
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
	public void warning(SAXParseException e) throws SAXException {
		error(e);
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		errorLine = e.getLineNumber();
		errorColumn = e.getColumnNumber();
		logger().log(OpLevel.TRACE,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "XMLFromBinDataPreParser.skipping"),
				bPos);
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		error(e);
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		// TODO Auto-generated method stub
	}
}