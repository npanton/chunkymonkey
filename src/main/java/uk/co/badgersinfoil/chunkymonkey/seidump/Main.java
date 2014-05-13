package uk.co.badgersinfoil.chunkymonkey.seidump;

import java.io.File;
import uk.co.badgersinfoil.chunkymonkey.ts.FileTransportStreamParser;
import uk.co.badgersinfoil.chunkymonkey.ts.FileTransportStreamParser.FileTsContext;

public class Main {

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Supply a single filename");
			System.exit(-1);
		}
		AppBuilder b = new AppBuilder();
		FileTransportStreamParser parser = b.createFileParser();
		File f = new File(args[0]);
		FileTsContext ctx = parser.createContext();
		parser.parse(ctx, f);
	}
}
