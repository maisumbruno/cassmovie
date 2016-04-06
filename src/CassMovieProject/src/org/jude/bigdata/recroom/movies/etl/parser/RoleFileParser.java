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
public class RoleFileParser extends MultilineFileParser {

	Logger logger = Logger.getLogger(RoleFileParser.class);

	String prevMovieID = null;

	/**
	 * Constructor. Takes file system directory path for file.
	 * 
	 * @param path
	 */
	public RoleFileParser(String path, String sourceName, String preHeaderLine,
			String headerLine, String endLine) {
		super(path, ETLConstants.FIELD_CONTRIB_ID, true, sourceName,
				preHeaderLine, headerLine, endLine);
	}

	@Override
	protected ParseResult parseOneLine(String line, ImdbRecord currentJSON)
			throws ETLException {

		// This is a just a role line
		if (line.startsWith("\t")) {
			line = line.substring(1).trim();
			validateKey(currentJSON);

			// build new role
			ImdbRecord newRole = buildRole(line,
					(String) (currentJSON.get(ETLConstants.FIELD_CONTRIB_ID)));
			prevMovieID = (String) (newRole.get(ETLConstants.FIELD_MOVIE_ID));

			String currMovieID = (String) (currentJSON
					.get(ETLConstants.FIELD_MOVIE_ID));
			if (!currMovieID.equals(prevMovieID)) {
				// if change in movie, flush the old one
				return new ParseResult(currentJSON, newRole);
			} else {
				// append role desc to current
				newRole.append(
						ETLConstants.FIELD_CONTRIB_ROLE,
						addToCSV((String) (currentJSON
								.get(ETLConstants.FIELD_CONTRIB_ROLE)),
								(String) (newRole
										.get(ETLConstants.FIELD_CONTRIB_ROLE))));
				newRole.append(
						ETLConstants.FIELD_CONTRIB_ROLEDETAIL,
						addToCSV(
								(String) (currentJSON
										.get(ETLConstants.FIELD_CONTRIB_ROLEDETAIL)),
								(String) (newRole
										.get(ETLConstants.FIELD_CONTRIB_ROLEDETAIL))));
				return new ParseResult(newRole, false);
			}

		} else {
			// consider it a new contributor
			line = line.trim();
			int firstTab = line.indexOf("\t");
			if (firstTab <= 0) {
				throw new ETLException(ETLConstants.ERR_MALFORMED_LINE,
						"Illegal line in lineno " + getLineNumber() + " line *"
								+ line + "*");
			}
			ImdbRecord newRole = buildRole(line.substring(firstTab).trim(),
					line.substring(0, firstTab).trim());
			prevMovieID = (String) (newRole.get(ETLConstants.FIELD_MOVIE_ID));
			if (currentJSON != null) {
				// flush the old one
				return new ParseResult(currentJSON, newRole);
			}

			// otherwise, return our new one, but hold until we conclude it
			return new ParseResult(newRole, false);
		}
	}

	// Just movie: Madre nana (1991)
	// Movie plus credit <>: Ulan, init at hamog (1987) <8>
	// Movie plus role []: All Americana (2009) [Joy]
	// Movie plus role [] amd credit <>: Ang tanging ina (2003) [Jenny] <33>
	// Weird because "(voice)": Argentine, les 500 b�b�s vol�s de la dictature
	// (2013) (TV) (voice) [Narrator]
	ImdbRecord buildRole(String roleDesc, String contrib) throws ETLException {

		ImdbRecord ret = new ImdbRecord();

		// My theory is that two spaces splits fields
		String toks[] = roleDesc.split("  ");
		if (toks == null || toks.length == 0) {
			throw new ETLException(ETLConstants.ERR_UNEXPECTED_LINE,
					"Unexpected movie role in lineno " + getLineNumber() + " *"
							+ roleDesc + "*");
		}
		String movieID = toks[0].trim();
		String role = "";
		String roleDetail = "";
		for (int i = 1; i < toks.length; i++) {
			toks[i] = toks[i].trim();
			if (toks[i].startsWith("[") && toks[i].endsWith("]")) {
				role = toks[i].substring(1, toks[i].length() - 1);
			} else if (toks[i].startsWith("(") && toks[i].endsWith(")")) {
				roleDetail += toks[i].substring(1, toks[i].length() - 1) + " ";
			} else if (toks[i].startsWith("<") && toks[i].endsWith(">")) {
				roleDetail += toks[i].substring(1, toks[i].length() - 1) + " ";
			} else {
				roleDetail += toks[i];

			}
		}

		ret.append(this.keyFieldName, contrib);
		ret.append(ETLConstants.FIELD_CONTRIB_CLASS, this.sourceName);
		ret.append(ETLConstants.FIELD_MOVIE_ID, movieID);
		ret.append(ETLConstants.FIELD_CONTRIB_ROLE, addToCSV(null, role));
		ret.append(ETLConstants.FIELD_CONTRIB_ROLEDETAIL,
				addToCSV(null, roleDetail));
		ret.append(ETLConstants.FIELD_CONTRIB_TYPE,
				ETLConstants.CONTRIB_TYPE_PERSON);

		return ret;
	}
}
