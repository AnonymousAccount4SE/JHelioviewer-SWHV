package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.Pair;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKDownloader;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParam;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.database.JHVDatabaseParam;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class HEKDownloader extends SWEKDownloader {
    private static final String _baseURL = "http://www.lmsal.com/hek/her?";

    @Override
    protected boolean parseEvents(JSONObject eventJSON, JHVEventType type) {
        JSONArray results = eventJSON.getJSONArray("result");
        ArrayList<EventDatabase.Event2Db> event2db_list = new ArrayList<EventDatabase.Event2Db>();

        for (int i = 0; i < results.length(); i++) {
            JSONObject result = results.getJSONObject(i);

            String uid;
            long start;
            long end;
            long archiv;
            ArrayList<JHVDatabaseParam> paramList = new ArrayList<JHVDatabaseParam>();
            try {
                start = TimeUtils.utcDateFormat.parse(result.getString("event_starttime")).getTime();
                end = TimeUtils.utcDateFormat.parse(result.getString("event_endtime")).getTime();
                archiv = TimeUtils.utcDateFormat.parse(result.getString("kb_archivdate")).getTime();
                uid = result.getString("kb_archivid");

                HashMap<String, String> dbFields = type.getEventType().getAllDatabaseFields();
                for (Map.Entry<String, String> entry : dbFields.entrySet()) {
                    String dbType = entry.getValue();
                    String fieldName = entry.getKey();

                    String lfieldName = fieldName.toLowerCase(Locale.ENGLISH);
                    if (!result.isNull(lfieldName)) {
                        if (dbType.equals(JHVDatabaseParam.DBINTTYPE)) {
                            paramList.add(new JHVDatabaseParam(JHVDatabaseParam.DBINTTYPE, result.getInt(lfieldName), fieldName));
                        } else if (dbType.equals(JHVDatabaseParam.DBSTRINGTYPE)) {
                            paramList.add(new JHVDatabaseParam(JHVDatabaseParam.DBSTRINGTYPE, result.getString(lfieldName), fieldName));
                        }
                    }
                }
            } catch (JSONException e) {
                return false;
            } catch (ParseException e) {
                return false;
            }

            byte[] compressed;
            try {
                compressed = JSONUtils.compressJSON(result);
            } catch (IOException e) {
                Log.error("compression error");
                return false;
            }

            event2db_list.add(new EventDatabase.Event2Db(compressed, start, end, archiv, uid, paramList));
        }

        int id = EventDatabase.dump_event2db(event2db_list, type);
        if (id == -1) {
            Log.error("failed to dump to database");
            return false;
        }
        return true;
    }

    @Override
    protected boolean parseAssociations(JSONObject eventJSON) {
        JSONArray associations = eventJSON.getJSONArray("association");
        int len = associations.length();
        Pair<String, String>[] assocs = new Pair[len];
        for (int i = 0; i < len; i++) {
            JSONObject asobj = associations.getJSONObject(i);
            assocs[i] = new Pair<String, String>(asobj.getString("first_ivorn"), asobj.getString("second_ivorn"));
        }
        return EventDatabase.dump_association2db(assocs) != -1;
    }

    @Override
    protected String createURL(SWEKEventType eventType, long start, long end, List<SWEKParam> params, int page) {
        StringBuilder baseURL = new StringBuilder(_baseURL);
        baseURL.append("cmd=search&");
        baseURL.append("type=column&");
        baseURL.append("event_type=").append(HEKEventFactory.getHEKEvent(eventType.getEventName()).getAbbreviation()).append('&');
        baseURL.append("event_coordsys=").append(eventType.getCoordinateSystem()).append('&');
        baseURL.append("x1=").append(eventType.getSpatialRegion().x1).append('&');
        baseURL.append("x2=").append(eventType.getSpatialRegion().x2).append('&');
        baseURL.append("y1=").append(eventType.getSpatialRegion().y1).append('&');
        baseURL.append("y2=").append(eventType.getSpatialRegion().y2).append('&');
        baseURL.append("cosec=2&");
        baseURL.append("param0=event_starttime&op0=<=&value0=").append(TimeUtils.utcDateFormat.format(end)).append('&');
        baseURL = appendParams(baseURL, params);
        baseURL.append("event_starttime=").append(TimeUtils.utcDateFormat.format(start)).append('&');
        long max = Math.max(System.currentTimeMillis(), end);
        baseURL.append("event_endtime=").append(TimeUtils.utcDateFormat.format(max)).append('&');
        baseURL.append("page=").append(page);
        return baseURL.toString();
    }

    private static StringBuilder appendParams(StringBuilder baseURL, List<SWEKParam> params) {
        int paramCount = 1;

        for (SWEKParam param : params) {
            if (param.getParam().toLowerCase().equals("provider")) {
                String encodedValue;
                try {
                    encodedValue = URLEncoder.encode(param.getValue(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    encodedValue = param.getValue();
                }
                baseURL.append("param").append(paramCount).append('=').append("frm_name").append('&').append("op").append(paramCount).append('=').append(param.getOperand().URLEncodedRepresentation()).append('&').append("value").append(paramCount).append('=').append(encodedValue).append('&');
                paramCount++;
            }
        }
        return baseURL;
    }

}
