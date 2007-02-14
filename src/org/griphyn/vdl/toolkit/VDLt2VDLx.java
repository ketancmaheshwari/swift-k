package org.griphyn.vdl.toolkit;

import java.io.InputStreamReader;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.griphyn.vdl.parser.VDLtLexer;
import org.griphyn.vdl.parser.VDLtParser;

/** Commandline tool to convert the textual form of SwiftScript into
	the XML form.

	Unix exit code is 0 on success; 1 on parse error; 2 on invocation error

*/

public class VDLt2VDLx {

	private static final Logger logger = Logger.getLogger(VDLt2VDLx.class);

	public static void main(String[] args) throws Exception {
		try {
			compile(args);
		} catch(ParsingException e) {
			System.exit(1);
		} catch(IncorrectInvocationException e) {
			System.exit(2);
		}
		System.exit(0);
	}

	public static int compile(String[] args) 
        throws ParsingException, IncorrectInvocationException {
		try {
			String templateFileName = null;
			switch(args.length) {
			case 0:
				templateFileName = "XDTM.stg";
				break;
			case 1:
				templateFileName = args[0];
				break;
			default:
				System.err.println("VDLt2VDLx: Too many parameters specified - expecting a single template filename");
				throw new IncorrectInvocationException();
			}
			StringTemplateGroup templates;
			templates = new StringTemplateGroup(new InputStreamReader(
					VDLt2VDLx.class.getClassLoader().getResource(templateFileName).openStream()));
			VDLtLexer lexer = new VDLtLexer(System.in);
			VDLtParser parser = new VDLtParser(lexer);
			parser.setTemplateGroup(templates);
			StringTemplate code = parser.program();
			System.out.println(code.toString());
		} catch(Exception e) {
			logger.error("Could not compile SwiftScript source: "+e);
			logger.debug("Full parser exception",e);
			throw new ParsingException();
		}
		return 0;
	}

	static public class IncorrectInvocationException extends Exception {}
	static public class ParsingException extends Exception {}

}

