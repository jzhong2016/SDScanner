package com.jzhong.sdscanner;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    protected FileScanner fileScanner;
    protected View contentView;
    protected TextView textInfo;
    protected Button buttonScan;
    protected ProgressBar progressBar;
    protected RecyclerView recyclerView;
    protected MyAdapter adapter;
    protected List<Object> displayList = new ArrayList<>();
    protected FloatingActionButton fabShare;


    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            FileScanner.LocalBinder binder = (FileScanner.LocalBinder) service;
            fileScanner = binder.getService();
            fileScanner.addFileScanListener(fileScanListener);
            modifyUIByScannerState(fileScanner.getState());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            fileScanner.removeFileScanListener(fileScanListener);
            fileScanner = null;
        }
    };

    private FileScanner.FileScanListener fileScanListener = new FileScanner.FileScanListener() {
        @Override
        public void onScanStateChanged(FileScanner.State state) {
            modifyUIByScannerState(state);
        }

        @Override
        public void onScanInProgress(int totalScanFileCount) {
            textInfo.setText(String.format(getString(R.string.scan_in_progress), totalScanFileCount));
        }
    };

    public MainActivityFragment() {

    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getContext(), FileScanner.class);
        getContext().startService(intent);
        getContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        getContext().unbindService(mConnection);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //setup UI
        contentView = inflater.inflate(R.layout.fragment_main, container, false);
        textInfo = (TextView) contentView.findViewById(R.id.textInfo);
        buttonScan = (Button) contentView.findViewById(R.id.buttonScan);
        progressBar = (ProgressBar) contentView.findViewById(R.id.progressBar);
        fabShare = (FloatingActionButton) contentView.findViewById(R.id.fabShare);
        fabShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        //setup RecyclerView
        recyclerView = (RecyclerView) contentView.findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new MyAdapter();
        recyclerView.setAdapter(adapter);

        //add File scan listener

        //modify UI by scanner state
        if(fileScanner!=null) {
            modifyUIByScannerState(fileScanner.getState());
        }

        return contentView;
    }

    @Override
    public void onDestroyView() {
        fileScanner.removeFileScanListener(fileScanListener);
        contentView = null;
        super.onDestroyView();
    }

    public void onBackPressed() {
        fileScanner.pauseScan();
    }

    protected void modifyUIByScannerState(FileScanner.State state) {
        if(contentView == null) {
            return;
        }
        switch (state) {
            case NotStart:
                fabShare.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                textInfo.setText(R.string.start_to_scan);
                buttonScan.setText(R.string.scan);
                buttonScan.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fileScanner.startScan(false);
                    }
                });
                break;
            case Started:
                fabShare.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                textInfo.setText(String.format(getString(R.string.scan_in_progress), 0));
                buttonScan.setText(R.string.stop);
                buttonScan.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        displayList.clear();
                        adapter.notifyDataSetChanged();
                        fileScanner.pauseScan();
                    }
                });
                break;
            case Paused:
                fabShare.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                textInfo.setText(R.string.scan_paused);
                buttonScan.setText(R.string.resume);
                buttonScan.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fileScanner.startScan(false);
                    }
                });
                break;
            case Finished:
                fabShare.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                textInfo.setText(getString(R.string.scan_completed) + " " + getString(R.string.restart_to_scan));
                buttonScan.setText(R.string.scan);
                buttonScan.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fileScanner.startScan(true);
                    }
                });
                buildFileList();
                break;
        }
    }

    protected void buildFileList() {
        displayList.clear();
        //Add average file size
        displayList.add(getString(R.string.average_file_size) + " " + formatFileSize(fileScanner.getAverageFileSize()));
        //add top 10 biggest file item
        List<FileScanner.FileItem> mostFrequentFile = fileScanner.getMostLargestFiles();
        displayList.add(getString(R.string.top_10_biggest_files));
        Collections.sort(mostFrequentFile, new Comparator<FileScanner.FileItem>() {
            @Override
            public int compare(FileScanner.FileItem lhs, FileScanner.FileItem rhs) {
                return (int) (rhs.fileSize - lhs.fileSize);
            }
        });
        displayList.addAll(mostFrequentFile);
        //add most frequent file extension
        List<FileScanner.FileExtItem> mostFrequentExts = fileScanner.getMostFrequentExts();
        displayList.add(getString(R.string.most_frequent_file_ext));
        Collections.sort(mostFrequentExts, new Comparator<FileScanner.FileExtItem>() {
            @Override
            public int compare(FileScanner.FileExtItem lhs, FileScanner.FileExtItem rhs) {
                return (int) (rhs.count - lhs.count);
            }
        });
        displayList.addAll(mostFrequentExts);
        adapter.notifyDataSetChanged();
    }

    //RecyclerView Adapter

    protected class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        final int VIEW_TYPE_TITLE = 0;
        final int VIEW_TYPE_FILE = 1;
        final int VIEW_TYPE_EXT  = 2;
        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView textName;
            public TextView textSize;

            public ViewHolder(View v) {
                super(v);
                textName = (TextView) v.findViewById(R.id.textName);
                textSize = (TextView) v.findViewById(R.id.textSize);
            }
        }

        public int getItemViewType(int position) {
            Object item = displayList.get(position);
            if(item instanceof FileScanner.FileExtItem) {
                return VIEW_TYPE_EXT;
            } else if(item instanceof FileScanner.FileItem) {
                return VIEW_TYPE_FILE;
            } else {
                return VIEW_TYPE_TITLE;
            }

        }


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            switch (viewType) {
                case VIEW_TYPE_FILE: {
                    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_file, parent, false);
                    ViewHolder vh = new ViewHolder(v);
                    return vh;
                }
                case VIEW_TYPE_EXT: {
                    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_ext, parent, false);
                    ViewHolder vh = new ViewHolder(v);
                    return vh;
                }
                default:{
                    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_title, parent, false);
                    ViewHolder vh = new ViewHolder(v);
                    return vh;
                }
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Object item = displayList.get(position);
            if(item instanceof FileScanner.FileExtItem) {
                FileScanner.FileExtItem fileExtItem = (FileScanner.FileExtItem) item;
                holder.textName.setText(fileExtItem.ext);
                holder.textSize.setText(String.format("%d", fileExtItem.count));
            } else if(item instanceof FileScanner.FileItem) {
                FileScanner.FileItem fileItem = (FileScanner.FileItem) item;
                holder.textName.setText(fileItem.fileName);
                holder.textSize.setText(formatFileSize(fileItem.fileSize));
            } else {
                String title = (String) item;
                holder.textName.setText(title);
            }

        }

        @Override
        public int getItemCount() {
            return displayList.size();
        }
    }


    protected String formatFileSize(long size) {
        if(size <= 0) return "0 B";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }



}
