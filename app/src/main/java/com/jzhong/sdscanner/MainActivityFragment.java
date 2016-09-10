package com.jzhong.sdscanner;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    FileScanner fileScanner;
    View contentView;
    TextView textInfo;
    Button buttonScan;
    ProgressBar progressBar;
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
        contentView = inflater.inflate(R.layout.fragment_main, container, false);
        textInfo = (TextView) contentView.findViewById(R.id.textInfo);
        buttonScan = (Button) contentView.findViewById(R.id.buttonScan);
        progressBar = (ProgressBar) contentView.findViewById(R.id.progressBar);
        modifyUIByScannerState(fileScanner.getState());
        fileScanner.addFileScanListener(fileScanListener);

        return contentView;
    }

    @Override
    public void onDestroyView() {
        fileScanner.removeFileScanListener(fileScanListener);
        super.onDestroyView();
    }

    void modifyUIByScannerState(FileScanner.State state) {
        switch (state) {
            case NotStart:
                progressBar.setVisibility(View.INVISIBLE);
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
                progressBar.setVisibility(View.INVISIBLE);
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
                progressBar.setVisibility(View.INVISIBLE);
                textInfo.setText(getString(R.string.scan_completed) + " " + getString(R.string.restart_to_scan));
                buttonScan.setText(R.string.scan);
                buttonScan.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fileScanner.startScan(true);
                    }
                });
                break;
        }
    }
}
