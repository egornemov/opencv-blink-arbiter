package com.ynemov.blinkarbiter;

import java.util.List;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ResultsFragment extends Fragment {

	//	private TextView mResults;
	LayoutInflater mInflater;
	View mView;
	String mResultsInfo;

	public ResultsFragment() {
		super();
	}
	
	public ResultsFragment(String string) {
		super();
		mResultsInfo = string;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mInflater = inflater;
		mView = inflater.inflate(R.layout.results_frag, container, false);
		((TextView) mView.findViewById(R.id.result_title)).setText(mResultsInfo);
		return mView;
	}
}
