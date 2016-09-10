package com.jzhong.sdscanner;

import android.os.Bundle;
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
    protected List<FileScanner.FileItem> mostFrequentFile = new ArrayList<>();

    FileScanner.FileScanListener fileScanListener = new FileScanner.FileScanListener() {
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
        fileScanner = ScannerApplication.application.getFileScanner();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //setup UI
        contentView = inflater.inflate(R.layout.fragment_main, container, false);
        textInfo = (TextView) contentView.findViewById(R.id.textInfo);
        buttonScan = (Button) contentView.findViewById(R.id.buttonScan);
        progressBar = (ProgressBar) contentView.findViewById(R.id.progressBar);

        //setup RecyclerView
        recyclerView = (RecyclerView) contentView.findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new MyAdapter();
        recyclerView.setAdapter(adapter);

        //add File scan listener
        fileScanner.addFileScanListener(fileScanListener);
        //modify UI by scanner state
        modifyUIByScannerState(fileScanner.getState());

        return contentView;
    }

    @Override
    public void onDestroyView() {
        fileScanner.removeFileScanListener(fileScanListener);
        super.onDestroyView();
    }

    protected void modifyUIByScannerState(FileScanner.State state) {
        switch (state) {
            case NotStart:
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
                progressBar.setVisibility(View.VISIBLE);
                textInfo.setText(String.format(getString(R.string.scan_in_progress), 0));
                buttonScan.setText(R.string.stop);
                buttonScan.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fileScanner.pauseScan();
                    }
                });
                break;
            case Paused:
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
        mostFrequentFile.clear();
        mostFrequentFile.addAll(fileScanner.getMostLargestFiles());
        adapter.notifyDataSetChanged();
    }

    //RecyclerView Adapter

    protected class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView textName;
            public TextView textSize;

            public ViewHolder(View v) {
                super(v);
                textName = (TextView) v.findViewById(R.id.textName);
                textSize = (TextView) v.findViewById(R.id.textSize);
            }
        }


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_file, parent, false);
            // set the view's size, margins, paddings and layout parameters
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            FileScanner.FileItem item = mostFrequentFile.get(position);
            holder.textName.setText(item.fileName);
            holder.textSize.setText(formatFileSize(item.fileSize));
        }

        @Override
        public int getItemCount() {
            return mostFrequentFile.size();
        }
    }


    protected String formatFileSize(long size) {
        if(size <= 0) return "0 B";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

}
