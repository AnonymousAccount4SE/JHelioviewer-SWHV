package org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.ChunkedInputStream;
//import org.helioviewer.jhv.viewmodel.view.jp2view.io.ChunkedInputStreamAlt;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPConstants;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPHeaderKey;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPRequest;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPResponse;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPSocket;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPRequest.Method;

/**
 * Assumes a persistent HTTP connection.
 *
 * @author caplins
 *
 */
public class JPIPSocket extends HTTPSocket {

    /**
     * The jpip channel ID for the connection (persistent)
     */
    private String jpipChannelID;

    /**
     * The path supplied on the uri line of the HTTP message. Generally for the
     * first request it is the image path in relative terms, but the response
     * could change it. The Kakadu server seems to change it to /jpip.
     */
    private String jpipPath;

    /** Amount of data (bytes) of the last response */
    private int receivedData = 0;

    /** Time when received the last reply text */
    private long replyTextTm = 0;

    /** Time when received the last reply data */
    private long replyDataTm = 0;

    private static final String[] cnewParams = { "cid", "transport", "host", "path", "port", "auxport" };

    /**
     * Connects to the specified URI. The second parameter only serves to
     * distinguish it from the super classes connect method (I want to return
     * something and the super class has a return type of void).
     *
     * @param _uri
     * @return The first response of the server when connecting.
     * @throws IOException
     */
    @Override
    public Object connect(URI _uri) throws IOException {
        super.connect(_uri);

        jpipPath = _uri.getPath();

        JPIPRequest req = new JPIPRequest(HTTPRequest.Method.GET);

        JPIPQuery query = new JPIPQuery();
        query.setField(JPIPRequestField.CNEW.toString(), "http");
        query.setField(JPIPRequestField.TYPE.toString(), "jpp-stream");
        query.setField(JPIPRequestField.TID.toString(), "0");
        /* deliberately small */
        query.setField(JPIPRequestField.LEN.toString(), "512");
        req.setQuery(query.toString());

        JPIPResponse res = null;
        while (res == null && isConnected()) {
            send(req);
            res = receive();
        }
        if (res == null)
            throw new IOException("The server did not send a response after connection.");

        HashMap<String, String> map = null;
        String cnew = res.getHeader("JPIP-cnew");
        if (cnew != null) {
            map = new HashMap<String, String>();
            String[] parts = cnew.split(",");
            for (String part : parts)
                for (String cnewParam : cnewParams)
                    if (part.startsWith(cnewParam + "="))
                        map.put(cnewParam, part.substring(cnewParam.length() + 1));
        }
        if (map == null)
            throw new IOException("The header 'JPIP-cnew' was not sent by the server!");

        jpipPath = "/" + map.get("path");

        jpipChannelID = map.get("cid");
        if (jpipChannelID == null)
            throw new IOException("The channel id was not sent by the server");

        if (!"http".equals(map.get("transport")))
            throw new IOException("The client currently only supports http transport.");

        return res;
    }

    /** Closes the JPIPSocket */
    @Override
    public void close() throws IOException {
        if (this.isClosed())
            return;

        try {
            if (jpipChannelID != null) {
                JPIPRequest req = new JPIPRequest(HTTPRequest.Method.GET);

                JPIPQuery query = new JPIPQuery();
                query.setField(JPIPRequestField.CCLOSE.toString(), jpipChannelID);
                query.setField(JPIPRequestField.LEN.toString(), "0");
                req.setQuery(query.toString());

                send(req);
            }

        } catch (IOException e) {
            // e.printStackTrace();
        } finally {
            super.close();
        }
    }

    /**
     * Sends a JPIPRequest
     *
     * @param _req
     * @throws IOException
     */
    public void send(JPIPRequest _req) throws IOException {
        String queryStr = _req.getQuery();

        // Adds some default headers if they were not already added.
        if (!_req.headerExists(HTTPHeaderKey.USER_AGENT.toString()))
            _req.setHeader(HTTPHeaderKey.USER_AGENT.toString(), JHVGlobals.getUserAgent());
        if (!_req.headerExists(HTTPHeaderKey.ACCEPT_ENCODING.toString()))
            _req.setHeader(HTTPHeaderKey.ACCEPT_ENCODING.toString(), "gzip");
        if (!_req.headerExists(HTTPHeaderKey.CACHE_CONTROL.toString()))
            _req.setHeader(HTTPHeaderKey.CACHE_CONTROL.toString(), "no-cache");
        if (!_req.headerExists(HTTPHeaderKey.HOST.toString()))
            _req.setHeader(HTTPHeaderKey.HOST.toString(), (getHost() + ":" + getPort()));
        // Adds a necessary JPIP request field
        if (jpipChannelID != null && !queryStr.contains("cid=") && !queryStr.contains("cclose"))
            queryStr += "&cid=" + jpipChannelID;

        if (_req.getMethod() == Method.GET) {
            if (!_req.headerExists(HTTPHeaderKey.CONNECTION.toString()))
                _req.setHeader(HTTPHeaderKey.CONNECTION.toString(), "Keep-Alive");
        } else if (_req.getMethod() == Method.POST) {
            if (!_req.headerExists(HTTPHeaderKey.CONTENT_TYPE.toString()))
                _req.setHeader(HTTPHeaderKey.CONTENT_TYPE.toString(), "application/x-www-form-urlencoded");
            if (!_req.headerExists(HTTPHeaderKey.CONTENT_LENGTH.toString()))
                _req.setHeader(HTTPHeaderKey.CONTENT_LENGTH.toString(), Integer.toString(queryStr.getBytes("UTF-8").length));
        }

        StringBuilder str = new StringBuilder();

        // Adds the URI line
        str.append(_req.getMethod()).append(' ').append(jpipPath);
        if (_req.getMethod() == Method.GET) {
            str.append('?').append(queryStr);
        }
        str.append(' ').append(HTTPConstants.versionText).append(HTTPConstants.CRLF);

        // Adds the headers
        for (String key : _req.getHeaders()) {
            str.append(key).append(": ").append(_req.getHeader(key)).append(HTTPConstants.CRLF);
        }
        str.append(HTTPConstants.CRLF);

        // Adds the message body if necessary
        if (_req.getMethod() == HTTPRequest.Method.POST)
            str.append(queryStr);

        // if (!isConnected())
        //    reconnect();

        // Writes the result to the output stream
        getOutputStream().write(str.toString().getBytes("UTF-8"));
    }

    /** Receives a JPIPResponse returning null if EOS reached */
    @Override
    public JPIPResponse receive() throws IOException {
        // long tini = System.currentTimeMillis();

        HTTPResponse httpRes = (HTTPResponse) super.receive();
        if (httpRes == null)
            return null;

        JPIPResponse res = new JPIPResponse(httpRes);

        if (res.getCode() != 200)
            throw new IOException("Invalid status code returned (" + res.getCode() + ")");
        if (!"chunked".equals(res.getHeader("Transfer-Encoding")))
            throw new IOException("Only chunked responses are supported");
        if (!"image/jpp-stream".equals(res.getHeader("Content-Type")))
            throw new IOException("Expected image/jpp-stream content!");

        replyTextTm = System.currentTimeMillis();

        ChunkedInputStream input = new ChunkedInputStream(inputStream);
        JPIPDataInputStream jpip;
        if ("gzip".equals(res.getHeader("Content-Encoding")))
            jpip = new JPIPDataInputStream(new GZIPInputStream(input));
        else
            jpip = new JPIPDataInputStream(input);

        try {
            JPIPDataSegment seg;
            while ((seg = jpip.readSegment()) != null)
                res.addJpipDataSegment(seg);
        } finally {
            // make sure the stream is exhausted
            input.close();
        }

        if ("close".equals(res.getHeader("Connection"))) {
            super.close();
        }
        replyDataTm = System.currentTimeMillis();
        receivedData = input.getTotalLength();

        // System.out.format("Bandwidth: %.2f KB/seg.\n", (double)(receivedData
        // * 1.0) / (double)(replyDataTm - tini));

        return res;
    }

    /** Returns the JPIP channel ID */
    public String getJpipChannelID() {
        return jpipChannelID;
    }

    /**
     * Returns the JPIP path.
     */
    public String getJpipPath() {
        return jpipPath;
    }

    /**
     * Returns the time when received the last reply text
     */
    public long getReplyTextTime() {
        return replyTextTm;
    }

    /**
     * Returns the time when received the last reply data
     */
    public long getReplyDataTime() {
        return replyDataTm;
    }

    /**
     * Returns the amount of data (bytes) of the last response.
     */
    public int getReceivedData() {
        return receivedData;
    }

}
