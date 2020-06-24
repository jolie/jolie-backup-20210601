package joliex.openapi;

import jolie.CommandLineException;
import jolie.CommandLineParser;

import java.io.IOException;
import java.util.List;

public class JolieOpenApiCommandLineParser extends CommandLineParser {

	public String getFormat() {
		return format;
	}

	public void setFormat( String format ) {
		this.format = format;
	}

	public String getInputPort() {
		return inputPort;
	}

	public void setInputPort( String inputPort ) {
		this.inputPort = inputPort;
	}

	public String getRouterHost() {
		return routerHost;
	}

	public void setRouterHost( String routerHost ) {
		this.routerHost = routerHost;
	}

	public String getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory( String outputDirectory ) {
		this.outputDirectory = outputDirectory;
	}

	public Boolean isEasyInterface() {
		return easyInterface;
	}

	public void setEasyInterface( Boolean easyInterface ) {
		this.easyInterface = easyInterface;
	}

	private String format = null;
	private String inputPort = null;
	private String routerHost = null;
	private String outputDirectory = null;
	private Boolean easyInterface = false;

	private static class JolieDummyArgumentHandler implements CommandLineParser.ArgumentHandler {

		private String format = null;
		private String inputPort = null;
		private String routerHost = null;
		private String outputDirectory = null;
		private Boolean easyInterface = false;

		public int onUnrecognizedArgument( List< String > argumentsList, int index )
			throws CommandLineException {
			if( "--format".equals( argumentsList.get( index ) ) ) {
				index++;
				this.format = argumentsList.get( index );
			} else if( "--inputPort".equals( argumentsList.get( index ) ) ) {
				index++;
				this.inputPort = argumentsList.get( index );
			} else if( "--routerHost".equals( argumentsList.get( index ) ) ) {
				index++;
				this.routerHost = argumentsList.get( index );
			} else if( "--outputDirectory".equals( argumentsList.get( index ) ) ) {
				index++;
				outputDirectory = argumentsList.get( index );
			} else if( "--easyInterface".equals( argumentsList.get( index ) ) ) {
				index++;
				easyInterface = new Boolean( argumentsList.get( index ) );
			} else {
				throw new CommandLineException( "Unrecognized command line option: " + argumentsList.get( index ) );
			}

			return index;
		}
	}

	public static JolieOpenApiCommandLineParser create( String[] args, ClassLoader parentClassLoader )
		throws CommandLineException, IOException {
		return new JolieOpenApiCommandLineParser( args, parentClassLoader, new JolieDummyArgumentHandler() );
	}

	private JolieOpenApiCommandLineParser( String[] args, ClassLoader parentClassLoader,
		JolieDummyArgumentHandler argHandler )
		throws CommandLineException, IOException {
		super( args, parentClassLoader, argHandler );
		format = argHandler.format;
		inputPort = argHandler.inputPort;
		routerHost = argHandler.routerHost;
		outputDirectory = argHandler.outputDirectory;
		easyInterface = argHandler.easyInterface;
	}
}
