package com.ynemov.blinkarbiter;

import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ResultsFragment extends Fragment {

	private static final String TAG = "com_ynemov_blinkarbiter";
	private static final String DETAILS_ARE_NOT_AVAILABLE = "Details are not available";
	
	private View mView;
	private String mResultsInfo;
	private List<String> mResultsDetails = new ArrayList<String>();

	public ResultsFragment() {
		super();
	}

	public ResultsFragment(String results, List<Long> details) {
		super();
		mResultsInfo = results;
		if(details.size() > 0) {
			for(Long item : details) {
				mResultsDetails.add(item.toString());
			}			
		}
		else {
			mResultsDetails.add(DETAILS_ARE_NOT_AVAILABLE);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mView = inflater.inflate(R.layout.results_frag, container, false);
		((TextView) mView.findViewById(R.id.result_title)).setText(mResultsInfo);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.simple_list_item, mResultsDetails);
		((ListView) mView.findViewById(R.id.result_details)).setAdapter(adapter);

		return mView;
	}
}
