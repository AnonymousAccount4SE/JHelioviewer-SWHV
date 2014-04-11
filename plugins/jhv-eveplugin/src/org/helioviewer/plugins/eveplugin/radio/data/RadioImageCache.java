package org.helioviewer.plugins.eveplugin.radio.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;

public class RadioImageCache {
	private Map<Long,DownloadedJPXData> dataCache;
	private Map<Long,Long> useCache;
	private Map<Date, DownloadedJPXData> startDates;
	
	
	private List<RadioImageCacheListener> listeners;  
	
	private static RadioImageCache instance;
	
	private RadioImageCache(){
		this.dataCache = new HashMap<Long, DownloadedJPXData>();
		this.useCache = new HashMap<Long,Long>();
		this.startDates = new HashMap<Date, DownloadedJPXData>();
		this.listeners = new ArrayList<RadioImageCacheListener>();
		
	}
	
	public void addRadioImageCacheListener(RadioImageCacheListener listener){
		this.listeners.add(listener);
	}
	
	public void removeRadioImageListener(RadioImageCacheListener listener){
		this.listeners.remove(listener);
	}
	
	public static RadioImageCache getInstance(){
		if(instance==null){
			instance = new RadioImageCache();
		}
		return instance;		
	}
	
	public void add(DownloadedJPXData jpxData){
		Log.debug("Try to add data in cache");
		synchronized (instance) {
			Log.debug("Could add data to cache");
			dataCache.put(jpxData.getImageID(), jpxData);
			useCache.put(jpxData.getImageID(), 0L);
			startDates.put(jpxData.getStartDate(), jpxData);
		}
	}
	
	public void remove(DownloadedJPXData jpxData) {
		remove(jpxData.getImageID());
	}
	
	public void remove(Long ID){		
		synchronized (instance) {
			DownloadedJPXData data = dataCache.get(ID);
			dataCache.remove(ID);
			useCache.remove(ID);
			startDates.remove(data.getStartDate());
		}
	}
		
	private Date findStartDate(Date start, Long stepsize){
		long divider = 1L;
		if(stepsize < 1000L){
			divider = 1000L;
		}else if (stepsize < 60L * 1000){
			divider = 60L * 1000;
		}else if (stepsize < 60L * 60 * 1000){
			divider = 60L * 60 * 1000;
		}else {
			divider = 24L * 60 * 60 * 1000;
		}
		return new Date(start.getTime() - start.getTime() % divider);
	}
	
	public RadioImageCacheResult getRadioImageCacheResultForInterval(Date start, Date end, Long stepsize){
		Date localStart = findStartDate(start, stepsize);
		List<Interval<Date>> intervalList = new ArrayList<Interval<Date>>();
		List<DownloadedJPXData> dataList = new ArrayList<DownloadedJPXData>();
		List<Long> toRemove = new ArrayList<Long>(dataCache.keySet());
		/*Log.debug("IDs in cache: ");
		for (Long temp : dataCache.keySet()){
			Log.debug(temp);
		}
		Log.debug("Values to remove before");
		for (Long temp : toRemove){
			Log.debug(temp);
		}*/
		while (localStart.before(end) || localStart.equals(end)){
			//Log.debug("Execute loop");
			if (!startDates.containsKey(localStart)){
				intervalList.add(new Interval<Date>(localStart, new Date(localStart.getTime()+stepsize)));
			}else {
				dataList.add(startDates.get(localStart));
				toRemove.remove(startDates.get(localStart).getImageID());
			}
			localStart = new Date(localStart.getTime()+stepsize);
		}
		/*Log.debug("Values to remove after");
		for (Long temp : toRemove){
			Log.debug(temp);
		}
		Log.debug("IDs in cache: ");
		for (Long temp : dataCache.keySet()){
			Log.debug(temp);
		}*/
		return new RadioImageCacheResult(dataList, intervalList, new ArrayList<Long>(toRemove));
	}
}
