package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParam;
import org.helioviewer.jhv.database.JHVDatabase;
import org.helioviewer.jhv.database.JHVDatabaseParam;
import org.helioviewer.jhv.plugins.swek.sources.SWEKDownloader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ComesepDownloader extends SWEKDownloader {

    private final Properties comesepSourceProperties;

    public ComesepDownloader() {
        ComesepProperties csp = ComesepProperties.getSingletonInstance();
        comesepSourceProperties = csp.getComesepProperties();
    }

    @Override
    protected boolean parseEvents(JSONObject eventJSON, JHVEventType type) {
        JSONArray results = eventJSON.getJSONArray("results");
        try {
            ArrayList<JHVDatabase.Event2Db> event2db_list = new ArrayList<JHVDatabase.Event2Db>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);

                String uid = result.getString("alertid");
                long start;
                long end;
                long archiv;

                start = result.getLong("atearliest") * 1000;
                end = result.getLong("atlatest") * 1000;
                if (result.has("liftoffduration_value")) {
                    long cactusLiftOff = result.getLong("liftoffduration_value");
                    end = end + cactusLiftOff * 60000;
                }
                archiv = start;
                byte[] compressedJson;
                try {
                    compressedJson = JHVDatabase.compress(result.toString());
                } catch (IOException e) {
                    Log.error("compression error");
                    return false;
                }
                event2db_list.add(new JHVDatabase.Event2Db(compressedJson, start, end, archiv, uid, new ArrayList<JHVDatabaseParam>()));
            }
            int id = JHVDatabase.dump_event2db(event2db_list, type);
            if (id == -1) {
                Log.error("failed to dump to database");
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected boolean parseAssociations(JSONObject eventJSON) {
        JSONArray associations = eventJSON.getJSONArray("associations");
        int len = associations.length();
        Pair<String, String>[] assocs = new Pair[len];
        for (int i = 0; i < len; i++) {
            JSONObject asobj = associations.getJSONObject(i);
            assocs[i] = new Pair<String, String>(asobj.getString("parent"), asobj.getString("child"));
        }
        int ret = JHVDatabase.dump_association2db(assocs);
        if (ret == -1) {
            return false;
        }
        return true;
    }

    @Override
    protected String createURL(SWEKEventType eventType, long start, long end, List<SWEKParam> params, int page) {
        StringBuilder baseURL = new StringBuilder(comesepSourceProperties.getProperty("comesepsource.baseurl")).append("?");
        baseURL = appendModel(baseURL, params).append("&");
        baseURL.append("startdate=").append(TimeUtils.utcDateFormat.format(start)).append("&");
        baseURL.append("enddate=").append(TimeUtils.utcDateFormat.format(end)).append("&");
        return baseURL.toString();
    }

    private StringBuilder appendModel(StringBuilder baseURL, List<SWEKParam> params) {
        String model = "";
        for (SWEKParam p : params) {
            if (p.param.equals("provider")) {
                model = p.value;
                break;
            }
        }
        return baseURL.append("model=").append(model);
    }

}
