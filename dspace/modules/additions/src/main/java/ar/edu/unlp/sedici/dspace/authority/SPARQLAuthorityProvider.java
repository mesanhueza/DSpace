package ar.edu.unlp.sedici.dspace.authority;

import org.apache.log4j.Logger;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.content.Collection;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public abstract class SPARQLAuthorityProvider implements ChoiceAuthority {

	protected static Logger log = Logger
			.getLogger(SPARQLAuthorityProvider.class);

	protected static final String NS_CIC = "http://www.cic.gba.gov.ar/ns#";
	protected static final String NS_RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	protected static final String NS_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	protected static final String NS_SKOS = "http://www.w3.org/2004/02/skos/core#";
	protected static final String NS_FOAF = "http://xmlns.com/foaf/0.1/";
	protected static final String NS_DC = "http://purl.org/dc/terms/";
	protected static final String NS_SIOC = "http://rdfs.org/sioc/ns#";
	protected static final String NS_CERIF = "http://spi-fm.uca.es/neologism/cerif/1.3#";

	private QuerySolutionMap globalParameters;

	public SPARQLAuthorityProvider() {
		this(new QuerySolutionMap());
	}

	public SPARQLAuthorityProvider(QuerySolutionMap globalParameters) {
		this.globalParameters = globalParameters;
	}
	
	protected String getSparqlEndpoint() {
		String endpoint = ConfigurationManager.getProperty("sparql-authorities", "endpoint.url");
		if (endpoint != null) {
			return endpoint;
		} else {
			throw new NullPointerException("Missing endpoint configuration.");
		}
	}

	@Override
	public Choices getMatches(String field, String text, Collection collection,
			int start, int limit, String locale) {
		if (text == null)
			text = "";
		else 
			text = text.replace("\"", "");

		ParameterizedSparqlString query = this.getSparqlSearch(
				field, text, locale,false);
		Choice[] choices = this.evalSparql(query, start, limit);
		log.trace(choices.length + "matches found for text " + text);
		return new Choices(choices, start, limit, Choices.CF_ACCEPTED, false);
		// TODO hasMore??
	}

	@Override
	public final Choices getBestMatch(String field, String text,
			Collection collection, String locale) {
		return this.getMatches(field, text, collection, 0, 1, locale);
	}

	@Override
	public String getLabel(String field, String key, String locale) {

		ParameterizedSparqlString query = this.getSparqlSearch(field,
				key, locale,true);
		Choice[] choices = this.evalSparql(query, 0,0);
		if (choices.length == 0)
			return null;
		else
			return choices[0].label;
	}

    /**
     * Este metodos construye la query sparql que posteriormente será ejecutada en drupal
     *
     *  @param idSearch Determina el tipo de query, si se hará la busqueda por id (key) de autoridad o por texto
     */
	protected abstract ParameterizedSparqlString getSparqlSearch(String field, String filter, String locale,boolean idSearch);

	protected abstract Choice[] extractChoicesfromQuery(QueryEngineHTTP httpQuery);

	protected Choice[] evalSparql(
			ParameterizedSparqlString parameterizedSparqlString, int offset,
			int limit) {

		parameterizedSparqlString.setParams(globalParameters);
		Query query = QueryFactory.create(normalizeTextForHttpQuery(parameterizedSparqlString.toString()),
				this.getSPARQLSyntax());
		query.setOffset(offset);

		if (limit == 0)
			query.setLimit(Query.NOLIMIT);
		else
			query.setLimit(limit);
		long inicio = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Excecuting SparqlQuery "
					+ query.toString(this.getSPARQLSyntax()));
		}

		QueryEngineHTTP httpQuery = new QueryEngineHTTP(this.getSparqlEndpoint(), query);
		httpQuery.setAllowDeflate(false);
		httpQuery.setAllowGZip(false);
		// TODO pull down de extractChocicesFromQuery a una nueva clase que haga un execSelect o un execConstruct
		Choice[] choices = extractChoicesfromQuery(httpQuery);
		httpQuery.close();

		if (log.isDebugEnabled()) {
			log.debug("El query tardó " + (System.currentTimeMillis() - inicio)
					+ "ms");
		}
		return choices;
	}


	private Syntax getSPARQLSyntax() {
		// FIXME: la sintaxis debería ser protected
		return Syntax.syntaxSPARQL_10;
	}

	private String normalizeTextForHttpQuery(String query) {
		if (query.indexOf("\\(") >= 0) {
			query = query.replace("\\(", "\\\\\\(");
		}
		if (query.indexOf("\\)") >= 0) {
			query = query.replace("\\)", "\\\\\\)");
		}
		return query;
	}

	protected String normalizeTextForParserSPARQL10(String text) {
		if (text.indexOf("(") >= 0) {
			text = text.replace("(", "\\\\(");
		}
		if (text.indexOf(")") >= 0) {
			text = text.replace(")", "\\\\)");
		}
		return text;
	}

}
