package org.jude.bigdata.recroom.movies.etl.parser;

import org.apache.log4j.Logger;
import org.jude.bigdata.recroom.movies.etl.ETLConstants;
import org.jude.bigdata.recroom.movies.etl.ETLException;

import org.jude.bigdata.recroom.movies.etl.ImdbRecord;

/**
 * Parses the Aka-Titles list
 * 
 * @author user
 * 
 */
public class TaglinesFileParser extends MultilineFileParser {
	static final String SOURCE_NAME = "taglines";
	static final String PRE_HEADER_LINE = "ftp.sunet.se  in  /pub/tv+movies/imdb/tools/w32/";
	static final String HEADER_LINE = "==============";
	static final String END_LINE = "--------------------------------------------------------------------------------";

	Logger logger = Logger.getLogger(TaglinesFileParser.class);

	/**
	 * Constructor. Takes file system directory path for file.
	 * 
	 * @param path
	 */
	public TaglinesFileParser(String path) {
		super(path, ETLConstants.FIELD_MOVIE_ID, false, SOURCE_NAME,
				PRE_HEADER_LINE, HEADER_LINE, END_LINE);
	}

	@Override
	protected ParseResult parseOneLine(String line, ImdbRecord currentJSON)
			throws ETLException {

		// This is the movie line
		if (line.startsWith("#")) {
			// consider it a new movie
			ImdbRecord ret = new ImdbRecord();
			ret.append(this.keyFieldName, line.substring(1).trim());
			ret.append(ETLConstants.FIELD_DOC_TYPE, sourceName);
			return new ParseResult(ret, false);
		} else {
			line = line.trim();
			String existingText = (String)(currentJSON
					.get(ETLConstants.FIELD_DOC_TEXT));
			if (existingText == null) {
				existingText = "";
			}
			existingText += line;
			currentJSON.append(ETLConstants.FIELD_DOC_TEXT, existingText);
			return new ParseResult(currentJSON, false);
		}
	}
}
