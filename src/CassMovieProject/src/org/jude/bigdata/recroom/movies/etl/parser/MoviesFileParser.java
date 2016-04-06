package org.jude.bigdata.recroom.movies.etl.parser;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jude.bigdata.recroom.movies.etl.ETLConstants;
import org.jude.bigdata.recroom.movies.etl.ETLException;
import org.jude.bigdata.recroom.movies.etl.ImdbRecord;

/**
 * Parses the main movie list
 * 
 * @author user
 * 
 */
public class MoviesFileParser extends ImdbLineParser {
	static final String SOURCE_NAME = "movies";
	static final String PRE_HEADER_LINE = "MOVIES LIST";
	static final String HEADER_LINE = "===";
	static final String END_LINE = "------------------------------";

	// "11er Haus" (2005) 2005-????
	// "11er Haus" (2005) {Abschlussfest 2001-2005 (#1.10)} 2005
	// "Breaking Bad" (2008) 2008-2013
	// Goodfellas (1990) 1990
	// Goodfellas (????) (TV) ????
	static final String REGEX = "([^\\t]+)(\\s+)(\\S{4})(-\\S{4})?";

	String mostRecenSeriesID = "";

	Logger logger = Logger.getLogger(MoviesFileParser.class);
	static Pattern pattern = Pattern.compile(REGEX);

	/**
	 * Constructor. Takes file system directory path for file.
	 * 
	 * @param path
	 */
	public MoviesFileParser(String path) {
		super(path, SOURCE_NAME, PRE_HEADER_LINE, HEADER_LINE, END_LINE);
	}

	@Override
	public ImdbRecord parseLine(String line) throws ETLException {
		List<String> toks = getPatternToks(pattern, line.trim());
		if (toks == null || toks.size() < 2 || toks.size() > 3) {
			throw new ETLException(ETLConstants.ERR_MALFORMED_LINE,
					"Illegal line *" + line + "*");
		}

		String movieID = toks.get(0);

		// movie is a feature if it is NOT double-quoted; otherwise, series or
		// episode
		String seriesType = null;
		String seriesID = null;
		if (movieID.startsWith("\"")) {
			// it's an episode if it begins with the ID of the most recent
			// series
			if (mostRecenSeriesID.equals("")
					|| !movieID.startsWith(mostRecenSeriesID)) {
				seriesType = ETLConstants.SERIES_SERIES;
				seriesID = movieID;
				mostRecenSeriesID = seriesID;
			} else {
				seriesType = ETLConstants.SERIES_EPISODE;
				seriesID = mostRecenSeriesID;
			}
		} else {
			seriesType = ETLConstants.SERIES_FEATURE;
			seriesID = movieID;
		}

		// next four characters is release year
		String year = toks.get(1);
		if (year.equals("????")) {
			year = "";
		} else {
			year = validateInt(year, false, true);
		}

		Integer iyear = null;
		if (year.length() == 0) {
			logger.debug("Unable to obtain year for movie in line "
					+ getLineNumber() + " movie is *" + movieID + "*");
			iyear = 0; // it's a required field, so set to zero if no value
		} else {
			iyear = Integer.parseInt(year);
		}

		String seYear = "";
		Integer iseYear = null;
		if (toks.size() == 3) {
			seYear = toks.get(2);
			if (seYear.equals("-????")) {
				seYear = "";
			} else {
				seYear = validateInt(toks.get(2), false, true);
				// chop off the -1 at the beginning of series end year
				if (seYear.startsWith("-")) {
					seYear = seYear.substring(1);
				}
				iseYear = Integer.parseInt(seYear);
			}
		}

		// Return our record
		ImdbRecord json = new ImdbRecord();
		json.append(ETLConstants.FIELD_MOVIE_ID, movieID);
		json.append(ETLConstants.FIELD_SERIES_ID, seriesID);
		json.append(ETLConstants.FIELD_SERIES_TYPE, seriesType);
		json.append(ETLConstants.FIELD_SERIES_END_YEAR, iseYear);
		json.append(ETLConstants.FIELD_RELEASE_YEAR, iyear);
		return json;
	}
}
