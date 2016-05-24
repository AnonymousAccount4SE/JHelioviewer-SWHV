package org.helioviewer.jhv.viewmodel.view.jp2view.kakadu;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;

import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus;
import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus.CacheStatus;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPDataSegment;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPDatabinClass;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPResponse;

/**
 * @author caplins
 * @author Juan Pablo
 */
public class JHV_Kdu_cache extends Kdu_cache {

    /**
     * Returns whether or not the databin is complete.
     * 
     * @param _binClass
     * @param _streamID
     * @param _binID
     * @return True, if the databin is complete, false otherwise
     * @throws JHV_KduException
     */
    public boolean isDataBinCompleted(JPIPDatabinClass _binClass, int _streamID, int _binID) throws JHV_KduException {
        boolean complete[] = new boolean[1];
        try {
            Get_databin_length(_binClass.getKakaduClassID(), _streamID, _binID, complete);
        } catch (KduException ex) {
            throw new JHV_KduException("Internal Kakadu error: " + ex.getMessage());
        }
        return complete[0];
    }

    /**
     * Adds a JPIPResponse to the cache object using the addDataSegment methods.
     * 
     * @param jRes
     * @return True, the response is complete
     * @throws Exception
     */
    public boolean addJPIPResponseData(JPIPResponse jRes, ImageCacheStatus status) throws JHV_KduException {
        JPIPDataSegment data;
        while ((data = jRes.removeJpipDataSegment()) != null && !data.isEOR)
            addDataSegment(data, status);
        return jRes.isResponseComplete();
    }

    /**
     * Adds a JPIPDataSegment to the cache object. Updates the newData variable.
     * 
     * @param _data
     * @throws JHV_KduException
     */
    private void addDataSegment(JPIPDataSegment _data, ImageCacheStatus status) throws JHV_KduException {
        try {
            Add_to_databin(_data.classID.getKakaduClassID(), _data.codestreamID, _data.binID, _data.data, _data.offset, _data.length, _data.isFinal, true, false);
        } catch (KduException ex) {
            throw new JHV_KduException("Internal Kakadu error: " + ex.getMessage());
        }

        int compositionLayer = (int) _data.codestreamID;
        if (compositionLayer >= 0) {
            if (_data.classID.getKakaduClassID() == KakaduConstants.KDU_PRECINCT_DATABIN && status.getImageStatus(compositionLayer) == CacheStatus.HEADER)
                status.setImageStatus(compositionLayer, CacheStatus.PARTIAL);
            else if (_data.isFinal && _data.classID.getKakaduClassID() == KakaduConstants.KDU_MAIN_HEADER_DATABIN)
                status.setImageStatus(compositionLayer, CacheStatus.HEADER);
        }
    }

}
