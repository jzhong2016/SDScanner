package com.jzhong.sdscanner;

import android.os.AsyncTask;

import java.io.File;
import java.lang.ref.WeakReference;
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

    public interface FileScanListener {
        public void onScanStateChanged(State state);
    }

    enum State {
        NotStart,
        Started,
        Paused,
        Finished
    }

    private State state;
    private PriorityQueue<FileItem> mostFrequentFiles = new PriorityQueue<>();
    private List<FileItem> listDirectory = new LinkedList<>();
    private List<WeakReference<FileScanListener>> fileScannerListeners = new LinkedList<>();
    private Map<String, Integer> extFrequency = new HashMap<>();
    private AsyncTask<Boolean, Void, Void> taskScan;

    public FileScanner() {
        state = State.NotStart;
        reset();
    }

    public State getState() {
        return state;
    }

    public void startScan(boolean forceRestart) {
        if(taskScan != null) {
            return;
        }
        taskScan = new AsyncTask<Boolean, Void, Void>() {
            @Override
            protected Void doInBackground(Boolean... params) {
                boolean isContinueScan = params[0];
                if(!isContinueScan) {
                    reset();
                }

                while(listDirectory.size() != 0) {
                    if (isCancelled()) {
                        break;
                    }
                    FileItem fileItem = listDirectory.get(0);
                    File dir = new File(fileItem.fileFullPath);
                    File[] files = dir.listFiles();
                    for(File file: files) {
                        if(file.isDirectory()) {
                            FileItem d = FileItem.buildDirectory(file.getPath());
                            listDirectory.add(d);
                        } else {
                            FileItem f = FileItem.buildFile(file.getPath(), file.getName(), file.length());
                            mostFrequentFiles.offer(f);
                            if(mostFrequentFiles.size() > 10) {
                                mostFrequentFiles.poll();
                            }
                            String ext = null;
                            int i = file.getName().lastIndexOf(".");
                            if(i >= 0) {
                                ext = file.getName().substring(i+1);
                            }
                            if(ext.length() > 0) {
                                if(extFrequency.containsKey(ext)) {
                                    extFrequency.put(ext, extFrequency.get(ext) + 1);
                                } else {
                                    extFrequency.put(ext, 1);
                                }
                            }
                        }
                    }
                }
                return null;
            }

            @Override
            protected void onPreExecute() {
                state = State.Started;
                notifiListener();
                //super.onPreExecute();
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                state = State.Finished;
                notifiListener();
                super.onPostExecute(aVoid);
            }

            @Override
            protected void onProgressUpdate(Void... values) {
                state = State.Started;
                notifiListener();
                super.onProgressUpdate(values);
            }
        };

        taskScan.execute();
    }

    public void pauseScan() {
        if(taskScan != null) {
            taskScan.cancel(true);
            taskScan = null;
        }
    }

    private void reset() {
        mostFrequentFiles.clear();
        listDirectory.clear();
        extFrequency.clear();
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


}
