package com.jzhong.sdscanner;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.lang.ref.WeakReference;
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
        public void onScanInProgress(int totalScanFileCount);
    }

    enum State {
        NotStart,
        Started,
        Paused,
        Finished
    }

    private State state;
    private PriorityQueue<FileItem> mostFrequentFiles;
    private List<FileItem> listDirectory = new LinkedList<>();
    private List<WeakReference<FileScanListener>> fileScannerListeners = new LinkedList<>();
    private Map<String, Integer> extFrequency = new HashMap<>();
    private AsyncTask<Boolean, Integer, Boolean> taskScan;
    private int totalScanFileCount;

    public FileScanner() {
        state = State.NotStart;
        mostFrequentFiles = new PriorityQueue<>(10, new Comparator<FileItem>() {
            @Override
            public int compare(FileItem lhs, FileItem rhs) {
                return (int) (rhs.fileSize - lhs.fileSize);
            }
        });
        reset();
    }

    public State getState() {
        return state;
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
                        totalScanFileCount++;
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
                            if(ext != null && ext.length() > 0) {
                                if(extFrequency.containsKey(ext)) {
                                    extFrequency.put(ext, extFrequency.get(ext) + 1);
                                } else {
                                    extFrequency.put(ext, 1);
                                }
                            }
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

    private void reset() {
        mostFrequentFiles.clear();
        listDirectory.clear();
        extFrequency.clear();
        totalScanFileCount = 0;
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
