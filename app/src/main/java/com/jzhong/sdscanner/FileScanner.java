package com.jzhong.sdscanner;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Created by busyzhong on 9/10/16.
 */
public class FileScanner {

    private static int MOST_LARGEST_FILE_COUNT = 10;
    private static int MOST_FREQUENT_EXT_COUNT = 5;

    public static class FileItem {
        public String fileName;
        public String fileFullPath;
        public boolean isDirectory;
        public long fileSize;

        public static FileItem buildDirectory(String fileFullPath) {
            FileItem item = new FileItem();
            item.fileFullPath = fileFullPath;
            item.isDirectory = true;
            return item;
        }

        public static FileItem buildFile(String fileFullPath, String fileName, long fileSize) {
            FileItem item = new FileItem();
            item.isDirectory = false;
            item.fileFullPath = fileFullPath;
            item.fileName = fileName;
            item.fileSize = fileSize;
            return item;
        }
    }

    public class FileExtItem {
        String ext;
        int count;
    }

    public interface FileScanListener {
        public void onScanStateChanged(State state);
        public void onScanInProgress(int totalScanFileCount);
    }

    enum State {
        NotStart,
        Started,
        Paused,
        Finished
    }

    private State state;
    private PriorityQueue<FileItem> mostLargestFileQueue;
    private List<FileItem> listDirectory = new LinkedList<>();
    private List<WeakReference<FileScanListener>> fileScannerListeners = new LinkedList<>();
    private Map<String, Integer> extFrequency = new HashMap<>();
    private AsyncTask<Boolean, Integer, Boolean> taskScan;
    private List<FileItem> mostLargestFiles = new ArrayList<>();
    private List<FileExtItem> mostFrequentExts = new ArrayList<>();
    private long averageFileSize;
    private int totalScanFileCount;

    public FileScanner() {
        state = State.NotStart;
        mostLargestFileQueue = new PriorityQueue<>(10, new Comparator<FileItem>() {
            @Override
            public int compare(FileItem lhs, FileItem rhs) {
                return (int) (lhs.fileSize - rhs.fileSize);
            }
        });
        reset();
    }

    public State getState() {
        return state;
    }

    public List<FileItem> getMostLargestFiles() {
        return mostLargestFiles;
    }

    public List<FileExtItem> getMostFrequentExts() {
        return mostFrequentExts;
    }

    public long getAverageFileSize() {
        return averageFileSize;
    }

    public void addFileScanListener(FileScanListener l) {
        fileScannerListeners.add(new WeakReference<FileScanListener>(l));
    }

    public void removeFileScanListener(FileScanListener l) {
        Iterator<WeakReference<FileScanListener>> iterator = fileScannerListeners.iterator();
        while (iterator.hasNext()) {
            WeakReference<FileScanListener> r = iterator.next();
            FileScanListener listener = r.get();
            if(listener == l) {
                iterator.remove();
                break;
            }
        }
    }

    public void startScan(boolean forceRestart) {
        if(taskScan != null) {
            return;
        }
        taskScan = new AsyncTask<Boolean, Integer, Boolean>() {
            @Override
            protected Boolean doInBackground(Boolean... params) {
                boolean isForceRestart = params[0];
                if(isForceRestart) {
                    reset();
                }
                boolean isCancelled = false;
                while(listDirectory.size() != 0) {
                    if (isCancelled()) {
                        isCancelled = true;
                        break;
                    }
                    FileItem fileItem = listDirectory.get(0);
                    listDirectory.remove(0);
                    File dir = new File(fileItem.fileFullPath);
                    File[] files = dir.listFiles();
                    for(File file: files) {
                        if(file.isDirectory()) {
                            FileItem d = FileItem.buildDirectory(file.getPath());
                            listDirectory.add(d);
                        } else {
                            totalScanFileCount++;
                            FileItem f = FileItem.buildFile(file.getPath(), file.getName(), file.length());
                            mostLargestFileQueue.offer(f);
                            if(mostLargestFileQueue.size() > MOST_LARGEST_FILE_COUNT) {
                                mostLargestFileQueue.poll();
                            }
                            String ext = null;
                            int i = file.getName().lastIndexOf(".");
                            if(i >= 0) {
                                ext = file.getName().substring(i).toLowerCase();
                            }
                            if(ext != null && ext.length() > 0) {
                                if(extFrequency.containsKey(ext)) {
                                    extFrequency.put(ext, extFrequency.get(ext) + 1);
                                } else {
                                    extFrequency.put(ext, 1);
                                }
                            }
                            averageFileSize =(long) (f.fileSize/totalScanFileCount + ((totalScanFileCount - 1) * 1.0/totalScanFileCount) * totalScanFileCount);
                        }
                    }
                    this.publishProgress(totalScanFileCount);
                }
                return isCancelled;
            }

            @Override
            protected void onPreExecute() {
                state = State.Started;
                notifiListener();
                //super.onPreExecute();
            }

            @Override
            protected void onCancelled() {
                state = State.Paused;
                notifiListener();
                taskScan = null;
                super.onCancelled();
            }

            @Override
            protected void onPostExecute(Boolean cancelled) {
                if(isCancelled()) {
                    state = State.Paused;
                } else {
                    state = State.Finished;
                    buildResultOnScannDone();
                }
                notifiListener();
                taskScan = null;
                super.onPostExecute(cancelled);
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                notifiListenerInProgress(values[0]);
                super.onProgressUpdate(values);
            }
        };

        taskScan.execute(forceRestart);
    }

    public void pauseScan() {
        if(taskScan != null) {
            taskScan.cancel(true);
            taskScan = null;
        }
    }

    private void buildResultOnScannDone() {

        mostLargestFiles.clear();
        mostLargestFiles.addAll(mostLargestFileQueue);
        mostLargestFileQueue.clear();
        mostFrequentExts.clear();
        PriorityQueue<FileExtItem> q = new PriorityQueue<>(MOST_FREQUENT_EXT_COUNT, new Comparator<FileExtItem>() {
            @Override
            public int compare(FileExtItem lhs, FileExtItem rhs) {
                return (lhs.count - rhs.count);
            }
        });
        for(Map.Entry<String, Integer> e: extFrequency.entrySet()) {
            FileExtItem item = new FileExtItem();
            item.count = e.getValue();
            item.ext = e.getKey();
            q.offer(item);
            if(q.size() > MOST_FREQUENT_EXT_COUNT) {
                q.poll();
            }
        }
        mostFrequentExts.addAll(q);
    }

    private void reset() {
        mostLargestFiles.clear();
        listDirectory.clear();
        extFrequency.clear();
        averageFileSize = 0;
        totalScanFileCount = 0;
        mostFrequentExts.clear();
        mostLargestFileQueue.clear();
        //add SD card root directory
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state) ) {
            listDirectory.add(FileItem.buildDirectory(Environment.getExternalStorageDirectory().getPath()));
        }

    }

    private void notifiListener() {
        Iterator<WeakReference<FileScanListener>> iterator = fileScannerListeners.iterator();
        while (iterator.hasNext()) {
            WeakReference<FileScanListener> r = iterator.next();
            FileScanListener l = r.get();
            if(l == null) {
                iterator.remove();
            } else {
                l.onScanStateChanged(state);
            }
        }
    }

    private void notifiListenerInProgress(int totalScan) {
        Iterator<WeakReference<FileScanListener>> iterator = fileScannerListeners.iterator();
        while (iterator.hasNext()) {
            WeakReference<FileScanListener> r = iterator.next();
            FileScanListener l = r.get();
            if(l == null) {
                iterator.remove();
            } else {
                l.onScanInProgress(totalScan);
            }
        }
    }

}
