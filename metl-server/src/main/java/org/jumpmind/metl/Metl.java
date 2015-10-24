package org.jumpmind.metl;

public class Metl {

	public static void main(String[] args) throws Exception {
		
		if (args.length == 0) {
			StartWebServer.runWebServer();
		} else {
			Wrapper.runServiceWrapper(args);
		}
	}

}
