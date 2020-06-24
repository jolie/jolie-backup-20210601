package joliex.openapi;

import jolie.CommandLineException;
import jolie.js.JsUtils;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.SemanticException;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.typing.Type;

import joliex.openapi.impl.OpenApiDocumentCreator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class JolieToOpenApi {

	public static void main( String[] args ) {
		try {
			JolieOpenApiCommandLineParser cmdParser =
				JolieOpenApiCommandLineParser.create( args, JolieToOpenApi.class.getClassLoader() );

			Program program = ParsingUtils.parseProgram(
				cmdParser.getInterpreterParameters().inputStream(),
				cmdParser.getInterpreterParameters().programFilepath().toURI(),
				cmdParser.getInterpreterParameters().charset(),
				cmdParser.getInterpreterParameters().includePaths(),
				cmdParser.getInterpreterParameters().jolieClassLoader(),
				cmdParser.getInterpreterParameters().constants(), false );

			ProgramInspector inspector = ParsingUtils.createInspector( program );

			String format = cmdParser.getFormat();
			String inputPort = cmdParser.getInputPort();
			String outputDirectory = cmdParser.getOutputDirectory();
			String routerHost = cmdParser.getRouterHost();
			Boolean easyInterface = cmdParser.isEasyInterface();

			if( "swagger".equalsIgnoreCase( format ) ) {
				System.out.println( "Swagger" );
			} else if( "openapi".equalsIgnoreCase( format ) ) {
				OpenApiDocumentCreator openApiDocumentCreator =
					new OpenApiDocumentCreator( inspector, inputPort, outputDirectory, routerHost );
				try {
					Value openApiDescriptor = openApiDocumentCreator.ConvertDocument();
					StringBuilder json = new StringBuilder();
					JsUtils.valueToJsonString( openApiDescriptor, true, Type.UNDEFINED, json );
					try( OutputStream fos = new FileOutputStream( inputPort + ".json", false ) ) {
						OutputStreamWriter writer = new OutputStreamWriter( fos, "UTF-8" );
						writer.write( json.toString() );
						writer.flush();
					}

				} catch( FaultException e ) {
					if( "RestTemplateError".equals( e.faultName() ) ) {
						System.err.println(
							"The file rest_template.json presents some errors please see the error.json for a detailed report" );
						StringBuilder json = new StringBuilder();
						JsUtils.valueToJsonString( e.value(), true, Type.UNDEFINED, json );
						try( OutputStream fos = new FileOutputStream( "error.json", false ) ) {
							OutputStreamWriter writer = new OutputStreamWriter( fos, "UTF-8" );
							writer.write( json.toString() );
							writer.flush();
						}
						System.exit( 2 );
					} else if( "RestApiMappingError".equals( e.faultName() ) ) {
						System.err.println(
							"The mapping between rest descriptors  amd the jolie port presents some errors please see the error.json for a detailed report" );
						StringBuilder json = new StringBuilder();
						JsUtils.valueToJsonString( e.value(), true, Type.UNDEFINED, json );

						try( OutputStream fos = new FileOutputStream( "error.json", false ) ) {
							OutputStreamWriter writer = new OutputStreamWriter( fos, "UTF-8" );
							writer.write( json.toString() );
							writer.flush();
						}
						System.exit( 2 );
					} else {
						System.err.println( "The program has terminated due to an unexpected error" );
						System.exit( 2 );
					}
				}

			}

		} catch( CommandLineException e ) {
			e.printStackTrace();
		} catch( IOException e ) {
			e.printStackTrace();
		} catch( SemanticException e ) {
			e.printStackTrace();
		} catch( ParserException e ) {
			e.printStackTrace();
		}
	}
}
