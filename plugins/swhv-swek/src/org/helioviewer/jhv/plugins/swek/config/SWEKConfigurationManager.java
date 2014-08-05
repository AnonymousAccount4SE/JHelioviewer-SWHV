/**
 *
 */
package org.helioviewer.jhv.plugins.swek.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;
import org.helioviewer.jhv.plugins.swek.settings.SWEKSettings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Bram.Bourgognie@oma.be
 * 
 */
public class SWEKConfigurationManager {

    /** Singleton instance */
    private static SWEKConfigurationManager singletonInstance;

    /** Config loaded */
    private boolean configLoaded;

    /** Config file URL */
    private URL configFileURL;

    /** The swek properties */
    private final Properties swekProperties;

    /** The loaded configuration */
    private SWEKConfiguration configuration;

    /** Map containing the sources */
    private final Map<String, SWEKSource> sources;

    /** Map containing the parameters */
    private final Map<String, SWEKParameter> parameters;

    /** Map containing the event types */
    private final Map<String, SWEKEventType> eventTypes;

    /**
     * private constructor
     */
    private SWEKConfigurationManager() {
        this.configLoaded = false;
        this.swekProperties = new Properties();
        this.sources = new HashMap<String, SWEKSource>();
        this.parameters = new HashMap<String, SWEKParameter>();
        this.eventTypes = new HashMap<String, SWEKEventType>();
    }

    /**
     * Gives access to the singleton instance
     * 
     * @return the singleton instance
     */
    public static SWEKConfigurationManager getSingletonInstance() {
        if (singletonInstance == null) {
            singletonInstance = new SWEKConfigurationManager();
        }
        return singletonInstance;
    }

    /**
     * Loads the configuration.
     * 
     * If no configuration file is set by the user, the program downloads the
     * configuration file online and saves it the
     * JHelioviewer/Plugins/swek-plugin folder.
     * 
     */
    public void loadConfiguration() {
        if (!this.configLoaded) {
            Log.debug("Load the swek internal settings");
            this.loadPluginSettings();
            Log.debug("search and open the configuration file");
            boolean isConfigParsed;
            if (checkAndOpenUserSetFile()) {
                isConfigParsed = parseConfigFile();
            } else if (checkAndOpenHomeDirectoryFile()) {
                isConfigParsed = parseConfigFile();
            } else if (checkAndOpenOnlineFile()) {
                isConfigParsed = parseConfigFile();
            } else {
                isConfigParsed = false;
            }
            if (!isConfigParsed) {
                // TODO set on the panel the config file could not be parsed.
                this.configLoaded = false;
            } else {
                this.configLoaded = true;
            }
        }
    }

    /**
     * Gives a map with all the event types. The event type name is the key and
     * the event type is the value.
     * 
     * @return map containing the event types found in the configuration file
     */
    public Map<String, SWEKEventType> getEventTypes() {
        loadConfiguration();
        return this.eventTypes;
    }

    /**
     * Loads the overall plugin settings.
     */
    private void loadPluginSettings() {
        InputStream defaultPropStream = SWEKPlugin.class.getResourceAsStream("/SWEK.properties");
        try {
            this.swekProperties.load(defaultPropStream);
        } catch (IOException ex) {
            Log.error("Could not load the swek settings : ", ex);
        }
    }

    /**
     * Downloads the SWEK configuration from the Internet and saves it in the
     * plugin home directory.
     * 
     * @return true if the the file was found and copied to the home directory,
     *         false if the file could not be found, copied or something else
     *         went wrong.
     */
    private boolean checkAndOpenOnlineFile() {
        Log.debug("Download the configuration file from the net and copy it to the plugin home directory");
        try {
            URL url = new URL(this.swekProperties.getProperty("plugin.swek.onlineconfigfile"));
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            String saveFile = SWEKSettings.SWEK_HOME + this.swekProperties.getProperty("plugin.swek.configfilename");
            FileOutputStream fos = new FileOutputStream(saveFile);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            this.configFileURL = new URL("file://" + saveFile);
            return true;
        } catch (MalformedURLException e) {
            Log.debug("Could not create a URL from the value found in the properties file: "
                    + this.swekProperties.getProperty("plugin.swek.onlineconfigfile") + " : " + e);
        } catch (IOException e) {
            Log.debug("Something went wrong downloading the configuration file from the server or saving it to the local machine : " + e);
        }
        return false;
    }

    /**
     * Checks the home directory of the plugin (normally
     * ~/JHelioviewer/Plugins/swek-plugin/) for the existence of the
     * SWEKSettings.json file.
     * 
     * @return true if the file was found and useful, false if the file was not
     *         found.
     */
    private boolean checkAndOpenHomeDirectoryFile() {
        String configFile = SWEKSettings.SWEK_HOME + this.swekProperties.getProperty("plugin.swek.configfilename");
        try {
            File f = new File(configFile);
            if (f.exists()) {
                this.configFileURL = new URL("file://" + configFile);
                return true;
            } else {
                Log.debug("File created from the settings : " + configFile + " does not exists on this system.");
            }
        } catch (MalformedURLException e) {
            Log.debug("File at possition " + configFile + " could not be parsed into an URL");
        }
        return false;
    }

    /**
     * Checks the jhelioviewer settings file for a swek configuration file.
     * 
     * @return true if the file as found and useful, false if the file was not
     *         found.
     */
    private boolean checkAndOpenUserSetFile() {
        Log.debug("Search for a user defined configuration file in the JHelioviewer setting file.");
        Settings jhvSettings = Settings.getSingletonInstance();
        String fileName = jhvSettings.getProperty("plugin.swek.configfile");
        if (fileName == null) {
            Log.debug("No configured filename found.");
            return false;
        } else {
            try {
                URI fileLocation = new URI(fileName);
                this.configFileURL = fileLocation.toURL();
                Log.debug("Config file : " + this.configFileURL.toString());
                return true;
            } catch (URISyntaxException e) {
                Log.debug("Wrong URI syntax for the found file name : " + fileName);
            } catch (MalformedURLException e) {
                Log.debug("Could not convert the URI in a correct URL. The found file name : " + fileName);
            }
            return false;
        }
    }

    /**
     * Parses the SWEK settings.
     */
    private boolean parseConfigFile() {
        try {
            InputStream configIs = this.configFileURL.openStream();
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(configIs));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            JSONObject configJSON = new JSONObject(sb.toString());
            return parseJSONConfig(configJSON);
        } catch (IOException e) {
            Log.debug("The configuration file could not be parsed : " + e);
        } catch (JSONException e) {
            Log.debug("Could not parse the given JSON : " + e);
        }
        return false;
    }

    /**
     * Parses the JSON from start
     * 
     * @param configJSON
     *            The JSON to parse
     * @return true if the JSON configuration could be parsed, false if not.
     */
    private boolean parseJSONConfig(JSONObject configJSON) {
        this.configuration = new SWEKConfiguration();
        try {
            this.configuration.setManuallyChanged(parseManuallyChanged(configJSON));
            this.configuration.setConfigurationVersion(parseVersion(configJSON));
            this.configuration.setSources(parseSources(configJSON));
            this.configuration.setEventTypes(parseEventTypes(configJSON));
            this.configuration.setRelatedEvents(parseRelatedEvents(configJSON));
            return true;
        } catch (JSONException e) {
            Log.error("Could not parse config json");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Parses a if the configuration was manually changed from a json.
     * 
     * @param jsonObject
     *            the json from which to parse the manually changed indication
     * @return the parsed manually changed indication
     * @throws JSONException
     *             if the manually changed indication could not be parsed
     */
    private boolean parseManuallyChanged(JSONObject configJSON) throws JSONException {
        return configJSON.getBoolean("manually_changed");
    }

    /**
     * Parses the configuration version from the big json.
     * 
     * @param configJSON
     *            The JSON from which to parse
     * @return The parsed configuration version
     * @throws JSONException
     *             If the configuration version could not be parsed.
     */
    private String parseVersion(JSONObject configJSON) throws JSONException {
        return configJSON.getString("config_version");
    }

    /**
     * Parses the list of sources from a json and adds the sources to a map
     * indexed on the name of the source.
     * 
     * @param configJSON
     *            the JSON from which to parse the sources
     * @return a list of sources parsed from the JSON
     * @throws JSONException
     *             if the sources could not be parsed
     */
    private List<SWEKSource> parseSources(JSONObject configJSON) throws JSONException {
        ArrayList<SWEKSource> swekSources = new ArrayList<SWEKSource>();
        JSONArray sourcesArray = configJSON.getJSONArray("sources");
        for (int i = 0; i < sourcesArray.length(); i++) {
            SWEKSource source = parseSource(sourcesArray.getJSONObject(i));
            this.sources.put(source.getSourceName(), source);
            swekSources.add(source);
        }
        return swekSources;
    }

    /**
     * Parses a source from a json.
     * 
     * @param jsonObject
     *            the json from which to parse the source
     * @return the parsed source
     * @throws JSONException
     *             if the source could not be parsed
     */
    private SWEKSource parseSource(JSONObject jsonObject) throws JSONException {
        SWEKSource source = new SWEKSource();
        source.setSourceName(parseSourceName(jsonObject));
        source.setProviderName(parseProviderName(jsonObject));
        source.setDownloaderClass(parseDownloader(jsonObject));
        source.setEventParserClass(parseEventParser(jsonObject));
        source.setJarLocation(parseJarLocation(jsonObject));
        source.setBaseURL(parseBaseURL(jsonObject));
        source.setGeneralParameters(parseGeneralParameters(jsonObject));
        return source;
    }

    /**
     * Parses the source from a json.
     * 
     * @param jsonObject
     *            the json from which to parse the source
     * @return the parsed source
     * @throws JSONException
     *             if the source could not be parsed
     */
    private String parseSourceName(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("name");
    }

    /**
     * Parses the provider name from a json.
     * 
     * @param jsonObject
     *            the json from which to parse the provider name
     * @return the parsed provider name
     * @throws JSONException
     *             if the provider name could not be parsed
     */
    private String parseProviderName(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("provider_name");
    }

    /**
     * Parses the downloader description from a json.
     * 
     * @param jsonObject
     *            the json from which to parse the downloader description
     * @return the parsed downloader description
     * @throws JSONException
     *             if the downloader description could not be parsed
     */
    private String parseDownloader(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("downloader");

    }

    /**
     * Parses the event parser from a json.
     * 
     * @param jsonObject
     *            the json from which to parse the event parser
     * @return the parsed event parser
     * @throws JSONException
     *             if the event parser could not be parsed
     */
    private String parseEventParser(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("event_parser");
    }

    /**
     * Parses the jar location from a json.
     * 
     * @param jsonObject
     *            the json from which to parse the jar location
     * @return the parsed jar location
     * @throws JSONException
     *             if the event parser could not be parsed
     */

    private String parseJarLocation(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("jar_location");
    }

    /**
     * Parses the base url from a json.
     * 
     * @param jsonObject
     *            the json from which to parse the base url
     * @return the parsed base url
     * @throws JSONException
     *             if the base url could not be parsed
     */
    private String parseBaseURL(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("base_url");
    }

    /**
     * Parses the general parameters from a json.
     * 
     * @param jsonObject
     *            the json from which to parse the general parameters
     * @return the parsed general parameters
     * @throws JSONException
     *             if the general parameters could not be parsed
     */
    private List<SWEKParameter> parseGeneralParameters(JSONObject jsonObject) throws JSONException {
        List<SWEKParameter> parameterList = new ArrayList<SWEKParameter>();
        JSONArray parameterArray = jsonObject.getJSONArray("general_parameters");
        for (int i = 0; i < parameterArray.length(); i++) {
            SWEKParameter parameter = parseParameter(parameterArray.getJSONObject(i));
            parameterList.add(parameter);
            this.parameters.put(parameter.getParameterName(), parameter);
        }
        return parameterList;
    }

    /**
     * Parses the list of event types from a json.
     * 
     * @param configJSON
     *            the JSON from which to parse the event types
     * @return a list of event types parsed from the JSON
     * @throws JSONException
     *             if the event types could not be parsed
     */
    private List<SWEKEventType> parseEventTypes(JSONObject configJSON) throws JSONException {
        List<SWEKEventType> result = new ArrayList<SWEKEventType>();
        JSONArray eventJSONArray = configJSON.getJSONArray("events_types");
        for (int i = 0; i < eventJSONArray.length(); i++) {
            SWEKEventType eventType = parseEventType(eventJSONArray.getJSONObject(i));
            result.add(eventType);
            this.eventTypes.put(eventType.getEventName(), eventType);
        }
        return result;
    }

    /**
     * Parses an event type from a json.
     * 
     * @param object
     *            The event type to parse
     * @return The parsed event type
     * @throws JSONException
     */
    private SWEKEventType parseEventType(JSONObject object) throws JSONException {
        SWEKEventType eventType = new SWEKEventType();
        eventType.setEventName(parseEventName(object));
        eventType.setSuppliers(parseSuppliers(object));
        eventType.setParameterList(parseParameterList(object));
        eventType.setRequestIntervalExtension(parseRequestIntervalExtension(object));
        eventType.setStandardSelected(parseStandardSelected(object));
        eventType.setGroupOn(parseGroupOn(object));
        return eventType;
    }

    /**
     * Parses the event name from a json.
     * 
     * @param object
     *            the json from which the event name is parsed
     * @return the event name
     * @throws JSONException
     *             if the supplier name could not be parsed
     */
    private String parseEventName(JSONObject object) throws JSONException {
        return object.getString("event_name");
    }

    /**
     * Parses the suppliers from a json.
     * 
     * @param jsonObject
     *            the json from which the suppliers are parsed
     * @return the suppliers list
     * @throws JSONException
     *             if the suppliers could not be parsed
     */
    private List<SWEKSupplier> parseSuppliers(JSONObject object) throws JSONException {
        List<SWEKSupplier> suppliers = new ArrayList<SWEKSupplier>();
        JSONArray suppliersArray = object.getJSONArray("suppliers");
        for (int i = 0; i < suppliersArray.length(); i++) {
            suppliers.add(parseSupplier(suppliersArray.getJSONObject(i)));
        }
        return suppliers;
    }

    /**
     * Parses a supplier from a json.
     * 
     * @param object
     *            the json from which the supplier is parsed
     * @return the supplier
     * @throws JSONException
     *             if the supplier could not be parsed
     */
    private SWEKSupplier parseSupplier(JSONObject object) throws JSONException {
        SWEKSupplier supplier = new SWEKSupplier();
        supplier.setSupplierName(parseSupplierName(object));
        supplier.setSource(parseSupplierSource(object));
        return supplier;
    }

    /**
     * Parses the supplier name from a json.
     * 
     * @param object
     *            the json from which the supplier name is parsed
     * @return the supplier name
     * @throws JSONException
     *             if the supplier name could not be parsed
     */
    private String parseSupplierName(JSONObject object) throws JSONException {
        return object.getString("supplier_name");
    }

    /**
     * Parses a supplier source from a json.
     * 
     * @param object
     *            the json from which the supplier source is parsed
     * @return the supplier source
     * @throws JSONException
     *             if the supplier source could not be parsed
     */
    private SWEKSource parseSupplierSource(JSONObject object) throws JSONException {
        return this.sources.get(object.getString("source"));
    }

    /**
     * Parses the parameter list from the given JSON.
     * 
     * @param object
     *            the json from which to parse the parameter list
     * @return the parameter list
     * @throws JSONException
     *             if the parameter list could not be parsed.
     */
    private List<SWEKParameter> parseParameterList(JSONObject object) throws JSONException {
        List<SWEKParameter> parameterList = new ArrayList<SWEKParameter>();
        JSONArray parameterListArray = object.getJSONArray("parameter_list");
        for (int i = 0; i < parameterListArray.length(); i++) {
            SWEKParameter parameter = parseParameter((JSONObject) parameterListArray.get(i));
            parameterList.add(parameter);
            this.parameters.put(parameter.getParameterName(), parameter);
        }
        return parameterList;
    }

    /**
     * Parses a parameter from json.
     * 
     * @param jsonObject
     *            the json from which to parse
     * @return the parsed parameter
     * @throws JSONException
     *             if the parameter could not be parsed
     */
    private SWEKParameter parseParameter(JSONObject jsonObject) throws JSONException {
        SWEKParameter parameter = new SWEKParameter();
        parameter.setSource(parseSourceInParameter(jsonObject));
        parameter.setParameterName(parseParameterName(jsonObject));
        parameter.setParameterDisplayName(parseParameterDisplayName(jsonObject));
        parameter.setParameterFilter(parseParameterFilter(jsonObject));
        parameter.setDefaultVisible(parseDefaultVisible(jsonObject));
        return parameter;
    }

    /**
     * Parses the source from a json.
     * 
     * @param jsonObject
     *            the json from which the source is parsed
     * @return the source
     * @throws JSONException
     *             if the source could not be parsed
     */
    private String parseSourceInParameter(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("source");
    }

    /**
     * Parses the parameter name from a json.
     * 
     * @param jsonObject
     *            the json from which the parameter name is parsed
     * @return the parameter name
     * @throws JSONException
     *             if the parameter name could not be parsed
     */
    private String parseParameterName(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("parameter_name");
    }

    /**
     * Parses the parameter display name from a json.
     * 
     * @param jsonObject
     *            the json from which the parameter display name is parsed
     * @return the parameter display name
     * @throws JSONException
     *             if the parameter display name could not be parsed
     */
    private String parseParameterDisplayName(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("parameter_display_name");
    }

    /**
     * Parses the parameter filter from a given json.
     * 
     * @param jsonObject
     *            the json to parse from
     * @return the parsed filter
     * @throws JSONException
     *             if the filter could not be parsed
     */
    private SWEKParameterFilter parseParameterFilter(JSONObject jsonObject) throws JSONException {
        SWEKParameterFilter filter = new SWEKParameterFilter();
        JSONObject filterobject = jsonObject.optJSONObject("filter");
        if (filterobject != null) {
            filter.setFilterType(parseFilterType(filterobject));
            filter.setMin(parseMin(filterobject));
            filter.setMax(parseMax(filterobject));
            return filter;
        } else {
            return null;
        }
    }

    /**
     * Parses the filter type from a json.
     * 
     * @param object
     *            the json from which the filter type is parsed
     * @return the filter type
     * @throws JSONException
     *             if the filter type could not be parsed
     */
    private String parseFilterType(JSONObject object) throws JSONException {
        return object.getString("filter_type");
    }

    /**
     * parses the minimum filter value from a json.
     * 
     * @param object
     *            the json from which to filter the minimum value
     * @return the minimum filter value
     * @throws JSONException
     *             if the minimum filter value could not be parsed
     */
    private double parseMin(JSONObject object) throws JSONException {
        return object.getDouble("min");
    }

    /**
     * Parses the maximum filter value from the json.
     * 
     * @param object
     *            the json from which to filter the maximum filter value
     * @return the maximum filter value
     * @throws JSONException
     *             if the maximum filter value could not be parsed
     */
    private double parseMax(JSONObject object) throws JSONException {
        return object.getDouble("max");
    }

    /**
     * Parses the default visible from the given json.
     * 
     * @param jsonObject
     *            the json from which to parse
     * @return the parsed default visible
     * @throws JSONException
     *             if the default visible could not be parsed
     */
    private boolean parseDefaultVisible(JSONObject jsonObject) throws JSONException {
        return jsonObject.getBoolean("default_visible");
    }

    /**
     * Parses the request interval extension from the json.
     * 
     * @param object
     *            the json from which to parse the request interval extension
     * @return the parsed request interval extension
     * @throws JSONException
     *             if the request interval extension could not be parsed
     */
    private Long parseRequestIntervalExtension(JSONObject object) throws JSONException {
        return object.getLong("request_interval_extension");
    }

    /**
     * Parses the standard selected from the json.
     * 
     * @param object
     *            the json from which to parse the standard selected
     * @return true if standard selected is true in json, false if standard
     *         selected is false in json
     * @throws JSONException
     *             if the standard selected could not be parsed from json.
     */
    private boolean parseStandardSelected(JSONObject object) throws JSONException {
        return object.getBoolean("standard_selected");
    }

    /**
     * Parses the "group on" from the json.
     * 
     * @param object
     *            the json from which to parse the "group on"
     * @return the group on parameter
     * @throws JSONException
     *             if the "group on" could not be parsed from the json.
     */
    private SWEKParameter parseGroupOn(JSONObject object) throws JSONException {
        return this.parameters.get(object.getString("group_on"));
    }

    /**
     * Parses the list of related events from a json.
     * 
     * @param jsonObject
     *            the json from which to parse the list of related events
     * @return the parsed list of related events
     * @throws JSONException
     *             if the list of related events could not be parsed
     */
    private List<SWEKRelatedEvents> parseRelatedEvents(JSONObject configJSON) throws JSONException {
        List<SWEKRelatedEvents> relatedEventsList = new ArrayList<SWEKRelatedEvents>();
        JSONArray relatedEventsArray = configJSON.getJSONArray("related_events");
        for (int i = 0; i < relatedEventsArray.length(); i++) {
            relatedEventsList.add(parseRelatedEvent(relatedEventsArray.getJSONObject(i)));
        }
        return relatedEventsList;
    }

    /**
     * Parses related events from a json.
     * 
     * @param jsonObject
     *            the json from which to parse related events
     * @return the parsed related events
     * @throws JSONException
     *             if the related events could not be parsed
     */
    private SWEKRelatedEvents parseRelatedEvent(JSONObject jsonObject) throws JSONException {
        SWEKRelatedEvents relatedEvents = new SWEKRelatedEvents();
        relatedEvents.setEvent(parseRelatedEventName(jsonObject));
        relatedEvents.setRelatedWith(parseRelatedWith(jsonObject));
        relatedEvents.setRelatedOnList(parseRelatedOnList(jsonObject));
        return relatedEvents;
    }

    /**
     * Parses a related event name from a json.
     * 
     * @param jsonObject
     *            the json from which to parse the related event name
     * @return the parsed related event type
     * @throws JSONException
     *             if the related event name could not be parsed
     */
    private SWEKEventType parseRelatedEventName(JSONObject jsonObject) throws JSONException {
        return this.eventTypes.get(jsonObject.getString("event_name"));
    }

    /**
     * Parses a "related with" from a json.
     * 
     * @param jsonObject
     *            the json from which to parse the "related with"
     * @return the parsed "related with" event type
     * @throws JSONException
     *             if the "related with" could not be parsed
     */
    private SWEKEventType parseRelatedWith(JSONObject jsonObject) throws JSONException {
        return this.eventTypes.get(jsonObject.getString("related_with"));
    }

    /**
     * Parses a list of "related on" from a json.
     * 
     * @param jsonObject
     *            the json from which to parse the "related on" list
     * @return the parsed "related on" list
     * @throws JSONException
     *             if the "related on" list could not be parsed
     */
    private List<SWEKRelatedOn> parseRelatedOnList(JSONObject jsonObject) throws JSONException {
        List<SWEKRelatedOn> relatedOnList = new ArrayList<SWEKRelatedOn>();
        JSONArray relatedOnArray = jsonObject.getJSONArray("related_on");
        for (int i = 0; i < relatedOnArray.length(); i++) {
            relatedOnList.add(parseRelatedOn(relatedOnArray.getJSONObject(i)));
        }
        return relatedOnList;
    }

    /**
     * Parses a "related on" from a json.
     * 
     * @param jsonObject
     *            the json from which to parse the "related on"
     * @return the parsed "related on"
     * @throws JSONException
     *             if the "related on" could not be parsed
     */
    private SWEKRelatedOn parseRelatedOn(JSONObject jsonObject) throws JSONException {
        SWEKRelatedOn relatedOn = new SWEKRelatedOn();
        relatedOn.setParameterFrom(parseParameterFrom(jsonObject));
        relatedOn.setParameterWith(parseParameterWith(jsonObject));
        return relatedOn;
    }

    /**
     * Parses a "parameter from" from a json.
     * 
     * @param jsonObject
     *            the json from which to parse the "parameter from"
     * @return the parsed "parameter from"
     * @throws JSONException
     *             if the "parameter from" could not be parsed
     */
    private SWEKParameter parseParameterFrom(JSONObject jsonObject) throws JSONException {
        return this.parameters.get(jsonObject.getString("parameter_from"));
    }

    /**
     * Parses a "parameter with" from a json.
     * 
     * @param jsonObject
     *            the json from which to parse the "parameter with"
     * @return the parsed "parameter with"
     * @throws JSONException
     *             if the "parameter with" could not be parsed
     */
    private SWEKParameter parseParameterWith(JSONObject jsonObject) throws JSONException {
        return this.parameters.get(jsonObject.getString("parameter_with"));
    }
}
